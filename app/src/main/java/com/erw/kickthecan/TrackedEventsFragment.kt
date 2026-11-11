package com.erw.kickthecan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.erw.kickthecan.calendar.CalendarAccess
import com.erw.kickthecan.calendar.CalendarSync
import com.erw.kickthecan.data.EventRepository
import com.erw.kickthecan.databinding.FragmentTrackedEventsBinding
import kotlinx.coroutines.launch

/** Lists every tracked event, upcoming and missed. */
class TrackedEventsFragment : Fragment() {

    private var _binding: FragmentTrackedEventsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: EventRepository
    private lateinit var calendarSync: CalendarSync
    private lateinit var adapter: MissedEventAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackedEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = EventRepository(requireContext())
        calendarSync = CalendarSync(requireContext())
        val dialogs = EventDialogs(requireContext(), repository) {
            refresh()
            syncNow()
        }
        adapter = MissedEventAdapter(onKick = dialogs::showKickOptions, onDelete = dialogs::confirmDelete)
        binding.eventList.layoutManager = LinearLayoutManager(requireContext())
        binding.eventList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
        syncNow()
    }

    private fun refresh() {
        val events = repository.all().sortedBy { it.startMillis }
        adapter.submitList(events)
        binding.emptyText.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun syncNow() {
        if (!CalendarAccess.hasPermission(requireContext())) return
        viewLifecycleOwner.lifecycleScope.launch {
            if (calendarSync.sync() == CalendarSync.Result.Success) refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
