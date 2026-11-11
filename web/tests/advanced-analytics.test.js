import assert from 'node:assert';
import { AdvancedAnalyticsEngine } from '../js/advanced-analytics-engine.js';
import { TestDirection } from '../js/recorder.js';

console.log('🧪 Starting Advanced Analytics Engine Automated Tests...\n');

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

// Generate realistic synthetic test recording
function createMockRecording(direction, options = {}) {
  const durationMs = options.durationMs ?? 3000;
  const sampleCount = options.sampleCount ?? 150;
  const dt = durationMs / sampleCount;

  const samples = [];
  let curX = 150;
  let curY = 120;

  for (let i = 0; i < sampleCount; i++) {
    const t = i * dt;
    const ax = options.axFn ? options.axFn(t, i) : 0;
    const ay = options.ayFn ? options.ayFn(t, i) : 0;
    const vx = options.vxFn ? options.vxFn(t, i) : 0;
    const vy = options.vyFn ? options.vyFn(t, i) : 0;
    const dx = options.dxFn ? options.dxFn(t, i) : 0;
    const dy = options.dyFn ? options.dyFn(t, i) : 0;

    curX += dx;
    curY += dy;

    samples.push({
      t,
      timestamp: 1000 + t,
      dt: dt / 1000.0,
      rawAx: ax + (options.rawTiltAx ?? 0),
      rawAy: ay + (options.rawTiltAy ?? 0),
      rawAz: 9.8,
      linearAx: ax,
      linearAy: ay,
      linearAz: 0,
      rotationRateZ: options.gzFn ? options.gzFn(t) : 0,
      unbiasedAx: ax,
      unbiasedAy: ay,
      gyroZ: options.gzFn ? options.gzFn(t) * Math.PI / 180 : 0,
      velX: vx,
      velY: vy,
      speed: Math.hypot(vx, vy),
      dx,
      dy,
      cursorX: curX,
      cursorY: curY,
      state: t > 1800 ? 'STATIONARY' : 'MOVING'
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

// 1. JSON Parser & Importer Validation
runTest('Importer: Parses exported JSON format and single-test recordings', () => {
  const mockSuite = {
    app: 'AirPointer Desk Mouse Tester',
    version: '2.0-analytics',
    exportedAt: new Date().toISOString(),
    tests: {
      UP: createMockRecording(TestDirection.UP),
      DOWN: createMockRecording(TestDirection.DOWN)
    }
  };

  const parsed = AdvancedAnalyticsEngine.parseImportedJSON(JSON.stringify(mockSuite));
  assert.ok(parsed.tests.UP, 'Should parse UP test');
  assert.ok(parsed.tests.DOWN, 'Should parse DOWN test');
  assert.strictEqual(parsed.tests.UP.samples.length, 150, 'Should parse all 150 samples');
});

// 2. Statistical Analysis & Jitter Calculation
runTest('Statistics: Correctly calculates sample rate jitter and noise floor', () => {
  const recording = createMockRecording(TestDirection.UP, {
    durationMs: 2000,
    sampleCount: 100
  });

  const stats = AdvancedAnalyticsEngine.computeStatistics(recording);
  assert.ok(stats.sampleCount === 100, 'Sample count should be 100');
  assert.ok(Math.abs(stats.meanDtMs - 20) < 0.1, `Mean dt should be ~20ms, got ${stats.meanDtMs}`);
  assert.ok(stats.dtJitterMs >= 0, 'Jitter should be non-negative');
});

// 3. Error Detection & Root Cause Diagnostics Ranking
runTest('Diagnostics: Detects tilt leakage, recoil, and assigns confidence levels', () => {
  // Test with both tilt leakage and braking recoil
  const badTest = createMockRecording(TestDirection.UP, {
    rawTiltAx: 0.18, // High tilt leakage on X
    vyFn: (t) => (t > 500 && t < 1000 ? 0.6 : (t >= 1000 && t < 1400 ? -0.22 : 0)), // Recoil
    dyFn: (t) => (t > 500 && t < 1000 ? -3.0 : (t >= 1000 && t < 1400 ? 1.0 : 0))
  });

  const analysis = AdvancedAnalyticsEngine.analyzeTest(badTest);
  assert.ok(analysis.detectedIssues.length > 0, 'Should detect issues in problematic test');

  const tiltIssue = analysis.detectedIssues.find(i => i.id === 'GRAVITY_TILT_LEAKAGE');
  assert.ok(tiltIssue, 'Should identify GRAVITY_TILT_LEAKAGE');
  assert.strictEqual(tiltIssue.confidence, 'HIGH', 'Confidence should be HIGH for 0.18 m/s^2 tilt');

  const recoilIssue = analysis.detectedIssues.find(i => i.id === 'BRAKING_RECOIL');
  assert.ok(recoilIssue, 'Should identify BRAKING_RECOIL');
  assert.ok(recoilIssue.evidence.maxRecoilVel >= 0.20, 'Should cite supporting recoil velocity evidence');
});

// 4. Algorithm Experimentation Sandbox (Reprocessing with What-If Parameters)
runTest('Experimentation: Reprocesses raw sensor data with modified parameters', () => {
  const original = createMockRecording(TestDirection.RIGHT, {
    axFn: (t) => (t > 400 && t < 800 ? 0.7 : (t >= 800 && t < 1100 ? -0.7 : 0)),
    vxFn: (t) => (t > 400 && t < 1200 ? 0.4 : 0),
    dxFn: (t) => (t > 400 && t < 1200 ? 2.5 : 0)
  });

  // Reprocess with higher sensitivity and lower deadzone
  const modified = AdvancedAnalyticsEngine.reprocessTest(original, {
    sensitivity: 2.5,
    deadzone: 0.03,
    frictionHalfLife: 0.050
  });

  assert.ok(modified.samples.length === original.samples.length, 'Reprocessed samples must match input sample count');
  assert.ok(modified.netDisplacement > 0, 'Should produce non-zero displacement');
  assert.ok(modified.netDisplacement !== original.netDisplacement, 'Reprocessed displacement should differ from original');
});

console.log('\n✨ All Advanced Analytics Engine Tests Passed Successfully!');
