/**
 * AirPointer Motion Analytics & Diagnostic Engine
 *
 * Evaluates real-world directional recordings (UP, DOWN, LEFT, RIGHT),
 * calculating:
 * - Trajectory alignment & directional error angle
 * - Orthogonal drift ratio
 * - Reversal / recoil bounce index
 * - ZUPT stop detection latency
 * - Gravity compensation & static tilt leakage
 * - Multi-axis comparisons (UP/DOWN vs LEFT/RIGHT)
 * - Specific, evidence-based diagnostic findings & tuning recommendations
 */

import { TestDirection } from './recorder.js';

export class MotionAnalytics {
  /**
   * Expected unit vector in cursor space for each direction:
   * Screen coordinates: +X is right, +Y is DOWN (so UP is -Y!)
   */
  static EXPECTED_CURSOR_VECTORS = {
    [TestDirection.UP]: { x: 0, y: -1 },
    [TestDirection.DOWN]: { x: 0, y: 1 },
    [TestDirection.LEFT]: { x: -1, y: 0 },
    [TestDirection.RIGHT]: { x: 1, y: 0 }
  };

  /**
   * Expected physical acceleration sign for each direction:
   * Phone coordinates: +X is right, +Y is toward phone top (forward)
   */
  static EXPECTED_ACCEL_SIGNS = {
    [TestDirection.UP]: { axis: 'y', sign: 1 },
    [TestDirection.DOWN]: { axis: 'y', sign: -1 },
    [TestDirection.LEFT]: { axis: 'x', sign: -1 },
    [TestDirection.RIGHT]: { axis: 'x', sign: 1 }
  };

  /**
   * Analyze a single recorded directional test.
   *
   * @param {Object} testRecord - Record from TestRecorder
   * @returns {Object} Comprehensive test analytics
   */
  static analyzeSingleTest(testRecord) {
    if (!testRecord || !testRecord.samples || testRecord.samples.length === 0) {
      return null;
    }

    const { direction, durationMs, samples } = testRecord;
    const expectedVec = this.EXPECTED_CURSOR_VECTORS[direction];
    const expectedAccel = this.EXPECTED_ACCEL_SIGNS[direction];

    let netDx = 0;
    let netDy = 0;
    let totalPath = 0;
    let peakVel = 0;
    let peakAx = 0;
    let peakAy = 0;
    let sumSqAx = 0;
    let sumSqAy = 0;

    let peakIntendedAccel = 0;
    let peakBrakingDecel = 0;
    let timeOfPeakDecel = 0;
    let zuptStopTime = null;

    let maxRecoilVel = 0;
    let recoilDurationMs = 0;
    let isRecoilActive = false;
    let recoilStartTime = 0;

    // Static resting analysis: first 150ms and last 200ms
    let restStartSumAx = 0, restStartSumAy = 0, restStartCount = 0;
    let restEndSumAx = 0, restEndSumAy = 0, restEndCount = 0;

    for (let i = 0; i < samples.length; i++) {
      const s = samples[i];
      netDx += s.dx;
      netDy += s.dy;

      const stepDist = Math.hypot(s.dx, s.dy);
      totalPath += stepDist;

      const currentSpeed = Math.hypot(s.velX, s.velY);
      if (currentSpeed > peakVel) peakVel = currentSpeed;

      const absAx = Math.abs(s.linearAx ?? s.unbiasedAx);
      const absAy = Math.abs(s.linearAy ?? s.unbiasedAy);
      if (absAx > peakAx) peakAx = absAx;
      if (absAy > peakAy) peakAy = absAy;

      sumSqAx += absAx * absAx;
      sumSqAy += absAy * absAy;

      // Primary axis acceleration & braking analysis
      const primAccel = expectedAccel.axis === 'y' ? s.unbiasedAy : s.unbiasedAx;
      const signedPush = primAccel * expectedAccel.sign;

      if (signedPush > peakIntendedAccel) {
        peakIntendedAccel = signedPush;
      }
      if (-signedPush > peakBrakingDecel) {
        peakBrakingDecel = -signedPush;
        timeOfPeakDecel = s.t;
      }

      // Check for state transition to STATIONARY after peak deceleration
      if (timeOfPeakDecel > 0 && !zuptStopTime && s.state === 'STATIONARY' && s.t > timeOfPeakDecel) {
        zuptStopTime = s.t;
      }

      // Reversal / Recoil analysis along the intended axis:
      // Velocity along intended axis in physical space
      const signedVel = expectedAccel.axis === 'y' ? (s.velY * expectedAccel.sign) : (s.velX * expectedAccel.sign);
      if (signedVel < -0.02) { // Negative velocity means backward motion
        const recoilMag = Math.abs(signedVel);
        if (recoilMag > maxRecoilVel) maxRecoilVel = recoilMag;

        if (!isRecoilActive) {
          isRecoilActive = true;
          recoilStartTime = s.t;
        }
      } else if (isRecoilActive) {
        isRecoilActive = false;
        recoilDurationMs += (s.t - recoilStartTime);
      }

      // Resting slices
      if (s.t <= 200) {
        restStartSumAx += s.unbiasedAx;
        restStartSumAy += s.unbiasedAy;
        restStartCount++;
      }
      if (s.t >= durationMs - 250) {
        restEndSumAx += s.unbiasedAx;
        restEndSumAy += s.unbiasedAy;
        restEndCount++;
      }
    }

    const n = samples.length;
    const rmsAx = Math.sqrt(sumSqAx / n);
    const rmsAy = Math.sqrt(sumSqAy / n);

    const netDisplacement = Math.hypot(netDx, netDy);
    const pathEfficiency = totalPath > 0 ? (netDisplacement / totalPath) : 0;

    // Directional alignment calculations
    let dotProduct = 0;
    let angleErrorDeg = 0;
    let cosineSim = 0;
    let perpendicularDrift = 0;
    let intendedDisplacement = 0;

    if (netDisplacement > 0.001) {
      const actualUnitX = netDx / netDisplacement;
      const actualUnitY = netDy / netDisplacement;

      dotProduct = actualUnitX * expectedVec.x + actualUnitY * expectedVec.y;
      cosineSim = Math.max(-1.0, Math.min(1.0, dotProduct));
      angleErrorDeg = (Math.acos(cosineSim) * 180.0) / Math.PI;

      // Intended vs Perpendicular breakdown
      if (expectedVec.x !== 0) {
        intendedDisplacement = Math.abs(netDx);
        perpendicularDrift = Math.abs(netDy);
      } else {
        intendedDisplacement = Math.abs(netDy);
        perpendicularDrift = Math.abs(netDx);
      }
    }

    const driftRatio = intendedDisplacement > 0 ? (perpendicularDrift / intendedDisplacement) : 1.0;
    const zuptLatencyMs = (timeOfPeakDecel > 0 && zuptStopTime) ? (zuptStopTime - timeOfPeakDecel) : null;

    const restStartMeanAx = restStartCount > 0 ? restStartSumAx / restStartCount : 0;
    const restStartMeanAy = restStartCount > 0 ? restStartSumAy / restStartCount : 0;
    const restEndMeanAx = restEndCount > 0 ? restEndSumAx / restEndCount : 0;
    const restEndMeanAy = restEndCount > 0 ? restEndSumAy / restEndCount : 0;

    const staticTiltOffset = Math.hypot(restStartMeanAx, restStartMeanAy);

    return {
      direction,
      durationMs,
      sampleCount: n,
      avgSampleRateHz: testRecord.averageSampleRateHz,
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
      rmsAx,
      rmsAy,
      peakVel,
      peakIntendedAccel,
      peakBrakingDecel,
      maxRecoilVel,
      recoilDurationMs,
      zuptLatencyMs,
      staticTiltOffset,
      restStartMeanAx,
      restStartMeanAy,
      restEndMeanAx,
      restEndMeanAy,
      isDirectionCorrect: cosineSim > 0.6 // Within ~53 degrees of intended direction
    };
  }

  /**
   * Run cross-comparison and comprehensive diagnostic analysis across all available tests.
   *
   * @param {Object} recordings - Dict of { UP: record, DOWN: record, ... }
   * @returns {Object} Multi-test diagnostics and evidence-based recommendations
   */
  static analyzeFullSuite(recordings) {
    const singleResults = {};
    const directions = Object.keys(recordings);

    for (const dir of directions) {
      singleResults[dir] = this.analyzeSingleTest(recordings[dir]);
    }

    const findings = [];
    const recommendations = [];

    if (directions.length === 0) {
      return {
        singleResults,
        findings: ['No test recordings available.'],
        recommendations: ['Record UP, DOWN, LEFT, and RIGHT tests to begin diagnostics.']
      };
    }

    // 1. Overall Directional Accuracy Check
    const correctCount = directions.filter(d => singleResults[d]?.isDirectionCorrect).length;
    findings.push(`Directional Accuracy: ${correctCount}/${directions.length} tests moved in the intended direction.`);

    // Check for reversed motion
    for (const dir of directions) {
      const res = singleResults[dir];
      if (res && res.cosineSim < -0.2) {
        findings.push(`⚠️ MOVEMENT REVERSAL on ${dir}: Cosine similarity was ${res.cosineSim.toFixed(2)} (actual movement opposed intended direction by ${res.angleErrorDeg.toFixed(1)}°).`);
        recommendations.push(`Invert axis mapping for ${dir} (check coordinate sign in motion-processor).`);
      }
    }

    // 2. Gravity Compensation & Static Tilt Leakage Diagnostic
    const highTiltTests = directions.filter(d => (singleResults[d]?.staticTiltOffset ?? 0) > 0.08);
    if (highTiltTests.length > 0) {
      const avgTilt = highTiltTests.reduce((acc, d) => acc + singleResults[d].staticTiltOffset, 0) / highTiltTests.length;
      findings.push(`⚠️ GRAVITY / TILT LEAKAGE DETECTED: Static resting offset averaged ${avgTilt.toFixed(3)} m/s² during rest phases in: ${highTiltTests.join(', ')}.`);
      recommendations.push(`Increase static calibration duration or tighten the gravity low-pass alpha filter to absorb phone incline.`);
    } else {
      findings.push(`✅ Gravity Compensation: Resting tilt offsets are well-controlled (< 0.08 m/s²).`);
    }

    // 3. Orthogonal Drift Diagnostic
    for (const dir of directions) {
      const res = singleResults[dir];
      if (res && res.driftRatio > 0.45 && res.intendedDisplacement > 5) {
        findings.push(`⚠️ EXCESSIVE CROSS-AXIS DRIFT on ${dir}: Perpendicular drift was ${(res.driftRatio * 100).toFixed(0)}% of intended movement.`);
      }
    }

    // 4. Recoil / Bounce Braking Diagnostic
    const recoilTests = directions.filter(d => (singleResults[d]?.maxRecoilVel ?? 0) > 0.04);
    if (recoilTests.length > 0) {
      const worstRecoil = recoilTests.reduce((prev, curr) =>
        (singleResults[curr].maxRecoilVel > (singleResults[prev]?.maxRecoilVel ?? 0) ? curr : prev), recoilTests[0]);
      const maxV = singleResults[worstRecoil].maxRecoilVel;
      findings.push(`⚠️ BOUNCE RECOIL OBSERVED in ${recoilTests.join(', ')}: Peak backward recoil reached ${maxV.toFixed(2)} m/s during braking.`);
      recommendations.push(`Strengthen directional braking clamp latch and lower ZUPT acceleration variance threshold.`);
    } else {
      findings.push(`✅ Anti-Bounce: No significant backwards recoil detected during braking.`);
    }

    // 5. ZUPT Stop Latency Diagnostic
    const testsWithZupt = directions.filter(d => singleResults[d]?.zuptLatencyMs !== null);
    if (testsWithZupt.length > 0) {
      const avgZuptLatency = testsWithZupt.reduce((acc, d) => acc + singleResults[d].zuptLatencyMs, 0) / testsWithZupt.length;
      if (avgZuptLatency > 220) {
        findings.push(`⚠️ SLUGGISH STOP DETECTION: ZUPT took ${avgZuptLatency.toFixed(0)}ms on average to zero residual velocity after braking.`);
        recommendations.push(`Reduce zuptWindowMs (e.g. from 120ms to 80ms) and reduce zuptMinStationaryMs for snappier stops.`);
      } else {
        findings.push(`✅ ZUPT Stop Latency: Good average stop reaction time of ${avgZuptLatency.toFixed(0)}ms.`);
      }
    }

    // 6. Up/Down vs Left/Right Axis Asymmetry (Rectangular phone kinematics)
    if (singleResults.UP && singleResults.DOWN && singleResults.LEFT && singleResults.RIGHT) {
      const avgYDisp = (singleResults.UP.netDisplacement + singleResults.DOWN.netDisplacement) / 2.0;
      const avgXDisp = (singleResults.LEFT.netDisplacement + singleResults.RIGHT.netDisplacement) / 2.0;
      const ratioYX = avgYDisp / Math.max(0.01, avgXDisp);

      if (ratioYX > 1.7) {
        findings.push(`ℹ️ ASYMMETRIC TRAVEL: Y-axis (UP/DOWN) displacement was ${(ratioYX).toFixed(1)}x larger than X-axis (LEFT/RIGHT). This is common due to ergonomic phone grip constraints.`);
        recommendations.push(`Apply axis-specific gain compensation (e.g. scale X-sensitivity by ${(ratioYX).toFixed(2)}x relative to Y).`);
      } else if (ratioYX < 0.6) {
        findings.push(`ℹ️ ASYMMETRIC TRAVEL: X-axis (LEFT/RIGHT) displacement was ${(1.0 / ratioYX).toFixed(1)}x larger than Y-axis (UP/DOWN).`);
      } else {
        findings.push(`✅ Axis Symmetry: Balanced sensitivity between X-axis and Y-axis travel (ratio: ${ratioYX.toFixed(2)}).`);
      }
    }

    if (recommendations.length === 0) {
      recommendations.push('Kinematic tracking is well-balanced across all 4 directions! Parameters are performing accurately.');
    }

    return {
      singleResults,
      findings,
      recommendations
    };
  }
}
