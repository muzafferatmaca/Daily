package com.muzafferatmaca.daily.model

/**
 * Created by Muzaffer Atmaca on 18.03.2024 at 13:49
 */
sealed class RequestState<out T> {
    object Idle : RequestState<Nothing>()
    object Loading : RequestState<Nothing>()
    data class Success<T>(val data : T) : RequestState<T>()
    data class Error(val error : Throwable) : RequestState<Nothing>()
}