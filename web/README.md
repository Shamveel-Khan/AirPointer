# AirPointer Desk Mouse Kinematics Web Test & Advanced Analytics Studio 🖱️

A standalone, mobile-first web application and advanced diagnostics studio designed to evaluate, record, diagnose, and experiment with smartphone sensor tracking accuracy when sliding across a mousepad or desk.

---

## 🧭 Application Structure

The web application consists of two complementary tools:
1. **Live Desk Mouse Tester & Test Recorder (`/web/index.html`):** Mobile-first UI for live sliding, testing the 5 kinematic fixes, and recording the 4 directional tests (UP, DOWN, LEFT, RIGHT).
2. **Dedicated Advanced Sensor Analytics Studio (`/web/analytics.html`):** Desktop & tablet-optimized interactive diagnostic dashboard featuring JSON import, 4-track synchronized time-series charts, 6-stage pipeline inspector, ranked root-cause engine, and an interactive **What-If Algorithm Experimentation Sandbox**.

---

## 🔬 Advanced Analytics Studio Features (`analytics.html`)

### 1. JSON Recording Import & Local Storage Sync
- **Drag-and-Drop Zone:** Drop any test recording `.json` directly into the browser.
- **File Picker:** Browse and upload exported datasets (full multi-test bundles or single tests).
- **1-Click Sync:** Tap **Load from Tester** to instantly pull tests recorded in the mobile browser tab.

### 2. Synchronized Multi-Track Time-Series Charts
Hovering over any graph highlights the exact timestamp across all 4 tracks and simultaneously moves the position marker on the 2D Trajectory map:
- **Track 1 (Acceleration):** Raw $A_x, A_y$ (dashed) vs Filtered/Unbiased $A_x, A_y$ (solid), with highlighted anomaly regions.
- **Track 2 (Gyroscope Z):** Angular rotation rate ($g_z$) to detect wrist-arc rotation vs pure linear translation.
- **Track 3 (Estimated Velocity):** $V_x, V_y$ over time with zero-reference line and braking clamp markers.
- **Track 4 (Cursor Movement):** $dX, dY$ pixel deltas per frame.

### 3. Reconstructed 2D Trajectory View
- Overlays a bold green **Ground-Truth Direction Vector** against the **Actual Estimated Trajectory**.
- Color-coded: Cyan for intended/forward movement; Red for backwards recoil or bounce.
- Overlays the **Modified / What-If Trajectory** in bright green when running experiments.

### 4. 6-Stage Kinematics Pipeline Breakdown
Inspects the mathematical transformation at every stage to see exactly where errors originate:
1. *Stage 1:* Raw Accel + Dynamic Gravity Removal $\to$ Linear Acceleration
2. *Stage 2:* Static Bias Subtraction $\to$ Unbiased Acceleration
3. *Stage 3:* Deadzone Thresholding $\to$ Effective Acceleration
4. *Stage 4:* Continuous Time-Based Integration $\to$ Velocity
5. *Stage 5:* Directional Braking Clamp / ZUPT $\to$ Cleaned Velocity
6. *Stage 6:* Ballistics + Wrist Pivot $\to$ Cursor Coordinates $(X, Y)$

### 5. Ranked Root-Cause Diagnostics
Automatically evaluates the data and ranks suspected tracking issues:
- **Severity Badge:** `CRITICAL`, `WARNING`, or `INFO`.
- **Confidence Rating:** `HIGH`, `MEDIUM`, or `AMBIGUOUS` (highlights uncertainty when IMU data alone cannot distinguish multiple causes).
- **Supporting Evidence:** Cites exact timestamps, peak recoil velocities, noise floors, and drift ratios.
- **Concrete Recommendations:** Specific algorithmic parameter and code adjustments to fix the issue.

### 6. Algorithm Experimentation Sandbox (What-If Reprocessor)
Experiment with algorithmic tuning without repeating physical movements:
- Adjust **Deadzone**, **Damping Half-Life**, **Sensitivity**, **ZUPT Variance Threshold**, and **ZUPT Latency**.
- Tap **⚡ Reprocess with Experimental Parameters**:
  - The actual `MotionProcessor` re-runs on the recorded raw sensor stream in milliseconds.
  - Compares the new trajectory side-by-side with the original recording.
  - Displays delta improvement badges (% reduction in recoil, % change in displacement, angle error delta).

### 7. Directional Pair Asymmetry Comparison
Compares opposing test pairs (**UP vs DOWN** and **LEFT vs RIGHT**):
- Identifies whether cross-axis drift is caused by physical hand curvature or uncompensated desk tilt.
- Compares push vs pull travel ratios.

### 8. Diagnostic Report Export
Tap **📄 Export Report** to generate a comprehensive Markdown diagnostics report containing summary statistics, detected issues, and recommended fixes.

---

## 🚀 Running on Your Smartphone / Computer

Mobile browsers (Android Chrome, iOS Safari) strictly require a **Secure Context** (`https://` or `localhost`) to access motion sensors (`DeviceMotionEvent`).

### Recommended Method: Android via ADB Reverse

1. Connect your Android phone to your PC via USB with USB Debugging enabled.
2. Start the local server:
   ```bash
   python3 web/serve.py
   ```
3. In another terminal, reverse port 8000:
   ```bash
   adb reverse tcp:8000 tcp:8000
   ```
4. Access the web app:
   - **On your phone:** `http://localhost:8000` (record directional tests).
   - **On your computer / phone:** `http://localhost:8000/analytics.html` (advanced analytics studio).

---

## 🤖 Running Automated Unit Tests

```bash
# Kinematics engine tests (5/5 tests)
node web/tests/kinematics.test.js

# Analytics & diagnostics tests (4/4 tests)
node web/tests/analytics.test.js

# Advanced analytics engine & What-If sandbox tests (4/4 tests)
node web/tests/advanced-analytics.test.js
```
