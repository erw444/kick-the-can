package com.erw.kickthecan.data

data class TrackedEvent(
    val id: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val googleEventId: String? = null,
    // True when local changes still need to be pushed to the device calendar.
    val dirty: Boolean = true,
    // Bumped on every local change so a sync can tell if the event changed while it was in flight.
    val version: Int = 0,
)
