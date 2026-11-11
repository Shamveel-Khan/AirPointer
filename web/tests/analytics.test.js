import assert from 'node:assert';
import { MotionAnalytics } from '../js/analytics.js';
import { TestDirection } from '../js/recorder.js';

console.log('🧪 Starting Motion Analytics Automated Tests...\n');

function runTest(name, fn) {
  try {
    fn();
    console.log(`✅ [PASS] ${name}`);
  } catch (err) {
    console.error(`❌ [FAIL] ${name}`);
    console.error(err);
    process.exitCode = 1;
  }
}

// Helper to generate synthetic test records
function createSyntheticTest(direction, options = {}) {
  const durationMs = options.durationMs ?? 3000;
  const sampleCount = options.sampleCount ?? 150;
  const dt = durationMs / sampleCount;

  const samples = [];
  let curX = 100;
  let curY = 100;

  for (let i = 0; i < sampleCount; i++) {
    const t = i * dt;
    const dx = options.dxFn ? options.dxFn(t, i) : 0;
    const dy = options.dyFn ? options.dyFn(t, i) : 0;
    curX += dx;
    curY += dy;

    samples.push({
      t,
      dt: dt / 1000.0,
      rawAx: options.rawAxFn ? options.rawAxFn(t, i) : 0,
      rawAy: options.rawAyFn ? options.rawAyFn(t, i) : 0,
      rawAz: 9.8,
      linearAx: options.linearAxFn ? options.linearAxFn(t, i) : 0,
      linearAy: options.linearAyFn ? options.linearAyFn(t, i) : 0,
      linearAz: 0,
      rotationRateZ: 0,
      unbiasedAx: options.unbiasedAxFn ? options.unbiasedAxFn(t, i) : 0,
      unbiasedAy: options.unbiasedAyFn ? options.unbiasedAyFn(t, i) : 0,
      velX: options.velXFn ? options.velXFn(t, i) : 0,
      velY: options.velYFn ? options.velYFn(t, i) : 0,
      speed: 0,
      dx,
      dy,
      cursorX: curX,
      cursorY: curY,
      state: options.stateFn ? options.stateFn(t, i) : (t > 1500 ? 'STATIONARY' : 'MOVING')
    });
  }

  return {
    direction,
    recordedAt: new Date().toISOString(),
    durationMs,
    sampleCount,
    averageSampleRateHz: 50.0,
    samples
  };
}

// 1. Directional Accuracy Test for UP
runTest('Analytics: Correctly calculates UP directional alignment and angle error', () => {
  // Pure UP movement: cursor moves negative Y (-dy), zero X
  const testUp = createSyntheticTest(TestDirection.UP, {
    dyFn: (t) => (t > 500 && t < 1500 ? -2.0 : 0),
    dxFn: () => 0,
    unbiasedAyFn: (t) => (t > 500 && t < 1000 ? 0.8 : (t >= 1000 && t < 1500 ? -0.8 : 0)),
    velYFn: (t) => (t > 500 && t < 1500 ? 0.5 : 0)
  });

  const res = MotionAnalytics.analyzeSingleTest(testUp);
  assert.ok(res.isDirectionCorrect, 'UP test should be recognized as correct direction');
  assert.ok(Math.abs(res.cosineSim - 1.0) < 0.001, `Cosine similarity should be 1.0, got ${res.cosineSim}`);
  assert.ok(res.angleErrorDeg < 0.1, `Angle error should be ~0 deg, got ${res.angleErrorDeg}`);
  assert.strictEqual(res.perpendicularDrift, 0, 'Perpendicular drift should be 0');
});

// 2. Reversal / Recoil Detection Test
runTest('Analytics: Detects backwards recoil during braking phase', () => {
  // Push forward then recoil backward
  const testRecoil = createSyntheticTest(TestDirection.UP, {
    velYFn: (t) => {
      if (t > 500 && t < 1000) return 0.6; // Moving forward
      if (t >= 1000 && t < 1400) return -0.25; // Negative recoil!
      return 0;
    }
  });

  const res = MotionAnalytics.analyzeSingleTest(testRecoil);
  assert.ok(res.maxRecoilVel >= 0.24, `Should detect max recoil vel >= 0.24, got ${res.maxRecoilVel}`);
  assert.ok(res.recoilDurationMs > 300, `Should record recoil duration > 300ms, got ${res.recoilDurationMs}`);
});

// 3. Static Tilt Leakage Test
runTest('Analytics: Identifies uncompensated static tilt bias', () => {
  // Incline: persistent resting offset
  const testTilt = createSyntheticTest(TestDirection.RIGHT, {
    unbiasedAxFn: () => 0.14 // Persistent 0.14 m/s^2 tilt
  });

  const res = MotionAnalytics.analyzeSingleTest(testTilt);
  assert.ok(res.staticTiltOffset > 0.10, `Should detect high static tilt offset, got ${res.staticTiltOffset}`);
});

// 4. Full Suite Diagnostic Synthesis
runTest('Analytics: Cross-evaluates 4-direction suite and identifies asymmetric travel', () => {
  const suite = {
    UP: createSyntheticTest(TestDirection.UP, { dyFn: (t) => (t > 500 && t < 1500 ? -3.0 : 0) }),
    DOWN: createSyntheticTest(TestDirection.DOWN, { dyFn: (t) => (t > 500 && t < 1500 ? 3.0 : 0) }),
    LEFT: createSyntheticTest(TestDirection.LEFT, { dxFn: (t) => (t > 500 && t < 1500 ? -1.0 : 0) }),
    RIGHT: createSyntheticTest(TestDirection.RIGHT, { dxFn: (t) => (t > 500 && t < 1500 ? 1.0 : 0) })
  };

  const analysis = MotionAnalytics.analyzeFullSuite(suite);
  assert.strictEqual(analysis.findings.length > 0, true, 'Should produce diagnostic findings');
  assert.strictEqual(analysis.recommendations.length > 0, true, 'Should produce actionable recommendations');

  // Since Y movement was 3x X movement, verify asymmetric travel finding is flagged
  const hasAsymmetryFinding = analysis.findings.some(f => f.includes('ASYMMETRIC TRAVEL'));
  assert.strictEqual(hasAsymmetryFinding, true, 'Should detect asymmetric travel ratio');
});

console.log('\n✨ All Motion Analytics Tests Passed Successfully!');
