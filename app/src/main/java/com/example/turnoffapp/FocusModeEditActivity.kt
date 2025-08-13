package com.example.turnoffapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog

class FocusModeEditActivity : AppCompatActivity() {

    private lateinit var etFocusModeName: EditText
    private lateinit var rgDurationType: RadioGroup
    private lateinit var rbUnlimited: RadioButton
    private lateinit var rbTimed: RadioButton
    private lateinit var layoutDurationInput: LinearLayout
    private lateinit var etDuration: EditText
    private lateinit var cbAllowBreaks: CheckBox
    private lateinit var layoutBreakSettings: LinearLayout
    private lateinit var etBreakInterval: EditText
    private lateinit var etBreakDuration: EditText
    private lateinit var btnSelectApps: Button
    private lateinit var btnManageWebsites: Button
    private lateinit var tvSelectedAppsCount: TextView
    private lateinit var tvSelectedWebsitesCount: TextView
    private lateinit var btnSave: Button

    private lateinit var settingsManager: SettingsManager
    private var focusMode: FocusMode? = null
    private var selectedApps = mutableSetOf<String>()
    private var selectedWebsites = mutableSetOf<String>()

    companion object {
        private const val REQUEST_APP_SELECTION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_mode_edit)

        initViews()
        initData()
        setupClickListeners()
        updateCountDisplay()
    }

    private fun initViews() {
        etFocusModeName = findViewById(R.id.et_focus_mode_name)
        rgDurationType = findViewById(R.id.rg_duration_type)
        rbUnlimited = findViewById(R.id.rb_unlimited)
        rbTimed = findViewById(R.id.rb_timed)
        layoutDurationInput = findViewById(R.id.layout_duration_input)
        etDuration = findViewById(R.id.et_duration)
        cbAllowBreaks = findViewById(R.id.cb_allow_breaks)
        layoutBreakSettings = findViewById(R.id.layout_break_settings)
        etBreakInterval = findViewById(R.id.et_break_interval)
        etBreakDuration = findViewById(R.id.et_break_duration)
        btnSelectApps = findViewById(R.id.btn_select_apps)
        btnManageWebsites = findViewById(R.id.btn_manage_websites)
        tvSelectedAppsCount = findViewById(R.id.tv_selected_apps_count)
        tvSelectedWebsitesCount = findViewById(R.id.tv_selected_websites_count)
        btnSave = findViewById(R.id.btn_save)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initData() {
        settingsManager = SettingsManager(this)
        
        // 편집 모드인지 확인
        val focusModeId = intent.getStringExtra("focus_mode_id")
        if (focusModeId != null) {
            focusMode = settingsManager.getFocusModes().find { it.id == focusModeId }
            focusMode?.let {
                title = "집중모드 편집"
                loadFocusModeData(it)
            }
        } else {
            title = "집중모드 추가"
            setDefaultValues()
        }
    }

    private fun loadFocusModeData(focusMode: FocusMode) {
        etFocusModeName.setText(focusMode.name)
        selectedApps.clear()
        selectedApps.addAll(focusMode.blockedApps)
        selectedWebsites.clear()
        selectedWebsites.addAll(focusMode.blockedWebsites)
        
        if (focusMode.isUnlimited()) {
            rbUnlimited.isChecked = true
            layoutDurationInput.visibility = View.GONE
        } else {
            rbTimed.isChecked = true
            layoutDurationInput.visibility = View.VISIBLE
            etDuration.setText(focusMode.durationMinutes.toString())
        }
        
        cbAllowBreaks.isChecked = focusMode.allowBreaks
        if (focusMode.allowBreaks) {
            layoutBreakSettings.visibility = View.VISIBLE
            etBreakInterval.setText(focusMode.breakIntervalMinutes.toString())
            etBreakDuration.setText(focusMode.breakDurationMinutes.toString())
        }
    }

    private fun setDefaultValues() {
        rbTimed.isChecked = true
        layoutDurationInput.visibility = View.VISIBLE
        etDuration.setText("25")
        etBreakInterval.setText("30")
        etBreakDuration.setText("5")
    }

    private fun setupClickListeners() {
        // 라디오 버튼 리스너
        rgDurationType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_unlimited -> layoutDurationInput.visibility = View.GONE
                R.id.rb_timed -> layoutDurationInput.visibility = View.VISIBLE
            }
        }
        
        // 휴식시간 체크박스 리스너
        cbAllowBreaks.setOnCheckedChangeListener { _, isChecked ->
            layoutBreakSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // 앱 선택 버튼
        btnSelectApps.setOnClickListener {
            val intent = Intent(this, ScheduleAppSelectionActivity::class.java).apply {
                putStringArrayListExtra("selected_apps", ArrayList(selectedApps))
            }
            startActivityForResult(intent, REQUEST_APP_SELECTION)
        }
        
        // 웹사이트 관리 버튼
        btnManageWebsites.setOnClickListener {
            showWebsiteManagementDialog { websites ->
                selectedWebsites.clear()
                selectedWebsites.addAll(websites)
                updateCountDisplay()
            }
        }

        // 저장 버튼
        btnSave.setOnClickListener {
            saveFocusMode()
        }
    }

    private fun updateCountDisplay() {
        tvSelectedAppsCount.text = "선택된 앱: ${selectedApps.size}개"
        tvSelectedWebsitesCount.text = "선택된 웹사이트: ${selectedWebsites.size}개"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_APP_SELECTION && resultCode == Activity.RESULT_OK) {
            val newSelectedApps = data?.getStringArrayListExtra("selected_apps") ?: arrayListOf()
            selectedApps.clear()
            selectedApps.addAll(newSelectedApps)
            updateCountDisplay()
        }
    }

    private fun showWebsiteManagementDialog(onWebsitesSelected: (List<String>) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_website_management, null)
        
        val etWebsiteUrl = dialogView.findViewById<EditText>(R.id.et_website_url)
        val btnAddWebsite = dialogView.findViewById<Button>(R.id.btn_add_website)
        val rvWebsites = dialogView.findViewById<RecyclerView>(R.id.rv_websites)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        
        val websiteAdapter = WebsiteAdapter { website ->
            (rvWebsites.adapter as WebsiteAdapter).removeWebsite(website)
        }
        
        rvWebsites.layoutManager = LinearLayoutManager(this)
        rvWebsites.adapter = websiteAdapter
        
        // 기존 웹사이트 목록 설정
        websiteAdapter.updateWebsites(selectedWebsites.toList())
        
        btnAddWebsite.setOnClickListener {
            val url = etWebsiteUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                websiteAdapter.addWebsite(url)
                etWebsiteUrl.text.clear()
            } else {
                Toast.makeText(this, "웹사이트 주소를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnConfirm.setOnClickListener {
            onWebsitesSelected(websiteAdapter.getWebsites())
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun saveFocusMode() {
        val name = etFocusModeName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "집중모드 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        val durationMinutes = if (rbUnlimited.isChecked) {
            0
        } else {
            val durationText = etDuration.text.toString().trim()
            if (durationText.isEmpty()) {
                Toast.makeText(this, "집중 시간을 입력해주세요", Toast.LENGTH_SHORT).show()
                return
            }
            try {
                durationText.toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "올바른 시간을 입력해주세요", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        val allowBreaks = cbAllowBreaks.isChecked
        var breakInterval = 30
        var breakDuration = 5
        
        if (allowBreaks) {
            val intervalText = etBreakInterval.text.toString().trim()
            val durationText = etBreakDuration.text.toString().trim()
            
            if (intervalText.isEmpty() || durationText.isEmpty()) {
                Toast.makeText(this, "휴식 시간 설정을 완료해주세요", Toast.LENGTH_SHORT).show()
                return
            }
            
            try {
                breakInterval = intervalText.toInt()
                breakDuration = durationText.toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "올바른 휴식 시간을 입력해주세요", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        val newFocusMode = if (focusMode != null) {
            focusMode!!.copy(
                name = name,
                durationMinutes = durationMinutes,
                blockedApps = selectedApps.toSet(),
                blockedWebsites = selectedWebsites.toSet(),
                allowBreaks = allowBreaks,
                breakIntervalMinutes = breakInterval,
                breakDurationMinutes = breakDuration
            )
        } else {
            FocusMode(
                name = name,
                durationMinutes = durationMinutes,
                blockedApps = selectedApps.toSet(),
                blockedWebsites = selectedWebsites.toSet(),
                allowBreaks = allowBreaks,
                breakIntervalMinutes = breakInterval,
                breakDurationMinutes = breakDuration
            )
        }
        
        if (focusMode != null) {
            settingsManager.updateFocusMode(newFocusMode)
            Toast.makeText(this, "'${name}' 집중모드가 수정되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            settingsManager.addFocusMode(newFocusMode)
            Toast.makeText(this, "'${name}' 집중모드가 추가되었습니다", Toast.LENGTH_SHORT).show()
        }
        
        setResult(RESULT_OK)
        finish()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}