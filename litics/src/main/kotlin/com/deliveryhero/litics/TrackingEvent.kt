package com.deliveryhero.litics

enum class AnalyticsPlatform {
    FIREBASE, BRAZE
}

data class TrackingEvent(
    val eventName: String,
    val parameters: Map<String, String>? = null,
    val supportedPlatforms: List<AnalyticsPlatform>,
)
