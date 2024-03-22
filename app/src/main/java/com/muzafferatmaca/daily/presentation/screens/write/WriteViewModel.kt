package com.muzafferatmaca.daily.presentation.screens.write

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.muzafferatmaca.daily.data.database.ImageToDeleteDao
import com.muzafferatmaca.daily.data.database.ImageUploadDao
import com.muzafferatmaca.daily.data.database.entitiy.ImageToDelete
import com.muzafferatmaca.daily.data.database.entitiy.ImageToUpload
import com.muzafferatmaca.daily.data.repository.MongoDB
import com.muzafferatmaca.daily.model.Daily
import com.muzafferatmaca.daily.model.GalleryImage
import com.muzafferatmaca.daily.model.GalleryState
import com.muzafferatmaca.daily.model.Mood
import com.muzafferatmaca.daily.util.Constants
import com.muzafferatmaca.daily.model.RequestState
import com.muzafferatmaca.daily.util.fetchImagesFromFirebase
import com.muzafferatmaca.daily.util.toRealmInstant
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Created by Muzaffer Atmaca on 19.03.2024 at 14:50
 */
@HiltViewModel
class WriteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageUploadDao: ImageUploadDao,
    private val imageToDeleteDao: ImageToDeleteDao,
) : ViewModel() {
    val galleryState = GalleryState()
    var uiState by mutableStateOf(UiState())
        private set

    init {
        getDailyIdArgument()
        fetchSelectedDaily()
    }

    private fun getDailyIdArgument() {
        uiState = uiState.copy(
            selectedDailyId = savedStateHandle.get<String>(
                key = Constants.WRITE_SCREEN_ARGUMENT_KEY
            )
        )
    }

    private fun fetchSelectedDaily() {
        if (uiState.selectedDailyId != null) {
            viewModelScope.launch(Dispatchers.Main) {
                MongoDB.getSelectedDaily(dailyId = ObjectId.invoke(uiState.selectedDailyId!!))
                    .catch {
                        emit(RequestState.Error(Exception("Daily is already deleted")))
                    }
                    .collect { daily ->
                        if (daily is RequestState.Success) {
                            setSelectedDaily(daily = daily.data)
                            setTitle(daily.data.title)
                            setDescription(daily.data.description)
                            setMood(Mood.valueOf(daily.data.mood))

                            fetchImagesFromFirebase(
                                remoteImagePaths = daily.data.images,
                                onImageDownload = { downloadedImage ->
                                    galleryState.addImage(
                                        GalleryImage(
                                            image = downloadedImage,
                                            remoteImagePath = extractImagePath(
                                                fullImageUrl = downloadedImage.toString()
                                            ),
                                        )
                                    )
                                }
                            )
                        }
                    }
            }
        }
    }

    private fun setSelectedDaily(daily: Daily) {
        uiState = uiState.copy(selectedDaily = daily)
    }

    fun setTitle(title: String) {
        uiState = uiState.copy(title = title)
    }

    fun setDescription(description: String) {
        uiState = uiState.copy(description = description)
    }

    private fun setMood(mood: Mood) {
        uiState = uiState.copy(mood = mood)
    }

    fun updateDateTime(zonedDateTime: ZonedDateTime) {
        uiState = uiState.copy(updatedDateTime = zonedDateTime.toInstant().toRealmInstant())
    }

    fun upsertDaily(
        daily: Daily,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.selectedDailyId != null) {
                updateDaily(daily, onSuccess, onError)
            } else {
                insertDaily(daily, onSuccess, onError)
            }
        }
    }

    private suspend fun insertDaily(
        daily: Daily,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val result = MongoDB.insertDaily(daily.apply {
            if (uiState.updatedDateTime != null) {
                date = uiState.updatedDateTime!!
            }
        })
        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else if (result is RequestState.Error) {
            withContext(Dispatchers.Main) {
                onError(result.error.message.toString())
            }
        }
    }

    private suspend fun updateDaily(
        daily: Daily,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val result = MongoDB.updateDaily(daily.apply {
            _id = ObjectId.invoke(uiState.selectedDailyId!!)
            date = if (uiState.updatedDateTime != null) {
                uiState.updatedDateTime!!
            } else {
                uiState.selectedDaily!!.date
            }
        })
        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            deleteImagesFromFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else if (result is RequestState.Error) {
            withContext(Dispatchers.Main) {
                onError(result.error.message.toString())
            }
        }
    }

    fun deleteDaily(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.selectedDailyId != null) {
                val result = MongoDB.deleteDaily(id = ObjectId.invoke(uiState.selectedDailyId!!))
                if (result is RequestState.Success) {
                    uiState.selectedDaily?.let { deleteImagesFromFirebase(images = it.images) }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else if (result is RequestState.Error) {
                    withContext(Dispatchers.Main) {
                        onError(result.error.message.toString())
                    }
                }
            }
        }
    }

    fun addImage(image: Uri, imageType: String) {
        val remoteImagePath = "images/${FirebaseAuth.getInstance().currentUser?.uid}/" +
                "${image.lastPathSegment}-${System.currentTimeMillis()}.$imageType"
        galleryState.addImage(
            GalleryImage(
                image = image,
                remoteImagePath = remoteImagePath
            )
        )
    }

    private fun uploadImagesToFirebase() {
        val storage = FirebaseStorage.getInstance().reference
        galleryState.images.forEach { galleryImage ->
            val imagePath = storage.child(galleryImage.remoteImagePath)
            imagePath.putFile(galleryImage.image)
                .addOnProgressListener {
                    val sessionUri = it.uploadSessionUri
                    if (sessionUri != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            imageUploadDao.addImageToUpload(
                                ImageToUpload(
                                    remoteImagePath = galleryImage.remoteImagePath,
                                    imageUri = galleryImage.image.toString(),
                                    sessionUri = sessionUri.toString()
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun extractImagePath(fullImageUrl: String): String {
        val chunks = fullImageUrl.split("%2F")
        val imageName = chunks[2].split("?").first()
        return "images/${Firebase.auth.currentUser?.uid}/$imageName"
    }

    private fun deleteImagesFromFirebase(images: List<String>? = null) {
        val storage = FirebaseStorage.getInstance().reference
        if (images != null) {
            images.forEach { remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener {
                        viewModelScope.launch(Dispatchers.IO) {
                            imageToDeleteDao.addImageToDelete(
                                ImageToDelete(
                                    remoteImagePath = remotePath
                                )
                            )
                        }
                    }
            }
        } else {
            galleryState.imagesToBeDeleted.map { it.remoteImagePath }.forEach {remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener {
                        viewModelScope.launch(Dispatchers.IO) {
                            imageToDeleteDao.addImageToDelete(
                                ImageToDelete(
                                    remoteImagePath = remotePath
                                )
                            )
                        }
                    }
            }
        }
    }


}

data class UiState(
    val selectedDailyId: String? = null,
    val selectedDaily: Daily? = null,
    val title: String = "",
    val description: String = "",
    val mood: Mood = Mood.Neutral,
    val updatedDateTime: RealmInstant? = null
)