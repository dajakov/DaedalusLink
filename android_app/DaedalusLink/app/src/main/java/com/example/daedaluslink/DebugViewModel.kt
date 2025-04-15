package com.example.daedaluslink

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import co.yml.charts.common.model.Point

class DebugViewModel : ViewModel() {
    private val _debugData = mutableStateMapOf<String, SnapshotStateList<Point>>()
    val debugData: Map<String, List<Point>> get() = _debugData

    private val maxPoints = 50

    fun addDataPoint(type: String, value: Float) {
        val points = _debugData.getOrPut(type) { mutableStateListOf() }
        val time = if (points.isNotEmpty()) points.last().x + 1 else 0f
        if (points.size >= maxPoints) points.removeAt(0)
        points.add(Point(time, value))
    }
}
