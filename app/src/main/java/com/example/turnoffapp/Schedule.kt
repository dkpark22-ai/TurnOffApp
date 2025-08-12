package com.example.turnoffapp

import java.util.*

data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startTime: String, // "HH:mm" 형식
    val endTime: String,   // "HH:mm" 형식
    val daysOfWeek: Set<Int>, // 1=일요일, 2=월요일, ..., 7=토요일
    val isEnabled: Boolean = true,
    val blockedApps: Set<String> = emptySet(),
    val blockedWebsites: Set<String> = emptySet()
) {
    fun isActiveNow(): Boolean {
        if (!isEnabled) return false
        
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentTime = String.format("%02d:%02d", 
            calendar.get(Calendar.HOUR_OF_DAY), 
            calendar.get(Calendar.MINUTE))
        
        val isActive = daysOfWeek.contains(currentDay) && 
               isTimeInRange(currentTime, startTime, endTime)
               
        android.util.Log.d("Schedule", "Schedule $name: currentDay=$currentDay, currentTime=$currentTime, daysOfWeek=$daysOfWeek, timeRange=$startTime-$endTime, isActive=$isActive")
        
        return isActive
    }
    
    private fun isTimeInRange(currentTime: String, startTime: String, endTime: String): Boolean {
        val currentMinutes = timeToMinutes(currentTime)
        val startMinutes = timeToMinutes(startTime)
        val endMinutes = timeToMinutes(endTime)
        
        return if (startMinutes <= endMinutes) {
            // 같은 날 (예: 09:00 ~ 18:00)
            currentMinutes in startMinutes..endMinutes
        } else {
            // 자정을 넘김 (예: 22:00 ~ 06:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
    
    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
    
    fun getDaysOfWeekString(): String {
        val dayNames = mapOf(
            1 to "일", 2 to "월", 3 to "화", 4 to "수",
            5 to "목", 6 to "금", 7 to "토"
        )
        return daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }
}