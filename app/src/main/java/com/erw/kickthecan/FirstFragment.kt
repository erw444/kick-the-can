package com.erw.kickthecan

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.lifecycleScope
import com.erw.kickthecan.calendar.CalendarAuth
import com.erw.kickthecan.calendar.CalendarSync
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.erw.kickthecan.data.EventRepository
import com.erw.kickthecan.data.TrackedEvent
import com.erw.kickthecan.databinding.FragmentFirstBinding
import java.util.Calendar

/** Home screen: lists tracked events that have already passed. */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: EventRepository
    private lateinit var calendarSync: CalendarSync

    private val consentLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val granted = result.resultCode == Activity.RESULT_OK && runCatching {
                Identity.getAuthorizationClient(requireContext())
                    .getAuthorizationResultFromIntent(result.data)
            }.isSuccess
            if (granted) {
                CalendarAuth.setConnected(requireContext(), true)
                syncNow()
            } else {
                Toast.makeText(requireContext(), R.string.connect_cancelled, Toast.LENGTH_SHORT).show()
            }
            updateBanner()
        }
    private val adapter = MissedEventAdapter(onKick = ::showKickOptions, onDelete = ::confirmDelete)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = EventRepository(requireContext())
        calendarSync = CalendarSync(requireContext())
        binding.connectButton.setOnClickListener { connect() }
        binding.missedList.layoutManager = LinearLayoutManager(requireContext())
        binding.missedList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
        updateBanner()
        syncNow()
    }

    private fun updateBanner() {
        binding.connectBanner.visibility =
            if (CalendarAuth.isConnected(requireContext())) View.GONE else View.VISIBLE
    }

    private fun connect() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val auth = CalendarAuth.authorize(requireContext())
                val pendingIntent = auth.pendingIntent
                if (auth.hasResolution() && pendingIntent != null) {
                    consentLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                } else {
                    CalendarAuth.setConnected(requireContext(), true)
                    updateBanner()
                    syncNow()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.sync_failed, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    // Pushes local changes and pulls remote ones; failed pushes stay dirty and retry next time.
    private fun syncNow() {
        if (!CalendarAuth.isConnected(requireContext())) return
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = calendarSync.sync()) {
                CalendarSync.Result.Success -> refresh()
                CalendarSync.Result.NeedsAuth -> {
                    CalendarAuth.setConnected(requireContext(), false)
                    updateBanner()
                }
                is CalendarSync.Result.Failed ->
                    Toast.makeText(requireContext(), getString(R.string.sync_failed, result.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refresh() {
        val missed = repository.missed()
        adapter.submitList(missed)
        binding.emptyText.visibility = if (missed.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showKickOptions(event: TrackedEvent) {
        val labels = arrayOf(
            getString(R.string.kick_tomorrow),
            getString(R.string.kick_three_days),
            getString(R.string.kick_next_week),
        )
        val days = intArrayOf(1, 3, 7)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.kick_the_can)
            .setItems(labels) { _, which ->
                repository.kick(event.id, shiftedStart(event, days[which]))
                refresh()
                syncNow()
            }
            .show()
    }

    // Same time of day, N days from today.
    private fun shiftedStart(event: TrackedEvent, daysFromToday: Int): Long {
        val original = Calendar.getInstance().apply { timeInMillis = event.startMillis }
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromToday)
            set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, original.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun confirmDelete(event: TrackedEvent) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_confirm, event.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                repository.delete(event.id)
                refresh()
                syncNow()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
