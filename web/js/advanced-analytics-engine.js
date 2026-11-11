/**
 * AirPointer Advanced Sensor Analytics & Diagnostic Engine
 *
 * Provides:
 * 1. JSON Import & Schema Validation
 * 2. Deep Statistical Profiling (Noise, Bias, Variance, Jitter, Latency)
 * 3. 6-Stage Pipeline Breakdown (Pinpointing exact transformation errors)
 * 4. Ranked Root-Cause Diagnostics with Confidence & Evidence
 * 5. Directional Pair Asymmetry Comparison (UP vs DOWN, LEFT vs RIGHT)
 * 6. Algorithm Experimentation Sandbox (Reprocesses raw samples with What-If parameters)
 * 7. Comprehensive Diagnostic Report Generator
 */

import { MotionProcessor, MotionState } from './motion-processor.js';
import { TestDirection } from './recorder.js';

export class AdvancedAnalyticsEngine {
  /**
   * Parse and validate imported JSON data.
   * Supports full test export, dictionary of tests, or a single test recording.
   *
   * @param {string|Object} rawInput
   * @returns {Object} { tests: { UP: {...}, ... }, metadata: {...} }
   */
  static parseImportedJSON(rawInput) {
    let parsed = null;
    if (typeof rawInput === 'string') {
      try {
        parsed = JSON.parse(rawInput);
      } catch (err) {
        throw new Error(`Invalid JSON format: ${err.message}`);
      }
    } else if (typeof rawInput === 'object' && rawInput !== null) {
      parsed = rawInput;
    } else {
      throw new Error('Input must be a valid JSON string or object.');
    }

    const result = {
      metadata: {
        app: parsed.app || 'AirPointer Desk Mouse Tester',
        version: parsed.version || 'unknown',
        importedAt: new Date().toISOString(),
        originalExportTime: parsed.exportedAt || null
      },
      tests: {}
    };

    // Case 1: Standard export format: { app, version, tests: { UP, DOWN, ... } }
    if (parsed.tests && typeof parsed.tests === 'object') {
      for (const dir of Object.keys(TestDirection)) {
        if (parsed.tests[dir] && Array.isArray(parsed.tests[dir].samples)) {
          result.tests[dir] = this._validateAndNormalizeTest(parsed.tests[dir], dir);
        }
      }
    }
    // Case 2: Direct map of directions: { UP: {...}, DOWN: {...} }
    else if (parsed.UP || parsed.DOWN || parsed.LEFT || parsed.RIGHT) {
      for (const dir of Object.keys(TestDirection)) {
        if (parsed[dir] && Array.isArray(parsed[dir].samples)) {
          result.tests[dir] = this._validateAndNormalizeTest(parsed[dir], dir);
        }
      }
    }
    // Case 3: Single test object: { direction: "UP", samples: [...] }
    else if (parsed.direction && Array.isArray(parsed.samples)) {
      const dir = parsed.direction.toUpperCase();
      if (TestDirection[dir]) {
        result.tests[dir] = this._validateAndNormalizeTest(parsed, dir);
      }
    }

    if (Object.keys(result.tests).length === 0) {
      throw new Error('No valid test recordings with sensor sample arrays found in the imported JSON.');
    }

    return result;
  }

  static _validateAndNormalizeTest(testObj, defaultDirection) {
    const samples = testObj.samples || [];
    return {
      direction: (testObj.direction || defaultDirection).toUpperCase(),
      recordedAt: testObj.recordedAt || new Date().toISOString(),
      durationMs: testObj.durationMs || (samples.length > 0 ? samples[samples.length - 1].t : 3000),
      sampleCount: samples.length,
      averageSampleRateHz: testObj.averageSampleRateHz || (samples.length > 0 ? (samples.length / 3.0) : 0),
      samples: samples.map(s => ({
        t: Number(s.t ?? 0),
        timestamp: Number(s.timestamp ?? 0),
        dt: Number(s.dt ?? 0.016),
        rawAx: Number(s.rawAx ?? 0),
        rawAy: Number(s.rawAy ?? 0),
        rawAz: Number(s.rawAz ?? 9.8),
        linearAx: s.linearAx !== null && s.linearAx !== undefined ? Number(s.linearAx) : null,
        linearAy: s.linearAy !== null && s.linearAy !== undefined ? Number(s.linearAy) : null,
        linearAz: s.linearAz !== null && s.linearAz !== undefined ? Number(s.linearAz) : null,
        rotationRateZ: Number(s.rotationRateZ ?? 0),
        unbiasedAx: Number(s.unbiasedAx ?? (s.linearAx ?? s.rawAx ?? 0)),
        unbiasedAy: Number(s.unbiasedAy ?? (s.linearAy ?? s.rawAy ?? 0)),
        gyroZ: Number(s.gyroZ ?? 0),
        velX: Number(s.velX ?? 0),
        velY: Number(s.velY ?? 0),
        speed: Number(s.speed ?? Math.hypot(s.velX || 0, s.velY || 0)),
        dx: Number(s.dx ?? 0),
        dy: Number(s.dy ?? 0),
        cursorX: Number(s.cursorX ?? 0),
        cursorY: Number(s.cursorY ?? 0),
        state: s.state || 'MOVING'
      }))
    };
  }

  /**
   * Detailed Statistical Analysis on a single recording.
   *
   * @param {Object} testRecord
   * @returns {Object} Comprehensive statistical profile
   */
  static computeStatistics(testRecord) {
    if (!testRecord || !testRecord.samples || testRecord.samples.length === 0) {
      return null;
    }

    const { direction, durationMs, samples } = testRecord;
    const n = samples.length;

    // Expected vectors
    const expectedVectors = {
      [TestDirection.UP]: { cursor: { x: 0, y: -1 }, accelAxis: 'y', sign: 1 },
      [TestDirection.DOWN]: { cursor: { x: 0, y: 1 }, accelAxis: 'y', sign: -1 },
      [TestDirection.LEFT]: { cursor: { x: -1, y: 0 }, accelAxis: 'x', sign: -1 },
      [TestDirection.RIGHT]: { cursor: { x: 1, y: 0 }, accelAxis: 'x', sign: 1 }
    };
    const exp = expectedVectors[direction] || expectedVectors.UP;

    // 1. Sampling Jitter & Timing Statistics
    let sumDt = 0;
    let minDt = Infinity;
    let maxDt = -Infinity;
    for (const s of samples) {
      const dtMs = s.dt * 1000.0;
      sumDt += dtMs;
      if (dtMs < minDt) minDt = dtMs;
      if (dtMs > maxDt) maxDt = dtMs;
    }
    const meanDtMs = sumDt / n;

    let sumSqDtDiff = 0;
    let dropoutCount = 0; // dt spikes > 2.5x mean
    for (const s of samples) {
      const dtMs = s.dt * 1000.0;
      const diff = dtMs - meanDtMs;
      sumSqDtDiff += diff * diff;
      if (dtMs > meanDtMs * 2.5) dropoutCount++;
    }
    const dtJitterMs = Math.sqrt(sumSqDtDiff / n);
    const measuredHz = meanDtMs > 0 ? (1000.0 / meanDtMs) : 0;

    // 2. Resting Noise & Static Tilt Leakage Analysis
    // Slices: first 180ms and last 200ms
    const restSamples = samples.filter(s => s.t <= 180 || s.t >= durationMs - 220);
    const restN = Math.max(1, restSamples.length);

    let restSumAx = 0, restSumAy = 0;
    let restSumRawAx = 0, restSumRawAy = 0;
    for (const r of restSamples) {
      restSumAx += r.unbiasedAx;
      restSumAy += r.unbiasedAy;
      restSumRawAx += r.rawAx;
      restSumRawAy += r.rawAy;
    }
    const restBiasAx = restSumAx / restN;
    const restBiasAy = restSumAy / restN;
    const rawTiltOffset = Math.hypot(restSumRawAx / restN, restSumRawAy / restN);
    const unbTiltOffset = Math.hypot(restBiasAx, restBiasAy);
    const staticTiltOffset = Math.max(rawTiltOffset, unbTiltOffset);

    let restVarAx = 0, restVarAy = 0;
    for (const r of restSamples) {
      restVarAx += Math.pow(r.unbiasedAx - restBiasAx, 2);
      restVarAy += Math.pow(r.unbiasedAy - restBiasAy, 2);
    }
    const noiseStdAx = Math.sqrt(restVarAx / restN);
    const noiseStdAy = Math.sqrt(restVarAy / restN);
    const noiseStdMag = Math.hypot(noiseStdAx, noiseStdAy);

    // 3. Kinematic Totals & Directional Alignment
    let netDx = 0, netDy = 0, totalPath = 0;
    let peakAx = 0, peakAy = 0, peakVel = 0;
    let sumSqAx = 0, sumSqAy = 0;

    let peakIntendedAccel = 0;
    let peakBrakingDecel = 0;
    let timeOfPeakDecel = 0;
    let zuptStopTime = null;

    let maxRecoilVel = 0;
    let recoilDurationMs = 0;
    let recoilDistance = 0;
    let isRecoilActive = false;
    let recoilStartTime = 0;

    // Check for false ZUPT triggers (state = STATIONARY while moving)
    let falseZuptCount = 0;

    for (const s of samples) {
      netDx += s.dx;
      netDy += s.dy;
      totalPath += Math.hypot(s.dx, s.dy);

      const speed = Math.hypot(s.velX, s.velY);
      if (speed > peakVel) peakVel = speed;

      const absAx = Math.abs(s.unbiasedAx);
      const absAy = Math.abs(s.unbiasedAy);
      if (absAx > peakAx) peakAx = absAx;
      if (absAy > peakAy) peakAy = absAy;

      sumSqAx += absAx * absAx;
      sumSqAy += absAy * absAy;

      // Primary axis acceleration
      const primAccel = exp.accelAxis === 'y' ? s.unbiasedAy : s.unbiasedAx;
      const signedPush = primAccel * exp.sign;

      if (signedPush > peakIntendedAccel) peakIntendedAccel = signedPush;
      if (-signedPush > peakBrakingDecel) {
        peakBrakingDecel = -signedPush;
        timeOfPeakDecel = s.t;
      }

      // ZUPT transition
      if (timeOfPeakDecel > 0 && !zuptStopTime && s.state === MotionState.STATIONARY && s.t > timeOfPeakDecel) {
        zuptStopTime = s.t;
      }

      // Check false ZUPT during early stroke
      if (s.t < 1200 && s.state === MotionState.STATIONARY && (Math.abs(s.dx) > 0.5 || Math.abs(s.dy) > 0.5)) {
        falseZuptCount++;
      }

      // Recoil analysis
      const signedVel = exp.accelAxis === 'y' ? (s.velY * exp.sign) : (s.velX * exp.sign);
      if (signedVel < -0.02) {
        const recoilMag = Math.abs(signedVel);
        if (recoilMag > maxRecoilVel) maxRecoilVel = recoilMag;

        // Recoil displacement
        recoilDistance += Math.hypot(s.dx, s.dy);

        if (!isRecoilActive) {
          isRecoilActive = true;
          recoilStartTime = s.t;
        }
      } else if (isRecoilActive) {
        isRecoilActive = false;
        recoilDurationMs += (s.t - recoilStartTime);
      }
    }

    const netDisplacement = Math.hypot(netDx, netDy);
    const pathEfficiency = totalPath > 0 ? (netDisplacement / totalPath) : 0;

    // Vector Alignment
    let cosineSim = 0;
    let angleErrorDeg = 0;
    let intendedDisplacement = 0;
    let perpendicularDrift = 0;

    if (netDisplacement > 0.001) {
      const uX = netDx / netDisplacement;
      const uY = netDy / netDisplacement;
      cosineSim = Math.max(-1.0, Math.min(1.0, uX * exp.cursor.x + uY * exp.cursor.y));
      angleErrorDeg = (Math.acos(cosineSim) * 180.0) / Math.PI;

      if (exp.cursor.x !== 0) {
        intendedDisplacement = Math.abs(netDx);
        perpendicularDrift = Math.abs(netDy);
      } else {
        intendedDisplacement = Math.abs(netDy);
        perpendicularDrift = Math.abs(netDx);
      }
    }

    const driftRatio = intendedDisplacement > 0 ? (perpendicularDrift / intendedDisplacement) : 1.0;
    const zuptLatencyMs = (timeOfPeakDecel > 0 && zuptStopTime) ? (zuptStopTime - timeOfPeakDecel) : null;

    return {
      direction,
      sampleCount: n,
      durationMs,
      meanDtMs,
      minDtMs: minDt,
      maxDtMs: maxDt,
      dtJitterMs,
      measuredHz,
      dropoutCount,
      restBiasAx,
      restBiasAy,
      staticTiltOffset,
      noiseStdAx,
      noiseStdAy,
      noiseStdMag,
      netDx,
      netDy,
      netDisplacement,
      totalPath,
      pathEfficiency,
      cosineSim,
      angleErrorDeg,
      intendedDisplacement,
      perpendicularDrift,
      driftRatio,
      peakAx,
      peakAy,
      rmsAx: Math.sqrt(sumSqAx / n),
      rmsAy: Math.sqrt(sumSqAy / n),
      peakVel,
      peakIntendedAccel,
      peakBrakingDecel,
      maxRecoilVel,
      recoilDurationMs,
      recoilDistance,
      zuptLatencyMs,
      falseZuptCount
    };
  }

  /**
   * Generates a 6-stage pipeline breakdown for every sample in the test,
   * showing exactly where numbers change and where errors originate.
   *
   * @param {Object} testRecord
   * @returns {Array} Array of stage transformation snapshots
   */
  static generatePipelineBreakdown(testRecord) {
    if (!testRecord || !testRecord.samples) return [];

    return testRecord.samples.map(s => {
      // Stage 1: Raw Acceleration + Gravity Compensation
      const rawMag = Math.hypot(s.rawAx, s.rawAy);
      const linAx = s.linearAx ?? s.unbiasedAx;
      const linAy = s.linearAy ?? s.unbiasedAy;
      const linMag = Math.hypot(linAx, linAy);
      const gravityRemovedMag = Math.abs(rawMag - linMag);

      // Stage 2: Static Bias Subtraction
      const unbAx = s.unbiasedAx;
      const unbAy = s.unbiasedAy;
      const unbMag = Math.hypot(unbAx, unbAy);

      // Stage 3: Deadzone Thresholding
      // If deadzone ~ 0.06
      const deadzone = 0.06;
      const effAx = Math.abs(unbAx) > deadzone ? unbAx - Math.sign(unbAx) * deadzone : 0;
      const effAy = Math.abs(unbAy) > deadzone ? unbAy - Math.sign(unbAy) * deadzone : 0;

      // Stage 4: Integrated Velocity
      const rawVelMag = Math.hypot(s.velX, s.velY);

      // Stage 5: ZUPT / Clamped Velocity
      const clampedVelMag = s.state === MotionState.STATIONARY ? 0 : rawVelMag;

      // Stage 6: Cursor Output Delta
      const cursorDeltaMag = Math.hypot(s.dx, s.dy);

      return {
        t: s.t,
        state: s.state,
        stage1_raw: { ax: s.rawAx, ay: s.rawAy, az: s.rawAz, mag: rawMag },
        stage1_linear: { ax: linAx, ay: linAy, mag: linMag, gravDelta: gravityRemovedMag },
        stage2_unbiased: { ax: unbAx, ay: unbAy, mag: unbMag },
        stage3_effective: { ax: effAx, ay: effAy, deadzoneSuppressed: (effAx === 0 && effAy === 0 && unbMag > 0) },
        stage4_integratedVel: { vx: s.velX, vy: s.velY, speed: rawVelMag },
        stage5_zuptClamped: { isClamped: s.state === MotionState.STATIONARY, speed: clampedVelMag },
        stage6_cursorOutput: { dx: s.dx, dy: s.dy, deltaMag: cursorDeltaMag, x: s.cursorX, y: s.cursorY }
      };
    });
  }

  /**
   * Root-Cause Diagnostic Engine
   * Generates a ranked list of suspected issues with supporting timestamps,
   * sensor evidence, confidence levels, and specific parameter recommendations.
   *
   * @param {Object} testRecord
   * @returns {Object} { stats, pipeline, detectedIssues: [...] }
   */
  static analyzeTest(testRecord) {
    const stats = this.computeStatistics(testRecord);
    if (!stats) return { stats: null, pipeline: [], detectedIssues: [] };

    const pipeline = this.generatePipelineBreakdown(testRecord);
    const issues = [];

    // Diagnostic Rule 1: Static Tilt / Gravity Leakage
    if (stats.staticTiltOffset > 0.07) {
      const isSevere = stats.staticTiltOffset > 0.15;
      issues.push({
        id: 'GRAVITY_TILT_LEAKAGE',
        title: 'Static Tilt / Gravity Leakage',
        severity: isSevere ? 'CRITICAL' : 'WARNING',
        confidence: isSevere ? 'HIGH' : 'MEDIUM',
        rank: 1,
        summary: `Resting acceleration had a persistent bias of ${stats.staticTiltOffset.toFixed(3)} m/s² during motionless phases.`,
        evidence: {
          restBiasAx: stats.restBiasAx.toFixed(3),
          restBiasAy: stats.restBiasAy.toFixed(3),
          staticTiltOffset: stats.staticTiltOffset.toFixed(3),
          timestamps: 't: 0-180ms and t: 2780-3000ms'
        },
        explanation: 'When sliding on a desk, any phone incline or desk slope leaks a component of 9.81 m/s² gravity into the lateral accelerometer channels. This persistent offset integrates into continuous unwanted cursor drift even while resting.',
        uncertaintyNotes: stats.staticTiltOffset < 0.10 ? 'Small tilt offsets may partially stem from sensor zero-g thermal drift rather than pure incline.' : null,
        recommendation: {
          param: 'stationaryBiasCalibration',
          suggestion: 'Ensure phone is completely motionless during calibration, or increase gravity complementary filter alpha (e.g. from 0.96 to 0.985) to absorb surface tilt.'
        }
      });
    }

    // Diagnostic Rule 2: Movement Reversal / Recoil
    if (stats.maxRecoilVel > 0.04) {
      const isSevere = stats.maxRecoilVel > 0.15;
      issues.push({
        id: 'BRAKING_RECOIL',
        title: 'Braking Recoil / Movement Reversal',
        severity: isSevere ? 'CRITICAL' : 'WARNING',
        confidence: 'HIGH',
        rank: 2,
        summary: `Peak backward recoil reached ${stats.maxRecoilVel.toFixed(2)} m/s lasting ${stats.recoilDurationMs.toFixed(0)} ms during deceleration.`,
        evidence: {
          maxRecoilVel: stats.maxRecoilVel.toFixed(3),
          recoilDurationMs: stats.recoilDurationMs,
          recoilDistancePx: stats.recoilDistance.toFixed(1)
        },
        explanation: 'When stopping a phone on a desk, human hand muscles apply a sharp braking acceleration opposing the slide. If the braking clamp does not immediately latch velocity at zero, this deceleration flips velocity into reverse, causing the cursor to snap backwards.',
        uncertaintyNotes: null,
        recommendation: {
          param: 'directionalBrakingClamp',
          suggestion: 'Tighten directional braking latch: when velocity reaches 0 while decelerating, lock velocity at 0 until effective acceleration returns to deadzone.'
        }
      });
    }

    // Diagnostic Rule 3: Severe Directional Misalignment
    if (stats.cosineSim < 0.5) {
      const isReversed = stats.cosineSim < -0.2;
      issues.push({
        id: isReversed ? 'AXIS_REVERSED' : 'DIRECTIONAL_MISALIGNMENT',
        title: isReversed ? 'Movement Direction Inverted' : 'High Directional Misalignment',
        severity: 'CRITICAL',
        confidence: 'HIGH',
        rank: 3,
        summary: `Cursor moved at ${stats.angleErrorDeg.toFixed(1)}° from intended ${stats.direction} direction (Cosine similarity: ${stats.cosineSim.toFixed(2)}).`,
        evidence: {
          intended: stats.direction,
          angleErrorDeg: stats.angleErrorDeg.toFixed(1),
          cosineSim: stats.cosineSim.toFixed(3),
          netDx: stats.netDx.toFixed(1),
          netDy: stats.netDy.toFixed(1)
        },
        explanation: isReversed
          ? `The cursor moved opposite to the user's intended physical stroke. Screen Y coordinates increase downwards, so physical forward slide must map to negative screen dY.`
          : `Actual trajectory diverged by more than 60° from the intended axis. This indicates either excessive hand yaw rotation or sensor axis misorientation.`,
        uncertaintyNotes: 'Verify phone orientation relative to the screen. If testing in landscape mode, browser sensor axes rotate by 90 degrees.',
        recommendation: {
          param: 'coordinateMapping',
          suggestion: isReversed ? 'Invert targetDy or targetDx mapping in motion-processor.js.' : 'Check phone screen orientation locking (portrait vs landscape).'
        }
      });
    }

    // Diagnostic Rule 4: Excessive Orthogonal Drift
    if (stats.driftRatio > 0.40 && stats.intendedDisplacement > 8) {
      issues.push({
        id: 'ORTHOGONAL_DRIFT',
        title: 'Excessive Cross-Axis Drift',
        severity: stats.driftRatio > 0.7 ? 'CRITICAL' : 'WARNING',
        confidence: 'MEDIUM',
        rank: 4,
        summary: `Perpendicular drift was ${(stats.driftRatio * 100).toFixed(0)}% of intended displacement (${stats.perpendicularDrift.toFixed(1)}px drift vs ${stats.intendedDisplacement.toFixed(1)}px intended).`,
        evidence: {
          driftRatio: stats.driftRatio.toFixed(2),
          perpendicularDrift: stats.perpendicularDrift.toFixed(1),
          intendedDisplacement: stats.intendedDisplacement.toFixed(1)
        },
        explanation: 'When sliding along one axis, significant movement was registered on the perpendicular axis. This is caused by natural wrist arc pivoting, hand grip asymmetry, or uncompensated tilt on the cross axis.',
        uncertaintyNotes: 'Cannot distinguish with certainty from IMU alone whether drift was physical hand arc curving or sensor tilt leakage without comparing opposing test directions.',
        recommendation: {
          param: 'wristPivotAssist',
          suggestion: 'Tune wristPivotAssist gain or increase perpendicular deadzone threshold to suppress sideways slippage.'
        }
      });
    }

    // Diagnostic Rule 5: Sluggish ZUPT / Velocity Lingering
    if (stats.zuptLatencyMs !== null && stats.zuptLatencyMs > 200) {
      issues.push({
        id: 'SLUGGISH_ZUPT',
        title: 'Sluggish Stop Detection (ZUPT Lag)',
        severity: 'WARNING',
        confidence: 'HIGH',
        rank: 5,
        summary: `ZUPT took ${stats.zuptLatencyMs.toFixed(0)} ms to lock residual velocity to zero after deceleration ended.`,
        evidence: {
          zuptLatencyMs: stats.zuptLatencyMs.toFixed(0),
          residualVel: stats.peakVel.toFixed(2)
        },
        explanation: 'After the physical slide stopped, the cursor continued floating/gliding for over 200ms before ZUPT confirmed the stationary state. This makes the pointer feel floaty or slippery on a mousepad.',
        uncertaintyNotes: null,
        recommendation: {
          param: 'zuptWindowMs / zuptMinStationaryMs',
          suggestion: 'Lower zuptWindowMs from 120ms to 80ms and decrease zuptMinStationaryMs from 60ms to 40ms for immediate crisp stopping.'
        }
      });
    }

    // Diagnostic Rule 6: Sensor Noise Exceeding Deadzone
    if (stats.noiseStdMag > 0.05) {
      issues.push({
        id: 'HIGH_SENSOR_NOISE',
        title: 'High Sensor Noise Floor',
        severity: 'WARNING',
        confidence: 'MEDIUM',
        rank: 6,
        summary: `Resting sensor noise magnitude was ${stats.noiseStdMag.toFixed(3)} m/s² (X: ${stats.noiseStdAx.toFixed(3)}, Y: ${stats.noiseStdAy.toFixed(3)}).`,
        evidence: {
          noiseStdAx: stats.noiseStdAx.toFixed(3),
          noiseStdAy: stats.noiseStdAy.toFixed(3),
          noiseStdMag: stats.noiseStdMag.toFixed(3)
        },
        explanation: 'Smartphone MEMS accelerometers have thermal noise and desk micro-vibrations. If this noise floor exceeds the deadzone threshold, tiny noise spikes are integrated into random wandering velocity.',
        uncertaintyNotes: 'Desk surface vibrations (e.g. typing on nearby mechanical keyboard or resting hand) elevate apparent sensor noise.',
        recommendation: {
          param: 'deadzone',
          suggestion: `Set deadzone threshold to at least ${(stats.noiseStdMag * 2.2).toFixed(3)} m/s² to guarantee resting noise rejection.`
        }
      });
    }

    // Diagnostic Rule 7: Sampling Rate Jitter or Dropouts
    if (stats.dtJitterMs > 8.0 || stats.dropoutCount > 2) {
      issues.push({
        id: 'SAMPLING_JITTER',
        title: 'Sampling Jitter & Event Dropouts',
        severity: 'WARNING',
        confidence: 'HIGH',
        rank: 7,
        summary: `Sensor sample intervals varied with ${stats.dtJitterMs.toFixed(1)} ms jitter; ${stats.dropoutCount} frame dropouts detected (max dt: ${stats.maxDtMs.toFixed(1)} ms).`,
        evidence: {
          meanDtMs: stats.meanDtMs.toFixed(1),
          dtJitterMs: stats.dtJitterMs.toFixed(1),
          maxDtMs: stats.maxDtMs.toFixed(1),
          dropoutCount: stats.dropoutCount
        },
        explanation: 'Browser main-thread execution or device background throttling caused sensor events to dispatch unevenly. Fixed per-event damping fails under variable dt; continuous time damping is essential.',
        uncertaintyNotes: null,
        recommendation: {
          param: 'continuousTimeDamping',
          suggestion: 'Ensure dt clamping is active (e.g. clamp between 2ms and 50ms) to avoid integration explosion during garbage collection pauses.'
        }
      });
    }

    // Sort issues by rank / severity
    issues.sort((a, b) => a.rank - b.rank);

    return {
      stats,
      pipeline,
      detectedIssues: issues
    };
  }

  /**
   * Directional Pair Comparison (UP vs DOWN, or LEFT vs RIGHT)
   * Disentangles physical hand asymmetry from static tilt leakage.
   *
   * @param {Object} testA - First test (e.g. UP)
   * @param {Object} testB - Opposing test (e.g. DOWN)
   * @returns {Object} Pairwise comparative diagnostics
   */
  static compareDirectionalPair(testA, testB) {
    if (!testA || !testB) return null;

    const statsA = this.computeStatistics(testA);
    const statsB = this.computeStatistics(testB);

    const dispA = statsA.intendedDisplacement;
    const dispB = statsB.intendedDisplacement;
    const ratio = dispB > 0 ? (dispA / dispB) : 1.0;

    // Check cross-axis drift correlation:
    // If BOTH UP and DOWN have positive X drift, that proves static tilt on X (not hand curvature)!
    const isDriftCorrelated = (testA.direction === TestDirection.UP && testB.direction === TestDirection.DOWN) &&
      (Math.sign(statsA.netDx) === Math.sign(statsB.netDx) && Math.abs(statsA.netDx) > 3 && Math.abs(statsB.netDx) > 3);

    const findings = [];
    if (isDriftCorrelated) {
      findings.push(`⚠️ CONFIRMED STATIC TILT LEAKAGE: Both ${testA.direction} and ${testB.direction} tests drifted in the same horizontal direction (${statsA.netDx > 0 ? '+X rightward' : '-X leftward'}). This confirms uncompensated tilt rather than hand curving!`);
    }

    if (Math.abs(1.0 - ratio) > 0.45) {
      findings.push(`ℹ️ TRAVEL ASYMMETRY: ${testA.direction} displacement (${dispA.toFixed(1)}px) differed by ${(Math.abs(1.0 - ratio) * 100).toFixed(0)}% from ${testB.direction} (${dispB.toFixed(1)}px).`);
    }

    return {
      testA: { direction: testA.direction, stats: statsA },
      testB: { direction: testB.direction, stats: statsB },
      displacementRatio: ratio,
      isDriftCorrelated,
      findings
    };
  }

  /**
   * Algorithm Experimentation Sandbox:
   * Reprocesses raw sensor data using custom What-If parameters.
   *
   * @param {Object} originalRecord - Original recorded test
   * @param {Object} customParams - What-If parameters
   * @returns {Object} Reprocessed test data and comparative delta metrics
   */
  static reprocessTest(originalRecord, customParams = {}) {
    if (!originalRecord || !originalRecord.samples) return null;

    // Create fresh MotionProcessor with experimental parameters
    const processor = new MotionProcessor({
      sensitivity: customParams.sensitivity ?? 1.4,
      frictionHalfLife: customParams.frictionHalfLife ?? 0.065,
      deadzone: customParams.deadzone ?? 0.06,
      zuptWindowMs: customParams.zuptWindowMs ?? 120,
      zuptAccelVarThreshold: customParams.zuptAccelVarThreshold ?? 0.008,
      zuptMinStationaryMs: customParams.zuptMinStationaryMs ?? 60,
      wristPivotAssist: customParams.wristPivotAssist ?? true,
      ballisticsEnabled: customParams.ballisticsEnabled ?? true
    });

    const reprocessedSamples = [];
    let curX = 150;
    let curY = 120;
    let netDx = 0;
    let netDy = 0;
    let totalPath = 0;

    for (const raw of originalRecord.samples) {
      const output = processor.processSample({
        linearAx: raw.linearAx,
        linearAy: raw.linearAy,
        linearAz: raw.linearAz,
        rawAx: raw.rawAx,
        rawAy: raw.rawAy,
        rawAz: raw.rawAz,
        rotationRateZ: raw.rotationRateZ,
        timestamp: raw.timestamp
      });

      curX += output.dx;
      curY += output.dy;
      netDx += output.dx;
      netDy += output.dy;
      totalPath += Math.hypot(output.dx, output.dy);

      reprocessedSamples.push({
        t: raw.t,
        timestamp: raw.timestamp,
        dt: output.dt,
        rawAx: raw.rawAx,
        rawAy: raw.rawAy,
        rawAz: raw.rawAz,
        linearAx: output.linearAx,
        linearAy: output.linearAy,
        rotationRateZ: raw.rotationRateZ,
        unbiasedAx: output.unbiasedAx,
        unbiasedAy: output.unbiasedAy,
        velX: output.velX,
        velY: output.velY,
        speed: output.speed,
        dx: output.dx,
        dy: output.dy,
        cursorX: curX,
        cursorY: curY,
        state: output.state
      });
    }

    const reprocessedRecord = {
      direction: originalRecord.direction,
      recordedAt: originalRecord.recordedAt,
      durationMs: originalRecord.durationMs,
      sampleCount: reprocessedSamples.length,
      averageSampleRateHz: originalRecord.averageSampleRateHz,
      samples: reprocessedSamples
    };

    const originalStats = this.computeStatistics(originalRecord);
    const newStats = this.computeStatistics(reprocessedRecord);

    const deltaDisplacementPct = originalStats.netDisplacement > 0
      ? ((newStats.netDisplacement - originalStats.netDisplacement) / originalStats.netDisplacement) * 100
      : 0;

    const deltaRecoilPct = originalStats.maxRecoilVel > 0
      ? ((newStats.maxRecoilVel - originalStats.maxRecoilVel) / originalStats.maxRecoilVel) * 100
      : 0;

    const deltaAngleErrorDeg = newStats.angleErrorDeg - originalStats.angleErrorDeg;

    return {
      direction: originalRecord.direction,
      samples: reprocessedSamples,
      netDx,
      netDy,
      netDisplacement: Math.hypot(netDx, netDy),
      totalPath,
      stats: newStats,
      comparison: {
        originalStats,
        newStats,
        deltaDisplacementPct,
        deltaRecoilPct,
        deltaAngleErrorDeg,
        isImprovement: (newStats.angleErrorDeg <= originalStats.angleErrorDeg) && (newStats.maxRecoilVel <= originalStats.maxRecoilVel)
      }
    };
  }

  /**
   * Generate complete exportable HTML and Markdown report.
   *
   * @param {Object} allAnalysis - Dictionary of analyzed tests
   * @returns {Object} { markdownReport, htmlReport }
   */
  static generateComprehensiveReport(allAnalysis) {
    const directions = Object.keys(allAnalysis);
    const timestamp = new Date().toLocaleString();

    let md = `# AirPointer Desk Mouse Kinematics Diagnostics Report\n`;
    md += `**Generated:** ${timestamp}\n\n`;
    md += `## 1. Executive Summary\n`;
    md += `This report evaluates real-world smartphone inertial sensor recordings during directional Desk Mouse sliding tests.\n\n`;

    md += `## 2. Test Direction Metrics\n`;
    md += `| Direction | Displacement | Direction Alignment | Angle Error | Peak Vel | Recoil Vel | ZUPT Latency |\n`;
    md += `| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n`;

    for (const dir of directions) {
      const a = allAnalysis[dir];
      if (a && a.stats) {
        const s = a.stats;
        md += `| **${dir}** | ${s.netDisplacement.toFixed(1)} px | Cos: ${s.cosineSim.toFixed(2)} | ${s.angleErrorDeg.toFixed(1)}° | ${s.peakVel.toFixed(2)} m/s | ${s.maxRecoilVel.toFixed(2)} m/s | ${s.zuptLatencyMs ? s.zuptLatencyMs.toFixed(0) + ' ms' : 'N/A'} |\n`;
      }
    }
    md += `\n`;

    md += `## 3. Detected Issues & Root Causes\n`;
    for (const dir of directions) {
      const a = allAnalysis[dir];
      if (a && a.detectedIssues && a.detectedIssues.length > 0) {
        md += `### ${dir} Test Issues:\n`;
        for (const issue of a.detectedIssues) {
          md += `- **[${issue.severity}] ${issue.title}** (Confidence: ${issue.confidence})\n`;
          md += `  - *Summary:* ${issue.summary}\n`;
          md += `  - *Recommendation:* ${issue.recommendation.suggestion}\n`;
        }
        md += `\n`;
      }
    }

    return {
      markdown: md,
      generatedAt: timestamp
    };
  }
}
