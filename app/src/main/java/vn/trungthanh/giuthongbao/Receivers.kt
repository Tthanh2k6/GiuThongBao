package vn.trungthanh.giuthongbao

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Heartbeat.send(ctx, "định kỳ")
        Heartbeat.schedule(ctx)
        // HyperOS có thể đã giết service; alarm là lúc hợp lệ để dựng lại.
        KeepService.start(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Heartbeat.send(ctx, if (intent.action == Intent.ACTION_BOOT_COMPLETED) "khởi động máy" else "cập nhật app")
        Heartbeat.schedule(ctx)
        KeepService.start(ctx)
    }
}
