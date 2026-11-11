/**
 * AirPointer Sensor Manager
 *
 * Manages DeviceMotionEvent and DeviceOrientationEvent lifecycle:
 * - Browser capability and secure context detection
 * - iOS 13+ permission request flow
 * - Event listener management and real-time sampling rate calculation
 */

export class SensorManager {
  constructor(options = {}) {
    this.onMotionSample = options.onMotionSample ?? null;
    this.onStatusChange = options.onStatusChange ?? null;

    this.isListening = false;
    this.sampleCount = 0;
    this.sampleRateHz = 0;
    this.lastSampleTime = 0;

    // Rolling window for sample rate calculation
    this.recentTimestamps = [];

    this._boundMotionHandler = this._handleDeviceMotion.bind(this);
  }

  static isSupported() {
    return typeof window !== 'undefined' && 'DeviceMotionEvent' in window;
  }

  static isSecureContext() {
    return typeof window !== 'undefined' && (window.isSecureContext || window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');
  }

  static requiresPermissionPrompt() {
    return typeof DeviceMotionEvent !== 'undefined' &&
           typeof DeviceMotionEvent.requestPermission === 'function';
  }

  async requestPermission() {
    if (SensorManager.requiresPermissionPrompt()) {
      try {
        const response = await DeviceMotionEvent.requestPermission();
        if (response === 'granted') {
          return { granted: true };
        } else {
          return { granted: false, error: 'Permission denied by user' };
        }
      } catch (err) {
        return { granted: false, error: err.message || 'Permission request failed' };
      }
    }
    return { granted: true };
  }

  start() {
    if (this.isListening) return;

    if (!SensorManager.isSupported()) {
      if (this.onStatusChange) {
        this.onStatusChange({
          error: 'DeviceMotionEvent is not supported on this browser or platform.'
        });
      }
      return false;
    }

    if (!SensorManager.isSecureContext()) {
      console.warn('AirPointer: Sensor events may be blocked because this is an insecure context.');
    }

    this.sampleCount = 0;
    this.recentTimestamps = [];
    this.lastSampleTime = performance.now();

    window.addEventListener('devicemotion', this._boundMotionHandler, { passive: true });
    this.isListening = true;

    if (this.onStatusChange) {
      this.onStatusChange({ isListening: true, error: null });
    }
    return true;
  }

  stop() {
    if (!this.isListening) return;

    window.removeEventListener('devicemotion', this._boundMotionHandler);
    this.isListening = false;
    this.sampleRateHz = 0;
    this.recentTimestamps = [];

    if (this.onStatusChange) {
      this.onStatusChange({ isListening: false, error: null });
    }
  }

  _handleDeviceMotion(event) {
    const now = performance.now();
    this.sampleCount++;

    // Calculate rolling sample rate (Hz) over last 1.0s
    this.recentTimestamps.push(now);
    const oneSecAgo = now - 1000;
    while (this.recentTimestamps.length > 0 && this.recentTimestamps[0] < oneSecAgo) {
      this.recentTimestamps.shift();
    }
    this.sampleRateHz = this.recentTimestamps.length;

    // Extract acceleration data
    const linear = event.acceleration;
    const raw = event.accelerationIncludingGravity;
    const rot = event.rotationRate;

    const sample = {
      linearAx: linear ? linear.x : null,
      linearAy: linear ? linear.y : null,
      linearAz: linear ? linear.z : null,
      rawAx: raw ? (raw.x || 0) : 0,
      rawAy: raw ? (raw.y || 0) : 0,
      rawAz: raw ? (raw.z || 0) : 9.8,
      rotationRateZ: rot ? (rot.alpha || rot.gamma || 0) : 0,
      timestamp: event.timeStamp > 0 ? event.timeStamp : now,
      sampleRateHz: this.sampleRateHz
    };

    if (this.onMotionSample) {
      this.onMotionSample(sample);
    }
  }
}
