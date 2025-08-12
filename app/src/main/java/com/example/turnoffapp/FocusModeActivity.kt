package com.example.turnoffapp

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FocusModeActivity : AppCompatActivity() {

    private lateinit var etTimerMinutes: EditText
    private lateinit var btnSelectApps: Button
    private lateinit var btnAddWebsite: Button
    private lateinit var etWebsiteUrl: EditText
    private lateinit var btnStartFocus: Button
    private lateinit var rvBlockedApps: RecyclerView
    private lateinit var rvBlockedWebsites: RecyclerView

    private lateinit var settingsManager: SettingsManager
    private lateinit var blockedAppsAdapter: BlockedAppsAdapter
    private lateinit var blockedWebsitesAdapter: BlockedWebsitesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_mode)

        initViews()
        initAdapters()
        initSettingsManager()
        setupClickListeners()
    }

    private fun initViews() {
        etTimerMinutes = findViewById(R.id.et_timer_minutes)
        btnSelectApps = findViewById(R.id.btn_select_apps)
        btnAddWebsite = findViewById(R.id.btn_add_website)
        etWebsiteUrl = findViewById(R.id.et_website_url)
        btnStartFocus = findViewById(R.id.btn_start_focus)
        rvBlockedApps = findViewById(R.id.rv_blocked_apps)
        rvBlockedWebsites = findViewById(R.id.rv_blocked_websites)
    }

    private fun initAdapters() {
        blockedAppsAdapter = BlockedAppsAdapter { packageName ->
            settingsManager.removeBlockedApp(packageName)
            blockedAppsAdapter.updateApps(settingsManager.getBlockedApps())
        }
        
        blockedWebsitesAdapter = BlockedWebsitesAdapter { url ->
            settingsManager.removeBlockedWebsite(url)
            blockedWebsitesAdapter.updateWebsites(settingsManager.getBlockedWebsites())
        }

        rvBlockedApps.layoutManager = LinearLayoutManager(this)
        rvBlockedApps.adapter = blockedAppsAdapter

        rvBlockedWebsites.layoutManager = LinearLayoutManager(this)
        rvBlockedWebsites.adapter = blockedWebsitesAdapter
    }

    private fun initSettingsManager() {
        settingsManager = SettingsManager(this)
        blockedAppsAdapter.updateApps(settingsManager.getBlockedApps())
        blockedWebsitesAdapter.updateWebsites(settingsManager.getBlockedWebsites())
    }

    private fun setupClickListeners() {
        btnSelectApps.setOnClickListener {
            showAppSelectionDialog()
        }

        btnAddWebsite.setOnClickListener {
            addWebsite()
        }

        btnStartFocus.setOnClickListener {
            startFocusMode()
        }
    }

    private fun showAppSelectionDialog() {
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "먼저 사용 통계 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, AppSelectionActivity::class.java)
        startActivity(intent)
    }

    private fun addWebsite() {
        val url = etWebsiteUrl.text.toString().trim()
        if (url.isNotEmpty()) {
            val formattedUrl = if (!url.startsWith("http")) "https://$url" else url
            settingsManager.addBlockedWebsite(formattedUrl)
            blockedWebsitesAdapter.updateWebsites(settingsManager.getBlockedWebsites())
            etWebsiteUrl.text.clear()
            Toast.makeText(this, "웹사이트가 추가되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFocusMode() {
        if (!hasAllPermissions()) {
            Toast.makeText(this, "모든 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val minutes = etTimerMinutes.text.toString().toIntOrNull() ?: 25
        val intent = Intent(this, FocusService::class.java)
        intent.putExtra("duration_minutes", minutes)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        Toast.makeText(this, "집중 모드가 시작되었습니다", Toast.LENGTH_SHORT).show()
        finish()
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

    override fun onResume() {
        super.onResume()
        blockedAppsAdapter.updateApps(settingsManager.getBlockedApps())
        blockedWebsitesAdapter.updateWebsites(settingsManager.getBlockedWebsites())
    }
}