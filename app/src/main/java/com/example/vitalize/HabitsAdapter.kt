package com.example.vitalize.ui.main

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vitalize.R

class HabitsAdapter(
    private val habits: MutableList<Habit>,
    private val onEdit: (Habit) -> Unit,
    private val onDelete: (Habit) -> Unit,
    private val onSave: () -> Unit // callback to save when status changes
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    inner class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvHabitName)
        val time: TextView = view.findViewById(R.id.tvHabitTime)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val row: View = view.findViewById(R.id.habitRow) //  background container
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]

        // Name & time
        holder.name.text = habit.name
        holder.time.text =
            "${habit.duration} mins | %02d:%02d".format(habit.startHour, habit.startMinute)

        //  Update visuals depending on status
        updateRowUI(holder, habit.status)

        // Edit habit
        holder.name.setOnClickListener { onEdit(habit) }

        // Delete habit
        holder.btnDelete.setOnClickListener { onDelete(habit) }
    }

    private fun updateRowUI(holder: HabitViewHolder, status: String) {
        when (status) {
            "done" -> {
                holder.name.paintFlags = holder.name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.name.setTextColor(holder.itemView.context.getColor(R.color.inactiveGray))
                holder.row.setBackgroundColor(holder.itemView.context.getColor(R.color.lightRose))
            }
            "canceled" -> {
                holder.name.paintFlags =
                    holder.name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                holder.name.setTextColor(holder.itemView.context.getColor(R.color.inactiveGray))
                holder.row.setBackgroundColor(holder.itemView.context.getColor(R.color.lightGray))
            }
            else -> { // pending
                holder.name.paintFlags =
                    holder.name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                holder.name.setTextColor(holder.itemView.context.getColor(R.color.black))
                holder.row.setBackgroundColor(holder.itemView.context.getColor(R.color.white))
            }
        }
    }

    override fun getItemCount() = habits.size
}
