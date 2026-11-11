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
 * Two-way sync between [EventRepository] and the device calendar:
 * pushes local deletes and dirty events, then pulls remote deletions/time changes.
 */
class CalendarSync(context: Context) {

    sealed interface Result {
        data object Success : Result
        data object NeedsPermission : Result
        data class Failed(val message: String) : Result
    }

    private val appContext = context.applicationContext
    private val repository = EventRepository(appContext)
    private val store = CalendarStore(appContext)

    suspend fun sync(): Result = mutex.withLock {
        if (!CalendarAccess.hasPermission(appContext)) return Result.NeedsPermission

        // Not cancellable: cancelling between an insert and markSynced would create duplicates.
        withContext(NonCancellable + Dispatchers.IO) {
            try {
                push()
                pull()
                Result.Success
            } catch (e: SecurityException) {
                Result.NeedsPermission
            } catch (e: IOException) {
                Result.Failed(e.message ?: "Calendar error")
            }
        }
    }

    private fun push() {
        repository.pendingDeletes().forEach {
            store.delete(it)
            repository.clearPendingDelete(it)
        }
        repository.all().filter { it.dirty }.forEach { event ->
            val existing = event.googleEventId
            val googleId = if (existing == null) {
                store.insert(event.title, event.startMillis, event.endMillis)
            } else if (store.move(existing, event.startMillis, event.endMillis)) {
                existing
            } else {
                // Removed from the calendar meanwhile; recreate it with the new time.
                store.insert(event.title, event.startMillis, event.endMillis)
            }
            repository.markSynced(event.id, googleId, event.version)
        }
    }

    private fun pull() {
        repository.all().filter { !it.dirty && it.googleEventId != null }.forEach { event ->
            val remote = store.get(event.googleEventId!!)
            if (remote == null) {
                repository.removeIfClean(event.id)
            } else if (remote.endMillis != null &&
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
