package com.deliveryhero.litics

public interface EventTracker {

    /**
     * @return true if the tracker supports event tracking
     */
    public fun supportsEventTracking(supportedPlatforms: List<String>): Boolean

    /**
     * Tracks event
     * @param trackingEvent - event to be tracked
     */
    public fun trackEvent(trackingEvent: TrackingEvent)
}
