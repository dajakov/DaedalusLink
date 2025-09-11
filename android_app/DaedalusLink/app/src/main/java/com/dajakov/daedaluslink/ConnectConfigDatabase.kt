package com.dajakov.daedaluslink

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

        fun resetDatabase(context: Context) {
            // Delete the old database
            context.deleteDatabase("connect_config_db")

            // Now Room will recreate the database with the new schema
            INSTANCE = null // Clear the instance so that it will be recreated
            getDatabase(context) // Recreate the database instance with the new schema
        }
    }
}
