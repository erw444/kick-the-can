package com.erw.kickthecan.fragments

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
import com.erw.kickthecan.CalendarService
import com.erw.kickthecan.EventAdapter
import com.erw.kickthecan.R
import com.erw.kickthecan.databinding.FragmentHomeBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var eventAdapter: EventAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        eventAdapter = EventAdapter()

        // Setup permission request launcher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                if (it == true) {
                    refreshRecyclerView()
                } else {
                    Toast.makeText(
                        this.requireContext(),
                        "Please allow this app to access your calendar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.recycler_list_view).let {
            it.layoutManager = LinearLayoutManager(activity)
            it.adapter = eventAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun refreshRecyclerView(){
        eventAdapter.clearData()
        val cans = CalendarService.collectCans(this.requireActivity())
        eventAdapter.pushData(cans)
    }
}