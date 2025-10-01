package com.dajakov.daedaluslink

import android.content.Context

class OssAnalyticsProvider : AnalyticsProvider {
    override fun getLogger(): AnalyticsLogger {
        return NoOpAnalyticsLogger()
    }
}

// Helper to get the provider instance (used by common MainActivity)
fun getAnalyticsProvider(context: Context): AnalyticsProvider {
    // Context is not used here but kept for signature consistency
    return OssAnalyticsProvider()
}
