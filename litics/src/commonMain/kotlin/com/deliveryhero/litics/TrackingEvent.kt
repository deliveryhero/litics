package com.deliveryhero.litics

import kotlin.js.JsExport

@JsExport
public data class TrackingEvent(
    val eventName: String,
    val parameters: Array<Parameter>,
) {

    public data class Parameter(val key: String, val value: String)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TrackingEvent

        if (eventName != other.eventName) return false
        if (!parameters.contentEquals(other.parameters)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventName.hashCode()
        result = 31 * result + parameters.contentHashCode()
        return result
    }
}
