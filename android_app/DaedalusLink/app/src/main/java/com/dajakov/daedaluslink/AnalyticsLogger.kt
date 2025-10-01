package com.dajakov.daedaluslink

interface AnalyticsLogger {
    fun logEvent(name: String, params: Map<String, Any?>? = null)
}
