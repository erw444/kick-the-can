package com.erw.kickthecan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter: RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    var data: MutableList<MyCalendar> = mutableListOf()

    fun clearData() {
        data.clear()
        notifyDataSetChanged()
    }

    fun pushData(myCalendars: ArrayList<MyCalendar>) {
        for(calendar in myCalendars){
            data.add(calendar)
        }
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textDisplayName: TextView = view.findViewById(R.id.list_item_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val datum = data[position]
        holder.textDisplayName.text = datum.displayName
    }

    override fun getItemCount(): Int {
        return data.size
    }
}