package com.deliveryhero.litics

data class TrackingEvent(
    val eventName: String,
    val parameters: Map<String, String>,
)
