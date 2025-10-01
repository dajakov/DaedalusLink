package com.dajakov.daedaluslink

// This interface will be implemented by flavor-specific providers
interface AnalyticsProvider {
    fun getLogger(): AnalyticsLogger
}
