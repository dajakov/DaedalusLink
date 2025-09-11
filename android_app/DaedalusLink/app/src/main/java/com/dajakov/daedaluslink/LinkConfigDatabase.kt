package com.dajakov.daedaluslink

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.json.Json

@Database(entities = [LinkConfig::class], version = 6, exportSchema = false)
@TypeConverters(InterfaceDataConverter::class)
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
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Create a type converter for Room to handle the List<InterfaceData>
class InterfaceDataConverter {
    @TypeConverter
    fun fromInterfaceDataList(value: List<InterfaceData>): String {
        return json.encodeToString(value) // Convert list to JSON string
    }

    @TypeConverter
    fun toInterfaceDataList(value: String): List<InterfaceData> {
        return json.decodeFromString(value) // Convert JSON string back to list
    }
}

// To use `@JsonIgnoreUnknownKeys` during JSON deserialization, ensure that you use `kotlinx.serialization.Json` with the appropriate configuration.
val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// Example usage for deserialization:
fun parseLinkConfig(jsonString: String): LinkConfig {
    return json.decodeFromString(jsonString)
}