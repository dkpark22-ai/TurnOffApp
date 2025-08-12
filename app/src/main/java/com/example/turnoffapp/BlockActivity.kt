package com.example.turnoffapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BlockActivity : AppCompatActivity() {

    private lateinit var tvBlockTitle: TextView
    private lateinit var btnAllowTemporarily: Button
    private lateinit var btnBackToHome: Button
    
    private var blockedAppPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        initViews()
        setupData()
        setupClickListeners()
        
        preventBackNavigation()
    }

    private fun initViews() {
        tvBlockTitle = findViewById(R.id.tv_block_title)
        btnAllowTemporarily = findViewById(R.id.btn_allow_temporarily)
        btnBackToHome = findViewById(R.id.btn_back_to_home)
    }

    private fun setupData() {
        blockedAppPackage = intent.getStringExtra("BLOCKED_APP_PACKAGE")
        
        if (blockedAppPackage != null) {
            tvBlockTitle.text = getString(R.string.app_blocked_title)
        } else {
            tvBlockTitle.text = getString(R.string.website_blocked_title)
        }
    }

    private fun setupClickListeners() {
        btnAllowTemporarily.setOnClickListener {
            allowTemporarily()
        }

        btnBackToHome.setOnClickListener {
            backToHome()
        }
    }

    private fun allowTemporarily() {
        blockedAppPackage?.let { packageName ->
            val tenMinutesInMillis = 10 * 60 * 1000L
            FocusService.allowAppTemporarily(packageName, tenMinutesInMillis)
            
            Toast.makeText(this, getString(R.string.temporary_allowed), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun backToHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun preventBackNavigation() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onBackPressed() {
        backToHome()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        setupData()
    }
}