package com.erw.kickthecan.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.erw.kickthecan.MainActivity
import com.erw.kickthecan.R
import com.erw.kickthecan.data.EventRepository
import com.erw.kickthecan.data.TrackedEvent

/** Posts a notification when a tracked event passes its start time, and keeps a single alarm set for the next one. */
object MissedNotifier {

    private const val CHANNEL_ID = "missed_events"
    private const val PREFS = "kick_the_can_notified"
    private const val KEY_IDS = "ids"
    private val lock = Any()

    /** Notifies for newly missed events, then re-arms the alarm. Called by the alarm/boot receiver. */
    fun check(context: Context) {
        val app = context.applicationContext
        val events = EventRepository(app).all()
        synchronized(lock) {
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val notified = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
            val now = System.currentTimeMillis()
            val fresh = events.filter { it.startMillis < now && it.id !in notified }
            fresh.forEach { notify(app, it) }
            prefs.edit().putStringSet(KEY_IDS, notified + fresh.map { it.id }).apply()
        }
        reschedule(app, events)
    }

    /** Dismisses notifications for events that are no longer missed and arms the next alarm. */
    fun reschedule(context: Context, events: List<TrackedEvent>) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val notified = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
            val missedIds = events.filter { it.startMillis < now }.map { it.id }.toSet()
            val stale = notified - missedIds
            if (stale.isNotEmpty()) {
                stale.forEach { NotificationManagerCompat.from(app).cancel(it.hashCode()) }
                prefs.edit().putStringSet(KEY_IDS, notified - stale).apply()
            }
        }

        val alarms = app.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            app, 0, Intent(app, MissedEventReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next = events.map { it.startMillis }.filter { it >= now }.minOrNull()
        if (next == null) {
            alarms.cancel(pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next + 1000, pending)
        }
    }

    private fun notify(context: Context, event: TrackedEvent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, context.getString(R.string.missed_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.missed_notification_title))
            .setContentText(context.getString(R.string.missed_notification_text, event.title))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(event.id.hashCode(), notification)
    }
}
