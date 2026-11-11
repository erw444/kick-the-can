package com.erw.kickthecan.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.io.IOException
import java.util.TimeZone

/** Reads and writes events through the on-device Calendar Provider. */
class CalendarStore(context: Context) {

    class NoCalendarException : IOException("No writable calendar found on this device")

    /** Times are null when the provider has no explicit end (e.g. recurring events). */
    data class StoredEvent(val startMillis: Long, val endMillis: Long?)

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    /** Returns the new event's provider id. */
    fun insert(title: String, startMillis: Long, endMillis: Long): String {
        val calendarId = CalendarAccess.findCalendarId(appContext) ?: throw NoCalendarException()
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, TITLE_TAG + title)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: throw IOException("Calendar provider rejected the event")
        return ContentUris.parseId(uri).toString()
    }

    /** Returns false if the event no longer exists. */
    fun move(eventId: String, startMillis: Long, endMillis: Long): Boolean {
        val uri = eventUri(eventId) ?: return false
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
        }
        // The provider forbids a selection on item URIs, so check for deletion separately.
        if (get(eventId) == null) return false
        return resolver.update(uri, values, null, null) > 0
    }

    fun delete(eventId: String) {
        val uri = eventUri(eventId) ?: return
        resolver.delete(uri, null, null)
    }

    /** Returns null if the event was deleted. */
    fun get(eventId: String): StoredEvent? {
        val uri = eventUri(eventId) ?: return null
        val projection = arrayOf(CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND)
        resolver.query(uri, projection, NOT_DELETED, null, null)?.use { c ->
            if (!c.moveToFirst()) return null
            return StoredEvent(c.getLong(0), if (c.isNull(1)) null else c.getLong(1))
        }
        return null
    }

    // Ids saved by older versions (Google API ids) aren't numeric; treat those as missing.
    private fun eventUri(eventId: String) =
        eventId.toLongOrNull()?.let { ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, it) }

    private companion object {
        const val TITLE_TAG = "[KtC] "
        const val NOT_DELETED = "${CalendarContract.Events.DELETED} = 0"
    }
}
