package com.example.daedaluslink

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ConnectConfig::class], version = 1, exportSchema = false)
abstract class ConnectConfigDatabase : RoomDatabase() {
    abstract fun connectConfigDao(): ConnectConfigDao

    companion object {
        @Volatile
        private var INSTANCE: ConnectConfigDatabase? = null

        fun getDatabase(context: Context): ConnectConfigDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConnectConfigDatabase::class.java,
                    "connect_config_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
