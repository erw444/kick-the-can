package com.erw.kickthecan.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when an event ends, and after boot (alarms don't survive a reboot). */
class MissedEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MissedNotifier.check(context)
    }
}
