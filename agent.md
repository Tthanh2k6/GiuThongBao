# agent.md — Quy chuẩn Vận hành, Bảo vệ Kiến trúc & Đồng bộ Tài liệu (`GiuThongBao`)

Tài liệu này là **Sổ tay Thực chiến Bắt buộc** dành cho mọi AI Agent và lập trình viên khi làm việc trên dự án **Giữ Thông Báo (`vn.trungthanh.giuthongbao`)** tại `d:\Project\GiuThongBao`.

---

## 1. Mục đích & Nguyên tắc Hoạt động

### 1.1. Vai trò của cặp tài liệu `agent.md` + `CODEBASE.md`
- **`CODEBASE.md`** là **Nguồn Sự thật Kỹ thuật Duy nhất (Single Source of Truth)** mô tả đầy đủ kiến trúc, luồng chạy nền Android, giao thức broadcast FCM, cấu trúc lưu trữ `SharedPreferences`, hệ thống giao diện Jetpack Compose và các giới hạn kỹ thuật của toàn bộ dự án.
- **`agent.md`** (tệp này) là **Giao thức Điều hành (Operating Protocol)** quy định:
  1. Cách tra cứu nhanh ngữ cảnh trước khi chạm vào mã nguồn (**Read-First Protocol**).
  2. Các rào chắn kỹ thuật đặc thù của Android 14–16 / HyperOS không được phép làm hỏng (**Project-Specific Guardrails**).
  3. Nghĩa vụ cập nhật đồng bộ tài liệu ngay sau mỗi lần sửa code (**Post-Edit Sync Protocol**).

### 1.2. Cảnh báo Định danh Nghiệp vụ Quan trọng Nhất
> **TUYỆT ĐỐI KHÔNG NHẦM LẪN TÊN DỰ ÁN**:  
> `GiuThongBao` (**Giữ Thông Báo**) là tiện ích **duy trì kết nối đẩy Google FCM (`mtalk.google.com:5228`)** trên ROM Xiaomi HyperOS / MIUI bằng cách phát broadcast `GTALK_HEARTBEAT` & `MCS_HEARTBEAT` kết hợp `ForegroundService` (`specialUse`) và `AlarmManager` (`USE_EXACT_ALARM`).  
> Ứng dụng **KHÔNG dùng `NotificationListenerService`**, **KHÔNG xin quyền `INTERNET`**, và **KHÔNG đọc/lưu nội dung thông báo của người dùng**. Mọi đề xuất thêm `NotificationListenerService` hay Room Database vì đoán mò theo tên ứng dụng đều là sai lệch hoàn toàn bản chất dự án!

---

## 2. Quy trình BẮT BUỘC Trước Khi Sửa Code (Read-First Protocol)

Ở bất kỳ phiên làm việc mới nào, trước khi thực hiện chỉnh sửa dù chỉ 1 dòng code, AI Agent / lập trình viên **PHẢI** tuân thủ 3 bước sau:

1. **Bước 1 — Đọc định hướng trong `agent.md`**: Xem *Bảng Định tuyến Nhanh* (Mục 2.1) và *Các Bất biến Kỹ thuật* (Mục 3) bên dưới.
2. **Bước 2 — Đọc mục tương ứng trong `CODEBASE.md`**: Nắm rõ mối liên kết giữa tệp sắp sửa với các thành phần khác (ví dụ: sửa chuỗi `reason` trong `KeepService.kt` sẽ ảnh hưởng trực tiếp tới `Ui.kt` và bộ lọc trong `Screens.kt`).
3. **Bước 3 — Đọc toàn bộ tệp mã nguồn mục tiêu bằng công cụ đọc file**: Vì toàn bộ phần Kotlin của dự án chỉ gồm 6 tệp gọn gàng (~1.036 dòng), hãy luôn đọc trọn vẹn tệp cần sửa trước khi viết thay đổi.

### 2.1. Bảng Định tuyến Nhanh (Quick Routing Table)

| Khi cần làm việc với mảng chức năng... | Đọc mục nào trong `CODEBASE.md` | Mở những tệp nguồn nào |
| :--- | :--- | :--- |
| **Cơ chế gửi Heartbeat tới GMS, chống gửi dồn (`minGapMs`), hoặc thêm chu kỳ phút** | Mục 2.3 (Luồng 2, 3), Mục 4.2, Mục 5.1, Mục 5.2.A | `app/src/main/java/vn/trungthanh/giuthongbao/Heartbeat.kt` |
| **Lập lịch hẹn giờ chính xác xuyên Doze (`AlarmManager`), tự khởi động khi bật máy / cập nhật APK** | Mục 2.3 (Luồng 2, 4), Mục 4.1, Mục 4.2, Mục 4.4 | `Heartbeat.kt`, `Receivers.kt`, `app/src/main/AndroidManifest.xml` |
| **Dịch vụ chạy nền `KeepService`, thông báo thường trực, bắt sự kiện mở khóa máy & có mạng mới** | Mục 2.2, Mục 2.3 (Luồng 3), Mục 4.3, Mục 8.1 | `KeepService.kt`, `AndroidManifest.xml`, `res/drawable/ic_bell.xml` |
| **Tích hợp quyền chạy nền Xiaomi HyperOS / MIUI, bỏ tối ưu pin, Deep-Link cài đặt hệ thống** | Mục 2.3 (Luồng 5), Mục 4.5, Mục 5.1 (Bảng 2), Mục 5.2.C | `MainActivity.kt`, `Screens.kt` (`SetupScreen`), `AndroidManifest.xml` |
| **Giao diện màn hình Tổng quan, vòng tròn đếm ngược `Canvas`, thống kê lần gửi** | Mục 4.5, Mục 4.6 (`HomeScreen`), Mục 6 | `Screens.kt`, `MainActivity.kt`, `Ui.kt` |
| **Giao diện màn hình Nhật ký, bộ lọc lý do đánh thức, phân tích & hiển thị ngày giờ** | Mục 4.2 (`entries`), Mục 4.6 (`LogScreen`), Mục 4.7 (`kindOf`), Mục 8.1 | `Heartbeat.kt`, `Screens.kt`, `Ui.kt` |
| **Bảng màu Dark Mode, thêm/sửa icon vector nét mảnh `24×24`, thanh điều hướng đáy `BottomNav`** | Mục 4.7, Mục 6.3, Mục 6.4 | `Ui.kt`, `res/drawable/*.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml` |
| **Nâng cấp phiên bản SDK, Gradle, Kotlin, Compose hoặc cấu hình ký APK Release** | Mục 1.2, Mục 4.1, Mục 7, Mục 8.2 | `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties` |

---

## 3. Các Bất biến Kỹ thuật & Quy tắc An toàn Riêng của Dự án (Project-Specific Guardrails)

Mọi thay đổi mã nguồn **BẮT BUỘC** phải tuân thủ 10 quy tắc bảo vệ kiến trúc sau đây:

1. **Bảo toàn Bộ ba Foreground Service `specialUse` cho Android 14–16 (`targetSdk = 36`)**:
   - Không bao giờ xóa hoặc làm lệch 3 thành phần đi đôi với nhau:
     - Quyền `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` trong `AndroidManifest.xml`.
     - Khai báo `android:foregroundServiceType="specialUse"` kèm thẻ `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" ... />` trong `<service android:name=".KeepService">`.
     - Cờ `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (khi `Build.VERSION.SDK_INT >= 34`) truyền vào `ServiceCompat.startForeground(this, 1, notif, type)` ngay đầu `KeepService.onCreate()`.
   - Luôn giữ khối `try { ... } catch (_: Exception)` trong `KeepService.start(ctx)` để tránh lỗi văng app `ForegroundServiceStartNotAllowedException` khi Android hạn chế mở dịch vụ từ nền.

2. **Bảo toàn Cơ chế Hẹn giờ Đơn nhất & Hạ cấp An toàn của `AlarmManager`**:
   - Trong `Heartbeat.schedule(ctx)`, `PendingIntent` gửi tới `AlarmReceiver` phải giữ cố định `requestCode = 0` và cờ `PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE` để lịch mới luôn ghi đè lịch cũ, tuyệt đối không tạo ra nhiều chuỗi alarm chạy trùng lặp.
   - Luôn kiểm tra `Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()` trước khi gọi `setExactAndAllowWhileIdle(...)`, và giữ nhánh dự phòng `setAndAllowWhileIdle(...)` để không bao giờ crash bởi `SecurityException`.

3. **Bảo toàn Phát sóng Kép (Dual-Broadcast) tới Google Play services**:
   - Trong `Heartbeat.send()`, với mỗi action (`GTALK_HEARTBEAT` và `MCS_HEARTBEAT`), phải giữ cả 2 lệnh: gửi broadcast ẩn (`Intent(action)`) và gửi broadcast chỉ định gói (`Intent(action).setPackage("com.google.android.gms")`). Bỏ 1 trong 2 sẽ khiến tín hiệu bị chặn trên một số đời Android hoặc bản HyperOS nội địa.

4. **Bảo vệ Định dạng Chuỗi Nhật ký & Khóa Lý do Tiếng Việt**:
   - Định dạng mỗi dòng trong key `"log"` của `SharedPreferences("hb")` là `"dd/MM HH:mm:ss  <reason>"` (ngăn cách bởi **đúng 2 dấu cách `"  "`**, nối các dòng bằng `"\n"`). Chuỗi `reason` không bao giờ được chứa `"\n"`.
   - Các chuỗi định danh lý do (`"định kỳ"`, `"mở máy"`, `"có mạng mới"`, `"bấm tay"`, `"khởi động máy"`, `"cập nhật app"`) đang được khớp trực tiếp trong `Ui.kt` (`kindOf`) và `Screens.kt` (`FILTERS`). Nếu thêm lý do mới hoặc sửa tên lý do, **bắt buộc** phải cập nhật đồng bộ tại nơi gọi `Heartbeat.send()`, hàm `kindOf()` trong `Ui.kt`, và danh sách `FILTERS` trong `Screens.kt`.

5. **Bảo vệ Hiệu năng Luồng Giao diện Chính (Main Thread 1-Second Polling)**:
   - `MainActivity` gọi `readStatus()` mỗi **1.000ms** trên Main Thread. Tuyệt đối không đưa tác vụ đọc/ghi đĩa lớn, truy vấn cơ sở dữ liệu nặng, hay IPC chặn luồng vào bên trong `readStatus()` hoặc `Heartbeat.entries()`. Không tăng `MAX_LOG` (hiện là `40`) lên mức hàng trăm/nghìn dòng nếu vẫn parse `SimpleDateFormat` trực tiếp trong `readStatus()`.

6. **An toàn Đa luồng khi Sửa `Heartbeat.send()`**:
   - Nhớ rằng `KeepService.netCallback.onAvailable()` gọi `Heartbeat.send()` từ luồng nền `ConnectivityThread`, trong khi `MainActivity` và các `BroadcastReceiver` gọi từ `Main` thread. Không dùng biến `SimpleDateFormat` tĩnh dùng chung giữa các luồng; nếu bổ sung trạng thái chia sẻ phức tạp vào `Heartbeat`, hãy dùng `@Volatile` hoặc khối `synchronized(this)`.

7. **Luôn Bọc Fallback khi Gọi Intent Độc quyền của OEM (`tryStart`)**:
   - Mọi Intent nhắm tới package/action riêng của Xiaomi (`com.miui.securitycenter`, `miui.intent.action.*`) hoặc hãng khác phải đi qua hàm `tryStart(intent)` để tự động chuyển hướng an toàn về `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` khi chạy trên ROM AOSP / Samsung / Oppo hoặc khi Xiaomi đổi tên class nội bộ.

8. **Giữ Nguyên Mã hóa UTF-8 Không BOM trên Mọi Tệp Nguồn**:
   - Toàn bộ văn bản tiếng Việt trên giao diện và các chuỗi khóa `reason` nằm trực tiếp trong các tệp `.kt` và `AndroidManifest.xml`. Khi dùng công cụ sửa file hoặc PowerShell trên Windows, bắt buộc đảm bảo encoding **UTF-8** chuẩn để không làm lỗi phông chữ tiếng Việt.

9. **Giữ Nguyên Quy chuẩn Icon Nhỏ Thanh Trạng thái & `allowBackup="false"`**:
   - Tệp `res/drawable/ic_bell.xml` dùng cho `Notification.Builder.setSmallIcon()` bắt buộc phải là hình vector đơn sắc trắng (`#FFFFFFFF`) trên nền trong suốt (nếu tô màu đa sắc Android sẽ biến cả icon thành ô vuông trắng đặc).
   - Giữ `android:allowBackup="false"` trong `AndroidManifest.xml` để tránh khôi phục sai lệch cờ thiết lập `setup.xml` khi đổi máy.

10. **Giữ Tính Tối giản của Hệ thống Icon Nội bộ (`object Ic` + `LineIcon`)**:
    - Khi cần thêm biểu tượng UI mới, ưu tiên bổ sung chuỗi SVG path khung `24×24` vào `object Ic` trong `Ui.kt` và vẽ bằng `LineIcon(...)` thay vì kéo thêm thư viện nặng nề `androidx.compose.material:material-icons-extended` (nặng hàng chục MB).

---

## 4. Quy trình BẮT BUỘC Cập nhật Tài liệu Sau Mỗi Lần Sửa Code (Post-Edit Sync Protocol)

> **QUY TẮC TỐI THƯỢNG**:  
> **Mọi thay đổi mã nguồn, cấu hình build, Manifest hoặc tài nguyên XML đều PHẢI đi kèm cập nhật `CODEBASE.md` (và `agent.md` nếu có thay đổi kiến trúc / quy tắc an toàn / định tuyến) ngay trong cùng một phiên làm việc, TRƯỚC KHI báo cáo hoàn thành cho người dùng.** Không bao giờ để tài liệu lệch với code thực tế!

### 4.1. Ma trận Đồng bộ Tài liệu (Documentation Sync Matrix)

| Loại thay đổi vừa thực hiện trong dự án | Các mục BẮT BUỘC cập nhật trong `CODEBASE.md` | Các mục cần cập nhật trong `agent.md` |
| :--- | :--- | :--- |
| **Thêm / xóa / đổi tên / tách tệp `.kt`, `.xml`, script** | Mục 3.1 (Cây thư mục), Mục 3.2 (Bảng kê khai đầy đủ + cập nhật số dòng), Mục 4 (Phân tích chi tiết tệp), Mục 9 (Changelog) | Mục 2.1 (Bảng Định tuyến Nhanh) |
| **Sửa logic gửi broadcast, debounce, lịch hẹn giờ, hoặc thêm lý do (`reason`) mới** | Mục 2.3 (Luồng dữ liệu), Mục 4.2 / 4.3 / 4.4 (Chi tiết hàm & hằng số), Mục 5.1 (Tập giá trị `<reason>`), Mục 7.2 (Bảng hằng số), Mục 9 | Mục 3 (Nếu thêm bất biến mới về chuỗi `reason` hoặc luồng chạy) |
| **Đổi khóa hoặc cấu trúc `SharedPreferences` (`"hb"`, `"setup"`), hoặc thêm cơ chế lưu trữ mới** | Mục 1.2 (Tech Stack nếu thêm thư viện), Mục 2.1 (Sơ đồ Mermaid), Mục 5.1 (Bảng Schema lưu trữ), Mục 9 | Mục 2.1 & Mục 3 (Quy tắc bảo toàn dữ liệu / migration) |
| **Thêm / sửa Intent cài đặt OEM (HyperOS, ColorOS, OneUI...) hoặc đổi quyền trong `AndroidManifest.xml`** | Mục 4.1 (Phân tích quyền Manifest), Mục 4.5 (`MainActivity`), Mục 5.2.B / 5.2.C (Bảng giao thức Intent), Mục 8.1 / 8.3, Mục 9 | Mục 3 (Cập nhật quy tắc an toàn quyền hệ thống & fallback OEM) |
| **Thay đổi giao diện màn hình, luồng điều hướng Tab, bảng màu Hex, Typography, hoặc thêm icon trong `Ic`** | Mục 3.2 (Số dòng `Screens.kt` / `Ui.kt`), Mục 4.6 & 4.7, Mục 6.1 (Sơ đồ chuyển màn hình), Mục 6.2 / 6.3 / 6.4, Mục 9 | Mục 2.1 (Nếu thêm màn hình hoặc tệp UI mới) |
| **Nâng cấp Gradle, AGP, Kotlin, Compose BOM, SDK version, hoặc cấu hình signing release** | Phần Header đầu file, Mục 1.2 (Bảng Tech Stack), Mục 4.1, Mục 7.1 & 7.3 (Yêu cầu & Lệnh build), Mục 8.2, Mục 9 | Mục 5 (Cập nhật lệnh kiểm tra & xác minh nếu đổi task Gradle) |
| **Khởi tạo kho `.git` / thêm remote URL, hoặc khắc phục được một Edge Case / phát hiện ràng buộc mới** | Mục 1.3 (Thông tin Git), Mục 8.1 / 8.3 (Cập nhật danh sách bất biến & giới hạn kỹ thuật), Mục 9 | Mục 3 (Bổ sung vào danh sách Guardrails nếu là bẫy dễ lỗi) |

### 4.2. Quy chuẩn Viết Bổ sung Tài liệu
- **Ngôn ngữ & Định dạng**: Viết bằng tiếng Việt kỹ thuật rõ ràng, chính xác; luôn bọc tên hàm, tên class, tên biến, key `SharedPreferences`, hằng số, action Intent và đường dẫn tệp trong dấu backtick `` `...` ``.
- **Ký hiệu đô-la (`\$`)**: Trong văn bản Markdown thông thường, luôn escape `\$` hoặc đặt biểu thức chuỗi nội suy Kotlin (như `` `"$time  $reason"` ``) vào trong dấu backtick để tránh trình hiển thị hiểu nhầm thành công thức toán LaTeX.
- **Sơ đồ Mermaid**: Mọi nhãn node có chứa dấu ngoặc đơn `()`, dấu hai chấm `:`, dấu gạch chéo `/` hoặc ký tự đặc biệt đều phải bọc trong dấu ngoặc kép (ví dụ: `KeepSvc["KeepService (ForegroundService)"]`), tuyệt đối không dùng thẻ HTML như `<br/>` bên trong Mermaid.
- **Bảo toàn độ chi tiết**: Khi sửa một mục trong `CODEBASE.md`, cập nhật chính xác cả số dòng tệp ở Mục 3.2 và chi tiết hàm ở Mục 4; không tóm tắt đại khái hay xóa nhầm mô tả của các thành phần khác vẫn đang còn hiệu lực.
- **Ghi nhận lịch sử**: Luôn thêm một dòng mới vào cuối bảng tại **Mục 9 (Nhật ký Cập nhật Tài liệu)** ghi rõ ngày cập nhật, các tệp/mục chịu ảnh hưởng và tóm tắt nội dung thay đổi.

---

## 5. Lệnh Kiểm tra & Xác minh Nhanh (Verification Commands)

Sau khi chỉnh sửa mã nguồn trên Windows (PowerShell), chạy các lệnh sau tại `d:\Project\GiuThongBao` để xác minh dự án biên dịch sạch sẽ và không phát sinh cảnh báo nghiêm trọng:

```powershell
Set-Location 'd:\Project\GiuThongBao'

# 1. Biên dịch bản Debug APK để kiểm tra toàn bộ cú pháp Kotlin, Compose Compiler và Android Manifest Merger
.\gradlew.bat assembleDebug

# 2. Chạy Android Lint kiểm tra quyền hệ thống, ForegroundService type, Exact Alarm và tương thích API 26..36
.\gradlew.bat lintDebug

# 3. (Nếu có thiết bị kết nối ADB) Cài đặt và kiểm tra dịch vụ + lịch hẹn giờ thực tế
.\gradlew.bat installDebug
adb shell dumpsys activity services vn.trungthanh.giuthongbao
adb shell dumpsys alarm | Select-String "vn.trungthanh.giuthongbao" -Context 2,6
```

### Checklist Hoàn tất Trước Khi Trả lời Người dùng

Trước khi kết thúc phiên làm việc và gửi báo cáo cho người dùng, hãy tự kiểm tra đủ 6 mục sau:
- [ ] **1. Đúng yêu cầu & Đúng bản chất**: Thay đổi có giải quyết đúng yêu cầu mà không phá vỡ kiến trúc tự hồi sinh (`KeepService` + `AlarmManager` + `Heartbeat`) của ứng dụng không?
- [ ] **2. An toàn UTF-8 & Chuỗi khóa**: Các tệp `.kt` / `.xml` vừa sửa có giữ nguyên mã hóa UTF-8 chuẩn tiếng Việt và khớp chính xác các chuỗi `reason` / key `SharedPreferences` không?
- [ ] **3. Tương thích Android SDK 26–36**: Có vi phạm quy định `ForegroundService` (`specialUse`), `PendingIntent.FLAG_IMMUTABLE`, hay làm nghẽn vòng lặp UI 1 giây không?
- [ ] **4. Biên dịch thành công**: Đã chạy `.\gradlew.bat assembleDebug` xác nhận build xanh 100% chưa (nếu môi trường có sẵn JDK & Android SDK)?
- [ ] **5. Đồng bộ `CODEBASE.md` & `agent.md`**: Đã cập nhật đầy đủ các mục tương ứng theo *Ma trận Đồng bộ Tài liệu* (Mục 4.1) và ghi thêm dòng mới vào **Mục 9 — Nhật ký Cập nhật Tài liệu** của `CODEBASE.md` chưa?
- [ ] **6. Không để rác**: Không tự ý sửa đổi ngoài phạm vi yêu cầu, không tạo file nháp thừa trong cây thư mục dự án.
