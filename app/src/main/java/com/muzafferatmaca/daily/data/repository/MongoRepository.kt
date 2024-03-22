package com.muzafferatmaca.daily.data.repository

import com.muzafferatmaca.daily.model.Daily
import com.muzafferatmaca.daily.model.RequestState
import kotlinx.coroutines.flow.Flow
import org.mongodb.kbson.ObjectId
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Created by Muzaffer Atmaca on 18.03.2024 at 10:48
 */

typealias Dailies = RequestState<Map<LocalDate, List<Daily>>>

interface MongoRepository {
    fun configureTheRealm()
    fun getAllDailies() : Flow<Dailies>
    fun getFilteredDailies(zonedDateTime: ZonedDateTime): Flow<Dailies>
    fun getSelectedDaily(dailyId: ObjectId) : Flow<RequestState<Daily>>
    suspend fun insertDaily(daily: Daily) : RequestState<Daily>
    suspend fun updateDaily(daily: Daily) : RequestState<Daily>
    suspend fun deleteDaily(id : ObjectId) : RequestState<Daily>
    suspend fun deleteAllDaily() : RequestState<Boolean>
}