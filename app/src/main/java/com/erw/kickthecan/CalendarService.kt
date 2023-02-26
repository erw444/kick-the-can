package com.erw.kickthecan

import android.app.Activity
import android.content.Context
import android.provider.CalendarContract
import com.erw.kickthecan.data.EventCan
import com.erw.kickthecan.data.MyCalendar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object CalendarService {

    var chosenMyCalendar: MyCalendar? = MyCalendar()

    private val CALENDAR_PROJECTION = arrayOf(
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

    private val EVENT_PROJECTION = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.DTSTART
    )
    private const val EVENT_PROJECTION_ID_INDEX = 0
    private const val EVENT_PROJECTION_DISPLAY_NAME_INDEX = 1
    private const val EVENT_PROJECTION_DESCRIPTION_INDEX = 2
    private const val EVENT_PROJECTION_START_DATE_INDEX = 3



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
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME}"
        val selectionArgs = emptyArray<String>()
        val cur = context.contentResolver.query(
            uri,
            CALENDAR_PROJECTION,
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

            val myCalendar = MyCalendar(
                    id = calId,
                    name = name,
                    displayName = displayName,
                    color = color,
                    visible = visible == 1,
                    syncEvents = syncEvents == 1,
                    accountName = accountName,
                    accountType = accountType,
            )

            myCalendar.accountName?.let { myCalendars.put(it, myCalendar) }

        }
        cur?.close()

        return myCalendars
    }

    fun collectCans(context: Context) : List<EventCan>{
        val uri = CalendarContract.Events.CONTENT_URI
        val selection = "${CalendarContract.Events.TITLE} LIKE '%${MainActivity.KICK_THE_CAN_EVENT_TAG}%'"
        val selectionArgs = emptyArray<String>()
        val cur = context.contentResolver.query(
            uri,
            EVENT_PROJECTION,
            selection, selectionArgs,
            null,
        )
        val cans = ArrayList<EventCan>()
        while (cur?.moveToNext() == true) {
            val eventId = cur.getLong(EVENT_PROJECTION_ID_INDEX)
            val displayName = cur.getString(EVENT_PROJECTION_DISPLAY_NAME_INDEX)
            val description = cur.getString(EVENT_PROJECTION_DESCRIPTION_INDEX)
            val eventDateMillis = cur.getLong(EVENT_PROJECTION_START_DATE_INDEX)

            val eventDateFormat = "MM-dd-yyyy"
            val formatter = SimpleDateFormat(eventDateFormat)

            // Create a calendar object that will convert the date and time value in milliseconds to date.

            // Create a calendar object that will convert the date and time value in milliseconds to date.
            val calendar: Calendar = Calendar.getInstance()
            calendar.setTimeInMillis(eventDateMillis)


            val eventCan = EventCan(
                id = eventId,
                name = displayName,
                description = description,
                date = formatter.format(calendar.time)
            )

            cans.add(eventCan)

        }
        cur?.close()

        return cans
    }
}