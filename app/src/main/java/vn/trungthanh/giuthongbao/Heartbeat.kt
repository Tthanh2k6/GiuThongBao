package vn.trungthanh.giuthongbao

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ROM HyperOS nội địa đôi khi làm Google Play services quên đặt lại hẹn giờ nối lại
 * mtalk.google.com:5228 sau khi kết nối FCM rớt → mất thông báo hàng giờ.
 * Gửi heartbeat là cách ép nó nối lại (giống `adb shell am broadcast -a ...MCS_HEARTBEAT`).
 */
object Heartbeat {
    private const val GMS = "com.google.android.gms"
    private val ACTIONS = listOf(
        "com.google.android.intent.action.GTALK_HEARTBEAT",
        "com.google.android.intent.action.MCS_HEARTBEAT",
    )

    val INTERVALS_MIN = listOf(3, 5, 10, 15)
    private const val DEFAULT_INTERVAL_MIN = 5
    private const val MAX_LOG = 40

    @Volatile private var lastSentElapsed = 0L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("hb", Context.MODE_PRIVATE)

    fun intervalMin(ctx: Context) = prefs(ctx).getInt("interval", DEFAULT_INTERVAL_MIN)
    fun setIntervalMin(ctx: Context, min: Int) {
        prefs(ctx).edit().putInt("interval", min).apply()
        schedule(ctx)
    }

    fun count(ctx: Context) = prefs(ctx).getInt("count", 0)
    fun lastTime(ctx: Context) = prefs(ctx).getLong("last", 0L)
    fun log(ctx: Context): List<String> =
        prefs(ctx).getString("log", "")!!.lines().filter { it.isNotBlank() }

    /** Giờ (wall clock) dự kiến alarm kế tiếp nổ; 0 nếu chưa hẹn lần nào. */
    fun nextTime(ctx: Context) = prefs(ctx).getLong("next", 0L)

    data class Entry(val time: Long, val reason: String)

    fun entries(ctx: Context): List<Entry> {
        val fmt = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
        val now = System.currentTimeMillis()
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return log(ctx).mapNotNull { line ->
            val parts = line.split("  ", limit = 2)
            if (parts.size < 2) return@mapNotNull null
            val date = runCatching { fmt.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
            // Log không lưu năm: lấy năm nay, nếu rơi vào tương lai thì là cuối năm ngoái.
            val c = Calendar.getInstance().apply { time = date; set(Calendar.YEAR, year) }
            if (c.timeInMillis > now + 86_400_000) c.add(Calendar.YEAR, -1)
            Entry(c.timeInMillis, parts[1].trim())
        }
    }

    /** [minGapMs] chống gửi dồn khi mạng chập chờn hoặc bật/tắt màn liên tục. */
    fun send(ctx: Context, reason: String, minGapMs: Long = 0) {
        val now = SystemClock.elapsedRealtime()
        if (minGapMs > 0 && now - lastSentElapsed < minGapMs) return
        lastSentElapsed = now

        for (action in ACTIONS) {
            ctx.sendBroadcast(Intent(action))
            ctx.sendBroadcast(Intent(action).setPackage(GMS))
        }

        val p = prefs(ctx)
        val time = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())
        val log = (listOf("$time  $reason") + log(ctx)).take(MAX_LOG).joinToString("\n")
        p.edit()
            .putInt("count", p.getInt("count", 0) + 1)
            .putLong("last", System.currentTimeMillis())
            .putString("log", log)
            .apply()
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val delayMs = intervalMin(ctx) * 60_000L
        val at = SystemClock.elapsedRealtime() + delayMs
        prefs(ctx).edit().putLong("next", System.currentTimeMillis() + delayMs).apply()
        val exactOk = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exactOk) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi)
        }
    }
}
