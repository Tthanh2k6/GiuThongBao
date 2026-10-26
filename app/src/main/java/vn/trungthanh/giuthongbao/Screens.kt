package vn.trungthanh.giuthongbao

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Ảnh chụp trạng thái app, đọc lại mỗi giây. */
data class Status(
    val now: Long,
    val next: Long,
    val last: Long,
    val count: Int,
    val interval: Int,
    val running: Boolean,
    val entries: List<Heartbeat.Entry>,
    /** Bỏ tối ưu pin · Tự khởi động · Pin không hạn chế */
    val steps: List<Boolean>,
) {
    val stepsDone get() = steps.count { it }
}

private fun fmt(pattern: String, t: Long) = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(t))

private fun minutesAgo(now: Long, t: Long) = ((now - t) / 60_000).coerceAtLeast(0)

private fun dayStart(t: Long) = Calendar.getInstance().apply {
    timeInMillis = t
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun deviceLabel(): String {
    val maker = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val rom = if (maker.equals("Xiaomi", ignoreCase = true)) "HyperOS" else "Android ${Build.VERSION.RELEASE}"
    return "$maker · $rom"
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Spacer(Modifier.statusBarsPadding().height(24.dp))
    Text(title, color = Fg, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
}

// ───────────────────────────── 01 — Tổng quan ─────────────────────────────

@Composable
fun HomeScreen(
    s: Status,
    onInterval: (Int) -> Unit,
    onWake: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenLog: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.statusBarsPadding().height(20.dp))
        Header(s.running)
        Spacer(Modifier.height(20.dp))
        HeroCard(s)
        Spacer(Modifier.height(26.dp))
        Text(
            "Chu kỳ đánh thức", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        IntervalPicker(s.interval, onInterval)
        Spacer(Modifier.height(20.dp))
        WakeButton(onWake)
        if (s.stepsDone < s.steps.size) {
            Spacer(Modifier.height(20.dp))
            SetupCard(s.stepsDone, s.steps.size, onOpenSetup)
        }
        Spacer(Modifier.height(14.dp))
        LastLog(s.entries.firstOrNull(), onOpenLog)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Header(running: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Surface)
                .border(1.dp, Line, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) { LineIcon(Ic.BELL, Accent) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Giữ Thông Báo", color = Fg, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            Text(deviceLabel(), color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        val pill = if (running) Success else Warning
        Row(
            Modifier.height(28.dp).clip(CircleShape).background(pill.copy(alpha = 0.14f)).padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(pill))
            Spacer(Modifier.width(6.dp))
            Text(
                if (running) "Đang chạy" else "Đã dừng",
                color = pill, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HeroCard(s: Status) {
    val total = s.interval * 60_000L
    val left = if (s.next == 0L) 0L else (s.next - s.now).coerceIn(0L, total)
    val progress = if (total == 0L) 0f else left.toFloat() / total
    val countdown = if (s.next == 0L) "--:--" else "%02d:%02d".format(left / 60_000, left / 1000 % 60)

    Column(Modifier.card(24.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(212.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(220.dp)) {
                val c = center
                drawCircle(
                    Brush.radialGradient(
                        listOf(Accent.copy(alpha = 0.16f), Accent.copy(alpha = 0f)), center = c, radius = 110.dp.toPx(),
                    ),
                    radius = 110.dp.toPx(), center = c,
                )
                val r = 86.dp.toPx()
                val w = 12.dp.toPx()
                drawCircle(Line, radius = r, center = c, style = Stroke(w))
                drawArc(
                    Accent, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                    style = Stroke(w, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lần đánh thức tới", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    countdown, color = Fg, fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp, style = TextStyle(fontFeatureSettings = "tnum"),
                )
                Text("chu kỳ ${s.interval} phút", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        HDivider(Modifier.padding(horizontal = 16.dp))
        Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
            val ago = when {
                s.last == 0L -> "chưa gửi"
                minutesAgo(s.now, s.last) < 1 -> "vừa xong"
                minutesAgo(s.now, s.last) < 60 -> "${minutesAgo(s.now, s.last)} phút trước"
                else -> "${minutesAgo(s.now, s.last) / 60} giờ trước"
            }
            Stat("Gần nhất", ago, Modifier.weight(1f).padding(start = 24.dp))
            Box(Modifier.width(1.dp).height(42.dp).background(Line))
            Stat(
                "Tổng số lần", NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(s.count),
                Modifier.weight(1f).padding(start = 20.dp),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Fg, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IntervalPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.card(14.dp).height(48.dp).padding(4.dp)) {
        Heartbeat.INTERVALS_MIN.forEach { m ->
            val on = m == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp))
                    .background(if (on) Accent else Color.Transparent)
                    .clickable { onSelect(m) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$m phút", color = if (on) Color.Black else Fg, fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun WakeButton(onClick: () -> Unit) {
    Row(
        Modifier.card(16.dp, Accent).height(56.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIcon(Ic.BOLT, Color.Black, 18.dp, fill = true)
        Spacer(Modifier.width(8.dp))
        Text("Đánh thức ngay", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SetupCard(done: Int, total: Int, onClick: () -> Unit) {
    Row(
        Modifier.card().border(1.dp, Warning.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(Ic.ALERT, Warning, box = 44.dp, radius = 12.dp, icon = 24.dp, alpha = 0.15f)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Chưa thiết lập xong", color = Fg, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("HyperOS có thể tắt app · $done/$total bước", color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Line)) {
                Box(Modifier.fillMaxWidth(done.toFloat() / total).height(4.dp).clip(CircleShape).background(Warning))
            }
        }
        Spacer(Modifier.width(16.dp))
        LineIcon(Ic.CHEVRON, Muted, 20.dp)
    }
}

@Composable
private fun LastLog(e: Heartbeat.Entry?, onOpenLog: () -> Unit) {
    Row(
        Modifier.card(12.dp, Surface.copy(alpha = 0.6f)).height(36.dp).clickable(onClick = onOpenLog)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (e != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(kindOf(e.reason).color))
            Spacer(Modifier.width(8.dp))
            Text(fmt("HH:mm:ss", e.time), color = Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(12.dp))
            Text(
                e.reason, color = Fg, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
        } else {
            Text("Chưa có lần đánh thức nào", color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
        Text("Nhật ký ›", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ───────────────────────────── 02 — Thiết lập ─────────────────────────────

@Composable
fun SetupScreen(
    s: Status,
    onIgnoreBattery: () -> Unit,
    onAutostart: () -> Unit,
    onBatteryConfig: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenTitle(
            "Thiết lập",
            "HyperOS hay tự tắt app chạy nền. Làm đủ ${s.steps.size} bước dưới đây để thông báo luôn đến đúng giờ.",
        )
        Spacer(Modifier.height(22.dp))

        Row(Modifier.card().height(72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tiến độ", color = Muted, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${s.stepsDone}/${s.steps.size} hoàn tất", color = Fg, fontSize = 18.sp, lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(s.steps.size) { i ->
                    Box(
                        Modifier.size(40.dp, 8.dp).clip(CircleShape)
                            .background(if (i < s.stepsDone) Success else Line),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StepCard(
                Ic.BATTERY, "Bỏ tối ưu pin",
                "Cho phép app chạy nền, không bị Android giới hạn khi tắt màn hình.",
                done = s.steps[0], onClick = onIgnoreBattery.takeUnless { s.steps[0] },
            )
            StepCard(
                Ic.POWER, "Bật Tự khởi động",
                "Để app tự chạy lại sau khi khởi động máy hoặc bị dọn khỏi bộ nhớ.",
                done = s.steps[1], onClick = onAutostart,
            )
            StepCard(
                Ic.LOCK, "Pin: Không hạn chế",
                "Trong Tiết kiệm pin, chọn “Không hạn chế” cho Giữ Thông Báo.",
                done = s.steps[2], onClick = onBatteryConfig,
            )
        }
        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.card(color = Accent.copy(alpha = 0.07f))
                .border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            LineIcon(Ic.INFO, Accent, 20.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Mẹo: khoá app trong Đa nhiệm", color = Fg, fontSize = 14.sp, lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mở Đa nhiệm, nhấn giữ thẻ Giữ Thông Báo rồi chọn biểu tượng ổ khoá.",
                    color = Muted, fontSize = 13.sp, lineHeight = 19.sp,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StepCard(icon: String, title: String, desc: String, done: Boolean, onClick: (() -> Unit)?) {
    val tint = if (done) Success else Accent
    Row(
        Modifier.card().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        IconTile(icon, tint, box = 40.dp, radius = 12.dp, icon = 20.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, color = Fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (done) {
                    Row(
                        Modifier.height(26.dp).clip(CircleShape).background(Success.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LineIcon(Ic.CHECK, Success, 14.dp, stroke = 2.5.dp)
                        Spacer(Modifier.width(4.dp))
                        Text("Đã xong", color = Success, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Box(
                        Modifier.size(60.dp, 30.dp).clip(CircleShape).background(Accent),
                        contentAlignment = Alignment.Center,
                    ) { Text("Mở", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(desc, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

// ───────────────────────────── 03 — Nhật ký ─────────────────────────────

private val FILTERS = listOf(
    "Tất cả" to null,
    "Định kỳ" to "định kỳ",
    "Mở máy" to "mở máy",
    "Có mạng" to "có mạng mới",
)

@Composable
fun LogScreen(s: Status) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val today = dayStart(s.now)
    val groups = s.entries
        .filter { filter == null || it.reason == filter }
        .groupBy { dayStart(it.time) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Column {
                ScreenTitle("Nhật ký", "${s.entries.size} lần đánh thức gần nhất")
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FILTERS.forEach { (label, reason) ->
                        val on = filter == reason
                        Box(
                            Modifier.height(34.dp).clip(CircleShape)
                                .background(if (on) Accent else Surface)
                                .then(if (on) Modifier else Modifier.border(1.dp, Line, CircleShape))
                                .clickable { filter = reason }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label, color = if (on) Color.Black else Fg, fontSize = 13.sp,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                Text(
                    "Chưa có lần đánh thức nào", color = Muted, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }

        groups.forEach { (day, list) ->
            item(key = day) {
                val label = when (day) {
                    today -> "HÔM NAY"
                    dayStart(today - 1) -> "HÔM QUA"
                    else -> fmt("dd/MM", day)
                }
                Column {
                    Text(
                        label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
                    )
                    Column(Modifier.card().padding(vertical = 5.dp)) {
                        list.forEachIndexed { i, e ->
                            if (i > 0) HDivider(Modifier.padding(start = 56.dp, end = 16.dp))
                            LogRow(e, isToday = day == today, now = s.now)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun LogRow(e: Heartbeat.Entry, isToday: Boolean, now: Long) {
    val k = kindOf(e.reason)
    val right = if (isToday) {
        val m = minutesAgo(now, e.time)
        when {
            m < 1 -> "vừa xong"
            m < 60 -> "$m phút"
            else -> "${m / 60} giờ"
        }
    } else fmt("HH:mm", e.time)

    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(k.icon, k.color, box = 32.dp, radius = 10.dp, icon = 18.dp, alpha = 0.15f)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(k.label, color = Fg, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
            Text(
                fmt("dd/MM HH:mm:ss", e.time), color = Muted, fontSize = 12.sp, lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Text(right, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
