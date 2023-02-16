package com.erw.kickthecan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.erw.kickthecan.databinding.FragmentPickCalendarBinding


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class PickCalendarFragment : Fragment() {

    private var _binding: FragmentPickCalendarBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var calendarNameToCalendar: HashMap<String, MyCalendar>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPickCalendarBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarNameToCalendar = CalendarService.getCalendars(this.requireActivity())

        val dropdown: Spinner = view.findViewById(R.id.spinner1)

        val calendars = calendarNameToCalendar.keys.toTypedArray()
        dropdown.setOnItemSelectedListener( this );
        val spinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(view.context, android.R.layout.simple_spinner_item,
                calendars)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdown.adapter = spinnerAdapter

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_PickCalendarFragment_to_HomeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}