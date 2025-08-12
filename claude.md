

# **📱 디지털 디톡스 앱 (Kotlin 기반)**

사용자의 집중력 향상과 스마트폰 사용 시간 관리를 돕기 위해, 지정된 시간 동안 특정 앱과 웹사이트 접근을 제한하는 안드로이드 애플리케이션입니다.

## **🎯 핵심 기능**

1. **앱 차단 기능**  
   * 사용자는 자신의 스마트폰에 설치된 앱 목록에서 차단하고 싶은 앱을 선택할 수 있습니다.  
   * **타이머 모드**: 한 번만 특정 시간(예: 1시간) 동안 앱을 차단할 수 있습니다.  
   * **스케줄 모드**: 매주 특정 요일과 시간대(예: 평일 오전 9시\~오후 6시)에 앱이 자동으로 차단되도록 설정할 수 있습니다.  
2. **웹사이트 차단 기능**  
   * 사용자가 차단하고 싶은 웹사이트 주소(URL)를 직접 입력하여 목록에 추가할 수 있습니다.  
   * 앱 차단과 동일하게 타이머 또는 스케줄에 따라 지정된 웹사이트 접속을 차단합니다.  
3. **차단 화면 제공**  
   * 차단된 앱을 실행하거나 웹사이트에 접속하려고 하면, 사용자 정의 메시지가 포함된 화면을 보여줍니다.  
4. **✨ 잠시 허용 기능**  
   * 차단 화면에서 **"10분만 사용하기"** 와 같은 버튼을 제공합니다.  
   * 사용자가 이 버튼을 누르면, 해당 앱을 설정된 시간(예: 10분) 동안 일시적으로 사용할 수 있습니다.  
   * 허용 시간이 지나면 해당 앱은 다시 자동으로 차단됩니다.  
5. **집중 모드 (Focus Mode)**  
   * 한 번의 탭으로 미리 설정된 앱과 웹사이트 목록을 즉시 차단하는 기능입니다.  
6. **휴식 시간 허용**  
   * 긴 집중 시간 중간에 짧은 휴식 시간(예: 5분)을 설정하여 일시적으로 차단을 해제할 수 있는 유연성을 제공합니다.

---

## **🛠️ 기술 구현 방안**

### **1\. 앱 차단 및 '잠시 허용' 구현**

앱 사용을 감지하기 위해 안드로이드의 **UsageStatsManager** API를 사용합니다. 여기에 '잠시 허용'을 위한 임시 예외 처리 로직을 추가합니다.

* **필요 권한**: PACKAGE\_USAGE\_STATS (사용 정보 접근 권한)  
* **구현 순서**:  
  1. 차단 안내 화면(BlockActivity)에 "10분만 사용하기" 버튼을 추가합니다.  
  2. 사용자가 이 버튼을 누르면, **임시 허용 목록**(예: Map\<String, Long\>)에 해당 앱의 패키지 이름과 허용 만료 시간(현재 시간 \+ 10분)을 저장합니다.  
  3. 백그라운드에서 계속 실행될 \*\*ForegroundService\*\*는 주기적으로 현재 실행 중인 앱을 확인합니다.  
  4. 실행 중인 앱이 차단 목록에 포함되어 있더라도, **임시 허용 목록에 있고 아직 만료 시간이 지나지 않았다면 차단하지 않고 건너뜁니다.**  
  5. 만료 시간이 지났다면, 해당 앱을 임시 허용 목록에서 제거하고 다시 차단을 시작합니다.

#### **예시 코드 구조 (FocusService.kt)**

Kotlin

import android.app.Service  
import android.content.Intent  
import android.os.IBinder  
import kotlinx.coroutines.\*  
import android.app.usage.UsageStatsManager  
import android.content.Context  
import java.util.concurrent.ConcurrentHashMap

class FocusService : Service() {

    private val job \= SupervisorJob()  
    private val scope \= CoroutineScope(Dispatchers.IO \+ job)  
    private lateinit var usageStatsManager: UsageStatsManager

    // Companion object를 사용해 임시 허용 목록을 다른 곳에서 접근 가능하게 함  
    companion object {  
        // 동시성 문제를 피하기 위해 ConcurrentHashMap 사용  
        val temporaryWhitelist \= ConcurrentHashMap\<String, Long\>() // \<PackageName, ExpiryTimestamp\>

        fun allowAppTemporarily(packageName: String, durationMillis: Long) {  
            val expiryTime \= System.currentTimeMillis() \+ durationMillis  
            temporaryWhitelist\[packageName\] \= expiryTime  
        }  
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {  
        usageStatsManager \= getSystemService(Context.USAGE\_STATS\_SERVICE) as UsageStatsManager

        scope.launch {  
            while (true) {  
                checkForegroundApp()  
                delay(500) // 0.5초마다 체크  
            }  
        }  
        // Foreground Service로 실행하기 위한 Notification 설정이 여기에 필요합니다.  
        return START\_STICKY  
    }

    private fun checkForegroundApp() {  
        val currentTime \= System.currentTimeMillis()

        // 만료된 임시 허용 앱이 있다면 목록에서 정리  
        temporaryWhitelist.entries.removeIf { it.value \< currentTime }

        val stats \= usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL\_DAILY, currentTime \- 1000 \* 10, currentTime)  
        if (stats \!= null && stats.isNotEmpty()) {  
            val currentApp \= stats.sortedBy { it.lastTimeUsed }.last().packageName

            // 1\. 임시 허용 목록에 있는지 먼저 확인  
            if (temporaryWhitelist.containsKey(currentApp)) {  
                return // 임시 허용된 앱이므로 차단 로직을 실행하지 않음  
            }

            // 2\. 차단 목록에 있고, 임시 허용된 앱이 아니라면 차단  
            // 'blockedApps'는 사용자가 설정한 영구 차단 앱 목록 (e.g., Set\<String\>)  
            if (currentApp in blockedApps) {  
                val intent \= Intent(this, BlockActivity::class.java).apply {  
                    addFlags(Intent.FLAG\_ACTIVITY\_NEW\_TASK)  
                    // 차단된 앱 정보를 BlockActivity로 전달해 '잠시 허용' 시 어떤 앱인지 알 수 있게 함  
                    putExtra("BLOCKED\_APP\_PACKAGE", currentApp)  
                }  
                startActivity(intent)  
            }  
        }  
    }

    override fun onBind(intent: Intent?): IBinder? \= null

    override fun onDestroy() {  
        super.onDestroy()  
        job.cancel()  
    }  
}

### **2\. 웹사이트 차단 구현**

웹사이트 차단은 **AccessibilityService** (접근성 서비스)를 사용하는 것이 가장 효과적입니다. 접근성 서비스는 다른 앱의 UI 요소를 읽을 수 있어, 웹 브라우저의 주소창(URL)을 확인할 수 있습니다.

* **필요 권한**: BIND\_ACCESSIBILITY\_SERVICE (접근성 서비스 바인딩 권한)  
* **구현 순서**:  
  1. AccessibilityService를 상속받는 서비스를 만듭니다.  
  2. onAccessibilityEvent() 콜백 함수에서 이벤트가 발생한 노드 트리를 탐색하여 브라우저의 URL을 읽어옵니다.  
  3. 읽어온 URL이 차단 목록에 포함되어 있다면 performGlobalAction(GLOBAL\_ACTION\_BACK)을 통해 '뒤로 가기'를 실행하거나 차단 안내 화면을 띄웁니다.  
  4. 웹사이트의 '잠시 허용' 기능 또한 앱과 마찬가지로 공유된 임시 허용 목록을 확인하는 로직으로 구현할 수 있습니다.

---

## **💡 추가 고려사항**

1. **사용자 권한 획득**:  
   * UsageStatsManager와 AccessibilityService는 매우 민감한 권한입니다. 사용자가 왜 이 권한이 필요한지 명확히 이해하고 직접 시스템 설정에서 활성화하도록 친절하게 안내하는 UI/UX가 필수적입니다.  
2. **배터리 소모**:  
   * 백그라운드에서 계속 실행되는 서비스는 배터리를 소모합니다. 화면이 꺼져 있을 때는 검사 주기를 늘리거나, WorkManager API를 활용하여 최적화하는 방안을 고려해야 합니다.  
3. **서비스 안정성**:  
   * 안드로이드 시스템은 메모리가 부족하면 백그라운드 서비스를 강제로 종료할 수 있습니다. ForegroundService로 만들고 상단 바에 항상 알림(Notification)을 표시하여 서비스가 종료될 확률을 크게 낮춰야 합니다.  
4. **'잠시 허용' 남용 방지**:  
   * 사용자가 '잠시 허용' 기능을 너무 자주 사용하는 것을 막기 위해, 하루에 허용되는 횟수를 제한하거나, 한 번 사용하면 일정 시간 동안은 다시 사용할 수 없도록 쿨타임을 두는 정책을 추가할 수 있습니다.  
5. **남은 시간 표시**:  
   * 앱을 잠시 허용했을 때, 화면 위에 작은 오버레이 창이나 상단 알림을 통해 "남은 허용 시간: 8분 32초" 와 같이 남은 시간을 시각적으로 보여주면 사용자가 시간을 더 잘 인지하고 관리할 수 있습니다. (이 기능을 위해서는 SYSTEM\_ALERT\_WINDOW 권한이 추가로 필요할 수 있습니다.)  
6. **AndroidManifest.xml 설정**:  
   * 아래와 같이 Service와 Permission을 AndroidManifest.xml에 정확하게 선언해야 합니다.  
7. XML

\<uses-permission android:name\="android.permission.PACKAGE\_USAGE\_STATS" tools:ignore\="ProtectedPermissions" /\>  
\<uses-permission android:name\="android.permission.FOREGROUND\_SERVICE" /\>  
\<uses-permission android:name\="android.permission.SYSTEM\_ALERT\_WINDOW"/\>

\<application ...\>  
    \<service  
        android:name\=".FocusService"  
        android:enabled\="true"  
        android:exported\="false" /\>

    \<service  
        android:name\=".WebsiteBlockerService"  
        android:permission\="android.permission.BIND\_ACCESSIBILITY\_SERVICE"  
        android:label\="@string/accessibility\_service\_label"  
        android:exported\="false"\>  
        \<intent-filter\>  
            \<action android:name\="android.accessibilityservice.AccessibilityService" /\>  
        \</intent-filter\>  
        \<meta-data  
            android:name\="android.accessibilityservice"  
            android:resource\="@xml/accessibility\_service\_config" /\>  
    \</service\>

    \<activity android:name\=".BlockActivity" ... /\>  
\</application\>

8.   
9. 

