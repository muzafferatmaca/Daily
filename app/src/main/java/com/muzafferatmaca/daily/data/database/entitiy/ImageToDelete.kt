package com.muzafferatmaca.daily.data.database.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.muzafferatmaca.daily.util.Constants

/**
 * Created by Muzaffer Atmaca on 21.03.2024 at 18:36
 */
@Entity(tableName = Constants.IMAGE_TO_DELETE_TABLE)
data class ImageToDelete(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    val remoteImagePath : String
)