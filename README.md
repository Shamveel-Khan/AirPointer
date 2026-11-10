# AirPointer 🖱️

> **Your Phone. Your Mouse. No Wires.**

A native Android application built with Kotlin and Jetpack Compose that transforms your smartphone into a wireless Bluetooth HID mouse, air motion pointer, touchpad, and keyboard for Windows and Linux laptops — with **zero desktop software, zero Python servers, zero Wi-Fi, and no internet required**.

---

## 🚀 Key Highlights

- **Triple Input Modes:**
  1. **Touchpad Mode:** Full-screen laptop-style trackpad with multi-touch gestures, pointer acceleration, and bottom click buttons.
  2. **Desk Mouse Mode (Physical Desk Slide):** Slide your phone flat across any table, desk, or mousepad just like a real optical mouse! Features dynamic DC tilt cancellation, surface friction modeling to eliminate braking recoil, and Gyroscope Z wrist arc assistance.
  3. **Air Mouse Mode (Physical Motion in Air):** Point and wave your phone in the air to guide the PC mouse cursor! Uses hardware gyroscope angular velocity with anti-tremor deadzone filtering and exponential smoothing.
- **Physical Volume Button Clicks & Accessibility Gestures:**
  - Press physical **Volume Down** for **Left Click**
  - Press physical **Volume Up** for **Right Click**
  - Works inside the app and system-wide via the optional `AirPointerAccessibilityService`.
- **Pure Bluetooth HID Profile:** Emulates a standard physical USB/Bluetooth Human Interface Device (`BluetoothHidDevice` API 28+). Your computer sees the phone as a genuine hardware mouse and keyboard.
- **No Companion App Required:** Works natively with Windows 10/11 and Linux (Ubuntu, Fedora, Arch, Debian) without installing any receiver daemon or background software on the PC.
- **Multi-Touch Gestures (Touchpad):**
  - 1 finger move → Move mouse cursor
  - 1 finger tap → Left click
  - 1 finger double tap → Double click
  - 2 finger tap → Right click
  - 2 finger vertical swipe → Smooth scroll (with Natural Scrolling toggle)
  - 2 finger horizontal swipe → Horizontal pan
  - Long-press (0.35s) & drag → Drag & Drop (Left click held)
  - Drag Lock toggle for effortless window moving and text selection
- **Desk Mouse Features:**
  - **Zero-Lag Kinematics:** Instant reaction to physical phone sliding.
  - **Braking Clamping (Zero Bounce):** Solves the classic accelerometer bounce issue where braking pushes the cursor backwards.
  - **Wrist Arc Pivoting Assist:** Combines Gyro Z rotation for natural wrist pivoting while sliding on a mousepad.
  - **Tap to Freeze / Lift:** Easily pause tracking with a single tap to reposition the phone on the desk.
  - **Zero Bias Button:** Instantly adapts to surface angle and resting position.
  - **On-Screen Desk Sensitivity Slider:** Tune your surface speed directly from the main screen.
  - **Thumb Scroll Strip:** Slide thumb on the right edge to scroll while moving the phone on the desk.
- **Air Mouse Motion Features:**
  - **Hold to Aim Button (Clutch) / Continuous Mode:** Free-hand air pointing with customizable deadzone.
  - **Interactive 3D Motion Sphere:** Real-time visual horizon reticle reflecting phone pitch and yaw angles.
  - **One-Tap Recenter:** Calibrates gyroscope sensor bias instantly.
  - **Side Scroll Strip:** Slide thumb along the right edge to scroll while aiming.
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
├── MainActivity.kt                # UI host, runtime permission orchestration, volume key interception
├── bluetooth/
│   ├── BluetoothHidManager.kt     # HID registration, profile proxy, connection state, device discovery
│   ├── HidReportDescriptor.kt     # Valid USB HID Combo descriptor (Mouse + Keyboard + Consumer)
│   ├── MouseReportSender.kt       # Relative mouse reports, buttons, scrolling, dragging
│   ├── KeyboardReportSender.kt    # Standard 8-byte keyboard reports, ASCII-to-HID mapper, media keys
│   └── AirPointerService.kt       # Foreground service (FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
├── motion/
│   └── AirMouseManager.kt         # Hardware gyroscope motion engine, low-pass filter, clutch logic
├── accessibility/
│   └── AirPointerAccessibilityService.kt # System-wide volume button click capture & gesture receiver
├── gesture/
│   ├── TouchpadGestureHandler.kt  # Multi-touch gesture recognition, ballistics acceleration curve
│   └── TouchpadListener.kt        # Callback interface for gesture events
├── data/
│   ├── UserSettings.kt            # Data model for user preferences (Touchpad & Air Mouse)
│   └── SettingsRepository.kt      # SharedPreferences persistence with reactive StateFlow
├── ui/
│   ├── theme/                     # Dark color palette, typography, status bar theming
│   ├── components/                # Header, StatusBadge, HapticHelper
│   ├── navigation/                # AppTab enum (Connect, Touchpad, Keyboard, Settings)
│   ├── screens/                   # ConnectionScreen, TouchpadScreen, KeyboardScreen, SettingsScreen
│   └── viewmodels/                # ConnectionViewModel, TouchpadViewModel, KeyboardViewModel, SettingsViewModel
```

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

## 💻 Pairing & Usage Instructions

### Pairing with Windows 10 & 11

1. Open **AirPointer** on your Android phone.
2. Grant Bluetooth permissions if prompted (`Nearby Devices` / `Bluetooth`).
3. In the **Connect** tab, tap **Make Discoverable**.
4. On your Windows laptop:
   - Go to **Settings** → **Bluetooth & devices**.
   - Turn Bluetooth **On**, then click **Add device** → **Bluetooth**.
   - Select **AirPointer** from the list of available devices.
   - Confirm the pairing code on both devices.
5. Windows will automatically configure AirPointer as a **Bluetooth HID Mouse & Keyboard**. No drivers or restarts needed!

### Using Touchpad vs Air Mouse

- Switch between **Touchpad** and **Air Mouse (Motion)** using the top pill bar in the Touchpad tab.
- In **Air Mouse Mode**:
  - Hold down the large **Hold to Aim & Move Mouse** button with your thumb and wave your phone in the air. Your computer's mouse cursor will mirror your hand movement!
  - Release the button to reposition your hand without moving the cursor.
  - Press the phone's physical **Volume Down** button for a real **Left Click**, and **Volume Up** for a **Right Click**!
  - Slide your thumb along the right edge scroll bar to scroll up and down.
  - Tap **Recenter** to calibrate the gyroscope at any time.

---

## 📄 License
Open source under the Apache 2.0 License.
