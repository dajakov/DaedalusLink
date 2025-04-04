package com.example.daedaluslink

import androidx.room.*
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "link_config")
data class LinkConfig(
    @PrimaryKey val linkId: Int,
    val name: String,
    val commandUpdateFrequency: Int,
    val sensorUpdateFrequency: Int,
    val debugLogUpdateFrequency: Int,
    @TypeConverters(InterfaceDataConverter::class) val interfaceData: List<InterfaceData>
)

@Serializable
data class InterfaceData(
    val type: String,
    val label: String,
    val position: List<Int>,
    val size: List<Int>,
    val pressCommand: String,
    val releaseCommand: String? = null, // releaseCommand = pressCommand if not defined
    val axes: List<String>? = null  // Optional, since it may not always be present
)
