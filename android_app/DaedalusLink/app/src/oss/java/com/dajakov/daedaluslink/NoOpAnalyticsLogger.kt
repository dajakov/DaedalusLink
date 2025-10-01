package com.dajakov.daedaluslink

class NoOpAnalyticsLogger : AnalyticsLogger {

    override fun logEvent(name: String, params: Map<String, Any?>?) {
        // No-operation: This logger does nothing for the OSS flavor.
    }
}
