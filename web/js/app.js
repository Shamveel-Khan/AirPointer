/**
 * AirPointer Desk Mouse Web Test & Analytics - Main Application
 */

import { MotionProcessor, MotionState } from './motion-processor.js';
import { SensorManager } from './sensor-manager.js';
import { CursorRenderer } from './cursor-renderer.js';
import { TelemetryDisplay } from './telemetry-display.js';
import { TestRecorder, TestDirection, RecordingPhase } from './recorder.js';
import { MotionAnalytics } from './analytics.js';
import { ChartRenderer } from './chart-renderer.js';

document.addEventListener('DOMContentLoaded', () => {
  // Navigation Tabs
  const tabBtnLive = document.getElementById('tab-btn-live');
  const tabBtnTesting = document.getElementById('tab-btn-testing');
  const viewLive = document.getElementById('view-live');
  const viewTesting = document.getElementById('view-testing');

  // Live Canvas & Controls
  const canvas = document.getElementById('desktop-canvas');
  const btnStart = document.getElementById('btn-start');
  const btnStop = document.getElementById('btn-stop');
  const btnCalibrate = document.getElementById('btn-calibrate');
  const btnReset = document.getElementById('btn-reset');
  const sliderSensitivity = document.getElementById('slider-sensitivity');
  const valSensitivity = document.getElementById('val-sensitivity');
  const sliderZupt = document.getElementById('slider-zupt');
  const valZupt = document.getElementById('val-zupt');
  const toggleTrails = document.getElementById('toggle-trails');
  const toggleRatio = document.getElementById('toggle-ratio');
  const bannerWarning = document.getElementById('security-warning');

  // Testing & Modal Elements
  const countdownModal = document.getElementById('countdown-modal');
  const modalDirTitle = document.getElementById('modal-direction-title');
  const modalCountdownNum = document.getElementById('modal-countdown-num');
  const modalHint = document.getElementById('modal-hint');
  const modalProgressBar = document.getElementById('modal-progress-bar');
  const btnCancelModal = document.getElementById('btn-cancel-modal');

  // Directional Buttons & Status
  const testButtons = {
    UP: document.getElementById('btn-run-UP'),
    DOWN: document.getElementById('btn-run-DOWN'),
    LEFT: document.getElementById('btn-run-LEFT'),
    RIGHT: document.getElementById('btn-run-RIGHT')
  };
  const testCards = {
    UP: document.getElementById('card-test-UP'),
    DOWN: document.getElementById('card-test-DOWN'),
    LEFT: document.getElementById('card-test-LEFT'),
    RIGHT: document.getElementById('card-test-RIGHT')
  };
  const statusPills = {
    UP: document.getElementById('status-UP'),
    DOWN: document.getElementById('status-DOWN'),
    LEFT: document.getElementById('status-LEFT'),
    RIGHT: document.getElementById('status-RIGHT')
  };
  const statsTexts = {
    UP: document.getElementById('stats-UP'),
    DOWN: document.getElementById('stats-DOWN'),
    LEFT: document.getElementById('stats-LEFT'),
    RIGHT: document.getElementById('stats-RIGHT')
  };

  // Visual Debugger Elements
  const trajectoryCanvas = document.getElementById('trajectory-canvas');
  const chartAccelCanvas = document.getElementById('chart-accel');
  const chartVelocityCanvas = document.getElementById('chart-velocity');
  const btnReplayPlay = document.getElementById('btn-replay-play');
  const replayScrubber = document.getElementById('replay-scrubber');
  const replayTimeTxt = document.getElementById('replay-time-txt');
  const selectReplaySpeed = document.getElementById('select-replay-speed');
  const replaySelectedBadge = document.getElementById('replay-selected-badge');
  const metricsDirBadge = document.getElementById('metrics-dir-badge');

  // Metrics Display
  const metricAlignment = document.getElementById('metric-alignment');
  const metricAngleError = document.getElementById('metric-angle-error');
  const metricDisp = document.getElementById('metric-disp');
  const metricDrift = document.getElementById('metric-drift');
  const metricRecoil = document.getElementById('metric-recoil');
  const metricZuptLat = document.getElementById('metric-zupt-lat');
  const findingsContainer = document.getElementById('findings-container');
  const recommendationsContainer = document.getElementById('recommendations-container');

  // Export Buttons
  const btnExportJson = document.getElementById('btn-export-json');
  const btnExportCsv = document.getElementById('btn-export-csv');
  const btnClearRecordings = document.getElementById('btn-clear-recordings');

  // Active Replay State
  let activeReplayDirection = TestDirection.UP;
  let isReplaying = false;
  let replayRafId = null;
  let replayStartTime = 0;
  let currentScrubTimeMs = 3000;

  // Security Context Check
  if (!SensorManager.isSecureContext()) {
    bannerWarning.classList.remove('hidden');
  }

  // 1. Initialize Kinematics Processor
  const motionProcessor = new MotionProcessor({
    sensitivity: parseFloat(sliderSensitivity.value),
    onCalibrationProgress: (current, total) => {
      telemetryDisplay.setCalibrationProgress(current, total);
    },
    onCalibrationComplete: (calibData) => {
      telemetryDisplay.setCalibrationFinished(calibData);
      setTimeout(() => {
        const calibStatus = document.getElementById('calib-status');
        if (calibStatus) calibStatus.textContent = 'Active (Calibrated)';
      }, 2000);
    }
  });

  // 2. Initialize Desktop Cursor Renderer
  const cursorRenderer = new CursorRenderer(canvas, {
    aspectRatioMode: toggleRatio.checked ? 'fill' : '16:9',
    showTrails: toggleTrails.checked
  });

  // 3. Initialize Telemetry HUD
  const telemetryDisplay = new TelemetryDisplay({
    stateBadge: document.getElementById('badge-state'),
    sampleRate: document.getElementById('val-hz'),
    rawAccel: document.getElementById('val-raw-accel'),
    linearAccel: document.getElementById('val-linear-accel'),
    gyroRate: document.getElementById('val-gyro'),
    velocity: document.getElementById('val-velocity'),
    cursorPos: document.getElementById('val-cursor'),
    deadzoneVal: document.getElementById('val-deadzone'),
    calibProgress: document.getElementById('calib-progress-bar'),
    calibStatus: document.getElementById('calib-status')
  });

  // 4. Initialize Test Recorder
  const testRecorder = new TestRecorder({
    countdownSeconds: 3,
    recordingDurationMs: 3000,
    onPhaseChange: (phase, direction) => {
      handleRecorderPhaseChange(phase, direction);
    },
    onCountdownTick: (remaining, direction) => {
      modalCountdownNum.textContent = remaining > 0 ? remaining : 'SLIDE!';
      modalProgressBar.style.width = '0%';
    },
    onSampleRecorded: (sample, relTimeMs, totalDurationMs) => {
      const pct = Math.min(100, (relTimeMs / totalDurationMs) * 100);
      modalProgressBar.style.width = `${pct}%`;
    },
    onRecordingComplete: (testRecord) => {
      countdownModal.classList.add('hidden');
      updateTestCardUI(testRecord.direction);
      runSuiteDiagnostics();
      selectReplayDirection(testRecord.direction);
    }
  });

  // 5. Initialize Sensor Manager
  const sensorManager = new SensorManager({
    onMotionSample: (sample) => {
      // Feed raw reading into kinematics engine
      const result = motionProcessor.processSample(sample);

      // Move cursor by subpixel deltas
      cursorRenderer.moveCursor(result.dx, result.dy);

      // Update live telemetry display
      telemetryDisplay.update(result, cursorRenderer.getCursorCoordinates(), sample.sampleRateHz);

      // Feed into recorder if a directional test is active
      if (testRecorder.currentPhase === RecordingPhase.RECORDING) {
        testRecorder.recordSample(sample, result, cursorRenderer.getCursorCoordinates());
      }
    },
    onStatusChange: (status) => {
      if (status.error) {
        alert(status.error);
      }
      updateControlsState(status.isListening);
    }
  });

  // ============================================================
  // TAB NAVIGATION
  // ============================================================
  tabBtnLive.addEventListener('click', () => {
    tabBtnLive.classList.add('active');
    tabBtnTesting.classList.remove('active');
    viewLive.classList.remove('hidden');
    viewTesting.classList.add('hidden');
    cursorRenderer.resize();
  });

  tabBtnTesting.addEventListener('click', () => {
    tabBtnTesting.classList.add('active');
    tabBtnLive.classList.remove('active');
    viewTesting.classList.remove('hidden');
    viewLive.classList.add('hidden');
    renderActiveReplay();
  });

  // ============================================================
  // DIRECTIONAL TESTING LOGIC
  // ============================================================
  function handleRecorderPhaseChange(phase, direction) {
    if (phase === RecordingPhase.COUNTDOWN) {
      countdownModal.classList.remove('hidden');
      modalDirTitle.textContent = `PREPARE TO SLIDE ${direction}`;
      modalHint.textContent = `Place phone flat on desk. Hand ready. 3s countdown...`;
      modalCountdownNum.textContent = '3';
      modalProgressBar.style.width = '0%';
    } else if (phase === RecordingPhase.RECORDING) {
      modalCountdownNum.textContent = 'SLIDE!';
      modalHint.textContent = `Slide phone firmly across the desk ${direction}!`;
    } else if (phase === RecordingPhase.IDLE || phase === RecordingPhase.COMPLETED) {
      countdownModal.classList.add('hidden');
    }
  }

  btnCancelModal.addEventListener('click', () => {
    testRecorder.cancelTest();
    countdownModal.classList.add('hidden');
  });

  // Wire each directional test button
  Object.keys(testButtons).forEach(dir => {
    testButtons[dir].addEventListener('click', async () => {
      // Ensure sensor manager is active
      if (!sensorManager.isListening) {
        const perm = await sensorManager.requestPermission();
        if (!perm.granted) {
          alert('Sensor permission required to record motion data.');
          return;
        }
        sensorManager.start();
      }

      // Reset cursor and velocity before test
      cursorRenderer.resetToCenter();
      motionProcessor.reset();

      // Launch test countdown
      testRecorder.startTest(dir);
    });
  });

  function updateTestCardUI(dir) {
    const record = testRecorder.getRecording(dir);
    const card = testCards[dir];
    const pill = statusPills[dir];
    const stats = statsTexts[dir];

    if (!record) {
      pill.textContent = 'Ready';
      pill.className = 'test-status-pill';
      stats.textContent = '--';
      card.className = 'test-card';
      return;
    }

    const singleAnalysis = MotionAnalytics.analyzeSingleTest(record);
    if (!singleAnalysis) return;

    const isPassed = singleAnalysis.isDirectionCorrect;
    pill.textContent = isPassed ? 'RECORDED' : 'CHECK';
    pill.className = `test-status-pill ${isPassed ? 'pill-recorded' : ''}`;
    card.className = `test-card ${isPassed ? 'passed' : 'failed'}`;

    stats.textContent = `${record.samples.length} pts | Cos: ${singleAnalysis.cosineSim.toFixed(2)} | Lat: ${singleAnalysis.zuptLatencyMs ? singleAnalysis.zuptLatencyMs.toFixed(0) + 'ms' : 'N/A'}`;
  }

  // Update all test cards from persistent storage on startup
  Object.keys(TestDirection).forEach(dir => updateTestCardUI(dir));

  // ============================================================
  // REPLAY STUDIO & VISUAL DEBUGGER
  // ============================================================
  const dirPills = {
    UP: document.getElementById('pill-UP'),
    DOWN: document.getElementById('pill-DOWN'),
    LEFT: document.getElementById('pill-LEFT'),
    RIGHT: document.getElementById('pill-RIGHT')
  };

  Object.keys(dirPills).forEach(dir => {
    dirPills[dir].addEventListener('click', () => {
      selectReplayDirection(dir);
    });
  });

  function selectReplayDirection(dir) {
    activeReplayDirection = dir;
    Object.keys(dirPills).forEach(d => {
      dirPills[d].classList.toggle('active', d === dir);
    });
    replaySelectedBadge.textContent = dir;
    metricsDirBadge.textContent = dir;

    const record = testRecorder.getRecording(dir);
    const maxDuration = record ? record.durationMs : 3000;
    replayScrubber.max = maxDuration;
    replayScrubber.value = maxDuration;
    currentScrubTimeMs = maxDuration;
    replayTimeTxt.textContent = `${(maxDuration / 1000).toFixed(1)}s`;

    stopReplay();
    renderActiveReplay();
    updateMetricsDisplay(dir);
  }

  function renderActiveReplay() {
    const record = testRecorder.getRecording(activeReplayDirection);
    if (!record) {
      // Clear canvases
      const ctx = trajectoryCanvas.getContext('2d');
      ctx.clearRect(0, 0, trajectoryCanvas.width, trajectoryCanvas.height);
      ctx.fillStyle = '#0F172A';
      ctx.fillRect(0, 0, trajectoryCanvas.width, trajectoryCanvas.height);
      ctx.fillStyle = '#64748B';
      ctx.font = '12px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(`No recording for ${activeReplayDirection}. Tap 'Start ${activeReplayDirection} Test' above.`, trajectoryCanvas.width / 2, trajectoryCanvas.height / 2);
      return;
    }

    // 1. Draw 2D Trajectory with Expected Arrow vs Actual Path
    ChartRenderer.renderTrajectory(trajectoryCanvas, record, currentScrubTimeMs);

    // 2. Draw Synchronized Acceleration Time-Series Chart
    ChartRenderer.renderTimeSeries(chartAccelCanvas, record, {
      type: 'accel',
      playheadTimeMs: currentScrubTimeMs
    });

    // 3. Draw Synchronized Velocity Time-Series Chart
    ChartRenderer.renderTimeSeries(chartVelocityCanvas, record, {
      type: 'velocity',
      playheadTimeMs: currentScrubTimeMs
    });
  }

  // Scrubber Listener
  replayScrubber.addEventListener('input', (e) => {
    stopReplay();
    currentScrubTimeMs = parseFloat(e.target.value);
    replayTimeTxt.textContent = `${(currentScrubTimeMs / 1000).toFixed(1)}s`;
    renderActiveReplay();
  });

  // Play / Pause Animation Loop
  btnReplayPlay.addEventListener('click', () => {
    if (isReplaying) {
      stopReplay();
    } else {
      startReplay();
    }
  });

  function startReplay() {
    const record = testRecorder.getRecording(activeReplayDirection);
    if (!record) return;

    isReplaying = true;
    btnReplayPlay.textContent = '⏸';
    const totalDuration = record.durationMs;

    if (currentScrubTimeMs >= totalDuration - 20) {
      currentScrubTimeMs = 0;
    }

    const speed = parseFloat(selectReplaySpeed.value);
    let lastAnimTime = performance.now();

    function step(now) {
      const delta = (now - lastAnimTime) * speed;
      lastAnimTime = now;

      currentScrubTimeMs += delta;
      if (currentScrubTimeMs >= totalDuration) {
        currentScrubTimeMs = totalDuration;
        replayScrubber.value = totalDuration;
        replayTimeTxt.textContent = `${(totalDuration / 1000).toFixed(1)}s`;
        renderActiveReplay();
        stopReplay();
        return;
      }

      replayScrubber.value = currentScrubTimeMs;
      replayTimeTxt.textContent = `${(currentScrubTimeMs / 1000).toFixed(1)}s`;
      renderActiveReplay();

      replayRafId = requestAnimationFrame(step);
    }

    replayRafId = requestAnimationFrame(step);
  }

  function stopReplay() {
    isReplaying = false;
    btnReplayPlay.textContent = '▶';
    if (replayRafId) {
      cancelAnimationFrame(replayRafId);
      replayRafId = null;
    }
  }

  // ============================================================
  // ANALYTICS & DIAGNOSTICS DISPLAY
  // ============================================================
  function updateMetricsDisplay(dir) {
    const record = testRecorder.getRecording(dir);
    if (!record) {
      metricAlignment.textContent = '--';
      metricAngleError.textContent = '--';
      metricDisp.textContent = '--';
      metricDrift.textContent = '--';
      metricRecoil.textContent = '--';
      metricZuptLat.textContent = '--';
      return;
    }

    const res = MotionAnalytics.analyzeSingleTest(record);
    if (!res) return;

    metricAlignment.textContent = `${(res.cosineSim >= 0 ? '+' : '')}${res.cosineSim.toFixed(2)}`;
    metricAlignment.style.color = res.cosineSim > 0.6 ? '#00E676' : (res.cosineSim < 0 ? '#EF4444' : '#FFB300');

    metricAngleError.textContent = `${res.angleErrorDeg.toFixed(1)}°`;
    metricDisp.textContent = `X:${res.netDx.toFixed(1)} Y:${res.netDy.toFixed(1)} (${res.netDisplacement.toFixed(1)}px)`;
    metricDrift.textContent = `${(res.driftRatio * 100).toFixed(0)}%`;
    metricRecoil.textContent = `${res.maxRecoilVel.toFixed(2)} m/s (${res.recoilDurationMs.toFixed(0)}ms)`;
    metricRecoil.style.color = res.maxRecoilVel > 0.05 ? '#EF4444' : '#00E5FF';

    metricZuptLat.textContent = res.zuptLatencyMs ? `${res.zuptLatencyMs.toFixed(0)} ms` : 'N/A';
  }

  function runSuiteDiagnostics() {
    const all = testRecorder.getAllRecordings();
    const suiteAnalysis = MotionAnalytics.analyzeFullSuite(all);

    // Update Findings
    findingsContainer.innerHTML = '';
    for (const f of suiteAnalysis.findings) {
      const item = document.createElement('div');
      item.className = 'finding-item';
      if (f.startsWith('⚠️')) item.classList.add('warn');
      if (f.startsWith('✅')) item.classList.add('success');
      item.textContent = f;
      findingsContainer.appendChild(item);
    }

    // Update Recommendations
    recommendationsContainer.innerHTML = '';
    for (const r of suiteAnalysis.recommendations) {
      const item = document.createElement('div');
      item.className = 'rec-item';
      item.textContent = r;
      recommendationsContainer.appendChild(item);
    }
  }

  // Initial diagnostics run on loaded recordings
  runSuiteDiagnostics();
  selectReplayDirection(TestDirection.UP);

  // ============================================================
  // EXPORT & CLEAR ACTIONS
  // ============================================================
  btnExportJson.addEventListener('click', () => {
    const jsonStr = testRecorder.exportToJSON();
    downloadFile(jsonStr, `airpointer_sensor_tests_${getTimestampStr()}.json`, 'application/json');
  });

  btnExportCsv.addEventListener('click', () => {
    const csvStr = testRecorder.exportToCSV();
    downloadFile(csvStr, `airpointer_sensor_dataset_${getTimestampStr()}.csv`, 'text/csv');
  });

  btnClearRecordings.addEventListener('click', () => {
    if (confirm('Are you sure you want to delete all recorded tests and analytics?')) {
      testRecorder.clearAllRecordings();
      Object.keys(TestDirection).forEach(dir => updateTestCardUI(dir));
      runSuiteDiagnostics();
      selectReplayDirection(TestDirection.UP);
    }
  });

  function downloadFile(content, fileName, mimeType) {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }, 100);
  }

  function getTimestampStr() {
    return new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  }

  // ============================================================
  // LIVE CONTROLS & LISTENERS
  // ============================================================
  function updateControlsState(isListening) {
    if (isListening) {
      btnStart.classList.add('hidden');
      btnStop.classList.remove('hidden');
      btnCalibrate.disabled = false;
    } else {
      btnStart.classList.remove('hidden');
      btnStop.classList.add('hidden');
    }
  }

  btnStart.addEventListener('click', async () => {
    const perm = await sensorManager.requestPermission();
    if (!perm.granted) {
      alert(`Sensor permission denied: ${perm.error || 'Please grant motion sensor access.'}`);
      return;
    }
    const started = sensorManager.start();
    if (started) {
      motionProcessor.startCalibration(80);
    }
  });

  btnStop.addEventListener('click', () => {
    sensorManager.stop();
    motionProcessor.reset();
  });

  btnCalibrate.addEventListener('click', () => {
    if (!sensorManager.isListening) {
      sensorManager.start();
    }
    motionProcessor.startCalibration(80);
  });

  btnReset.addEventListener('click', () => {
    cursorRenderer.resetToCenter();
    motionProcessor.reset();
  });

  sliderSensitivity.addEventListener('input', (e) => {
    const val = parseFloat(e.target.value);
    motionProcessor.sensitivity = val;
    valSensitivity.textContent = `${val.toFixed(1)}x`;
  });

  sliderZupt.addEventListener('input', (e) => {
    const val = parseFloat(e.target.value);
    motionProcessor.zuptAccelVarThreshold = val;
    valZupt.textContent = val.toFixed(4);
  });

  toggleTrails.addEventListener('change', (e) => {
    cursorRenderer.showTrails = e.target.checked;
    cursorRenderer.trail = [];
    cursorRenderer.renderFrame();
  });

  toggleRatio.addEventListener('change', (e) => {
    cursorRenderer.setAspectRatioMode(e.target.checked ? 'fill' : '16:9');
  });

  canvas.addEventListener('click', (e) => {
    const rect = canvas.getBoundingClientRect();
    cursorRenderer.cursorX = e.clientX - rect.left;
    cursorRenderer.cursorY = e.clientY - rect.top;
    cursorRenderer.renderFrame();
  });
});
