package com.muzafferatmaca.daily.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muzafferatmaca.daily.data.database.entitiy.ImageToDelete
import com.muzafferatmaca.daily.data.database.entitiy.ImageToUpload

/**
 * Created by Muzaffer Atmaca on 21.03.2024 at 15:46
 */
@Database(
    entities = [ImageToUpload::class,ImageToDelete::class],
    version = 2,
    exportSchema = false
)
abstract class ImagesDatabase  : RoomDatabase(){
    abstract fun imageToUploadDao() : ImageUploadDao
    abstract fun imageToDeleteDao() : ImageToDeleteDao
}