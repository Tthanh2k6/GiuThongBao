package vn.trungthanh.giuthongbao

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Bg.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(NavBg.toArgb()),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        KeepService.start(this)
        Heartbeat.schedule(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Surface)) {
                App()
            }
        }
    }

    @Composable
    private fun App() {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var status by remember { mutableStateOf(readStatus()) }
        val refresh = { status = readStatus() }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000); refresh()
            }
        }
        BackHandler(enabled = tab != 0) { tab = 0 }

        Column(Modifier.fillMaxSize().background(Bg)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Crossfade(tab, label = "tab") { t ->
                    when (t) {
                        0 -> HomeScreen(
                            status,
                            onInterval = { Heartbeat.setIntervalMin(this@MainActivity, it); refresh() },
                            onWake = { Heartbeat.send(this@MainActivity, "bấm tay"); refresh() },
                            onOpenSetup = { tab = 1 },
                            onOpenLog = { tab = 2 },
                        )
                        1 -> SetupScreen(
                            status,
                            onIgnoreBattery = { requestIgnoreBattery() },
                            onAutostart = { markStep(STEP_AUTOSTART); openAutostart() },
                            onBatteryConfig = { markStep(STEP_BATTERY); openBatteryConfig() },
                        )
                        else -> LogScreen(status)
                    }
                }
            }
            BottomNav(tab, setupBadge = status.steps.size - status.stepsDone) { tab = it }
        }
    }

    private fun readStatus() = Status(
        now = System.currentTimeMillis(),
        next = Heartbeat.nextTime(this),
        last = Heartbeat.lastTime(this),
        count = Heartbeat.count(this),
        interval = Heartbeat.intervalMin(this),
        running = KeepService.running,
        entries = Heartbeat.entries(this),
        steps = listOf(isIgnoringBattery(), stepMarked(STEP_AUTOSTART), stepMarked(STEP_BATTERY)),
    )

    // HyperOS không cho đọc trạng thái Tự khởi động / chế độ pin, nên coi là xong khi người dùng đã mở màn đó.
    private fun setupPrefs() = getSharedPreferences("setup", Context.MODE_PRIVATE)
    private fun stepMarked(key: String) = setupPrefs().getBoolean(key, false)
    private fun markStep(key: String) = setupPrefs().edit().putBoolean(key, true).apply()

    private fun isIgnoringBattery() =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBattery() {
        tryStart(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }

    private fun openAutostart() {
        tryStart(Intent().setComponent(ComponentName(
            "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity",
        )))
    }

    private fun openBatteryConfig() {
        tryStart(Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY")
            .putExtra("package_name", packageName)
            .putExtra("package_label", "Giữ Thông Báo"))
    }

    /** Màn riêng của MIUI có thể đổi tên theo bản ROM; hỏng thì về trang thông tin app. */
    private fun tryStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private companion object {
        const val STEP_AUTOSTART = "autostart"
        const val STEP_BATTERY = "battery"
    }
}
