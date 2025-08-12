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

class MainActivity : AppCompatActivity() {

    private lateinit var btnUsageStats: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnOverlay: Button
    private lateinit var btnStartFocus: Button
    private lateinit var btnScheduleManagement: Button
    private lateinit var rvActiveSchedules: RecyclerView
    private lateinit var layoutPermissionsHeader: View
    private lateinit var layoutPermissionButtons: View
    private lateinit var tvPermissionsToggle: TextView

    private lateinit var settingsManager: SettingsManager
    private lateinit var activeSchedulesAdapter: ActiveSchedulesAdapter

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
        btnStartFocus = findViewById(R.id.btn_start_focus)
        btnScheduleManagement = findViewById(R.id.btn_schedule_management)
        rvActiveSchedules = findViewById(R.id.rv_active_schedules)
        layoutPermissionsHeader = findViewById(R.id.layout_permissions_header)
        layoutPermissionButtons = findViewById(R.id.layout_permission_buttons)
        tvPermissionsToggle = findViewById(R.id.tv_permissions_toggle)
    }

    private fun initAdapters() {
        activeSchedulesAdapter = ActiveSchedulesAdapter { schedule ->
            // 스케줄 수정 화면으로 바로 이동
            val intent = Intent(this, EditScheduleActivity::class.java).apply {
                putExtra("schedule_id", schedule.id)
            }
            startActivity(intent)
        }

        rvActiveSchedules.layoutManager = LinearLayoutManager(this)
        rvActiveSchedules.adapter = activeSchedulesAdapter
    }

    private fun initSettingsManager() {
        settingsManager = SettingsManager(this)
        updateActiveSchedules()
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

        btnStartFocus.setOnClickListener {
            val intent = Intent(this, FocusModeActivity::class.java)
            startActivity(intent)
        }

        btnScheduleManagement.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
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
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
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
        
        btnUsageStats.isEnabled = !hasUsageStats
        btnAccessibility.isEnabled = !hasAccessibility
        btnOverlay.isEnabled = !hasOverlay
        
        // 모든 권한이 허용된 경우 권한 섹션을 접기
        if (hasUsageStats && hasAccessibility && hasOverlay && isPermissionsExpanded) {
            isPermissionsExpanded = false
            layoutPermissionButtons.visibility = View.GONE
            tvPermissionsToggle.text = "▼"
        }
        
        // 권한이 필요한 경우에만 권한 섹션을 보여주고 펼치기
        if ((!hasUsageStats || !hasAccessibility || !hasOverlay) && !isPermissionsExpanded) {
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

    override fun onResume() {
        super.onResume()
        updatePermissionButtons()
        updateActiveSchedules()
        startScheduleMonitoring() // 권한이 새로 허용된 경우를 위해
    }
}