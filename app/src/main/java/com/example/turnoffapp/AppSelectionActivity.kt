package com.example.turnoffapp

import android.app.ProgressDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvApps: RecyclerView
    private lateinit var appSelectionAdapter: AppSelectionAdapter
    private lateinit var settingsManager: SettingsManager
    private var progressDialog: ProgressDialog? = null
    
    private var allApps: List<ApplicationInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        initViews()
        initSettingsManager()
        loadInstalledApps()
        setupSearch()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.et_search)
        rvApps = findViewById(R.id.rv_apps)
        rvApps.layoutManager = LinearLayoutManager(this)
    }

    private fun initSettingsManager() {
        settingsManager = SettingsManager(this)
    }

    private fun loadInstalledApps() {
        // 로딩 다이얼로그 표시
        progressDialog = ProgressDialog(this).apply {
            setMessage("앱 목록을 불러오는 중...")
            setCancelable(false)
            show()
        }
        
        // 백그라운드에서 앱 로딩
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val packageManager = packageManager
                
                // 모든 설치된 앱을 가져오기
                val apps = packageManager.getInstalledApplications(0)
                    .filter { app ->
                        try {
                            // 앱 이름이 있고, 자기 자신이 아닌 앱들만 포함
                            val appName = packageManager.getApplicationLabel(app).toString()
                            appName.isNotEmpty() && 
                            app.packageName != packageName &&
                            app.packageName != "android" // 안드로이드 시스템 제외
                        } catch (e: Exception) {
                            false
                        }
                    }
                    .sortedBy { app ->
                        try {
                            packageManager.getApplicationLabel(app).toString().lowercase()
                        } catch (e: Exception) {
                            app.packageName.lowercase()
                        }
                    }
                
                // UI 스레드에서 결과 처리
                withContext(Dispatchers.Main) {
                    allApps = apps
                    val blockedApps = settingsManager.getBlockedApps()

                    appSelectionAdapter = AppSelectionAdapter(allApps, blockedApps) { packageName, isSelected ->
                        if (isSelected) {
                            settingsManager.addBlockedApp(packageName)
                        } else {
                            settingsManager.removeBlockedApp(packageName)
                        }
                    }

                    rvApps.adapter = appSelectionAdapter
                    
                    // 로딩 다이얼로그 숨기기
                    progressDialog?.dismiss()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    allApps = emptyList()
                    progressDialog?.dismiss()
                }
            }
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }
        })
    }

    private fun filterApps(query: String) {
        val filteredApps = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { app ->
                val appName = packageManager.getApplicationLabel(app).toString()
                appName.lowercase().contains(query.lowercase()) ||
                app.packageName.lowercase().contains(query.lowercase())
            }
        }
        
        val blockedApps = settingsManager.getBlockedApps()
        appSelectionAdapter.updateApps(filteredApps, blockedApps)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
    }
}