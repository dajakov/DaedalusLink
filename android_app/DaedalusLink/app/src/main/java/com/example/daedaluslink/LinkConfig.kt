package com.example.daedaluslink

import androidx.room.*

@Entity(tableName = "link_config")
data class LinkConfig(
    @PrimaryKey val linkId: Int,
    val name: String,

    val commandUpdateFrequency: Int,
    val sensorUpdateFrequency: Int,
    val debugLogUpdateFrequency: Int,

    val jsonData: String  // Stores JSON representation of `RobotDataContainer`
)

data class RobotDataContainer(
    val commands: List<CommandData>,
    val sensors: List<SensorData>,
    val debugLogs: List<DebugLogData>
)

data class CommandData(
    val command: String, // "move", "stop", etc.
    val x: Int = 0,
    val y: Int = 0
)

data class SensorData(
    val type: String, // "temperature", "battery", "distance"
    val value: Float
)

data class DebugLogData(
    val logLevel: String, // "INFO", "ERROR"
    val message: String
)
