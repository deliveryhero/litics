package com.deliveryhero.litics

interface EventTracker {

    /**
     * @return true if the tracker supports event tracking
     */
    fun supportsEventTracking(supportedPlatforms: List<AnalyticsPlatform>): Boolean

    /**
     * Tracks event
     * @param trackingEvent - event to be tracked
     */
    fun trackEvent(trackingEvent: TrackingEvent)
}
