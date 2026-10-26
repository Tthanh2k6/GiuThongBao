# GiuThongBao — Android FCM Push Keep-Alive & HyperOS Doze Bypass Utility

<div align="center">
  <p align="center">
    <img src="https://img.shields.io/badge/Kotlin-2.1+-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/Android-8.0%2B_(API_26--36)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 8.0+" />
    <img src="https://img.shields.io/badge/Privacy-Zero_Notification_Read-10B981?style=flat-square" alt="Zero Notification Read" />
  </p>
</div>

---

## ⚡ Overview

**GiuThongBao (`vn.trungthanh.giuthongbao`)** is a native Android utility written in **Kotlin + Jetpack Compose** designed to prevent delayed or dropped push notifications on aggressive Chinese OEM ROMs (especially **Xiaomi HyperOS China ROM** and **MIUI**).

> **Privacy by Design:** Despite the name (*"Keep Notifications"*), this app **does NOT use `NotificationListenerService`** and never reads, intercepts, or stores notification contents. Instead, it keeps the underlying **Google Firebase Cloud Messaging (FCM / MCS `mtalk.google.com:5228`)** TCP socket alive.

---

## 🔧 How It Works

On aggressive OEM battery-saver ROMs, background freezer mechanisms frequently sever Google Play Services' (`com.google.android.gms`) persistent TCP connection to `mtalk.google.com:5228` during Wi-Fi ↔ 4G/5G handoffs or deep sleep without scheduling a reconnect alarm.

**GiuThongBao** solves this through a 3-pronged wake & heartbeat architecture:

```mermaid
flowchart LR
    Alarm["1. Exact Doze Alarm\n(setExactAndAllowWhileIdle\nevery 3/5/10/15 min)"] --> Pulse["FCM Heartbeat Dispatcher"]
    Net["2. Network Callback\n(onAvailable: Wi-Fi / 4G)"] --> Pulse
    Unlock["3. Screen Unlock\n(ACTION_USER_PRESENT)"] --> Pulse
    Pulse -->|"GTALK_HEARTBEAT +\nMCS_HEARTBEAT"| GMS["Google Play Services\n(mtalk.google.com:5228)"]
```

1. **Periodic Doze-Piercing Heartbeats (`AlarmManager.setExactAndAllowWhileIdle`):** Fires at a user-configurable interval (`3`, `5`, `10`, or `15` minutes) even when the device is in Deep Doze.
2. **Event-Driven Instant Recovery (`KeepService` ForegroundService):** Listens for the two exact moments FCM sockets drop most often—**new network availability (`NetworkCallback.onAvailable`)** and **device unlock (`ACTION_USER_PRESENT`)**—and immediately dispatches `com.google.android.intent.action.GTALK_HEARTBEAT` and `com.google.android.intent.action.MCS_HEARTBEAT`.
3. **HyperOS / MIUI Deep-Link Setup Wizard:** Provides one-tap intents directly into hidden Xiaomi system screens (`AutoStartManagementActivity`, `HIDDEN_APPS_CONFIG_ACTIVITY`) so users can whitelist background execution in seconds.

---

## 🚀 Build & Install

```powershell
git clone https://github.com/Tthanh2k6/GiuThongBao.git
cd GiuThongBao
.\gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

**Author:** **Trung Thanh Tran** (*Trần Trung Thành*) · [GitHub `@Tthanh2k6`](https://github.com/Tthanh2k6) · [Upwork Profile](https://www.upwork.com/freelancers/~01ee7c2348cb4b62fe)
