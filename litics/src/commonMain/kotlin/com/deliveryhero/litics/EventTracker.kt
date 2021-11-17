package com.deliveryhero.litics

import kotlin.js.JsExport

@JsExport
public abstract class EventTracker {

    /**
     * @return true if the tracker supports event tracking
     */
    public abstract fun supportsEventTracking(supportedPlatforms: Array<String>): Boolean

    /**
     * Tracks event
     * @param trackingEvent - event to be tracked
     */
    public abstract fun trackEvent(trackingEvent: TrackingEvent)
}
