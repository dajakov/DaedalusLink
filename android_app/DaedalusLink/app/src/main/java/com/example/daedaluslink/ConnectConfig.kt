package com.example.daedaluslink

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

//@Entity(tableName = "link_config")
//data class LinkConfig(
//    @PrimaryKey(autoGenerate = true) val id: Int = 0,
//    val connectionType: String,
//    val address: String,
//    val heartbeatFrequency: Int
//)