package com.erw.kickthecan

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.erw.kickthecan.data.TrackedEvent
import com.erw.kickthecan.databinding.ItemMissedEventBinding

class MissedEventAdapter(
    private val onKick: (TrackedEvent) -> Unit,
    private val onDelete: (TrackedEvent) -> Unit,
) : ListAdapter<TrackedEvent, MissedEventAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemMissedEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMissedEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position)
        with(holder.binding) {
            title.text = event.title
            whenText.text = DateUtils.formatDateTime(
                root.context, event.startMillis,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY
            )
            kickButton.setOnClickListener { onKick(event) }
            deleteButton.setOnClickListener { onDelete(event) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TrackedEvent>() {
        override fun areItemsTheSame(a: TrackedEvent, b: TrackedEvent) = a.id == b.id
        override fun areContentsTheSame(a: TrackedEvent, b: TrackedEvent) = a == b
    }
}
