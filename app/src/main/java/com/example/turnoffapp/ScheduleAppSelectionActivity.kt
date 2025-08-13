package com.example.turnoffapp

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class ScheduleAppSelectionActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvApps: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvLoading: TextView
    private lateinit var appSelectionAdapter: ScheduleAppSelectionAdapter
    
    private var allApps: List<ApplicationInfo> = emptyList()
    private var selectedApps: MutableSet<String> = mutableSetOf()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var searchJob: Job? = null
    private var appNamesCache: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_app_selection)

        initViews()
        loadPreselectedApps()
        setupSearch()
        setupClickListeners()
        loadInstalledAppsAsync()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.et_search)
        rvApps = findViewById(R.id.rv_apps)
        btnSave = findViewById(R.id.btn_save)
        progressLoading = findViewById(R.id.progress_loading)
        tvLoading = findViewById(R.id.tv_loading)
        rvApps.layoutManager = LinearLayoutManager(this)
    }

    private fun loadPreselectedApps() {
        // 이전에 선택된 앱들 로드
        val preselectedApps = intent.getStringArrayListExtra("selected_apps") ?: arrayListOf()
        selectedApps = preselectedApps.toMutableSet()
    }

    private fun loadInstalledAppsAsync() {
        // 로딩 UI 표시
        showLoading(true)
        
        scope.launch {
            try {
                // 백그라운드에서 앱 목록 로드
                val apps = withContext(Dispatchers.IO) {
                    loadInstalledApps()
                }
                
                // 메인 스레드에서 UI 업데이트
                allApps = apps
                createAppNamesCache()
                setupAdapter()
                showLoading(false)
                
            } catch (e: Exception) {
                allApps = emptyList()
                setupAdapter()
                showLoading(false)
            }
        }
    }
    
    private fun loadInstalledApps(): List<ApplicationInfo> {
        val packageManager = packageManager
        
        return try {
            // 사용자가 설치한 앱만 가져오기
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    try {
                        // 시스템 앱 및 시스템 패키지 제외
                        val isUserApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                                       (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        
                        // 패키지 이름으로 시스템 패키지 제외
                        val isNotSystemPackage = !app.packageName.startsWith("com.android") &&
                                                !app.packageName.startsWith("android") &&
                                                !app.packageName.startsWith("com.samsung") &&
                                                !app.packageName.startsWith("com.sec.") &&
                                                app.packageName != packageName
                        
                        val appName = packageManager.getApplicationLabel(app).toString()
                        
                        isUserApp && isNotSystemPackage && appName.isNotEmpty()
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
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun createAppNamesCache() {
        val cache = mutableMapOf<String, String>()
        allApps.forEach { app ->
            try {
                val appName = packageManager.getApplicationLabel(app).toString()
                cache[app.packageName] = appName
            } catch (e: Exception) {
                cache[app.packageName] = app.packageName
            }
        }
        appNamesCache = cache
    }
    
    private fun setupAdapter() {
        appSelectionAdapter = ScheduleAppSelectionAdapter(allApps, selectedApps) { packageName, isSelected ->
            if (isSelected) {
                selectedApps.add(packageName)
            } else {
                selectedApps.remove(packageName)
            }
        }
        rvApps.adapter = appSelectionAdapter
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressLoading.visibility = View.VISIBLE
            tvLoading.visibility = View.VISIBLE
            rvApps.visibility = View.GONE
            etSearch.isEnabled = false
            btnSave.isEnabled = false
        } else {
            progressLoading.visibility = View.GONE
            tvLoading.visibility = View.GONE
            rvApps.visibility = View.VISIBLE
            etSearch.isEnabled = true
            btnSave.isEnabled = true
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterAppsWithDelay(s.toString())
            }
        })
    }

    private fun filterAppsWithDelay(query: String) {
        // 이전 검색 작업 취소
        searchJob?.cancel()
        
        searchJob = scope.launch {
            // 300ms 딜레이로 디바운싱
            delay(300)
            
            val filteredApps = withContext(Dispatchers.Default) {
                filterApps(query)
            }
            
            // 메인 스레드에서 UI 업데이트
            appSelectionAdapter.updateApps(filteredApps, selectedApps)
        }
    }

    private fun filterApps(query: String): List<ApplicationInfo> {
        if (query.isEmpty()) {
            return allApps
        }
        
        val queryLower = query.lowercase()
        return allApps.filter { app ->
            val appName = appNamesCache[app.packageName] ?: app.packageName
            appName.lowercase().contains(queryLower) ||
            app.packageName.lowercase().contains(queryLower)
        }
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            val intent = Intent().apply {
                putStringArrayListExtra("selected_apps", ArrayList(selectedApps))
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
        job.cancel()
    }
}