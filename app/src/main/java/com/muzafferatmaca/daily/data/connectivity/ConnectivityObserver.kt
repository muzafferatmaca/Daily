package com.muzafferatmaca.daily.data.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Created by Muzaffer Atmaca on 21.03.2024 at 23:02
 */
interface ConnectivityObserver {

    fun observe(): Flow<Status>

    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}