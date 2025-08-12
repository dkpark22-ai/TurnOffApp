package com.example.turnoffapp

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScheduleAppSelectionActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvApps: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var appSelectionAdapter: ScheduleAppSelectionAdapter
    
    private var allApps: List<ApplicationInfo> = emptyList()
    private var selectedApps: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_app_selection)

        initViews()
        loadPreselectedApps()
        loadInstalledApps()
        setupSearch()
        setupClickListeners()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.et_search)
        rvApps = findViewById(R.id.rv_apps)
        btnSave = findViewById(R.id.btn_save)
        rvApps.layoutManager = LinearLayoutManager(this)
    }

    private fun loadPreselectedApps() {
        // 이전에 선택된 앱들 로드
        val preselectedApps = intent.getStringArrayListExtra("selected_apps") ?: arrayListOf()
        selectedApps = preselectedApps.toMutableSet()
    }

    private fun loadInstalledApps() {
        val packageManager = packageManager
        
        try {
            // 모든 설치된 앱을 가져오기
            allApps = packageManager.getInstalledApplications(0)
                .filter { app ->
                    try {
                        val appName = packageManager.getApplicationLabel(app).toString()
                        appName.isNotEmpty() && 
                        app.packageName != packageName &&
                        app.packageName != "android"
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
            allApps = emptyList()
        }

        appSelectionAdapter = ScheduleAppSelectionAdapter(allApps, selectedApps) { packageName, isSelected ->
            if (isSelected) {
                selectedApps.add(packageName)
            } else {
                selectedApps.remove(packageName)
            }
        }

        rvApps.adapter = appSelectionAdapter
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
                val appName = try {
                    packageManager.getApplicationLabel(app).toString()
                } catch (e: Exception) {
                    app.packageName
                }
                appName.lowercase().contains(query.lowercase()) ||
                app.packageName.lowercase().contains(query.lowercase())
            }
        }
        
        appSelectionAdapter.updateApps(filteredApps, selectedApps)
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
}