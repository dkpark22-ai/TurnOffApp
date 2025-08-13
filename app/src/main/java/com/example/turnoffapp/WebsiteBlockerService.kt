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

    companion object {
        private var websiteTemporaryAllowEndTime: Long = 0
        
        fun allowWebsiteTemporarily(durationMillis: Long) {
            websiteTemporaryAllowEndTime = System.currentTimeMillis() + durationMillis
        }
        
        private fun isWebsiteTemporarilyAllowed(): Boolean {
            return System.currentTimeMillis() < websiteTemporaryAllowEndTime
        }
    }

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
        
        // 웹사이트가 임시 허용되었는지 먼저 확인
        if (isWebsiteTemporarilyAllowed()) {
            android.util.Log.d("WebsiteBlockerService", "Website temporarily allowed")
            return
        }
        
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
        // First, check for common resource IDs for URL bars.
        if (node.viewIdResourceName?.contains("url_bar") == true ||
            node.viewIdResourceName?.contains("address_bar") == true) {
            return node
        }

        // Fallback: Check for an EditText that likely contains a URL.
        // This is less reliable but can work on some browsers.
        if (node.className?.contains("EditText") == true) {
            val text = node.text?.toString() ?: ""
            // A simple heuristic: if it contains a dot and no spaces, it could be a URL.
            if (text.contains(".") && !text.contains(" ")) {
                return node
            }
        }

        // Recursively search in children.
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findUrlNode(child)
            if (result != null) {
                return result
            }
            // It's important to release the node info to avoid memory leaks
            child.recycle()
        }
        return null
    }

    private fun isWebsiteBlocked(url: String): Boolean {
        val isFocusActive = settingsManager.isFocusActive()
        val activeSchedule = settingsManager.getActiveSchedule()

        if (!isFocusActive && activeSchedule == null) {
            return false
        }

        val blockedWebsites = settingsManager.getActiveBlockedWebsites()
        val currentDomain = extractDomain(url)

        android.util.Log.d("WebsiteBlockerService", "Checking domain: $currentDomain")
        android.util.Log.d("WebsiteBlockerService", "Blocked websites: $blockedWebsites")

        if (currentDomain.isEmpty()) return false

        val isBlocked = blockedWebsites.any { blockedUrl ->
            val storedDomain = extractDomain(blockedUrl)
            val result = currentDomain.equals(storedDomain, ignoreCase = true)
            android.util.Log.d("WebsiteBlockerService", "Comparing '$currentDomain' with stored domain '$storedDomain': $result")
            result
        }

        android.util.Log.d("WebsiteBlockerService", "URL is blocked: $isBlocked")
        return isBlocked
    }

    private fun extractDomain(url: String): String {
        return try {
            val host = if (url.startsWith("http://") || url.startsWith("https://")) {
                java.net.URI(url).host
            } else {
                // 스키마가 없는 경우 임시로 추가하여 URI 파싱
                java.net.URI("http://$url").host
            }
            // 'www.' 접두사 제거
            host?.removePrefix("www.") ?: ""
        } catch (e: java.net.URISyntaxException) {
            // 정식 URL이 아닌 경우 (예: "dogdrip.net")
            // "www." 접두사 제거 후 반환
            url.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }

    private fun blockWebsite() {
        try {
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