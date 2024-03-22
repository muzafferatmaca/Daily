package com.muzafferatmaca.daily.presentation.screens.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.muzafferatmaca.daily.data.connectivity.ConnectivityObserver
import com.muzafferatmaca.daily.data.connectivity.NetworkConnectivityObserver
import com.muzafferatmaca.daily.data.database.ImageToDeleteDao
import com.muzafferatmaca.daily.data.database.entitiy.ImageToDelete
import com.muzafferatmaca.daily.data.repository.Dailies
import com.muzafferatmaca.daily.data.repository.MongoDB
import com.muzafferatmaca.daily.model.RequestState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Created by Muzaffer Atmaca on 18.03.2024 at 14:24
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectivity : NetworkConnectivityObserver,
    private val imageToDeleteDao: ImageToDeleteDao
): ViewModel() {
    private lateinit var allDiariesJob: Job
    private lateinit var filteredDiariesJob: Job
    private var network by mutableStateOf(ConnectivityObserver.Status.Unavailable)
    var dailies: MutableState<Dailies> = mutableStateOf(RequestState.Idle)
    var dateIsSelected by mutableStateOf(false)
        private set

    init {
        observeAllDailies()
        getDailies()
        viewModelScope.launch {
            connectivity.observe().collect{
                network = it
            }
        }
    }

    fun getDailies(zonedDateTime: ZonedDateTime? = null){
        dateIsSelected = zonedDateTime != null
        dailies.value = RequestState.Loading
        if (dateIsSelected && zonedDateTime != null) {
            observeFilteredDailies(zonedDateTime = zonedDateTime)
        } else {
            observeAllDailies()
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeAllDailies() {

        allDiariesJob = viewModelScope.launch {
            if (::filteredDiariesJob.isInitialized) {
                filteredDiariesJob.cancelAndJoin()
            }
            MongoDB.getAllDailies().debounce(2000).collect { result ->
                dailies.value = result
            }
        }
    }

    private fun observeFilteredDailies(zonedDateTime: ZonedDateTime) {
        filteredDiariesJob = viewModelScope.launch {
            if (::allDiariesJob.isInitialized) {
                allDiariesJob.cancelAndJoin()
            }
            MongoDB.getFilteredDailies(zonedDateTime = zonedDateTime).collect { result ->
                dailies.value = result
            }
        }
    }

    fun deleteAllDailies(
        onSuccess: () -> Unit,
        onError : (Throwable) -> Unit,
    ){
        if (network == ConnectivityObserver.Status.Available){
            var userId = FirebaseAuth.getInstance().currentUser?.uid
            val imagesDirectory = "images/${userId}"
            val storage = FirebaseStorage.getInstance().reference
            storage.child(imagesDirectory)
                .listAll()
                .addOnSuccessListener {
                    it.items.forEach { ref ->
                        val imagePath = "images/${userId}/${ref.name}"
                        storage.child(imagePath).delete()
                            .addOnFailureListener {
                                viewModelScope.launch(Dispatchers.IO){
                                    imageToDeleteDao.addImageToDelete(
                                        ImageToDelete(
                                            remoteImagePath = imagePath
                                        )
                                    )
                                }
                            }
                    }
                    viewModelScope.launch (Dispatchers.IO){
                        val result = MongoDB.deleteAllDaily()
                        if (result is RequestState.Success){
                            withContext(Dispatchers.Main){
                                onSuccess()
                            }
                        }else if (result is RequestState.Error){
                            withContext(Dispatchers.Main){
                                onError(result.error)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    onError(it)
                }
        }else{
         onError(Exception("No Internet Connection"))
        }
    }

}