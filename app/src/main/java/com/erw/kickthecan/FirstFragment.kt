package com.erw.kickthecan

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.erw.kickthecan.calendar.CalendarAccess
import com.erw.kickthecan.calendar.CalendarSync
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.erw.kickthecan.data.EventRepository
import com.erw.kickthecan.databinding.FragmentFirstBinding

/** Home screen: lists tracked events that have already passed. */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: EventRepository
    private lateinit var calendarSync: CalendarSync
    private lateinit var dialogs: EventDialogs

    // Re-evaluates what counts as missed while the screen is open.
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (_binding == null) return
            refresh()
            handler.postDelayed(this, 15_000)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (CalendarAccess.hasPermission(requireContext())) {
                syncNow()
            } else {
                Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
            updateBanner()
        }
    private val adapter = MissedEventAdapter(
        onKick = { dialogs.showKickOptions(it) },
        onDelete = { dialogs.confirmDelete(it) },
    )

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
        dialogs = EventDialogs(requireContext(), repository) {
            refresh()
            syncNow()
        }
        binding.connectButton.setOnClickListener { connect() }
        binding.missedList.layoutManager = LinearLayoutManager(requireContext())
        binding.missedList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
        updateBanner()
        syncNow()
        ticker.run()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun updateBanner() {
        binding.connectBanner.visibility =
            if (CalendarAccess.hasPermission(requireContext())) View.GONE else View.VISIBLE
    }

    private fun connect() = permissionLauncher.launch(CalendarAccess.PERMISSIONS)

    // Pushes local changes and pulls calendar ones; failed pushes stay dirty and retry next time.
    private fun syncNow() {
        if (!CalendarAccess.hasPermission(requireContext())) return
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = calendarSync.sync()) {
                CalendarSync.Result.Success -> refresh()
                CalendarSync.Result.NeedsPermission -> updateBanner()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
