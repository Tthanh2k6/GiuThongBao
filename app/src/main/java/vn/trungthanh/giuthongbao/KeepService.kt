package vn.trungthanh.giuthongbao

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat

/**
 * Giữ tiến trình sống (HyperOS ít giết app có foreground service) và bắt hai thời điểm
 * FCM hay rớt: đổi mạng (Wi-Fi ↔ 4G) và mở máy sau một lúc ngủ.
 */
class KeepService : Service() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            Heartbeat.send(ctx, "mở máy", minGapMs = 2 * 60_000)
        }
    }

    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Heartbeat.send(this@KeepService, "có mạng mới", minGapMs = 20_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Chạy nền", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) },
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Đang giữ kết nối thông báo")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, 1, notif, type)

        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(netCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        running = false
        unregisterReceiver(screenReceiver)
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(netCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "keep"

        /** Cùng tiến trình với MainActivity nên đọc thẳng được để hiện trạng thái. */
        @Volatile var running = false
            private set

        fun start(ctx: Context) {
            try {
                ctx.startForegroundService(Intent(ctx, KeepService::class.java))
            } catch (_: Exception) {
                // Android chặn mở foreground service từ nền ở vài thời điểm; alarm sau sẽ thử lại.
            }
        }
    }
}
