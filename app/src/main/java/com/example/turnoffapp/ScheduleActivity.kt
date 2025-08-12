package com.example.turnoffapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScheduleActivity : AppCompatActivity() {

    private lateinit var rvSchedules: RecyclerView
    private lateinit var btnAddSchedule: Button
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        initViews()
        initSettingsManager()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun initViews() {
        rvSchedules = findViewById(R.id.rv_schedules)
        btnAddSchedule = findViewById(R.id.btn_add_schedule)
    }

    private fun initSettingsManager() {
        settingsManager = SettingsManager(this)
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(
            onEditClick = { schedule ->
                editSchedule(schedule)
            },
            onDeleteClick = { schedule ->
                deleteSchedule(schedule)
            },
            onToggleClick = { schedule ->
                toggleSchedule(schedule)
            }
        )
        
        rvSchedules.layoutManager = LinearLayoutManager(this)
        rvSchedules.adapter = scheduleAdapter
        
        updateScheduleList()
    }

    private fun setupClickListeners() {
        btnAddSchedule.setOnClickListener {
            addNewSchedule()
        }
    }

    private fun updateScheduleList() {
        val schedules = settingsManager.getSchedules()
        scheduleAdapter.updateSchedules(schedules)
    }

    private fun addNewSchedule() {
        val intent = Intent(this, EditScheduleActivity::class.java)
        startActivity(intent)
    }

    private fun editSchedule(schedule: Schedule) {
        val intent = Intent(this, EditScheduleActivity::class.java)
        intent.putExtra("schedule_id", schedule.id)
        startActivity(intent)
    }

    private fun deleteSchedule(schedule: Schedule) {
        settingsManager.removeSchedule(schedule.id)
        updateScheduleList()
    }

    private fun toggleSchedule(schedule: Schedule) {
        val updatedSchedule = schedule.copy(isEnabled = !schedule.isEnabled)
        settingsManager.updateSchedule(updatedSchedule)
        updateScheduleList()
    }

    override fun onResume() {
        super.onResume()
        updateScheduleList()
    }
}