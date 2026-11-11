package com.erw.kickthecan.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

object CalendarAccess {

    val PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

    fun hasPermission(context: Context) = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Picks a writable calendar, preferring an account's own (primary) calendar. Null if none. */
    fun findCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val args = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, selection, args,
            "${CalendarContract.Calendars._ID} ASC"
        )?.use { c ->
            var fallback: Long? = null
            while (c.moveToNext()) {
                val id = c.getLong(0)
                if (c.getString(1) == c.getString(2)) return id
                if (fallback == null) fallback = id
            }
            return fallback
        }
        return null
    }
}
