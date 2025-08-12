package com.example.turnoffapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(
    private val onEditClick: (Schedule) -> Unit,
    private val onDeleteClick: (Schedule) -> Unit,
    private val onToggleClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private var schedules: List<Schedule> = emptyList()

    fun updateSchedules(newSchedules: List<Schedule>) {
        schedules = newSchedules
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.bind(schedule, onEditClick, onDeleteClick, onToggleClick)
    }

    override fun getItemCount(): Int = schedules.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvScheduleName: TextView = itemView.findViewById(R.id.tv_schedule_name)
        private val tvScheduleTime: TextView = itemView.findViewById(R.id.tv_schedule_time)
        private val tvScheduleDays: TextView = itemView.findViewById(R.id.tv_schedule_days)
        private val switchEnabled: Switch = itemView.findViewById(R.id.switch_enabled)
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(
            schedule: Schedule,
            onEditClick: (Schedule) -> Unit,
            onDeleteClick: (Schedule) -> Unit,
            onToggleClick: (Schedule) -> Unit
        ) {
            tvScheduleName.text = schedule.name
            tvScheduleTime.text = "${schedule.startTime} - ${schedule.endTime}"
            tvScheduleDays.text = schedule.getDaysOfWeekString()
            switchEnabled.isChecked = schedule.isEnabled

            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != schedule.isEnabled) {
                    onToggleClick(schedule)
                }
            }

            btnEdit.setOnClickListener {
                onEditClick(schedule)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(schedule)
            }

            // 활성화된 스케줄 강조 표시
            if (schedule.isActiveNow()) {
                itemView.setBackgroundResource(R.drawable.active_schedule_background)
            } else {
                itemView.setBackgroundResource(R.drawable.normal_schedule_background)
            }
        }
    }
}