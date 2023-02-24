package com.erw.kickthecan

import android.app.Activity
import android.content.Context
import android.provider.CalendarContract


object CalendarService {

    var chosenMyCalendar: MyCalendar? = MyCalendar()

    private val EVENT_PROJECTION = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.NAME,
        CalendarContract.Calendars.CALENDAR_COLOR,
        CalendarContract.Calendars.VISIBLE,
        CalendarContract.Calendars.SYNC_EVENTS,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.ACCOUNT_TYPE,
    )

    private const val PROJECTION_ID_INDEX = 0
    private const val PROJECTION_DISPLAY_NAME_INDEX = 1
    private const val PROJECTION_NAME_INDEX = 2
    private const val PROJECTION_CALENDAR_COLOR_INDEX = 3
    private const val PROJECTION_VISIBLE_INDEX = 4
    private const val PROJECTION_SYNC_EVENTS_INDEX = 5
    private const val PROJECTION_ACCOUNT_NAME_INDEX = 6
    private const val PROJECTION_ACCOUNT_TYPE_INDEX = 7

    private fun CalendarService() {}

    fun init(context: Context) {
        val sharedPref = context.getSharedPreferences(context.packageName, Activity.MODE_PRIVATE)
        val label = context.getString(R.string.setting_chosen_calendar)
        val chosenCalendarName = sharedPref.getString(label, "");
        val calendars = getCalendars(context)

        chosenMyCalendar = calendars[chosenCalendarName]
    }

    fun getCalendars(context: Context) : HashMap<String, MyCalendar> {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val selection = ""
        val selectionArgs = emptyArray<String>()
        val cur = context.contentResolver.query(
            uri,
            EVENT_PROJECTION,
            selection, selectionArgs,
            null,
        )
        val myCalendars = HashMap<String, MyCalendar>()
        while (cur?.moveToNext() == true) {
            val calId = cur.getLong(PROJECTION_ID_INDEX)
            val displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
            val name = cur.getString(PROJECTION_NAME_INDEX)
            val color = cur.getInt(PROJECTION_CALENDAR_COLOR_INDEX)
            val visible = cur.getInt(PROJECTION_VISIBLE_INDEX)
            val syncEvents = cur.getInt(PROJECTION_SYNC_EVENTS_INDEX)
            val accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
            val accountType = cur.getString(PROJECTION_ACCOUNT_TYPE_INDEX)

            val myCalendarItem = MyCalendar(
                    id = calId,
                    name = name,
                    displayName = displayName,
                    color = color,
                    visible = visible == 1,
                    syncEvents = syncEvents == 1,
                    accountName = accountName,
                    accountType = accountType,
            )

            myCalendarItem.accountName?.let { myCalendars.put(it, myCalendarItem) }

        }
        cur?.close()

        return myCalendars
    }
}