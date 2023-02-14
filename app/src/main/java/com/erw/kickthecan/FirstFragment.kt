package com.erw.kickthecan

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erw.kickthecan.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var calendarService: CalendarService
    private lateinit var calendarItemAdapter: CalendarItemAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        calendarService = CalendarService()
        calendarItemAdapter = CalendarItemAdapter()

        // Setup permission request launcher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                if (it == true) {
                    val calendars = calendarService.getCalendars(this.requireActivity())
                    calendarItemAdapter.pushData(calendars)
                } else {
                    Toast.makeText(
                        this.requireContext(),
                        "Please allow this app to access your calendar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.recycler_list_view).let {
            it.layoutManager = LinearLayoutManager(activity)
            it.adapter = calendarItemAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}