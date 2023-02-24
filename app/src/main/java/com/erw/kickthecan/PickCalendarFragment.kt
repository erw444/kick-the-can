package com.erw.kickthecan

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.erw.kickthecan.databinding.FragmentPickCalendarBinding
import kotlinx.coroutines.NonDisposableHandle.parent
import kotlinx.coroutines.selects.select


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
        val selectedCalendar = CalendarService.chosenMyCalendar


        val dropdown: Spinner = view.findViewById(R.id.spinner1)
        val calendars = calendarNameToCalendar.keys.toTypedArray()

        val dropdownOptions = ArrayList<String>()
        dropdownOptions.add("")
        for(calendar in calendars){
            dropdownOptions.add(calendar)
        }

        val spinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(view.context, android.R.layout.simple_spinner_item,
                dropdownOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdown.adapter = spinnerAdapter

        val selectedCalendarPosition = spinnerAdapter.getPosition(selectedCalendar?.accountName)
        dropdown.setSelection(selectedCalendarPosition)

        binding.spinner1?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCalendar = parent?.getItemAtPosition(position)
                if(selectedCalendar is String){
                    CalendarService.chosenMyCalendar = calendarNameToCalendar[selectedCalendar];
                    val sharedPref = activity?.getSharedPreferences(activity!!.packageName, Context.MODE_PRIVATE) ?: return
                    with (sharedPref.edit()) {
                        putString(getString(R.string.setting_chosen_calendar), selectedCalendar)
                        apply()
                    }
                }
            }

        }

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_PickCalendarFragment_to_HomeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}