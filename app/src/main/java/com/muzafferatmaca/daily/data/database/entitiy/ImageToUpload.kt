package com.muzafferatmaca.daily.data.database.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.muzafferatmaca.daily.util.Constants.IMAGE_TO_UPLOAD_TABLE

/**
 * Created by Muzaffer Atmaca on 21.03.2024 at 15:41
 */
@Entity(tableName = IMAGE_TO_UPLOAD_TABLE)
data class ImageToUpload(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val remoteImagePath: String,
    val imageUri: String,
    val sessionUri: String
)