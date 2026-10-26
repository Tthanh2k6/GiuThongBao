package vn.trungthanh.giuthongbao

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Design tokens theo design/giu-thong-bao-redesign.svg
val Bg = Color(0xFF0E0E10)
val Surface = Color(0xFF1A1A1D)
val Line = Color(0xFF2C2C31)
val NavBg = Color(0xFF141416)
val Accent = Color(0xFFFFD60A)
val Fg = Color(0xFFF5F5F7)
val Muted = Color(0xFF8E8E93)
val Success = Color(0xFF32D74B)
val Warning = Color(0xFFFF9F0A)
val Network = Color(0xFF0A84FF)
val Unlock = Color(0xFFBF5AF2)

/** Path icon dạng nét, khung 24×24 (cùng bộ với file thiết kế). */
object Ic {
    const val BELL = "M6 9a6 6 0 0 1 12 0v4l2 3H4l2-3z M10 19a2 2 0 0 0 4 0"
    const val ALERT = "M12 3 2 20h20z M12 10v4M12 17h.01"
    const val CHEVRON = "M9 6l6 6-6 6"
    const val HOME = "M3 11 12 4l9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"
    const val SHIELD = "M12 3 5 6v6c0 4.5 3 7.5 7 9 4-1.5 7-4.5 7-9V6z M9 12l2 2 4-4"
    const val LIST = "M9 6h12M9 12h12M9 18h12M4 6h.01M4 12h.01M4 18h.01"
    const val BOLT = "M13 2 4 14h7l-1 8 9-12h-7z"
    const val BATTERY = "M4 7h13a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z M22 11v2M6 10v4"
    const val CHECK = "M5 12l5 5 9-10"
    const val POWER = "M12 3v8M6.3 6.3a8 8 0 1 0 11.4 0"
    const val LOCK = "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z M8 11V8a4 4 0 0 1 8 0v3"
    const val INFO = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0z M12 11v5M12 8h.01"
    const val CLOCK = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0z M12 7v5l3 2"
    const val WIFI = "M2 9a15 15 0 0 1 20 0M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0M12 19.5h.01"
    const val PHONE = "M9 2h6a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H9a3 3 0 0 1-3-3V5a3 3 0 0 1 3-3z M11 18h2"
    const val TAP = "M9 11V5a2 2 0 0 1 4 0v6M13 10a2 2 0 0 1 4 0v2M17 11a2 2 0 0 1 4 0v4a7 7 0 0 1-7 7h-1a7 7 0 0 1-6-3l-2.5-4a2 2 0 0 1 3.4-2L9 15"
}

/** Nét luôn dày [stroke] dù vẽ icon to hay nhỏ, giống bản thiết kế. */
@Composable
fun LineIcon(d: String, color: Color, size: Dp = 24.dp, stroke: Dp = 2.dp, fill: Boolean = false) {
    val path = remember(d) { PathParser().parsePathString(d).toPath() }
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(
                path, color,
                style = if (fill) Fill else Stroke(stroke.toPx() / s, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** Ô vuông bo góc có nền nhạt cùng màu với icon. */
@Composable
fun IconTile(d: String, color: Color, box: Dp, radius: Dp, icon: Dp, alpha: Float = 0.14f) {
    Box(
        Modifier.size(box).clip(RoundedCornerShape(radius)).background(color.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) { LineIcon(d, color, icon) }
}

fun Modifier.card(radius: Dp = 20.dp, color: Color = Surface) =
    fillMaxWidth().clip(RoundedCornerShape(radius)).background(color)

@Composable
fun HDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Line))
}

/** Kiểu hiển thị cho từng lý do đánh thức ghi trong log. */
class Kind(val label: String, val icon: String, val color: Color)

fun kindOf(reason: String) = when (reason) {
    "định kỳ" -> Kind("Định kỳ", Ic.CLOCK, Accent)
    "có mạng mới" -> Kind("Có mạng mới", Ic.WIFI, Network)
    "mở máy" -> Kind("Mở máy", Ic.PHONE, Unlock)
    "bấm tay" -> Kind("Bấm tay", Ic.TAP, Warning)
    else -> Kind(reason.replaceFirstChar { it.uppercase() }, Ic.POWER, Success)
}

@Composable
fun BottomNav(tab: Int, setupBadge: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(NavBg).navigationBarsPadding()) {
        HDivider()
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp)) {
            NavItem("Tổng quan", Ic.HOME, tab == 0) { onSelect(0) }
            NavItem("Thiết lập", Ic.SHIELD, tab == 1, badge = setupBadge) { onSelect(1) }
            NavItem("Nhật ký", Ic.LIST, tab == 2) { onSelect(2) }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    label: String, icon: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit,
) {
    val color = if (selected) Accent else Muted
    Column(
        Modifier.weight(1f).clickable(interactionSource = null, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                Modifier.size(64.dp, 32.dp).clip(CircleShape)
                    .background(if (selected) Accent.copy(alpha = 0.14f) else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) { LineIcon(icon, color) }
            if (badge > 0) {
                Box(
                    Modifier.offset(38.dp, (-3).dp).size(16.dp).clip(CircleShape).background(Warning),
                    contentAlignment = Alignment.Center,
                ) { Text("$badge", color = Color.Black, fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label, color = color, fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
