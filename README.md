# AirPointer 🖱️

> **Your Phone. Your Mouse. No Wires.**

A native Android application built with Kotlin and Jetpack Compose that turns your smartphone into a wireless Bluetooth HID mouse, touchpad, and keyboard for Windows and Linux laptops — with **zero desktop software, zero Python servers, zero Wi-Fi, and no internet required**.

---

## 🚀 Key Highlights

- **Pure Bluetooth HID Profile:** Emulates a standard physical USB/Bluetooth Human Interface Device (`BluetoothHidDevice` API 28+). Your computer sees the phone as a genuine hardware mouse and keyboard.
- **No Companion App Required:** Works natively with Windows 10/11 and Linux (Ubuntu, Fedora, Arch, Debian) without installing any receiver daemon or background software on the PC.
- **Ultra-Responsive Low-Latency Touchpad:** Direct MotionEvent processing with pointer ballistics acceleration curve for pixel-precise slow movement and swift screen traversal.
- **Multi-Touch Gestures:**
  - One-finger move → Move mouse cursor
  - Single tap → Left click
  - Double tap → Double click
  - Two-finger tap → Right click
  - Two-finger vertical swipe → Smooth scroll (with Natural Scrolling toggle)
  - Two-finger horizontal swipe → Horizontal pan
  - Long-press (0.35s) & drag → Drag & Drop (Left click held)
  - Drag Lock toggle for effortless window moving and text selection
- **Desktop Keyboard & IME Typing:**
  - Send text typed with Gboard, Samsung Keyboard, or Voice Typing directly to the PC
  - Hardware desktop keys: `Esc`, `Tab`, `Backspace`, `Delete`, `Enter`, `Home`, `End`, `PgUp`, `PgDn`
  - Arrow navigation cluster (`▲`, `◀`, `▼`, `▶`)
  - Latchable modifiers: `Ctrl`, `Alt`, `Shift`, `Win / ⌘`
  - Quick shortcuts: `Ctrl+C`, `Ctrl+V`, `Ctrl+Z`, `Ctrl+A`, `Alt+Tab`
  - Full function row: `F1` through `F12`
  - Consumer media controls: `Mute`, `Vol -`, `Vol +`, `Prev`, `Play/Pause`, `Next`
- **Haptic Feedback:** Real-time subtle vibration feedback on taps, clicks, and drag locks.
- **Foreground Service Protection:** Keeps HID connection persistent in the background with an ongoing status notification.

---

## 🛠️ Architecture & Tech Stack

- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose (BOM 2026.02.01) + Material 3
- **Design:** Dark theme with neon cyan & electric blue accents
- **Architecture:** Clean MVVM with Kotlin Coroutines & `StateFlow`
- **Android Target:** minSdk 28 (Android 9.0 Pie), targetSdk 37

### Core Components

```
com.airpointer.app/
├── AirPointerApplication.kt       # Application class, singletons lifecycle
├── MainActivity.kt                # UI host, runtime permission orchestration, tab navigation
├── bluetooth/
│   ├── BluetoothHidManager.kt     # HID registration, profile proxy, connection state, device discovery
│   ├── HidReportDescriptor.kt     # Valid USB HID Combo descriptor (Mouse + Keyboard + Consumer)
│   ├── MouseReportSender.kt       # Relative mouse reports, buttons, scrolling, dragging
│   ├── KeyboardReportSender.kt    # Standard 8-byte keyboard reports, ASCII-to-HID mapper, media keys
│   └── AirPointerService.kt       # Foreground service (FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
├── gesture/
│   ├── TouchpadGestureHandler.kt  # Multi-touch gesture recognition, ballistics acceleration curve
│   └── TouchpadListener.kt        # Callback interface for gesture events
├── data/
│   ├── UserSettings.kt            # Data model for user preferences
│   └── SettingsRepository.kt      # SharedPreferences persistence with reactive StateFlow
├── ui/
│   ├── theme/                     # Dark color palette, typography, status bar theming
│   ├── components/                # Header, StatusBadge, HapticHelper
│   ├── navigation/                # AppTab enum (Connect, Touchpad, Keyboard, Settings)
│   ├── screens/                   # ConnectionScreen, TouchpadScreen, KeyboardScreen, SettingsScreen
│   └── viewmodels/                # ConnectionViewModel, TouchpadViewModel, KeyboardViewModel, SettingsViewModel
```

---

## 📦 HID Report Descriptors

AirPointer registers a standard USB HID Combo device with three Report IDs:

1. **Report ID 1 — Mouse:**
   - 5 buttons (Left, Right, Middle, Back, Forward)
   - Relative X and Y (-127 to +127, 8-bit signed)
   - Relative Wheel / Vertical Scroll (-127 to +127)
   - Relative AC Pan / Horizontal Scroll (-127 to +127)
2. **Report ID 2 — Keyboard:**
   - 8-bit modifiers byte (`LCtrl`, `LShift`, `LAlt`, `LGUI`, `RCtrl`, `RShift`, `RAlt`, `RGUI`)
   - 1 reserved byte
   - 6-key array for simultaneous key presses (rollover)
   - Output LED report for Caps Lock & Num Lock
3. **Report ID 3 — Consumer Controls:**
   - 16-bit usage for Volume Up/Down, Mute, Play/Pause, Next/Previous Track

---

## 📲 Installation

The debug APK is compiled and ready at the root of the project:

```bash
# Path to APK
AirPointer.apk (or app/build/outputs/apk/debug/app-debug.apk)

# Install via ADB:
adb install -r AirPointer.apk
```

Or copy `AirPointer.apk` to your Android device via USB file transfer or Google Drive and install it directly.

---

## 💻 Pairing Instructions

### Pairing with Windows 10 & 11

1. Open **AirPointer** on your Android phone.
2. Grant Bluetooth permissions if prompted (`Nearby Devices` / `Bluetooth`).
3. In the **Connect** tab, tap **Make Discoverable**.
4. On your Windows laptop:
   - Go to **Settings** → **Bluetooth & devices**.
   - Make sure Bluetooth is **On**, then click **Add device** → **Bluetooth**.
   - Select **AirPointer** from the list of available devices.
   - Confirm the pairing code on both the phone and laptop.
5. Windows will automatically configure AirPointer as a **Bluetooth HID Mouse & Keyboard**. No drivers or restarts needed!
6. Switch to the **Touchpad** tab in AirPointer and move your finger — your laptop cursor will move instantly!

### Pairing with Linux (Ubuntu, Fedora, Arch, Debian)

1. Open **AirPointer** on your phone and tap **Make Discoverable** in the **Connect** tab.
2. Open Bluetooth Settings in GNOME / KDE or use `bluetoothctl`:
   ```bash
   bluetoothctl
   [bluetooth]# scan on
   # Locate AirPointer's MAC address (e.g. 12:34:56:78:9A:BC)
   [bluetooth]# pair 12:34:56:78:9A:BC
   [bluetooth]# trust 12:34:56:78:9A:BC
   [bluetooth]# connect 12:34:56:78:9A:BC
   ```
3. The Linux Kernel HID subsystem (`hid-generic` / `input`) immediately recognizes both the mouse pointer and keyboard.

---

## ⚙️ Crash Fix Details (TargetSDK 34+ / 37 Compatibility)

### The Issue
On Android 14+ (targetSdk ≥ 34), invoking `startForeground()` with `ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` before runtime permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`) are granted throws a fatal `SecurityException`:
```
java.lang.SecurityException: Starting FGS with type connectedDevice requires permissions:
allOf=true [android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE]
anyOf=false [android.permission.BLUETOOTH_ADVERTISE, android.permission.BLUETOOTH_CONNECT, ...]
```
Additionally, querying `bluetoothAdapter.isEnabled` or `adapter.bondedDevices` on cold start without granted permissions throws a `SecurityException` on Android 12+.

### The Solution
1. **Deferred Service Launch:** `AirPointerService` is never started on cold start before permissions are granted. It only starts after the user grants permissions via the permission modal or if already granted.
2. **Defensive Service Guards:** `AirPointerService.startForegroundWithNotification()` validates permissions before calling `startForeground()`, and wraps the call in `try / catch (e: SecurityException)`.
3. **Defensive Bluetooth Checks:** In `BluetoothHidManager`, all calls to `isEnabled`, `bondedDevices`, `getProfileProxy()`, and discovery are guarded by `hasBluetoothPermissions()` and wrapped in safety try-catches.
4. **Broadcast Receiver Flags:** Broadcast receivers use `Context.RECEIVER_EXPORTED` on Android 13+ (API 33+) to comply with modern Android broadcast security policies.

---

## 📄 License
Open source under the Apache 2.0 License.
