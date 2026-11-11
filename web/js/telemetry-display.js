/**
 * AirPointer Telemetry Display HUD
 *
 * Efficiently updates live diagnostics:
 * - Sampling frequency (Hz)
 * - Acceleration (raw and linear m/s^2)
 * - Gyroscope rotation rate (rad/s)
 * - Integrated velocity (m/s)
 * - Cursor coordinates (subpixel precision)
 * - Movement state badge (CALIBRATING, STATIONARY, MOVING, STOPPING)
 */

import { MotionState } from './motion-processor.js';

export class TelemetryDisplay {
  constructor(elements = {}) {
    this.el = {
      stateBadge: elements.stateBadge,
      sampleRate: elements.sampleRate,
      rawAccel: elements.rawAccel,
      linearAccel: elements.linearAccel,
      gyroRate: elements.gyroRate,
      velocity: elements.velocity,
      cursorPos: elements.cursorPos,
      deadzoneVal: elements.deadzoneVal,
      calibProgress: elements.calibProgress,
      calibStatus: elements.calibStatus
    };

    this.pendingData = null;
    this.isRafScheduled = false;
  }

  update(telemetry, cursorCoords, sampleRateHz) {
    this.pendingData = { telemetry, cursorCoords, sampleRateHz };
    if (!this.isRafScheduled) {
      this.isRafScheduled = true;
      requestAnimationFrame(() => this._render());
    }
  }

  _render() {
    this.isRafScheduled = false;
    if (!this.pendingData) return;

    const { telemetry, cursorCoords, sampleRateHz } = this.pendingData;

    // 1. State badge
    if (this.el.stateBadge) {
      this.el.stateBadge.textContent = telemetry.state;
      this.el.stateBadge.className = 'state-badge ' + this._getStateClass(telemetry.state);
    }

    // 2. Sampling Rate
    if (this.el.sampleRate) {
      this.el.sampleRate.textContent = `${sampleRateHz || 0} Hz`;
    }

    // 3. Acceleration
    if (this.el.rawAccel) {
      this.el.rawAccel.textContent = `X: ${telemetry.rawAx.toFixed(2)} | Y: ${telemetry.rawAy.toFixed(2)}`;
    }
    if (this.el.linearAccel) {
      this.el.linearAccel.textContent = `X: ${telemetry.linearAx.toFixed(2)} | Y: ${telemetry.linearAy.toFixed(2)}`;
    }

    // 4. Gyroscope Z
    if (this.el.gyroRate) {
      this.el.gyroRate.textContent = `${telemetry.gyroZ.toFixed(2)} rad/s`;
    }

    // 5. Velocity
    if (this.el.velocity) {
      this.el.velocity.textContent = `Vx: ${telemetry.velX.toFixed(2)} | Vy: ${telemetry.velY.toFixed(2)}`;
    }

    // 6. Cursor Position
    if (this.el.cursorPos && cursorCoords) {
      this.el.cursorPos.textContent = `X: ${cursorCoords.x.toFixed(1)} | Y: ${cursorCoords.y.toFixed(1)}`;
    }
  }

  setCalibrationProgress(current, total) {
    if (this.el.calibProgress) {
      const pct = Math.round((current / total) * 100);
      this.el.calibProgress.style.width = `${pct}%`;
    }
    if (this.el.calibStatus) {
      this.el.calibStatus.textContent = `Calibrating: keep phone still... (${current}/${total})`;
    }
  }

  setCalibrationFinished(calibData) {
    if (this.el.calibProgress) {
      this.el.calibProgress.style.width = '100%';
    }
    if (this.el.calibStatus) {
      this.el.calibStatus.textContent = `Calibrated! Deadzone: ${calibData.deadzone.toFixed(3)} m/s²`;
    }
    if (this.el.deadzoneVal) {
      this.el.deadzoneVal.textContent = calibData.deadzone.toFixed(3);
    }
  }

  _getStateClass(state) {
    switch (state) {
      case MotionState.CALIBRATING:
        return 'state-calibrating';
      case MotionState.STATIONARY:
        return 'state-stationary';
      case MotionState.MOVING:
        return 'state-moving';
      case MotionState.STOPPING:
        return 'state-stopping';
      default:
        return '';
    }
  }
}
