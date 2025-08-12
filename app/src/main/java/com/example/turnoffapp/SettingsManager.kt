package com.example.turnoffapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("digital_detox_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_BLOCKED_WEBSITES = "blocked_websites"
        private const val KEY_FOCUS_DURATION = "focus_duration"
        private const val KEY_IS_FOCUS_ACTIVE = "is_focus_active"
        private const val KEY_SCHEDULES = "schedules"
    }

    fun addBlockedApp(packageName: String) {
        val currentApps = getBlockedApps().toMutableSet()
        currentApps.add(packageName)
        saveBlockedApps(currentApps)
    }

    fun removeBlockedApp(packageName: String) {
        val currentApps = getBlockedApps().toMutableSet()
        currentApps.remove(packageName)
        saveBlockedApps(currentApps)
    }

    fun getBlockedApps(): Set<String> {
        return sharedPreferences.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    private fun saveBlockedApps(apps: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_BLOCKED_APPS, apps)
            .apply()
    }

    fun addBlockedWebsite(url: String) {
        val currentWebsites = getBlockedWebsites().toMutableSet()
        currentWebsites.add(url)
        saveBlockedWebsites(currentWebsites)
    }

    fun removeBlockedWebsite(url: String) {
        val currentWebsites = getBlockedWebsites().toMutableSet()
        currentWebsites.remove(url)
        saveBlockedWebsites(currentWebsites)
    }

    fun getBlockedWebsites(): Set<String> {
        return sharedPreferences.getStringSet(KEY_BLOCKED_WEBSITES, emptySet()) ?: emptySet()
    }

    private fun saveBlockedWebsites(websites: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_BLOCKED_WEBSITES, websites)
            .apply()
    }

    fun setFocusDuration(minutes: Int) {
        sharedPreferences.edit()
            .putInt(KEY_FOCUS_DURATION, minutes)
            .apply()
    }

    fun getFocusDuration(): Int {
        return sharedPreferences.getInt(KEY_FOCUS_DURATION, 25)
    }

    fun setFocusActive(isActive: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_FOCUS_ACTIVE, isActive)
            .apply()
    }

    fun isFocusActive(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_FOCUS_ACTIVE, false)
    }

    fun clearAllSettings() {
        sharedPreferences.edit().clear().apply()
    }

    // 스케줄 관리 메서드들
    fun addSchedule(schedule: Schedule) {
        val schedules = getSchedules().toMutableList()
        schedules.add(schedule)
        saveSchedules(schedules)
    }

    fun updateSchedule(schedule: Schedule) {
        val schedules = getSchedules().toMutableList()
        val index = schedules.indexOfFirst { it.id == schedule.id }
        if (index != -1) {
            schedules[index] = schedule
            saveSchedules(schedules)
        }
    }

    fun removeSchedule(scheduleId: String) {
        val schedules = getSchedules().toMutableList()
        schedules.removeAll { it.id == scheduleId }
        saveSchedules(schedules)
    }

    fun getSchedules(): List<Schedule> {
        val json = sharedPreferences.getString(KEY_SCHEDULES, "[]")
        val type = object : TypeToken<List<Schedule>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveSchedules(schedules: List<Schedule>) {
        val json = gson.toJson(schedules)
        sharedPreferences.edit()
            .putString(KEY_SCHEDULES, json)
            .apply()
    }

    fun getActiveSchedule(): Schedule? {
        return getSchedules().firstOrNull { it.isActiveNow() }
    }

    fun getActiveSchedules(): List<Schedule> {
        return getSchedules().filter { it.isEnabled }
    }

    fun getActiveBlockedApps(): Set<String> {
        val activeSchedule = getActiveSchedule()
        return if (activeSchedule != null) {
            activeSchedule.blockedApps
        } else {
            getBlockedApps() // 일반 차단 목록 사용
        }
    }

    fun getActiveBlockedWebsites(): Set<String> {
        val activeSchedule = getActiveSchedule()
        return if (activeSchedule != null) {
            activeSchedule.blockedWebsites
        } else {
            getBlockedWebsites() // 일반 차단 목록 사용
        }
    }
}