package com.dajakov.daedaluslink

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel

class DebugViewModel : ViewModel() {

    private val maxPoints = 100

//    private val _debugData = mutableStateMapOf<String, SnapshotStateList<Point>>()
//    val debugData: Map<String, List<Point>> = _debugData

    // New: Track the x-value index for each key
    private val xIndexMap = mutableMapOf<String, Float>()

    fun addDataPoint(key: String, value: Float) {
//        val oldList = _debugData[key] ?: mutableStateListOf()
//        val newList = oldList.toMutableList() // Copy the old list to avoid concurrent modification
//
//        val nextX = xIndexMap.getOrDefault(key, 0f)
//        newList.add(Point(nextX, value))
//        xIndexMap[key] = nextX + 1f
//
//        if (newList.size > maxPoints) {
//            newList.removeAt(0)
//        }
//
//        _debugData[key] = mutableStateListOf<Point>().apply { addAll(newList) } // Replace the whole list
    }


    fun clear() {
//        _debugData.clear()
//        xIndexMap.clear()
    }
}

