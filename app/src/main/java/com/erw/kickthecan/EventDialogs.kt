package com.erw.kickthecan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.appcompat.app.AlertDialog
import com.erw.kickthecan.data.EventRepository
import com.erw.kickthecan.data.TrackedEvent
import java.util.Calendar

/** Kick/delete dialogs shared by the event lists. [onChanged] runs after the repository is modified. */
class EventDialogs(
    private val context: Context,
    private val repository: EventRepository,
    private val onChanged: () -> Unit,
) {

    fun showKickOptions(event: TrackedEvent) {
        val labels = arrayOf(
            context.getString(R.string.kick_next_day),
            context.getString(R.string.kick_next_week),
            context.getString(R.string.kick_next_month),
            context.getString(R.string.kick_custom),
        )
        AlertDialog.Builder(context)
            .setTitle(R.string.kick_the_can)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> applyKick(event, shiftedStart(event, Calendar.DAY_OF_YEAR, 1))
                    1 -> applyKick(event, shiftedStart(event, Calendar.WEEK_OF_YEAR, 1))
                    2 -> applyKick(event, shiftedStart(event, Calendar.MONTH, 1))
                    else -> pickCustomDateTime(event)
                }
            }
            .show()
    }

    fun confirmDelete(event: TrackedEvent) {
        AlertDialog.Builder(context)
            .setTitle(R.string.delete)
            .setMessage(context.getString(R.string.delete_confirm, event.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                repository.delete(event.id)
                onChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyKick(event: TrackedEvent, newStart: Long) {
        repository.kick(event.id, newStart)
        onChanged()
    }

    private fun pickCustomDateTime(event: TrackedEvent) {
        val initial = Calendar.getInstance().apply {
            timeInMillis = shiftedStart(event, Calendar.DAY_OF_YEAR, 1)
        }
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, R.style.SpinnerTimePickerDialog, { _, hour, minute ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                applyKick(event, chosen.timeInMillis)
            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(context)).show()
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH))
            .apply { datePicker.minDate = System.currentTimeMillis() - 1000 }
            .show()
    }

    // Same time of day, one unit of `field` from today.
    private fun shiftedStart(event: TrackedEvent, field: Int, amount: Int): Long {
        val original = Calendar.getInstance().apply { timeInMillis = event.startMillis }
        return Calendar.getInstance().apply {
            add(field, amount)
            set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, original.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
