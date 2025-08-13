package com.example.turnoffapp

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.os.Looper
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnUsageStats: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnOverlay: Button
    private lateinit var btnStopFocus: Button
    private lateinit var btnScheduleManagement: Button
    private lateinit var btnFocusModeManagement: Button
    private lateinit var rvActiveSchedules: RecyclerView
    private lateinit var rvFocusModes: RecyclerView
    private lateinit var cardPermissions: View
    private lateinit var cardFocusStatus: View
    private lateinit var layoutPermissionsHeader: View
    private lateinit var layoutPermissionButtons: View
    private lateinit var tvPermissionsToggle: TextView
    private lateinit var tvFocusModeName: TextView
    private lateinit var tvFocusStartTime: TextView
    private lateinit var tvFocusEndTime: TextView
    private lateinit var tvFocusRemainingTime: TextView
    private lateinit var tvBlockedAppsList: TextView
    private lateinit var tvBlockedWebsitesList: TextView

    private lateinit var settingsManager: SettingsManager
    private lateinit var activeSchedulesAdapter: ActiveSchedulesAdapter
    private lateinit var focusModesAdapter: FocusModesAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private var isPermissionsExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initAdapters()
        initSettingsManager()
        setupClickListeners()
        updatePermissionButtons()
        startScheduleMonitoring()
    }

    private fun initViews() {
        btnUsageStats = findViewById(R.id.btn_usage_stats)
        btnAccessibility = findViewById(R.id.btn_accessibility)
        btnOverlay = findViewById(R.id.btn_overlay)
        btnStopFocus = findViewById(R.id.btn_stop_focus)
        btnScheduleManagement = findViewById(R.id.btn_schedule_management)
        btnFocusModeManagement = findViewById(R.id.btn_focus_mode_management)
        rvActiveSchedules = findViewById(R.id.rv_active_schedules)
        rvFocusModes = findViewById(R.id.rv_focus_modes)
        cardPermissions = findViewById(R.id.card_permissions)
        cardFocusStatus = findViewById(R.id.card_focus_status)
        layoutPermissionsHeader = findViewById(R.id.layout_permissions_header)
        layoutPermissionButtons = findViewById(R.id.layout_permission_buttons)
        tvPermissionsToggle = findViewById(R.id.tv_permissions_toggle)
        tvFocusModeName = findViewById(R.id.tv_focus_mode_name)
        tvFocusStartTime = findViewById(R.id.tv_focus_start_time)
        tvFocusEndTime = findViewById(R.id.tv_focus_end_time)
        tvFocusRemainingTime = findViewById(R.id.tv_focus_remaining_time)
        tvBlockedAppsList = findViewById(R.id.tv_blocked_apps_list)
        tvBlockedWebsitesList = findViewById(R.id.tv_blocked_websites_list)
    }

    private fun initAdapters() {
        activeSchedulesAdapter = ActiveSchedulesAdapter { schedule ->
            // 스케줄 수정 화면으로 바로 이동
            val intent = Intent(this, EditScheduleActivity::class.java).apply {
                putExtra("schedule_id", schedule.id)
            }
            startActivity(intent)
        }

        focusModesAdapter = FocusModesAdapter(
            onStartFocusMode = { focusMode ->
                startFocusMode(focusMode)
            },
            onEditFocusMode = { focusMode ->
                // 집중모드 수정 화면으로 이동
                val intent = Intent(this, FocusModeEditActivity::class.java).apply {
                    putExtra("focus_mode_id", focusMode.id)
                }
                startActivity(intent)
            },
            getAppName = this::getAppName
        )

        rvActiveSchedules.layoutManager = LinearLayoutManager(this)
        rvActiveSchedules.adapter = activeSchedulesAdapter

        rvFocusModes.layoutManager = LinearLayoutManager(this)
        rvFocusModes.adapter = focusModesAdapter
    }

    private fun initSettingsManager() {
        settingsManager = SettingsManager(this)
        updateActiveSchedules()
        updateFocusModes()
    }

    private fun setupClickListeners() {
        btnUsageStats.setOnClickListener {
            requestUsageStatsPermission()
        }

        btnAccessibility.setOnClickListener {
            requestAccessibilityPermission()
        }

        btnOverlay.setOnClickListener {
            requestOverlayPermission()
        }

        btnScheduleManagement.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
        }

        btnFocusModeManagement.setOnClickListener {
            val intent = Intent(this, FocusModeManagementActivity::class.java)
            startActivity(intent)
        }

        btnStopFocus.setOnClickListener {
            stopFocusMode()
        }

        layoutPermissionsHeader.setOnClickListener {
            togglePermissionsSection()
        }
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun updateActiveSchedules() {
        val activeSchedules = settingsManager.getActiveSchedules()
        activeSchedulesAdapter.updateSchedules(activeSchedules)
    }

    private fun updateFocusModes() {
        val focusModes = settingsManager.getFocusModes()
        focusModesAdapter.updateFocusModes(focusModes)
    }

    private fun togglePermissionsSection() {
        isPermissionsExpanded = !isPermissionsExpanded
        
        if (isPermissionsExpanded) {
            layoutPermissionButtons.visibility = View.VISIBLE
            tvPermissionsToggle.text = "▲"
        } else {
            layoutPermissionButtons.visibility = View.GONE
            tvPermissionsToggle.text = "▼"
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasAccessibilityPermission(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(packageName) == true
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun hasAllPermissions(): Boolean {
        return hasUsageStatsPermission() && hasAccessibilityPermission() && hasOverlayPermission()
    }

    private fun updatePermissionButtons() {
        val hasUsageStats = hasUsageStatsPermission()
        val hasAccessibility = hasAccessibilityPermission()
        val hasOverlay = hasOverlayPermission()
        val hasAllPermissions = hasUsageStats && hasAccessibility && hasOverlay
        
        // 모든 권한이 허용된 경우 권한 카드 자체를 숨김
        if (hasAllPermissions) {
            cardPermissions.visibility = View.GONE
            return
        }
        
        // 권한이 필요한 경우 카드를 보이고 버튼 상태 업데이트
        cardPermissions.visibility = View.VISIBLE
        
        btnUsageStats.isEnabled = !hasUsageStats
        btnAccessibility.isEnabled = !hasAccessibility
        btnOverlay.isEnabled = !hasOverlay
        
        // 권한이 필요한 경우 자동으로 펼치기
        if (!isPermissionsExpanded) {
            isPermissionsExpanded = true
            layoutPermissionButtons.visibility = View.VISIBLE
            tvPermissionsToggle.text = "▲"
        }
    }

    private fun startScheduleMonitoring() {
        if (hasAllPermissions()) {
            val intent = Intent(this, ScheduleMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun stopFocusMode() {
        // 집중 모드 상태를 먼저 비활성화
        settingsManager.setFocusMode(false)
        settingsManager.setCurrentFocusMode(null)
        
        // 서비스 중단
        val intent = Intent(this, FocusService::class.java)
        stopService(intent)
        
        Toast.makeText(this, "집중 모드가 중단되었습니다", Toast.LENGTH_SHORT).show()
        
        // UI 즉시 업데이트
        updateFocusStatus()
    }

    private fun startFocusMode(focusMode: FocusMode) {
        val currentTime = System.currentTimeMillis()
        val endTime = if (focusMode.isUnlimited()) {
            Long.MAX_VALUE
        } else {
            currentTime + (focusMode.durationMinutes * 60 * 1000L)
        }
        
        settingsManager.setFocusMode(true, currentTime, endTime, focusMode.name)
        settingsManager.setCurrentFocusMode(focusMode)
        
        val intent = Intent(this, FocusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        Toast.makeText(this, "${focusMode.name} 집중모드를 시작했습니다", Toast.LENGTH_SHORT).show()
        updateFocusStatus()
        
        // 집중 모드 시작 후 진행중인 집중모드 카드로 스크롤
        handler.postDelayed({
            val scrollView = findViewById<ScrollView>(R.id.main_scroll_view)
            scrollView?.let { sv ->
                val location = IntArray(2)
                cardFocusStatus.getLocationOnScreen(location)
                val parentLocation = IntArray(2)
                sv.getLocationOnScreen(parentLocation)
                val targetY = location[1] - parentLocation[1] - 100 // 100dp 위쪽 여백
                sv.smoothScrollTo(0, targetY.coerceAtLeast(0))
            }
        }, 300) // 300ms 딜레이로 UI 업데이트 완료 후 스크롤
    }

    private fun updateFocusStatus() {
        val isFocusActive = settingsManager.isFocusActive()
        
        if (isFocusActive) {
            cardFocusStatus.visibility = View.VISIBLE
            
            tvFocusModeName.text = settingsManager.getFocusModeName()
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = settingsManager.getFocusStartTime()
            val endTime = settingsManager.getFocusEndTime()
            
            tvFocusStartTime.text = timeFormat.format(Date(startTime))
            
            if (endTime == Long.MAX_VALUE) {
                tvFocusEndTime.text = "무제한"
            } else {
                tvFocusEndTime.text = timeFormat.format(Date(endTime))
            }
            
            updateRemainingTime()
            updateBlockedItemsList()
            startTimeUpdater()
        } else {
            cardFocusStatus.visibility = View.GONE
            stopTimeUpdater()
        }
    }

    private fun updateRemainingTime() {
        val remainingMs = settingsManager.getRemainingFocusTime()
        
        if (remainingMs <= 0) {
            tvFocusRemainingTime.text = "완료"
            return
        }
        
        val endTime = settingsManager.getFocusEndTime()
        if (endTime == Long.MAX_VALUE) {
            tvFocusRemainingTime.text = "무제한"
            return
        }
        
        val remainingMinutes = (remainingMs / (1000 * 60)).toInt()
        val remainingSeconds = ((remainingMs % (1000 * 60)) / 1000).toInt()
        
        tvFocusRemainingTime.text = if (remainingMinutes > 0) {
            "${remainingMinutes}분 ${remainingSeconds}초"
        } else {
            "${remainingSeconds}초"
        }
    }

    private fun startTimeUpdater() {
        stopTimeUpdater()
        updateRunnable = object : Runnable {
            override fun run() {
                if (settingsManager.isFocusActive()) {
                    updateRemainingTime()
                    handler.postDelayed(this, 1000) // 1초마다 업데이트
                } else {
                    updateFocusStatus() // 집중 모드가 종료되면 UI 업데이트
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopTimeUpdater() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }

    private fun updateBlockedItemsList() {
        val currentFocusMode = settingsManager.getCurrentFocusMode()
        if (currentFocusMode != null) {
            // 차단된 앱 목록 표시
            if (currentFocusMode.blockedApps.isNotEmpty()) {
                val appNames = currentFocusMode.blockedApps.map { packageName ->
                    getAppName(packageName)
                }.joinToString(", ")
                tvBlockedAppsList.text = "앱: $appNames"
            } else {
                tvBlockedAppsList.text = "앱: 없음"
            }
            
            // 차단된 웹사이트 목록 표시
            if (currentFocusMode.blockedWebsites.isNotEmpty()) {
                val websites = currentFocusMode.blockedWebsites.joinToString(", ")
                tvBlockedWebsitesList.text = "웹사이트: $websites"
            } else {
                tvBlockedWebsitesList.text = "웹사이트: 없음"
            }
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName // 앱 이름을 가져올 수 없으면 패키지명 사용
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionButtons()
        updateActiveSchedules()
        updateFocusModes()
        updateFocusStatus()
        startScheduleMonitoring()
    }

    override fun onPause() {
        super.onPause()
        stopTimeUpdater()
    }
}