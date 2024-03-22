package com.muzafferatmaca.daily.model

import com.muzafferatmaca.daily.util.toRealmInstant
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import java.time.Instant

/**
 * Created by Muzaffer Atmaca on 15.03.2024 at 15:11
 */
open class Daily : RealmObject {
    @PrimaryKey
    var _id : ObjectId = ObjectId.invoke()
    var description : String = ""
    var mood : String = Mood.Neutral.name
    var ownerId : String = ""
    var title : String = ""
    var images : RealmList<String> = realmListOf()
    var date : RealmInstant = Instant.now().toRealmInstant()
}