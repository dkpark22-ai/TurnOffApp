package com.example.turnoffapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FocusModeManagementActivity : AppCompatActivity() {

    private lateinit var btnAddFocusMode: Button
    private lateinit var rvFocusModes: RecyclerView
    private lateinit var tvEmptyMessage: TextView

    private lateinit var settingsManager: SettingsManager
    private lateinit var focusModeAdapter: FocusModeManagementAdapter

    companion object {
        private const val REQUEST_FOCUS_MODE_EDIT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_mode_management)

        initViews()
        initAdapter()
        initSettingsManager()
        setupClickListeners()
        updateFocusModeList()
    }

    private fun initViews() {
        btnAddFocusMode = findViewById(R.id.btn_add_focus_mode)
        rvFocusModes = findViewById(R.id.rv_focus_modes)
        tvEmptyMessage = findViewById(R.id.tv_empty_message)
    }

    private fun initAdapter() {
        focusModeAdapter = FocusModeManagementAdapter(
            onEditFocusMode = { focusMode ->
                val intent = Intent(this, FocusModeEditActivity::class.java).apply {
                    putExtra("focus_mode_id", focusMode.id)
                }
                startActivityForResult(intent, REQUEST_FOCUS_MODE_EDIT)
            },
            onDeleteFocusMode = { focusMode ->
                showDeleteConfirmDialog(focusMode)
            }
        )

        rvFocusModes.layoutManager = LinearLayoutManager(this)
        rvFocusModes.adapter = focusModeAdapter
    }

    private fun initSettingsManager() {
        settingsManager = SettingsManager(this)
    }

    private fun setupClickListeners() {
        btnAddFocusMode.setOnClickListener {
            val intent = Intent(this, FocusModeEditActivity::class.java)
            startActivityForResult(intent, REQUEST_FOCUS_MODE_EDIT)
        }
    }

    private fun updateFocusModeList() {
        val focusModes = settingsManager.getFocusModes()
        focusModeAdapter.updateFocusModes(focusModes)
        
        if (focusModes.isEmpty()) {
            rvFocusModes.visibility = View.GONE
            tvEmptyMessage.visibility = View.VISIBLE
        } else {
            rvFocusModes.visibility = View.VISIBLE
            tvEmptyMessage.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FOCUS_MODE_EDIT && resultCode == RESULT_OK) {
            updateFocusModeList()
        }
    }

    private fun showDeleteConfirmDialog(focusMode: FocusMode) {
        AlertDialog.Builder(this)
            .setTitle("집중모드 삭제")
            .setMessage("'${focusMode.name}' 집중모드를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteFocusMode(focusMode)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteFocusMode(focusMode: FocusMode) {
        settingsManager.removeFocusMode(focusMode.id)
        updateFocusModeList()
        Toast.makeText(this, "'${focusMode.name}' 집중모드가 삭제되었습니다", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateFocusModeList()
    }
}