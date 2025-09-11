package com.dajakov.daedaluslink

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConnectConfig)

    @Query("SELECT * FROM connect_config")
    fun getAllConfigs(): Flow<List<ConnectConfig>>

    @Delete
    suspend fun deleteConfig(config: ConnectConfig)
}