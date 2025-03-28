package com.example.daedaluslink

import android.content.Context
import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LinkConfig::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LinkConfigDatabase : RoomDatabase() {
    abstract fun linkConfigDao(): LinkConfigDao

    companion object {
        @Volatile
        private var INSTANCE: LinkConfigDatabase? = null

        fun getDatabase(context: Context): LinkConfigDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LinkConfigDatabase::class.java,
                    "link_config_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
