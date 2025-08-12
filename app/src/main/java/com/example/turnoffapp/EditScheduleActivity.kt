package com.example.turnoffapp

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class EditScheduleActivity : AppCompatActivity() {

    private lateinit var etScheduleName: EditText
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var cbSunday: CheckBox
    private lateinit var cbMonday: CheckBox
    private lateinit var cbTuesday: CheckBox
    private lateinit var cbWednesday: CheckBox
    private lateinit var cbThursday: CheckBox
    private lateinit var cbFriday: CheckBox
    private lateinit var cbSaturday: CheckBox
    private lateinit var btnSelectApps: Button
    private lateinit var btnSelectWebsites: Button
    private lateinit var tvSelectedApps: TextView
    private lateinit var tvSelectedWebsites: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var settingsManager: SettingsManager
    private var currentSchedule: Schedule? = null
    private var startTime = "09:00"
    private var endTime = "18:00"
    private var selectedApps: MutableSet<String> = mutableSetOf()
    private var selectedWebsites: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_schedule)

        initViews()
        initSettingsManager()
        loadScheduleData()
        setupClickListeners()
    }

    private fun initViews() {
        etScheduleName = findViewById(R.id.et_schedule_name)
        btnStartTime = findViewById(R.id.btn_start_time)
        btnEndTime = findViewById(R.id.btn_end_time)
        cbSunday = findViewById(R.id.cb_sunday)
        cbMonday = findViewById(R.id.cb_monday)
        cbTuesday = findViewById(R.id.cb_tuesday)
        cbWednesday = findViewById(R.id.cb_wednesday)
        cbThursday = findViewById(R.id.cb_thursday)
        cbFriday = findViewById(R.id.cb_friday)
        cbSaturday = findViewById(R.id.cb_saturday)
        btnSelectApps = findViewById(R.id.btn_select_apps)
        btnSelectWebsites = findViewById(R.id.btn_select_websites)
        tvSelectedApps = findViewById(R.id.tv_selected_apps)
        tvSelectedWebsites = findViewById(R.id.tv_selected_websites)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
    }

    private fun initSettingsManager() {
        settingsManager = SettingsManager(this)
    }

    private fun loadScheduleData() {
        val scheduleId = intent.getStringExtra("schedule_id")
        if (scheduleId != null) {
            currentSchedule = settingsManager.getSchedules().find { it.id == scheduleId }
            currentSchedule?.let { schedule ->
                etScheduleName.setText(schedule.name)
                startTime = schedule.startTime
                endTime = schedule.endTime
                selectedApps = schedule.blockedApps.toMutableSet()
                selectedWebsites = schedule.blockedWebsites.toMutableSet()
                
                updateTimeButtons()
                updateDayCheckboxes(schedule.daysOfWeek)
                updateSelectedLists()
            }
        } else {
            // 기본값 설정
            etScheduleName.setText("새 스케줄")
            updateTimeButtons()
            updateSelectedLists()
        }
    }

    private fun updateTimeButtons() {
        btnStartTime.text = "시작: $startTime"
        btnEndTime.text = "종료: $endTime"
    }

    private fun updateDayCheckboxes(daysOfWeek: Set<Int>) {
        cbSunday.isChecked = daysOfWeek.contains(1)
        cbMonday.isChecked = daysOfWeek.contains(2)
        cbTuesday.isChecked = daysOfWeek.contains(3)
        cbWednesday.isChecked = daysOfWeek.contains(4)
        cbThursday.isChecked = daysOfWeek.contains(5)
        cbFriday.isChecked = daysOfWeek.contains(6)
        cbSaturday.isChecked = daysOfWeek.contains(7)
    }

    private fun updateSelectedLists() {
        // 선택된 앱 목록 표시
        if (selectedApps.isEmpty()) {
            tvSelectedApps.text = "선택된 앱이 없습니다"
        } else {
            val appNames = selectedApps.mapNotNull { packageName ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }
            }
            tvSelectedApps.text = appNames.joinToString(", ")
        }

        // 선택된 웹사이트 목록 표시
        if (selectedWebsites.isEmpty()) {
            tvSelectedWebsites.text = "선택된 웹사이트가 없습니다"
        } else {
            tvSelectedWebsites.text = selectedWebsites.joinToString(", ")
        }
    }

    private fun setupClickListeners() {
        btnStartTime.setOnClickListener {
            showTimePicker(true)
        }

        btnEndTime.setOnClickListener {
            showTimePicker(false)
        }

        btnSelectApps.setOnClickListener {
            selectApps()
        }

        btnSelectWebsites.setOnClickListener {
            selectWebsites()
        }

        btnSave.setOnClickListener {
            saveSchedule()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val currentTime = if (isStartTime) startTime else endTime
        val timeParts = currentTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val time = String.format("%02d:%02d", selectedHour, selectedMinute)
            if (isStartTime) {
                startTime = time
            } else {
                endTime = time
            }
            updateTimeButtons()
        }, hour, minute, true).show()
    }

    private fun selectApps() {
        val intent = Intent(this, ScheduleAppSelectionActivity::class.java)
        intent.putStringArrayListExtra("selected_apps", ArrayList(selectedApps))
        startActivityForResult(intent, REQUEST_CODE_SELECT_APPS)
    }

    private fun selectWebsites() {
        // 간단한 다이얼로그로 웹사이트 URL 입력
        val builder = android.app.AlertDialog.Builder(this)
        val editText = android.widget.EditText(this)
        editText.hint = "웹사이트 URL 입력"
        
        builder.setTitle("웹사이트 추가")
            .setView(editText)
            .setPositiveButton("추가") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    val formattedUrl = if (!url.startsWith("http")) "https://$url" else url
                    selectedWebsites.add(formattedUrl)
                    updateSelectedLists()
                    Toast.makeText(this, "웹사이트가 추가되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun saveSchedule() {
        val name = etScheduleName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "스케줄 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val daysOfWeek = getSelectedDaysOfWeek()
        if (daysOfWeek.isEmpty()) {
            Toast.makeText(this, "요일을 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val schedule = if (currentSchedule != null) {
            currentSchedule!!.copy(
                name = name,
                startTime = startTime,
                endTime = endTime,
                daysOfWeek = daysOfWeek,
                blockedApps = selectedApps,
                blockedWebsites = selectedWebsites
            )
        } else {
            Schedule(
                name = name,
                startTime = startTime,
                endTime = endTime,
                daysOfWeek = daysOfWeek,
                blockedApps = selectedApps,
                blockedWebsites = selectedWebsites
            )
        }

        if (currentSchedule != null) {
            settingsManager.updateSchedule(schedule)
        } else {
            settingsManager.addSchedule(schedule)
        }

        Toast.makeText(this, "스케줄이 저장되었습니다", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun getSelectedDaysOfWeek(): Set<Int> {
        val days = mutableSetOf<Int>()
        if (cbSunday.isChecked) days.add(1)
        if (cbMonday.isChecked) days.add(2)
        if (cbTuesday.isChecked) days.add(3)
        if (cbWednesday.isChecked) days.add(4)
        if (cbThursday.isChecked) days.add(5)
        if (cbFriday.isChecked) days.add(6)
        if (cbSaturday.isChecked) days.add(7)
        return days
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_SELECT_APPS -> {
                    val apps = data.getStringArrayListExtra("selected_apps") ?: emptyList()
                    selectedApps = apps.toMutableSet()
                    updateSelectedLists()
                    Toast.makeText(this, "${selectedApps.size}개 앱이 선택되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SELECT_APPS = 1001
    }
}