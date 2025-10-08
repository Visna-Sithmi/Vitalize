package com.example.vitalize.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vitalize.R

class MoodsCalendarAdapter(private val moods: List<MoodEntry>) :
    RecyclerView.Adapter<MoodsCalendarAdapter.CalendarViewHolder>() {

    private val daysInMonth = 31

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = position + 1
        val moodEntry = moods.find { it.date.endsWith("-%02d".format(day)) }
        holder.bind(day, moodEntry)

        // 🔹 Animate each calendar cell as it appears
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_scale)
        holder.itemView.startAnimation(animation)
    }

    override fun getItemCount(): Int = daysInMonth

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvDayEmoji)
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)

        fun bind(day: Int, moodEntry: MoodEntry?) {
            // Show emoji if exists, otherwise empty circle
            tvEmoji.text = moodEntry?.emoji ?: "⚪"
            // Always show date number
            tvDayNumber.text = day.toString()
        }
    }
}
