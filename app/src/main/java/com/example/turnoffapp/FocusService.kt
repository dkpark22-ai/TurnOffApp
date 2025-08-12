package com.example.turnoffapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class FocusService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var settingsManager: SettingsManager
    
    private var focusEndTime: Long = 0
    private var isRunning = false
    private var isScheduleMode = false
    private var scheduleId: String? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "FocusServiceChannel"
        
        val temporaryWhitelist = ConcurrentHashMap<String, Long>()

        fun allowAppTemporarily(packageName: String, durationMillis: Long) {
            val expiryTime = System.currentTimeMillis() + durationMillis
            temporaryWhitelist[packageName] = expiryTime
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        settingsManager = SettingsManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isScheduleMode = intent?.getBooleanExtra("is_schedule_mode", false) ?: false
        scheduleId = intent?.getStringExtra("schedule_id")
        
        if (isScheduleMode) {
            // 스케줄 모드 - 무한 실행
            focusEndTime = Long.MAX_VALUE
        } else {
            // 타이머 모드 - 지정된 시간만 실행
            val durationMinutes = intent?.getIntExtra("duration_minutes", 25) ?: 25
            focusEndTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
        }
        
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())

        scope.launch {
            while (isRunning) {
                if (isScheduleMode) {
                    // 스케줄 모드에서는 스케줄이 여전히 활성화되어 있는지 체크
                    val activeSchedule = settingsManager.getActiveSchedule()
                    if (activeSchedule == null || activeSchedule.id != scheduleId) {
                        // 스케줄이 비활성화되거나 다른 스케줄로 변경됨
                        stopSelf()
                        break
                    }
                } else {
                    // 타이머 모드에서는 시간 체크
                    if (System.currentTimeMillis() >= focusEndTime) {
                        stopSelf()
                        break
                    }
                }
                
                checkForegroundApp()
                delay(500)
            }
        }

        return START_STICKY
    }

    private fun checkForegroundApp() {
        val currentTime = System.currentTimeMillis()

        // 만료된 임시 허용 앱 제거
        temporaryWhitelist.entries.removeIf { it.value < currentTime }

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 1000 * 10,
            currentTime
        )

        if (stats != null && stats.isNotEmpty()) {
            val currentApp = stats.sortedByDescending { it.lastTimeUsed }.firstOrNull()?.packageName
            
            // 디버깅용 로그 (나중에 제거)
            android.util.Log.d("FocusService", "Current app: $currentApp")

            if (currentApp != null && currentApp != packageName) {
                // 임시 허용된 앱인지 확인
                if (temporaryWhitelist.containsKey(currentApp)) {
                    android.util.Log.d("FocusService", "App $currentApp is temporarily allowed")
                    return
                }

                val blockedApps = settingsManager.getActiveBlockedApps()
                android.util.Log.d("FocusService", "Blocked apps: $blockedApps")
                
                if (blockedApps.contains(currentApp)) {
                    android.util.Log.d("FocusService", "Blocking app: $currentApp")
                    val intent = Intent(this, BlockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("BLOCKED_APP_PACKAGE", currentApp)
                    }
                    startActivity(intent)
                } else {
                    android.util.Log.d("FocusService", "App $currentApp is not blocked")
                }
            }
        } else {
            android.util.Log.d("FocusService", "No usage stats available")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "디지털 디톡스 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "집중 모드 실행 중"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val contentText = if (isScheduleMode) {
            val activeSchedule = settingsManager.getActiveSchedule()
            "스케줄 '${activeSchedule?.name ?: "알 수 없음"}' 실행 중"
        } else {
            val remainingTime = focusEndTime - System.currentTimeMillis()
            val remainingMinutes = (remainingTime / (1000 * 60)).toInt()
            getString(R.string.remaining_time, "${remainingMinutes}분")
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_running))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        job.cancel()
        temporaryWhitelist.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}