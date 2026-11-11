/**
 * AirPointer Desk Mouse Kinematics Processor
 *
 * Implements 5 critical fixes over the Android Desk Mouse implementation:
 * 1. Gravity compensation: Separates linear motion from gravitational vector.
 * 2. Stationary bias calibration: Computes baseline at rest and freezes during motion.
 * 3. Continuous time-based velocity integration: Replaces per-frame friction with continuous dt exponential decay.
 * 4. Intelligent Zero Velocity Update (ZUPT): Windowed variance + gyro rate detection to reset velocity cleanly.
 * 5. Fractional cursor accumulation: Full floating-point subpixel resolution without premature rounding/clamping.
 */

export const MotionState = {
  CALIBRATING: 'CALIBRATING',
  STATIONARY: 'STATIONARY',
  MOVING: 'MOVING',
  STOPPING: 'STOPPING'
};

export class MotionProcessor {
  constructor(options = {}) {
    // Configuration
    this.sensitivity = options.sensitivity ?? 1.4;
    this.frictionHalfLife = options.frictionHalfLife ?? 0.065; // Seconds to decay 50% velocity
    this.zuptWindowMs = options.zuptWindowMs ?? 120;           // Rolling window for variance calculation
    this.zuptAccelVarThreshold = options.zuptAccelVarThreshold ?? 0.008; // (m/s^2)^2 variance
    this.zuptGyroThreshold = options.zuptGyroThreshold ?? 0.12;          // rad/s
    this.zuptMinStationaryMs = options.zuptMinStationaryMs ?? 60;        // Min duration below threshold to trigger ZUPT
    this.deadzone = options.deadzone ?? 0.06;                            // Minimum effective accel (m/s^2)
    this.wristPivotAssist = options.wristPivotAssist ?? true;
    this.ballisticsEnabled = options.ballisticsEnabled ?? true;

    // State
    this.state = MotionState.STATIONARY;
    this.velX = 0;
    this.velY = 0;
    this.lastTimestamp = 0;
    this.intentX = 0; // +1 = moving right, -1 = moving left, 0 = neutral
    this.intentY = 0; // +1 = moving forward, -1 = moving back, 0 = neutral

    // Filtered outputs
    this.filteredDx = 0;
    this.filteredDy = 0;
    this.deskSpeed = 0;

    // Calibration
    this.isCalibrating = false;
    this.calibrationSamples = [];
    this.targetCalibrationSamples = 80;
    this.biasAx = 0;
    this.biasAy = 0;
    this.biasAz = 0;
    this.biasGz = 0;
    this.calibratedNoiseStd = 0.02;

    // Dynamic Gravity Estimator (fallback if browser only gives accelerationIncludingGravity)
    this.gravX = 0;
    this.gravY = 0;
    this.gravZ = 9.80665;
    this.gravityInitialized = false;

    // ZUPT Ring Buffer
    this.zuptBuffer = []; // { time, mag, gz }
    this.stationaryStartTime = null;

    // Callbacks
    this.onCalibrationProgress = options.onCalibrationProgress ?? null;
    this.onCalibrationComplete = options.onCalibrationComplete ?? null;
  }

  startCalibration(sampleCount = 80) {
    this.isCalibrating = true;
    this.state = MotionState.CALIBRATING;
    this.calibrationSamples = [];
    this.targetCalibrationSamples = sampleCount;
    this.velX = 0;
    this.velY = 0;
    this.intentX = 0;
    this.intentY = 0;
    this.filteredDx = 0;
    this.filteredDy = 0;
  }

  /**
   * Main motion ingestion method.
   *
   * @param {Object} input
   * @param {number|null} input.linearAx - Linear acceleration X without gravity (m/s^2)
   * @param {number|null} input.linearAy - Linear acceleration Y without gravity (m/s^2)
   * @param {number|null} input.linearAz - Linear acceleration Z without gravity (m/s^2)
   * @param {number} input.rawAx - Raw acceleration X including gravity (m/s^2)
   * @param {number} input.rawAy - Raw acceleration Y including gravity (m/s^2)
   * @param {number} input.rawAz - Raw acceleration Z including gravity (m/s^2)
   * @param {number} input.rotationRateZ - Gyroscope Z rotation rate (deg/s or rad/s)
   * @param {number} input.timestamp - Event timestamp in ms (performance.now() or event.timeStamp)
   * @returns {Object} Processed kinematics telemetry and cursor deltas
   */
  processSample({ linearAx, linearAy, linearAz, rawAx, rawAy, rawAz, rotationRateZ = 0, timestamp }) {
    // 1. Calculate continuous dt
    const now = timestamp || performance.now();
    let dt = 0.016; // default 60Hz frame
    if (this.lastTimestamp > 0) {
      dt = (now - this.lastTimestamp) / 1000.0;
      // Clamp dt to avoid time-travel or huge sleep spikes
      dt = Math.max(0.002, Math.min(0.060, dt));
    }
    this.lastTimestamp = now;

    // Standardize Gyro Z to rad/s (DeviceMotionEvent gives deg/s)
    const gzRad = (rotationRateZ * Math.PI) / 180.0;

    // 2. CRITICAL FIX 1: Gravity compensation
    let ax = 0;
    let ay = 0;
    let az = 0;

    const hasValidLinear = linearAx !== null && linearAx !== undefined &&
                          (linearAx !== 0 || linearAy !== 0 || linearAz !== 0);

    if (hasValidLinear) {
      ax = linearAx;
      ay = linearAy;
      az = linearAz || 0;
    } else {
      // Fallback: estimate gravity vector via low-pass filter and subtract
      if (!this.gravityInitialized) {
        this.gravX = rawAx;
        this.gravY = rawAy;
        this.gravZ = rawAz;
        this.gravityInitialized = true;
      } else {
        const alpha = 0.96;
        this.gravX = alpha * this.gravX + (1.0 - alpha) * rawAx;
        this.gravY = alpha * this.gravY + (1.0 - alpha) * rawAy;
        this.gravZ = alpha * this.gravZ + (1.0 - alpha) * rawAz;
      }
      ax = rawAx - this.gravX;
      ay = rawAy - this.gravY;
      az = rawAz - this.gravZ;
    }

    // 3. CRITICAL FIX 2: Stationary Bias Calibration
    if (this.isCalibrating) {
      this.calibrationSamples.push({ ax, ay, az, gz: gzRad });
      const count = this.calibrationSamples.length;

      if (this.onCalibrationProgress) {
        this.onCalibrationProgress(count, this.targetCalibrationSamples);
      }

      if (count >= this.targetCalibrationSamples) {
        // Compute mean offsets
        let sumAx = 0, sumAy = 0, sumAz = 0, sumGz = 0;
        for (const s of this.calibrationSamples) {
          sumAx += s.ax;
          sumAy += s.ay;
          sumAz += s.az;
          sumGz += s.gz;
        }
        this.biasAx = sumAx / count;
        this.biasAy = sumAy / count;
        this.biasAz = sumAz / count;
        this.biasGz = sumGz / count;

        // Compute noise standard deviation for dynamic deadzone
        let sumSqErr = 0;
        for (const s of this.calibrationSamples) {
          const errX = s.ax - this.biasAx;
          const errY = s.ay - this.biasAy;
          sumSqErr += (errX * errX + errY * errY) / 2.0;
        }
        this.calibratedNoiseStd = Math.sqrt(sumSqErr / count);
        // Adaptive deadzone: at least 0.04 m/s^2, or 2.5x standard deviation
        this.deadzone = Math.max(0.04, Math.min(0.15, this.calibratedNoiseStd * 2.5));

        this.isCalibrating = false;
        this.state = MotionState.STATIONARY;
        if (this.onCalibrationComplete) {
          this.onCalibrationComplete({
            biasAx: this.biasAx,
            biasAy: this.biasAy,
            biasGz: this.biasGz,
            deadzone: this.deadzone,
            std: this.calibratedNoiseStd
          });
        }
      }

      return {
        dx: 0,
        dy: 0,
        velX: 0,
        velY: 0,
        speed: 0,
        state: this.state,
        rawAx,
        rawAy,
        linearAx: ax,
        linearAy: ay,
        unbiasedAx: 0,
        unbiasedAy: 0,
        gyroZ: gzRad,
        dt
      };
    }

    // Apply stationary bias
    const unbiasedAx = ax - this.biasAx;
    const unbiasedAy = ay - this.biasAy;
    const unbiasedGz = gzRad - this.biasGz;

    const accelMag = Math.hypot(unbiasedAx, unbiasedAy);
    this.deskSpeed = accelMag;

    // 4. CRITICAL FIX 4: Intelligent Zero Velocity Update (ZUPT)
    // Maintain rolling buffer within zuptWindowMs
    this.zuptBuffer.push({ time: now, mag: accelMag, gz: Math.abs(unbiasedGz) });
    const cutoffTime = now - this.zuptWindowMs;
    while (this.zuptBuffer.length > 0 && this.zuptBuffer[0].time < cutoffTime) {
      this.zuptBuffer.shift();
    }

    // Compute variance of acceleration magnitude in rolling window
    let avgMag = 0;
    let maxGz = 0;
    for (const b of this.zuptBuffer) {
      avgMag += b.mag;
      if (b.gz > maxGz) maxGz = b.gz;
    }
    avgMag = this.zuptBuffer.length > 0 ? avgMag / this.zuptBuffer.length : 0;

    let varMag = 0;
    for (const b of this.zuptBuffer) {
      const diff = b.mag - avgMag;
      varMag += diff * diff;
    }
    varMag = this.zuptBuffer.length > 0 ? varMag / this.zuptBuffer.length : 0;

    const isSubThreshold = (varMag < this.zuptAccelVarThreshold) && (maxGz < this.zuptGyroThreshold) && (accelMag < this.deadzone * 1.5);

    if (isSubThreshold) {
      if (!this.stationaryStartTime) {
        this.stationaryStartTime = now;
      }
      const stationaryDuration = now - this.stationaryStartTime;

      if (stationaryDuration >= this.zuptMinStationaryMs) {
        // CONFIRMED STATIONARY: ZUPT triggers!
        this.state = MotionState.STATIONARY;
        this.velX = 0;
        this.velY = 0;
        this.intentX = 0;
        this.intentY = 0;
        this.filteredDx *= 0.5;
        this.filteredDy *= 0.5;

        // Slow baseline anchor tracking while confirmed stationary (prevents thermal drift)
        this.biasAx += (ax - this.biasAx) * 0.005;
        this.biasAy += (ay - this.biasAy) * 0.005;

        return {
          dx: 0,
          dy: 0,
          velX: 0,
          velY: 0,
          speed: 0,
          state: this.state,
          rawAx,
          rawAy,
          linearAx: ax,
          linearAy: ay,
          unbiasedAx,
          unbiasedAy,
          gyroZ: unbiasedGz,
          dt
        };
      } else {
        this.state = MotionState.STOPPING;
      }
    } else {
      this.stationaryStartTime = null;
      this.state = MotionState.MOVING;
    }

    // 5. CRITICAL FIX 3: Time-based velocity integration & exponential damping
    // Deadzone application
    const effAx = Math.abs(unbiasedAx) > this.deadzone ? unbiasedAx - Math.sign(unbiasedAx) * this.deadzone : 0;
    const effAy = Math.abs(unbiasedAy) > this.deadzone ? unbiasedAy - Math.sign(unbiasedAy) * this.deadzone : 0;

    // Continuous decay lambda from half-life: lambda = ln(2) / t_half
    const lambda = Math.LN2 / Math.max(0.01, this.frictionHalfLife);
    const decayFactor = Math.exp(-lambda * dt);

    // Apply continuous damping and impulse
    this.velX = this.velX * decayFactor + effAx * dt * 14.0;
    this.velY = this.velY * decayFactor + effAy * dt * 14.0;

    // Intelligent Directional Braking Clamp (Anti-Bounce Recoil Prevention):
    // If moving in intent direction, opposing acceleration is recognized as braking.
    // While braking, velocity cannot cross zero into reverse.
    if (this.intentX === 1 && effAx <= 0) {
      if (this.velX <= 0) {
        this.velX = 0;
        if (effAx === 0) this.intentX = 0;
      }
    } else if (this.intentX === -1 && effAx >= 0) {
      if (this.velX >= 0) {
        this.velX = 0;
        if (effAx === 0) this.intentX = 0;
      }
    } else if (effAx !== 0) {
      this.intentX = Math.sign(effAx);
    }

    if (this.intentY === 1 && effAy <= 0) {
      if (this.velY <= 0) {
        this.velY = 0;
        if (effAy === 0) this.intentY = 0;
      }
    } else if (this.intentY === -1 && effAy >= 0) {
      if (this.velY >= 0) {
        this.velY = 0;
        if (effAy === 0) this.intentY = 0;
      }
    } else if (effAy !== 0) {
      this.intentY = Math.sign(effAy);
    }

    // 6. Ballistics acceleration curve & Wrist arc pivot assist
    let speedFactor = 1.0;
    if (this.ballisticsEnabled) {
      if (accelMag < 0.25) {
        speedFactor = 0.85;
      } else if (accelMag > 1.6) {
        speedFactor = Math.min(2.6, 1.3 + (accelMag - 1.6) * 0.45);
      } else {
        speedFactor = 1.0 + (accelMag - 0.25) * 0.22;
      }
    }

    // Wrist arc pivot assist from Gyroscope Z:
    // When pivoting wrist, Gyro Z detects rotation around desk vertical axis
    let pivotDx = 0;
    if (this.wristPivotAssist && Math.abs(unbiasedGz) > 0.05) {
      pivotDx = -unbiasedGz * this.sensitivity * 24.0;
    }

    const sensitivityMultiplier = this.sensitivity * 30.0;

    // Smartphone coordinate mapping:
    // Moving phone right (+X) -> PC cursor moves right (+X)
    // Moving phone forward (+Y) -> PC cursor moves up (-Y in screen pixel coordinates)
    const targetDx = (this.velX * sensitivityMultiplier * speedFactor) + pivotDx;
    const targetDy = (-this.velY * sensitivityMultiplier * speedFactor);

    // Exponential smoothing for cursor output
    const ALPHA = 0.65;
    this.filteredDx = ALPHA * targetDx + (1.0 - ALPHA) * this.filteredDx;
    this.filteredDy = ALPHA * targetDy + (1.0 - ALPHA) * this.filteredDy;

    // 7. CRITICAL FIX 5: Fractional cursor accumulation
    // Returns full-precision floating point deltas (no premature roundToInt clamp)
    return {
      dx: this.filteredDx,
      dy: this.filteredDy,
      velX: this.velX,
      velY: this.velY,
      speed: Math.hypot(this.velX, this.velY),
      state: this.state,
      rawAx,
      rawAy,
      linearAx: ax,
      linearAy: ay,
      unbiasedAx,
      unbiasedAy,
      gyroZ: unbiasedGz,
      dt
    };
  }

  reset() {
    this.velX = 0;
    this.velY = 0;
    this.intentX = 0;
    this.intentY = 0;
    this.filteredDx = 0;
    this.filteredDy = 0;
    this.deskSpeed = 0;
    this.lastTimestamp = 0;
    this.stationaryStartTime = null;
    this.zuptBuffer = [];
    this.state = MotionState.STATIONARY;
  }
}
