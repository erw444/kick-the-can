package com.erw.kickthecan.calendar

import android.content.Context
import com.erw.kickthecan.data.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Two-way sync between [EventRepository] and Google Calendar:
 * pushes local deletes and dirty events, then pulls remote deletions/time changes.
 */
class CalendarSync(context: Context) {

    sealed interface Result {
        data object Success : Result
        data object NeedsAuth : Result
        data class Failed(val message: String) : Result
    }

    private val appContext = context.applicationContext
    private val repository = EventRepository(appContext)
    private val api = CalendarApi()

    suspend fun sync(): Result = mutex.withLock {
        val auth = try {
            CalendarAuth.authorize(appContext)
        } catch (e: Exception) {
            return Result.Failed(e.message ?: "Authorization failed")
        }
        val token = auth.accessToken
        if (auth.hasResolution() || token == null) return Result.NeedsAuth

        // Not cancellable: cancelling between an insert and markSynced would create duplicates.
        withContext(NonCancellable + Dispatchers.IO) {
            try {
                push(token)
                pull(token)
                Result.Success
            } catch (e: CalendarApi.AuthException) {
                Result.NeedsAuth
            } catch (e: IOException) {
                Result.Failed(e.message ?: "Network error")
            }
        }
    }

    private fun push(token: String) {
        repository.pendingDeletes().forEach {
            api.delete(token, it)
            repository.clearPendingDelete(it)
        }
        repository.all().filter { it.dirty }.forEach { event ->
            val existing = event.googleEventId
            val googleId = if (existing == null) {
                api.insert(token, event.title, event.startMillis, event.endMillis, event.id)
            } else if (api.move(token, existing, event.startMillis, event.endMillis)) {
                existing
            } else {
                // Removed in Calendar meanwhile; recreate it with the new time.
                api.insert(token, event.title, event.startMillis, event.endMillis, event.id)
            }
            repository.markSynced(event.id, googleId, event.version)
        }
    }

    private fun pull(token: String) {
        repository.all().filter { !it.dirty && it.googleEventId != null }.forEach { event ->
            val remote = api.get(token, event.googleEventId!!)
            if (remote == null) {
                repository.removeIfClean(event.id)
            } else if (remote.startMillis != null && remote.endMillis != null &&
                (remote.startMillis != event.startMillis || remote.endMillis != event.endMillis)
            ) {
                repository.applyRemoteTimes(event.id, remote.startMillis, remote.endMillis)
            }
        }
    }

    private companion object {
        val mutex = Mutex()
    }
}
