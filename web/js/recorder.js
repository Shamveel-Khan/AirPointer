/**
 * AirPointer Directional Test Recorder
 *
 * Captures high-frequency real sensor and kinematics data during structured
 * directional movement tests (UP, DOWN, LEFT, RIGHT).
 *
 * Preserves every raw hardware event and intermediate pipeline variable
 * with microsecond/millisecond precision without rounding.
 */

export const TestDirection = {
  UP: 'UP',
  DOWN: 'DOWN',
  LEFT: 'LEFT',
  RIGHT: 'RIGHT'
};

export const RecordingPhase = {
  IDLE: 'IDLE',
  COUNTDOWN: 'COUNTDOWN',
  RECORDING: 'RECORDING',
  COMPLETED: 'COMPLETED'
};

export class TestRecorder {
  constructor(options = {}) {
    this.storageKey = options.storageKey ?? 'airpointer_test_recordings_v1';
    this.countdownSeconds = options.countdownSeconds ?? 3;
    this.recordingDurationMs = options.recordingDurationMs ?? 3000;

    this.currentPhase = RecordingPhase.IDLE;
    this.currentDirection = null;
    this.countdownRemaining = 0;
    this.recordingStartTime = 0;

    // Active recording buffer
    this.activeSamples = [];

    // Stored test recordings keyed by direction: { UP: {...}, DOWN: {...}, ... }
    this.recordings = this._loadFromStorage();

    // Callbacks
    this.onPhaseChange = options.onPhaseChange ?? null;
    this.onCountdownTick = options.onCountdownTick ?? null;
    this.onSampleRecorded = options.onSampleRecorded ?? null;
    this.onRecordingComplete = options.onRecordingComplete ?? null;

    this._timerId = null;
  }

  getRecording(direction) {
    return this.recordings[direction] || null;
  }

  getAllRecordings() {
    return this.recordings;
  }

  hasAllFourTests() {
    return !!(this.recordings.UP && this.recordings.DOWN && this.recordings.LEFT && this.recordings.RIGHT);
  }

  getCompletedDirections() {
    return Object.keys(this.recordings);
  }

  /**
   * Start the testing sequence for a specific direction.
   *
   * @param {string} direction - UP, DOWN, LEFT, or RIGHT
   */
  startTest(direction) {
    if (!TestDirection[direction]) {
      throw new Error(`Invalid direction: ${direction}`);
    }

    this._clearTimer();
    this.currentDirection = direction;
    this.activeSamples = [];
    this.countdownRemaining = this.countdownSeconds;
    this._setPhase(RecordingPhase.COUNTDOWN);

    if (this.onCountdownTick) {
      this.onCountdownTick(this.countdownRemaining, this.currentDirection);
    }

    // Countdown loop (3, 2, 1, GO)
    this._timerId = setInterval(() => {
      this.countdownRemaining--;
      if (this.countdownRemaining > 0) {
        if (this.onCountdownTick) {
          this.onCountdownTick(this.countdownRemaining, this.currentDirection);
        }
      } else {
        clearInterval(this._timerId);
        this._startActiveRecording();
      }
    }, 1000);
  }

  _startActiveRecording() {
    this._setPhase(RecordingPhase.RECORDING);
    this.recordingStartTime = performance.now();
    this.activeSamples = [];

    // Optional haptic vibration if supported
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try { navigator.vibrate(80); } catch (_) {}
    }

    // Set auto-stop timer after recordingDurationMs
    this._timerId = setTimeout(() => {
      this.stopTest();
    }, this.recordingDurationMs);
  }

  /**
   * Called on every hardware sensor event while recording is active.
   * Ingests both raw hardware inputs and full intermediate processor outputs.
   */
  recordSample(rawInput, processedOutput, cursorState) {
    if (this.currentPhase !== RecordingPhase.RECORDING) return;

    const relTimeMs = performance.now() - this.recordingStartTime;

    const sample = {
      t: relTimeMs, // milliseconds from recording start
      timestamp: rawInput.timestamp,
      dt: processedOutput.dt,

      // Raw Sensor Inputs
      rawAx: rawInput.rawAx,
      rawAy: rawInput.rawAy,
      rawAz: rawInput.rawAz,
      linearAx: rawInput.linearAx,
      linearAy: rawInput.linearAy,
      linearAz: rawInput.linearAz,
      rotationRateZ: rawInput.rotationRateZ,

      // Intermediate Kinematics Values
      unbiasedAx: processedOutput.unbiasedAx,
      unbiasedAy: processedOutput.unbiasedAy,
      gyroZ: processedOutput.gyroZ,
      velX: processedOutput.velX,
      velY: processedOutput.velY,
      speed: processedOutput.speed,
      state: processedOutput.state,

      // Cursor Output
      dx: processedOutput.dx,
      dy: processedOutput.dy,
      cursorX: cursorState ? cursorState.x : 0,
      cursorY: cursorState ? cursorState.y : 0
    };

    this.activeSamples.push(sample);

    if (this.onSampleRecorded) {
      this.onSampleRecorded(sample, relTimeMs, this.recordingDurationMs);
    }
  }

  stopTest() {
    this._clearTimer();
    if (this.currentPhase !== RecordingPhase.RECORDING) {
      this._setPhase(RecordingPhase.IDLE);
      return;
    }

    const duration = performance.now() - this.recordingStartTime;
    const testRecord = {
      direction: this.currentDirection,
      recordedAt: new Date().toISOString(),
      durationMs: duration,
      sampleCount: this.activeSamples.length,
      averageSampleRateHz: duration > 0 ? (this.activeSamples.length / (duration / 1000.0)) : 0,
      samples: this.activeSamples
    };

    // Save to persistent storage
    this.recordings[this.currentDirection] = testRecord;
    this._saveToStorage();

    this._setPhase(RecordingPhase.COMPLETED);

    // Optional haptic cue
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try { navigator.vibrate([40, 60, 40]); } catch (_) {}
    }

    if (this.onRecordingComplete) {
      this.onRecordingComplete(testRecord);
    }
  }

  cancelTest() {
    this._clearTimer();
    this.activeSamples = [];
    this.currentDirection = null;
    this._setPhase(RecordingPhase.IDLE);
  }

  deleteRecording(direction) {
    if (this.recordings[direction]) {
      delete this.recordings[direction];
      this._saveToStorage();
    }
  }

  clearAllRecordings() {
    this.cancelTest();
    this.recordings = {};
    this._saveToStorage();
  }

  _setPhase(phase) {
    this.currentPhase = phase;
    if (this.onPhaseChange) {
      this.onPhaseChange(phase, this.currentDirection);
    }
  }

  _clearTimer() {
    if (this._timerId) {
      clearInterval(this._timerId);
      clearTimeout(this._timerId);
      this._timerId = null;
    }
  }

  _saveToStorage() {
    if (typeof localStorage === 'undefined') return;
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(this.recordings));
    } catch (e) {
      console.warn('Failed to save recordings to localStorage (quota or disabled):', e);
    }
  }

  _loadFromStorage() {
    if (typeof localStorage === 'undefined') return {};
    try {
      const data = localStorage.getItem(this.storageKey);
      return data ? JSON.parse(data) : {};
    } catch (e) {
      console.warn('Failed to load recordings from localStorage:', e);
      return {};
    }
  }

  /**
   * Export all recorded tests as a single structured JSON string.
   */
  exportToJSON() {
    const payload = {
      app: 'AirPointer Desk Mouse Tester',
      version: '2.0-analytics',
      exportedAt: new Date().toISOString(),
      tests: this.recordings
    };
    return JSON.stringify(payload, null, 2);
  }

  /**
   * Export a specific test recording (or all tests concatenated) as CSV.
   *
   * @param {string|null} direction - specific direction, or null for all
   */
  exportToCSV(direction = null) {
    const testsToExport = direction && this.recordings[direction]
      ? [this.recordings[direction]]
      : Object.values(this.recordings);

    const headers = [
      'direction',
      'time_ms',
      'raw_ax',
      'raw_ay',
      'raw_az',
      'linear_ax',
      'linear_ay',
      'linear_az',
      'gyro_z',
      'unbiased_ax',
      'unbiased_ay',
      'vel_x',
      'vel_y',
      'speed',
      'dx',
      'dy',
      'cursor_x',
      'cursor_y',
      'state'
    ];

    const rows = [headers.join(',')];

    for (const test of testsToExport) {
      for (const s of test.samples) {
        rows.push([
          test.direction,
          s.t.toFixed(1),
          s.rawAx.toFixed(4),
          s.rawAy.toFixed(4),
          s.rawAz.toFixed(4),
          (s.linearAx ?? 0).toFixed(4),
          (s.linearAy ?? 0).toFixed(4),
          (s.linearAz ?? 0).toFixed(4),
          s.rotationRateZ.toFixed(4),
          s.unbiasedAx.toFixed(4),
          s.unbiasedAy.toFixed(4),
          s.velX.toFixed(4),
          s.velY.toFixed(4),
          s.speed.toFixed(4),
          s.dx.toFixed(4),
          s.dy.toFixed(4),
          s.cursorX.toFixed(2),
          s.cursorY.toFixed(2),
          s.state
        ].join(','));
      }
    }

    return rows.join('\n');
  }
}
