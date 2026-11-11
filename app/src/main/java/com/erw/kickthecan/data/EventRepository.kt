package com.erw.kickthecan.data

import android.content.Context
import com.erw.kickthecan.notify.MissedNotifier
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local store for events created by the app (SharedPreferences, JSON). Local changes are
 * flagged dirty and pushed to the device calendar by [com.erw.kickthecan.calendar.CalendarSync].
 * All access is serialized on [lock] because sync runs on a background thread.
 */
class EventRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun all(): List<TrackedEvent> = synchronized(lock) { load() }

    fun missed(now: Long = System.currentTimeMillis()): List<TrackedEvent> =
        all().filter { it.startMillis < now }.sortedBy { it.startMillis }

    fun add(title: String, startMillis: Long, endMillis: Long): TrackedEvent = synchronized(lock) {
        val event = TrackedEvent(UUID.randomUUID().toString(), title, startMillis, endMillis)
        save(load() + event)
        event
    }

    /** Moves the event to [newStartMillis], keeping its duration. */
    fun kick(id: String, newStartMillis: Long) = update(id) {
        it.copy(
            startMillis = newStartMillis,
            endMillis = newStartMillis + (it.endMillis - it.startMillis),
            dirty = true,
            version = it.version + 1,
        )
    }

    fun delete(id: String) = synchronized(lock) {
        val events = load()
        events.firstOrNull { it.id == id }?.googleEventId?.let {
            prefs.edit().putStringSet(KEY_PENDING_DELETES, pendingDeletes() + it).apply()
        }
        save(events.filterNot { it.id == id })
    }

    fun pendingDeletes(): Set<String> =
        synchronized(lock) { prefs.getStringSet(KEY_PENDING_DELETES, emptySet()).orEmpty().toSet() }

    fun clearPendingDelete(googleEventId: String) = synchronized(lock) {
        prefs.edit().putStringSet(KEY_PENDING_DELETES, pendingDeletes() - googleEventId).apply()
    }

    /** Records the calendar event id; clears the dirty flag only if the event wasn't changed since [version]. */
    fun markSynced(id: String, googleEventId: String, version: Int) = update(id) {
        it.copy(googleEventId = googleEventId, dirty = it.dirty && it.version != version)
    }

    /** Applies a change made in the device calendar, unless there are unsynced local changes. */
    fun applyRemoteTimes(id: String, startMillis: Long, endMillis: Long) = update(id) {
        if (it.dirty) it else it.copy(startMillis = startMillis, endMillis = endMillis)
    }

    /** Drops an event that no longer exists in the device calendar, unless it has unsynced local changes. */
    fun removeIfClean(id: String) = synchronized(lock) {
        save(load().filterNot { it.id == id && !it.dirty })
    }

    private fun update(id: String, transform: (TrackedEvent) -> TrackedEvent) = synchronized(lock) {
        save(load().map { if (it.id == id) transform(it) else it })
    }

    private fun load(): List<TrackedEvent> {
        val json = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        val array = JSONArray(json)
        return List(array.length()) { i ->
            val o = array.getJSONObject(i)
            TrackedEvent(
                id = o.getString("id"),
                title = o.getString("title"),
                startMillis = o.getLong("start"),
                endMillis = o.getLong("end"),
                googleEventId = o.optString("googleEventId").ifEmpty { null },
                dirty = o.optBoolean("dirty", true),
                version = o.optInt("version", 0),
            )
        }
    }

    private fun save(events: List<TrackedEvent>) {
        MissedNotifier.reschedule(appContext, events)
        val array = JSONArray()
        events.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("start", it.startMillis)
                    .put("end", it.endMillis)
                    .put("googleEventId", it.googleEventId ?: "")
                    .put("dirty", it.dirty)
                    .put("version", it.version)
            )
        }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "kick_the_can"
        const val KEY_EVENTS = "events"
        const val KEY_PENDING_DELETES = "pending_deletes"
        val lock = Any()
    }
}
