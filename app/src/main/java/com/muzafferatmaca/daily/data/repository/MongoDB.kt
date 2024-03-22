package com.muzafferatmaca.daily.data.repository

import com.muzafferatmaca.daily.model.Daily
import com.muzafferatmaca.daily.util.Constants
import com.muzafferatmaca.daily.model.RequestState
import com.muzafferatmaca.daily.util.toInstant
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Created by Muzaffer Atmaca on 18.03.2024 at 10:53
 */
object MongoDB : MongoRepository {

    private val app = App.Companion.create(Constants.APP_ID)
    private val user = app.currentUser
    private lateinit var realm: Realm

    init {
        configureTheRealm()
    }

    override fun configureTheRealm() {
        if (user != null) {
            val config = SyncConfiguration.Builder(user, setOf(Daily::class))
                .initialSubscriptions { sub ->
                    add(
                        query = sub.query<Daily>("ownerId == $0", user.id),
                        name = "User's Dailies"
                    )
                }
                .log(LogLevel.ALL)
                .build()

            realm = Realm.open(config)

        }

    }

    override fun getAllDailies(): Flow<Dailies> {
        return if (user != null) {
            try {

                realm.query<Daily>("ownerId == $0", user.id)
                    .sort(property = "date", sortOrder = Sort.DESCENDING)
                    .asFlow()
                    .map { result ->
                        RequestState.Success(
                            data = result.list.groupBy {
                                it.date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                        )
                    }

            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticatedException())) }
        }
    }

    override fun getFilteredDailies(zonedDateTime: ZonedDateTime): Flow<Dailies> {
        return if (user != null) {
            try {
                realm.query<Daily>(
                    "ownerId == $0 AND date < $1 AND date > $2",
                    user.id,
                    RealmInstant.from(
                        LocalDateTime.of(
                            zonedDateTime.toLocalDate().plusDays(1),
                            LocalTime.MIDNIGHT
                        ).toEpochSecond(zonedDateTime.offset), 0
                    ),
                    RealmInstant.from(
                        LocalDateTime.of(
                            zonedDateTime.toLocalDate(),
                            LocalTime.MIDNIGHT
                        ).toEpochSecond(zonedDateTime.offset), 0
                    ),
                ).asFlow().map { result ->
                    RequestState.Success(
                        data = result.list.groupBy {
                            it.date.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                    )
                }
            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticatedException())) }
        }
    }

    override fun getSelectedDaily(dailyId: ObjectId): Flow<RequestState<Daily>> {
        return if (user != null) {
            try {
                realm.query<Daily>(query = "_id == $0", dailyId).asFlow().map {
                    RequestState.Success(data = it.list.first())
                }
            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticatedException())) }
        }
    }

    override suspend fun insertDaily(daily: Daily): RequestState<Daily> {
        return if (user != null) {
            realm.write {
                try {
                    val addedDaily = copyToRealm(daily.apply { ownerId = user.id })
                    RequestState.Success(data = addedDaily)
                } catch (e: Exception) {
                    RequestState.Error(e)
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

    override suspend fun updateDaily(daily: Daily): RequestState<Daily> {
        return if (user != null) {
            realm.write {
                val queriedDaily = query<Daily>(query = "_id == $0", daily._id).first().find()
                if (queriedDaily != null) {
                    queriedDaily.title = daily.title
                    queriedDaily.description = daily.description
                    queriedDaily.mood = daily.mood
                    queriedDaily.images = daily.images
                    queriedDaily.date = daily.date
                    RequestState.Success(data = queriedDaily)
                } else {
                    RequestState.Error(error = Exception("Queried Diary does not exist."))
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

    override suspend fun deleteDaily(id: ObjectId): RequestState<Daily> {
        return if (user != null) {
            realm.write {
                val daily = query<Daily>(query = "_id == $0 AND ownerId == $1", id, user.id)
                    .first().find()
                if (daily != null) {
                    try {
                        delete(daily)
                        RequestState.Success(data = daily)
                    } catch (e: Exception) {
                        RequestState.Error(e)
                    }
                } else {
                    RequestState.Error(Exception("Daily does not exist "))
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

    override suspend fun deleteAllDaily(): RequestState<Boolean> {
        return if (user != null) {
            realm.write {
                val dailies = this.query<Daily>("ownerId == $0", user.id).find()
                try {
                    delete(dailies)
                    RequestState.Success(data = true)
                } catch (e: Exception) {
                    RequestState.Error(e)
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }
}

private class UserNotAuthenticatedException : Exception("User is not Logged in.")