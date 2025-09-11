package com.dajakov.daedaluslink

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connect_config")
data class ConnectConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val connectionType: String,
    val address: String,
    val heartbeatFrequency: Int,
    val iconId: String
)
