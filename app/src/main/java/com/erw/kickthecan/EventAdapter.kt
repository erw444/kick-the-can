package com.erw.kickthecan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.erw.kickthecan.data.EventCan
import org.w3c.dom.Text

class EventAdapter: RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    var data: MutableList<EventCan> = mutableListOf()

    fun clearData() {
        data.clear()
        notifyDataSetChanged()
    }

    fun pushData(eventCans: List<EventCan>) {
        for(can in eventCans){
            data.add(can)
        }
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDisplayName: TextView = view.findViewById(R.id.event_item_name)
        val textDescription: TextView = view.findViewById(R.id.event_item_description)
        val textDate: TextView = view.findViewById(R.id.event_item_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_event_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = data[position]
        holder.textDisplayName.text = datum.name
        holder.textDescription.text = datum.description
        holder.textDate.text = datum.date
    }

    override fun getItemCount(): Int {
        return data.size
    }
}