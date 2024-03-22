package com.muzafferatmaca.daily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.muzafferatmaca.daily.data.database.ImageToDeleteDao
import com.muzafferatmaca.daily.data.database.ImageUploadDao
import com.muzafferatmaca.daily.data.repository.MongoDB
import com.muzafferatmaca.daily.navigation.Screen
import com.muzafferatmaca.daily.navigation.SetupNavGraph
import com.muzafferatmaca.daily.ui.theme.DailyTheme
import com.muzafferatmaca.daily.ui.theme.DisappointedColor
import com.muzafferatmaca.daily.util.Constants
import com.muzafferatmaca.daily.util.retryDeletingImageFromFirebase
import com.muzafferatmaca.daily.util.retryUploadingImageToFirebase
import dagger.hilt.android.AndroidEntryPoint
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    var keepSplashOpened = true

    @Inject
    lateinit var imageUploadDao: ImageUploadDao

    @Inject
    lateinit var imageToDeleteDao: ImageToDeleteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition {
            keepSplashOpened
        }
        FirebaseApp.initializeApp(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            DailyTheme(dynamicColor = false) {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController,
                    onDataLoaded = {
                        keepSplashOpened = false
                    }
                )
            }
        }

        cleanUpCheck(
            scope = lifecycleScope,
            imageUploadDao = imageUploadDao,
            imageToDeleteDao = imageToDeleteDao
        )
    }
}

private fun cleanUpCheck(
    scope: CoroutineScope,
    imageUploadDao: ImageUploadDao,
    imageToDeleteDao: ImageToDeleteDao

) {
    scope.launch(Dispatchers.IO) {
        val result = imageUploadDao.getAllImages()
        result.forEach { imageToUpload ->
            retryUploadingImageToFirebase(
                imageToUpload = imageToUpload,
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        imageUploadDao.cleanupImage(imageToUpload.id)
                    }
                }
            )
        }

        val result2 = imageToDeleteDao.getAllImages()
        result2.forEach { imageToDelete ->
            retryDeletingImageFromFirebase(
                imageToDelete = imageToDelete,
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        imageToDeleteDao.cleanupImage(imageId = imageToDelete.id)
                    }
                }
            )
        }
    }
}

private fun getStartDestination(): String {
    val user = App.create(Constants.APP_ID).currentUser
    return if (user != null && user.loggedIn) Screen.Home.route
    else Screen.Authentication.route
}
