package com.example.turnoffapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*

class ScheduleMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var settingsManager: SettingsManager
    private var isRunning = false
    private var currentActiveScheduleId: String? = null

    companion object {
        const val NOTIFICATION_ID = 2
        const val CHANNEL_ID = "ScheduleMonitorChannel"
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())

        scope.launch {
            while (isRunning) {
                checkSchedules()
                delay(60000) // 1분마다 체크
            }
        }

        return START_STICKY
    }

    private fun checkSchedules() {
        val activeSchedule = settingsManager.getActiveSchedule()
        
        android.util.Log.d("ScheduleMonitorService", "Checking schedules, active: ${activeSchedule?.name}")
        
        if (activeSchedule != null) {
            // 현재 활성화된 스케줄이 있음
            if (currentActiveScheduleId != activeSchedule.id) {
                // 새로운 스케줄이 활성화됨
                android.util.Log.d("ScheduleMonitorService", "Starting scheduled focus: ${activeSchedule.name}")
                currentActiveScheduleId = activeSchedule.id
                startScheduledFocus(activeSchedule)
            }
        } else {
            // 활성화된 스케줄이 없음
            if (currentActiveScheduleId != null) {
                // 이전에 활성화된 스케줄이 종료됨
                android.util.Log.d("ScheduleMonitorService", "Stopping scheduled focus")
                currentActiveScheduleId = null
                stopScheduledFocus()
            }
        }
    }

    private fun startScheduledFocus(schedule: Schedule) {
        // FocusService 시작 (스케줄 기반)
        val intent = Intent(this, FocusService::class.java)
        intent.putExtra("is_schedule_mode", true)
        intent.putExtra("schedule_id", schedule.id)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        // 알림 업데이트
        updateNotification("스케줄 '${schedule.name}' 실행 중")
    }

    private fun stopScheduledFocus() {
        // FocusService 중지
        val intent = Intent(this, FocusService::class.java)
        stopService(intent)
        
        // 알림 업데이트
        updateNotification("스케줄 대기 중")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "스케줄 모니터링",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "스케줄 자동 실행 모니터링"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String = "스케줄 대기 중"): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("디지털 디톡스 스케줄")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(message))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}