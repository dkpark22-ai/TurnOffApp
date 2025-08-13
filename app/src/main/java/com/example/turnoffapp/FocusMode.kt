package com.example.turnoffapp

import java.util.*

data class FocusMode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationMinutes: Int = 0, // 0이면 무제한
    val blockedApps: Set<String> = emptySet(),
    val blockedWebsites: Set<String> = emptySet(),
    val allowBreaks: Boolean = false,
    val breakIntervalMinutes: Int = 30,
    val breakDurationMinutes: Int = 5
) {
    fun isUnlimited(): Boolean = durationMinutes == 0
    
    fun getDurationText(): String {
        return if (isUnlimited()) {
            "무제한"
        } else {
            "${durationMinutes}분"
        }
    }
    
    fun getBreakText(): String {
        return if (allowBreaks) {
            "휴식: ${breakIntervalMinutes}분마다 ${breakDurationMinutes}분"
        } else {
            "휴식 없음"
        }
    }
}