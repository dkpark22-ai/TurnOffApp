package com.example.turnoffapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ActiveSchedulesAdapter(
    private val onScheduleClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ActiveSchedulesAdapter.ScheduleViewHolder>() {

    private var schedules = mutableListOf<Schedule>()

    fun updateSchedules(newSchedules: List<Schedule>) {
        schedules.clear()
        schedules.addAll(newSchedules)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(schedules[position])
    }

    override fun getItemCount(): Int = schedules.size

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvScheduleTitle: TextView = itemView.findViewById(R.id.tv_schedule_title)
        private val tvScheduleTime: TextView = itemView.findViewById(R.id.tv_schedule_time)
        private val tvScheduleStatus: TextView = itemView.findViewById(R.id.tv_schedule_status)
        private val tvBlockedApps: TextView = itemView.findViewById(R.id.tv_blocked_apps)
        private val tvBlockedWebsites: TextView = itemView.findViewById(R.id.tv_blocked_websites)

        fun bind(schedule: Schedule) {
            tvScheduleTitle.text = schedule.name

            val daysText = schedule.daysOfWeek.joinToString(", ") { dayOfWeek ->
                when (dayOfWeek) {
                    Calendar.SUNDAY -> "일"
                    Calendar.MONDAY -> "월"
                    Calendar.TUESDAY -> "화"
                    Calendar.WEDNESDAY -> "수"
                    Calendar.THURSDAY -> "목"
                    Calendar.FRIDAY -> "금"
                    Calendar.SATURDAY -> "토"
                    else -> ""
                }
            }

            val startTime = schedule.startTime
            val endTime = schedule.endTime
            
            tvScheduleTime.text = "$daysText | $startTime - $endTime"

            val isActive = isScheduleCurrentlyActive(schedule)
            tvScheduleStatus.text = if (isActive) "활성화" else "비활성화"
            tvScheduleStatus.setTextColor(
                if (isActive) 
                    itemView.context.getColor(android.R.color.holo_green_dark)
                else 
                    itemView.context.getColor(android.R.color.darker_gray)
            )

            // 차단 앱 정보 표시
            val appsText = if (schedule.blockedApps.isNotEmpty()) {
                "차단 앱: ${schedule.blockedApps.joinToString(", ") { getAppName(it) }}"
            } else {
                "차단 앱: 없음"
            }
            tvBlockedApps.text = appsText

            // 차단 웹사이트 정보 표시
            val websitesText = if (schedule.blockedWebsites.isNotEmpty()) {
                "차단 사이트: ${schedule.blockedWebsites.joinToString(", ") { getDomainName(it) }}"
            } else {
                "차단 사이트: 없음"
            }
            tvBlockedWebsites.text = websitesText

            itemView.setOnClickListener {
                onScheduleClick(schedule)
            }
        }

        private fun getAppName(packageName: String): String {
            return try {
                val pm = itemView.context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName.substringAfterLast(".")
            }
        }

        private fun getDomainName(url: String): String {
            return try {
                val domain = url.replace("https://", "").replace("http://", "")
                domain.split("/")[0]
            } catch (e: Exception) {
                url
            }
        }

        private fun isScheduleCurrentlyActive(schedule: Schedule): Boolean {
            return schedule.isActiveNow()
        }
    }
}