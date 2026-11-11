package com.erw.kickthecan.calendar

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

object CalendarAuth {

    private const val CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events"
    private const val PREFS_NAME = "kick_the_can_auth"
    private const val KEY_CONNECTED = "connected"

    /**
     * Returns a result with an access token if consent was already granted, or with a pending
     * intent (hasResolution) that the UI must launch to ask the user.
     */
    suspend fun authorize(context: Context): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(CALENDAR_EVENTS_SCOPE)))
            .build()
        return Identity.getAuthorizationClient(context).authorize(request).await()
    }

    fun isConnected(context: Context) = prefs(context).getBoolean(KEY_CONNECTED, false)

    fun setConnected(context: Context, connected: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONNECTED, connected).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
