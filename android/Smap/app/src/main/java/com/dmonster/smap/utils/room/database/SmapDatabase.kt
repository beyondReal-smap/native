package com.dmonster.smap.utils.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dmonster.smap.utils.room.dao.StepDataDao
import com.dmonster.smap.utils.room.data.StepData

@Database(entities = [
    StepData::class
], version = 1, exportSchema = false)
abstract class SmapDatabase: RoomDatabase() {
    abstract fun stepDataDao(): StepDataDao

    companion object {
        private var instance: SmapDatabase? = null

        @Synchronized
        fun getInstance(context: Context): SmapDatabase? {
            if (instance == null) {
                synchronized(SmapDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        SmapDatabase::class.java,
                        "smap-database"
                    ).build()
                }
            }
            return instance
        }
    }
}