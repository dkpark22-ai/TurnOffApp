package com.example.turnoffapp

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WebsiteBlockerService : AccessibilityService() {

    private lateinit var settingsManager: SettingsManager
    private val browserPackages = setOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.brave.browser"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsManager = SettingsManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        val packageName = event.packageName?.toString()
        if (packageName in browserPackages) {
            checkWebsiteBlocking(event)
        }
    }

    private fun checkWebsiteBlocking(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val rootNode = rootInActiveWindow ?: return
                checkUrlInBrowser(rootNode)
            }
        }
    }

    private fun checkUrlInBrowser(rootNode: AccessibilityNodeInfo) {
        val url = extractUrlFromBrowser(rootNode)
        android.util.Log.d("WebsiteBlockerService", "Extracted URL: $url")
        
        if (url != null && isWebsiteBlocked(url)) {
            android.util.Log.d("WebsiteBlockerService", "Blocking website: $url")
            blockWebsite()
        }
    }

    private fun extractUrlFromBrowser(node: AccessibilityNodeInfo): String? {
        return try {
            findUrlNode(node)?.text?.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun findUrlNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.viewIdResourceName?.contains("url_bar") == true ||
            node.viewIdResourceName?.contains("address_bar") == true ||
            node.className?.contains("EditText") == true && 
            node.text?.toString()?.startsWith("http") == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findUrlNode(child)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun isWebsiteBlocked(url: String): Boolean {
        val blockedWebsites = settingsManager.getActiveBlockedWebsites()
        
        android.util.Log.d("WebsiteBlockerService", "Checking URL: $url")
        android.util.Log.d("WebsiteBlockerService", "Blocked websites: $blockedWebsites")
        
        val isBlocked = blockedWebsites.any { blockedUrl ->
            val domain = extractDomain(blockedUrl)
            val result = url.contains(domain, ignoreCase = true)
            android.util.Log.d("WebsiteBlockerService", "Checking domain '$domain' in URL: $result")
            result
        }
        
        android.util.Log.d("WebsiteBlockerService", "URL is blocked: $isBlocked")
        return isBlocked
    }

    private fun extractDomain(url: String): String {
        return try {
            val cleanUrl = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
            cleanUrl.split("/")[0]
        } catch (e: Exception) {
            url
        }
    }

    private fun blockWebsite() {
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            
            val intent = Intent(this, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("BLOCKED_WEBSITE", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
    }
}