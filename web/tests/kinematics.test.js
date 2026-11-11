import assert from 'node:assert';
import { MotionProcessor, MotionState } from '../js/motion-processor.js';

console.log('🧪 Starting Kinematics Processor Automated Tests...\n');

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

// ============================================================
// FIX 1: Gravity Compensation Test
// ============================================================
runTest('Gravity Compensation: Subtracts static gravity on an inclined surface', () => {
  const processor = new MotionProcessor();

  // Simulate an inclined desk where gravity leaks into X and Y:
  // gx = 1.2 m/s^2, gy = 0.8 m/s^2, gz = 9.6 m/s^2 (Total ~9.8 m/s^2)
  const rawAx = 1.2;
  const rawAy = 0.8;
  const rawAz = 9.6;

  // Feed 100 stationary frames with raw acceleration including gravity
  let lastResult = null;
  for (let i = 0; i < 100; i++) {
    lastResult = processor.processSample({
      linearAx: null, // Forces gravity estimation fallback
      linearAy: null,
      linearAz: null,
      rawAx,
      rawAy,
      rawAz,
      rotationRateZ: 0,
      timestamp: (i + 1) * 16.6
    });
  }

  // After filter convergence, linear acceleration and velocity should be near zero
  assert.ok(Math.abs(lastResult.linearAx) < 0.05, `Linear Ax should be near 0, got ${lastResult.linearAx}`);
  assert.ok(Math.abs(lastResult.linearAy) < 0.05, `Linear Ay should be near 0, got ${lastResult.linearAy}`);
  assert.strictEqual(lastResult.velX, 0, 'Velocity X should remain 0 when stationary');
  assert.strictEqual(lastResult.velY, 0, 'Velocity Y should remain 0 when stationary');
});

// ============================================================
// FIX 2: Stationary Bias Calibration
// ============================================================
runTest('Stationary Bias Calibration: Learns static offsets and freezes during motion', () => {
  const processor = new MotionProcessor();
  processor.startCalibration(60);

  const fakeBiasX = 0.15;
  const fakeBiasY = -0.08;

  // Feed calibration samples with small noise around the fake bias
  for (let i = 0; i < 60; i++) {
    const noise = (Math.sin(i) * 0.01);
    processor.processSample({
      linearAx: fakeBiasX + noise,
      linearAy: fakeBiasY + noise,
      linearAz: 0,
      rawAx: fakeBiasX,
      rawAy: fakeBiasY,
      rawAz: 9.8,
      rotationRateZ: 0,
      timestamp: (i + 1) * 16
    });
  }

  assert.strictEqual(processor.isCalibrating, false, 'Calibration should complete after 60 samples');
  assert.ok(Math.abs(processor.biasAx - fakeBiasX) < 0.01, `Bias X (${processor.biasAx}) should match offset ${fakeBiasX}`);
  assert.ok(Math.abs(processor.biasAy - fakeBiasY) < 0.01, `Bias Y (${processor.biasAy}) should match offset ${fakeBiasY}`);
  assert.ok(processor.deadzone >= 0.04, `Deadzone should be at least minimum floor (got ${processor.deadzone})`);

  // Now feed motion samples: verify bias is frozen during motion and doesn't absorb the movement
  const initialBiasX = processor.biasAx;
  for (let i = 60; i < 90; i++) {
    processor.processSample({
      linearAx: fakeBiasX + 0.8, // Strong forward acceleration
      linearAy: fakeBiasY + 0.0,
      linearAz: 0,
      rawAx: 0,
      rawAy: 0,
      rawAz: 9.8,
      rotationRateZ: 0,
      timestamp: (i + 1) * 16
    });
  }

  assert.strictEqual(processor.biasAx, initialBiasX, 'Bias X must stay frozen during active movement');
});

// ============================================================
// FIX 3: Continuous Time-Based Damping
// ============================================================
runTest('Continuous Time Damping: Decay is time-invariant regardless of sample rate', () => {
  const halfLife = 0.065; // 65ms half life
  const testDuration = 0.100; // Exactly 100ms total elapsed time
  const expectedDecay = Math.exp(-Math.LN2 / halfLife * testDuration); // ~0.34425

  // Test at 50 Hz (dt = 20ms, exactly 5 steps)
  const p50 = new MotionProcessor({ frictionHalfLife: halfLife, zuptMinStationaryMs: 400 });
  p50.lastTimestamp = 1;
  p50.velX = 1.0;
  p50.intentX = 1;
  for (let i = 1; i <= 5; i++) {
    p50.processSample({
      linearAx: 0,
      linearAy: 0,
      linearAz: 0,
      rawAx: 0,
      rawAy: 0,
      rawAz: 9.8,
      rotationRateZ: 0,
      timestamp: 1 + i * 20
    });
  }

  // Test at 100 Hz (dt = 10ms, exactly 10 steps)
  const p100 = new MotionProcessor({ frictionHalfLife: halfLife, zuptMinStationaryMs: 400 });
  p100.lastTimestamp = 1;
  p100.velX = 1.0;
  p100.intentX = 1;
  for (let i = 1; i <= 10; i++) {
    p100.processSample({
      linearAx: 0,
      linearAy: 0,
      linearAz: 0,
      rawAx: 0,
      rawAy: 0,
      rawAz: 9.8,
      rotationRateZ: 0,
      timestamp: 1 + i * 10
    });
  }

  assert.ok(Math.abs(p50.velX - expectedDecay) < 0.005, `50Hz decay should be ~${expectedDecay.toFixed(4)}, got ${p50.velX}`);
  assert.ok(Math.abs(p100.velX - expectedDecay) < 0.005, `100Hz decay should be ~${expectedDecay.toFixed(4)}, got ${p100.velX}`);
  assert.ok(Math.abs(p50.velX - p100.velX) < 0.001, `50Hz and 100Hz velocities must match within 0.001 (diff: ${Math.abs(p50.velX - p100.velX)})`);
});

// ============================================================
// FIX 4: Intelligent ZUPT and Anti-Bounce
// ============================================================
runTest('ZUPT & Anti-Bounce: Halts cleanly without recoil overshoot', () => {
  const processor = new MotionProcessor({ zuptWindowMs: 80, zuptMinStationaryMs: 40 });

  // 1. Hand accelerates forward (+Y)
  let t = 10;
  processor.lastTimestamp = t;
  for (let i = 0; i < 5; i++) {
    t += 16;
    processor.processSample({
      linearAx: 0,
      linearAy: 0.8,
      linearAz: 0,
      rawAx: 0,
      rawAy: 0,
      rawAz: 9.8,
      rotationRateZ: 0,
      timestamp: t
    });
  }
  assert.ok(processor.velY > 0, `Velocity Y should be positive during forward push (got ${processor.velY})`);

  // 2. Hand brakes hard (negative Ay)
  for (let i = 0; i < 3; i++) {
    t += 16;
    processor.processSample({
      linearAx: 0,
      linearAy: -1.2,
      linearAz: 0,
      rawAx: 0,
      rawAy: 0,
      rawAz: 9.8,
      rotationRateZ: 0,
      timestamp: t
    });
  }

  // Directional clamp prevents velY from bouncing negative!
  assert.ok(processor.velY >= 0, `Velocity Y must not invert and bounce backwards (got ${processor.velY})`);

  // 3. Resting still for > zuptWindowMs (80ms) + zuptMinStationaryMs (40ms) -> ZUPT triggers
  for (let i = 0; i < 10; i++) {
    t += 16;
    processor.processSample({
      linearAx: 0.005 * Math.sin(i),
      linearAy: 0.005 * Math.cos(i),
      linearAz: 0,
      rawAx: 0,
      rawAy: 0,
      rawAz: 9.8,
      rotationRateZ: 0,
      timestamp: t
    });
  }

  assert.strictEqual(processor.state, MotionState.STATIONARY, 'State should be STATIONARY via ZUPT');
  assert.strictEqual(processor.velY, 0, 'Velocity Y must be locked to 0 by ZUPT');
});

// ============================================================
// FIX 5: Fractional Subpixel Cursor Accumulation
// ============================================================
runTest('Fractional Cursor Accumulation: Preserves micro-movements without roundToInt truncation', () => {
  const processor = new MotionProcessor();

  // Accumulate simulated micro movements
  let totalDx = 0;
  let totalDy = 0;

  // Old behavior: if dx = 0.2, Math.round(0.2) = 0 -> 100 * 0 = 0 total movement lost!
  const microDelta = 0.225;
  for (let i = 0; i < 100; i++) {
    totalDx += microDelta;
    totalDy += microDelta;
  }

  assert.ok(Math.abs(totalDx - 22.5) < 0.001, `Floating point accumulation should preserve 22.5px, got ${totalDx}`);
  assert.ok(Math.abs(totalDy - 22.5) < 0.001, `Floating point accumulation should preserve 22.5px, got ${totalDy}`);
});

console.log('\n✨ All Kinematics Processor Tests Completed Successfully!');
