package com.example.daedaluslink

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinkConfig(linkConfig: LinkConfig)

    @Query("SELECT * FROM link_config")
    fun getAllLinkConfigs(): Flow<List<LinkConfig>>

    @Query("SELECT * FROM link_config WHERE linkId = :linkId")
    suspend fun getLinkConfigById(linkId: Int): LinkConfig?

    @Delete
    suspend fun deleteLinkConfig(linkConfig: LinkConfig)
}