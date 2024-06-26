package com.muzafferatmaca.daily.presentation.screens.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzafferatmaca.daily.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.Credentials
import io.realm.kotlin.mongodb.GoogleAuthType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Created by Muzaffer Atmaca on 14.03.2024 at 14:52
 */
@HiltViewModel
class AuthenticationViewModel @Inject constructor(): ViewModel() {

    var authenticated = mutableStateOf(false)
        private set

    var loadingState = mutableStateOf(false)
        private set

    fun setLoading(loading: Boolean){
        loadingState.value = loading
    }

    fun signInWithMongoAtlas(
        tokenId : String,
        onSuccess:() -> Unit,
        onError :(Exception) -> Unit
    ){
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO){
                    App.create(Constants.APP_ID).login(
                        Credentials.jwt(tokenId)
                        //Credentials.google(tokenId,GoogleAuthType.ID_TOKEN)
                    ).loggedIn
                }
                withContext(Dispatchers.Main){
                   if (result){
                       onSuccess()
                       delay(600)
                       authenticated.value = true
                   }else{
                       onError(Exception("User is not logged in."))
                   }
                }
            }catch (e : Exception){
                withContext(Dispatchers.Main){
                    onError(e)
                }
            }
        }

    }
}