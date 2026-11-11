package com.erw.kickthecan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.erw.kickthecan.data.EventRepository
import com.erw.kickthecan.databinding.FragmentSecondBinding
import java.util.Calendar

/** Add-event screen. */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private val start: Calendar = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateButtons()

        binding.dateButton.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                start.set(y, m, d)
                updateButtons()
            }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.timeButton.setOnClickListener {
            TimePickerDialog(requireContext(), R.style.SpinnerTimePickerDialog, { _, h, min ->
                start.set(Calendar.HOUR_OF_DAY, h)
                start.set(Calendar.MINUTE, min)
                updateButtons()
            }, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), false).show()
        }
        binding.saveButton.setOnClickListener {
            val title = binding.titleInput.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                binding.titleInput.error = getString(R.string.title_required)
                return@setOnClickListener
            }
            val startMillis = start.timeInMillis
            EventRepository(requireContext()).add(title, startMillis, startMillis + DEFAULT_DURATION_MS)
            findNavController().navigateUp()
        }
    }

    private fun updateButtons() {
        binding.dateButton.text = DateUtils.formatDateTime(
            requireContext(), start.timeInMillis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY
        )
        binding.timeButton.text = DateUtils.formatDateTime(
            requireContext(), start.timeInMillis, DateUtils.FORMAT_SHOW_TIME
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DEFAULT_DURATION_MS = 60 * 60 * 1000L
    }
}
