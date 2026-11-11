/**
 * AirPointer Advanced Sensor Analytics Studio - Application Controller
 */

import { AdvancedAnalyticsEngine } from './advanced-analytics-engine.js';
import { AdvancedChartRenderer } from './advanced-chart-renderer.js';
import { TestDirection } from './recorder.js';

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const fileInput = document.getElementById('json-file-input');
  const btnImportJson = document.getElementById('btn-import-json');
  const btnLoadStorage = document.getElementById('btn-load-storage');
  const btnExportReport = document.getElementById('btn-export-report');
  const dropzone = document.getElementById('dropzone');

  // Views & Tabs
  const mainStudioView = document.getElementById('main-studio-view');
  const comparisonStudioView = document.getElementById('comparison-studio-view');
  const dirTabs = {
    UP: document.getElementById('tab-UP'),
    DOWN: document.getElementById('tab-DOWN'),
    LEFT: document.getElementById('tab-LEFT'),
    RIGHT: document.getElementById('tab-RIGHT'),
    COMPARISON: document.getElementById('tab-COMPARISON')
  };
  const statusDots = {
    UP: document.getElementById('dot-UP'),
    DOWN: document.getElementById('dot-DOWN'),
    LEFT: document.getElementById('dot-LEFT'),
    RIGHT: document.getElementById('dot-RIGHT')
  };

  // Trajectory & Replay
  const trajectoryCanvas = document.getElementById('adv-trajectory-canvas');
  const activeTestBadge = document.getElementById('active-test-badge');
  const btnAdvPlay = document.getElementById('btn-adv-play');
  const advScrubber = document.getElementById('adv-scrubber');
  const advTimeTxt = document.getElementById('adv-time-txt');
  const advSpeedSelect = document.getElementById('adv-speed-select');

  // Synchronized Sensor Track Canvases
  const trackCanvases = {
    accel: document.getElementById('track-accel'),
    gyro: document.getElementById('track-gyro'),
    velocity: document.getElementById('track-velocity'),
    cursor: document.getElementById('track-cursor')
  };

  // Hover Readouts
  const hoverReadouts = {
    accel: document.getElementById('hover-accel-val'),
    gyro: document.getElementById('hover-gyro-val'),
    velocity: document.getElementById('hover-vel-val'),
    cursor: document.getElementById('hover-cursor-val')
  };

  // Metric Boxes
  const metricHzBadge = document.getElementById('metric-hz-badge');
  const mCosine = document.getElementById('m-cosine');
  const mAngle = document.getElementById('m-angle');
  const mDisp = document.getElementById('m-disp');
  const mDrift = document.getElementById('m-drift');
  const mRecoil = document.getElementById('m-recoil');
  const mZuptLat = document.getElementById('m-zupt-lat');
  const mTilt = document.getElementById('m-tilt');
  const mJitter = document.getElementById('m-jitter');

  // Diagnostics & Pipeline Table
  const issuesContainer = document.getElementById('adv-issues-container');
  const issuesCountBadge = document.getElementById('issues-count-badge');
  const pipelineTbody = document.getElementById('pipeline-tbody');

  // Experimentation Sandbox
  const sliderDeadzone = document.getElementById('slider-exp-deadzone');
  const valExpDeadzone = document.getElementById('val-exp-deadzone');
  const sliderDamping = document.getElementById('slider-exp-damping');
  const valExpDamping = document.getElementById('val-exp-damping');
  const sliderSens = document.getElementById('slider-exp-sens');
  const valExpSens = document.getElementById('val-exp-sens');
  const sliderZuptVar = document.getElementById('slider-exp-zupt-var');
  const valExpZuptVar = document.getElementById('val-exp-zupt-var');
  const sliderZuptDur = document.getElementById('slider-exp-zupt-dur');
  const valExpZuptDur = document.getElementById('val-exp-zupt-dur');
  const btnRunExperiment = document.getElementById('btn-run-experiment');
  const comparisonRow = document.getElementById('experiment-comparison-row');
  const badgeDeltaDisp = document.getElementById('badge-delta-disp');
  const badgeDeltaRecoil = document.getElementById('badge-delta-recoil');
  const badgeDeltaAngle = document.getElementById('badge-delta-angle');

  // State
  let loadedSuite = { tests: {}, metadata: {} };
  let activeDirection = TestDirection.UP;
  let reprocessedRecord = null;
  let currentScrubTimeMs = 3000;
  let isPlaying = false;
  let replayRafId = null;

  // ============================================================
  // INITIALIZATION: Load from Local Storage on startup
  // ============================================================
  loadFromLocalStorage();

  function loadFromLocalStorage() {
    try {
      const stored = localStorage.getItem('airpointer_test_recordings_v1');
      if (stored) {
        const parsed = JSON.parse(stored);
        if (parsed && typeof parsed === 'object') {
          loadedSuite = AdvancedAnalyticsEngine.parseImportedJSON({ tests: parsed });
          updateTabStatusDots();
          renderActiveView();
        }
      }
    } catch (e) {
      console.warn('Could not load existing recordings from localStorage:', e);
    }
  }

  // ============================================================
  // FILE IMPORT & DRAG-AND-DROP
  // ============================================================
  btnImportJson.addEventListener('click', () => fileInput.click());
  btnLoadStorage.addEventListener('click', () => {
    loadFromLocalStorage();
    alert('Loaded latest test recordings from browser storage!');
  });

  fileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) handleFileImport(file);
  });

  dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
  });

  dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));

  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileImport(e.dataTransfer.files[0]);
    }
  });

  function handleFileImport(file) {
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const text = event.target.result;
        loadedSuite = AdvancedAnalyticsEngine.parseImportedJSON(text);
        updateTabStatusDots();

        // Select first available direction
        const availableDirs = Object.keys(loadedSuite.tests);
        if (availableDirs.length > 0) {
          activeDirection = availableDirs[0];
        }

        reprocessedRecord = null;
        comparisonRow.classList.add('hidden');
        renderActiveView();

        alert(`Successfully imported ${availableDirs.length} test recording(s): ${availableDirs.join(', ')}`);
      } catch (err) {
        alert(`Error importing file: ${err.message}`);
      }
    };
    reader.readAsText(file);
  }

  function updateTabStatusDots() {
    Object.keys(statusDots).forEach(dir => {
      const hasData = !!(loadedSuite.tests && loadedSuite.tests[dir]);
      statusDots[dir].classList.toggle('recorded', hasData);
    });
  }

  // ============================================================
  // DIRECTIONAL TAB SWITCHING
  // ============================================================
  Object.keys(dirTabs).forEach(tabKey => {
    dirTabs[tabKey].addEventListener('click', () => {
      Object.keys(dirTabs).forEach(k => dirTabs[k].classList.remove('active'));
      dirTabs[tabKey].classList.add('active');

      if (tabKey === 'COMPARISON') {
        mainStudioView.classList.add('hidden');
        comparisonStudioView.classList.remove('hidden');
        renderPairwiseComparison();
      } else {
        mainStudioView.classList.remove('hidden');
        comparisonStudioView.classList.add('hidden');
        activeDirection = tabKey;
        reprocessedRecord = null;
        comparisonRow.classList.add('hidden');
        renderActiveView();
      }
    });
  });

  // ============================================================
  // MAIN STUDIO RENDERING
  // ============================================================
  function renderActiveView() {
    activeTestBadge.textContent = activeDirection;
    const testRecord = loadedSuite.tests ? loadedSuite.tests[activeDirection] : null;

    if (!testRecord) {
      renderEmptyState();
      return;
    }

    // Reset scrubber to max duration
    currentScrubTimeMs = testRecord.durationMs || 3000;
    advScrubber.max = currentScrubTimeMs;
    advScrubber.value = currentScrubTimeMs;
    advTimeTxt.textContent = `${(currentScrubTimeMs / 1000).toFixed(1)}s`;

    // 1. Run Complete Analysis & Diagnostics
    const analysis = AdvancedAnalyticsEngine.analyzeTest(testRecord);

    // 2. Render 2D Trajectory with Expected Vector vs Original vs Experiment
    AdvancedChartRenderer.renderTrajectory(trajectoryCanvas, testRecord, {
      reprocessedRecord,
      hoverTimeMs: currentScrubTimeMs
    });

    // 3. Render 4 Synchronized Time-Series Sensor Tracks
    renderSensorTracks(testRecord, analysis);

    // 4. Update Metric Summary Grid
    updateMetricsUI(analysis.stats);

    // 5. Update Ranked Diagnostics Accordion
    updateDiagnosticsUI(analysis.detectedIssues);

    // 6. Update Pipeline Breakdown Table
    updatePipelineTable(analysis.pipeline);
  }

  function renderEmptyState() {
    metricHzBadge.textContent = '-- Hz';
    mCosine.textContent = '--';
    mAngle.textContent = '--';
    mDisp.textContent = '--';
    mDrift.textContent = '--';
    mRecoil.textContent = '--';
    mZuptLat.textContent = '--';
    mTilt.textContent = '--';
    mJitter.textContent = '--';

    issuesCountBadge.textContent = '0 Issues';
    issuesContainer.innerHTML = `
      <div style="font-size: 12px; color: var(--text-muted); padding: 16px; text-align: center;">
        No test recording available for ${activeDirection}. Click <strong>Import JSON</strong> or record this direction in the Tester.
      </div>`;

    pipelineTbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 12px;">No active test loaded</td></tr>`;

    // Clear canvases
    const ctx = trajectoryCanvas.getContext('2d');
    ctx.clearRect(0, 0, trajectoryCanvas.width, trajectoryCanvas.height);
    ctx.fillStyle = '#0F172A';
    ctx.fillRect(0, 0, trajectoryCanvas.width, trajectoryCanvas.height);
    ctx.fillStyle = '#64748B';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(`No ${activeDirection} test loaded. Drop or import JSON file.`, trajectoryCanvas.width / 2, trajectoryCanvas.height / 2);
  }

  function renderSensorTracks(testRecord, analysis) {
    const anomalies = [];
    if (analysis && analysis.stats) {
      if (analysis.stats.maxRecoilVel > 0.04) {
        anomalies.push({ startT: 1000, endT: 1600, color: 'rgba(239, 68, 68, 0.18)' });
      }
    }

    AdvancedChartRenderer.renderTimeSeriesTrack(trackCanvases.accel, testRecord, {
      trackType: 'accel',
      hoverTimeMs: currentScrubTimeMs,
      anomalies
    });

    AdvancedChartRenderer.renderTimeSeriesTrack(trackCanvases.gyro, testRecord, {
      trackType: 'gyro',
      hoverTimeMs: currentScrubTimeMs
    });

    AdvancedChartRenderer.renderTimeSeriesTrack(trackCanvases.velocity, testRecord, {
      trackType: 'velocity',
      hoverTimeMs: currentScrubTimeMs,
      anomalies
    });

    AdvancedChartRenderer.renderTimeSeriesTrack(trackCanvases.cursor, testRecord, {
      trackType: 'cursor',
      hoverTimeMs: currentScrubTimeMs
    });
  }

  // ============================================================
  // SYNCHRONIZED TIMELINE & HOVER CROSSHAIR
  // ============================================================
  Object.values(trackCanvases).forEach(canvas => {
    canvas.addEventListener('mousemove', (e) => {
      const testRecord = loadedSuite.tests ? loadedSuite.tests[activeDirection] : null;
      if (!testRecord) return;

      const rect = canvas.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const padLeft = 40;
      const padRight = 12;
      const plotW = rect.width - padLeft - padRight;

      if (x >= padLeft && x <= rect.width - padRight) {
        const ratio = (x - padLeft) / plotW;
        const totalDuration = testRecord.durationMs || 3000;
        const t = Math.max(0, Math.min(totalDuration, ratio * totalDuration));
        updateSynchronizedTimeline(t);
      }
    });
  });

  advScrubber.addEventListener('input', (e) => {
    stopPlayback();
    updateSynchronizedTimeline(parseFloat(e.target.value));
  });

  function updateSynchronizedTimeline(timeMs) {
    currentScrubTimeMs = timeMs;
    advScrubber.value = timeMs;
    advTimeTxt.textContent = `${(timeMs / 1000).toFixed(1)}s`;

    const testRecord = loadedSuite.tests ? loadedSuite.tests[activeDirection] : null;
    if (!testRecord) return;

    // Synchronously re-render trajectory and sensor tracks with new hover position
    AdvancedChartRenderer.renderTrajectory(trajectoryCanvas, testRecord, {
      reprocessedRecord,
      hoverTimeMs: currentScrubTimeMs
    });

    const analysis = AdvancedAnalyticsEngine.analyzeTest(testRecord);
    renderSensorTracks(testRecord, analysis);

    // Update real-time tooltip readouts
    const sample = testRecord.samples.reduce((prev, curr) =>
      Math.abs(curr.t - timeMs) < Math.abs(prev.t - timeMs) ? curr : prev, testRecord.samples[0]);

    if (sample) {
      hoverReadouts.accel.textContent = `Ax: ${sample.unbiasedAx.toFixed(2)} | Ay: ${sample.unbiasedAy.toFixed(2)}`;
      hoverReadouts.gyro.textContent = `${(sample.rotationRateZ || sample.gyroZ || 0).toFixed(2)} rad/s`;
      hoverReadouts.velocity.textContent = `Vx: ${sample.velX.toFixed(2)} | Vy: ${sample.velY.toFixed(2)}`;
      hoverReadouts.cursor.textContent = `dX: ${sample.dx.toFixed(2)} | dY: ${sample.dy.toFixed(2)}`;
    }
  }

  // ============================================================
  // REPLAY PLAY / PAUSE
  // ============================================================
  btnAdvPlay.addEventListener('click', () => {
    if (isPlaying) stopPlayback();
    else startPlayback();
  });

  function startPlayback() {
    const testRecord = loadedSuite.tests ? loadedSuite.tests[activeDirection] : null;
    if (!testRecord) return;

    isPlaying = true;
    btnAdvPlay.textContent = '⏸';
    const totalDuration = testRecord.durationMs || 3000;

    if (currentScrubTimeMs >= totalDuration - 20) {
      currentScrubTimeMs = 0;
    }

    const speed = parseFloat(advSpeedSelect.value);
    let lastTime = performance.now();

    function step(now) {
      const delta = (now - lastTime) * speed;
      lastTime = now;

      currentScrubTimeMs += delta;
      if (currentScrubTimeMs >= totalDuration) {
        currentScrubTimeMs = totalDuration;
        updateSynchronizedTimeline(totalDuration);
        stopPlayback();
        return;
      }

      updateSynchronizedTimeline(currentScrubTimeMs);
      replayRafId = requestAnimationFrame(step);
    }
    replayRafId = requestAnimationFrame(step);
  }

  function stopPlayback() {
    isPlaying = false;
    btnAdvPlay.textContent = '▶';
    if (replayRafId) {
      cancelAnimationFrame(replayRafId);
      replayRafId = null;
    }
  }

  // ============================================================
  // METRICS & DIAGNOSTICS UI
  // ============================================================
  function updateMetricsUI(stats) {
    if (!stats) return;

    metricHzBadge.textContent = `${stats.measuredHz.toFixed(0)} Hz (${stats.sampleCount} pts)`;
    mCosine.textContent = `${stats.cosineSim >= 0 ? '+' : ''}${stats.cosineSim.toFixed(2)}`;
    mCosine.style.color = stats.cosineSim > 0.6 ? '#00E676' : (stats.cosineSim < 0 ? '#EF4444' : '#FFB300');

    mAngle.textContent = `${stats.angleErrorDeg.toFixed(1)}°`;
    mDisp.textContent = `${stats.netDisplacement.toFixed(1)} px`;
    mDrift.textContent = `${(stats.driftRatio * 100).toFixed(0)}%`;
    mRecoil.textContent = `${stats.maxRecoilVel.toFixed(2)} m/s`;
    mRecoil.style.color = stats.maxRecoilVel > 0.04 ? '#EF4444' : '#00E5FF';

    mZuptLat.textContent = stats.zuptLatencyMs ? `${stats.zuptLatencyMs.toFixed(0)} ms` : 'N/A';
    mTilt.textContent = `${stats.staticTiltOffset.toFixed(3)} m/s²`;
    mTilt.style.color = stats.staticTiltOffset > 0.08 ? '#EF4444' : '#94A3B8';

    mJitter.textContent = `±${stats.dtJitterMs.toFixed(1)} ms`;
  }

  function updateDiagnosticsUI(issues) {
    issuesContainer.innerHTML = '';
    issuesCountBadge.textContent = `${issues.length} Issue${issues.length === 1 ? '' : 's'}`;

    if (issues.length === 0) {
      issuesContainer.innerHTML = `
        <div style="font-size: 12px; color: var(--green-success); padding: 12px; text-align: center;">
          ✅ No tracking anomalies detected! This directional test performs accurately.
        </div>`;
      return;
    }

    for (const issue of issues) {
      const card = document.createElement('div');
      const sevClass = issue.severity === 'CRITICAL' ? 'critical' : (issue.severity === 'WARNING' ? 'warning' : 'info');
      card.className = `issue-card ${sevClass}`;

      const confClass = issue.confidence === 'HIGH' ? 'conf-high' : (issue.confidence === 'MEDIUM' ? 'conf-med' : 'conf-ambiguous');

      card.innerHTML = `
        <div class="issue-header">
          <div class="issue-title-group">
            <span class="issue-rank">#${issue.rank}</span>
            <span class="issue-title">${issue.title}</span>
          </div>
          <span class="issue-confidence ${confClass}">${issue.confidence} CONFIDENCE</span>
        </div>
        <div class="issue-body">
          <p>${issue.summary}</p>
          <div class="issue-evidence">
            <strong>Evidence Metrics:</strong> ${JSON.stringify(issue.evidence).replace(/["{}]/g, ' ')}
          </div>
          <p style="font-size: 11px; color: #94A3B8;">${issue.explanation}</p>
          ${issue.uncertaintyNotes ? `<p style="font-size: 11px; color: #FCD34D;">ℹ️ <em>Uncertainty:</em> ${issue.uncertaintyNotes}</p>` : ''}
          <div class="issue-recommendation">
            <strong>Recommended Fix:</strong> ${issue.recommendation.suggestion}
          </div>
        </div>
      `;

      issuesContainer.appendChild(card);
    }
  }

  function updatePipelineTable(pipeline) {
    pipelineTbody.innerHTML = '';
    if (!pipeline || pipeline.length === 0) return;

    // Show a sample every 4 frames so table isn't 200 rows long
    const step = Math.max(1, Math.floor(pipeline.length / 15));
    for (let i = 0; i < pipeline.length; i += step) {
      const p = pipeline[i];
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${p.t.toFixed(0)}ms</td>
        <td>Ax:${p.stage1_raw.ax.toFixed(2)} Ay:${p.stage1_raw.ay.toFixed(2)}</td>
        <td>Ax:${p.stage2_unbiased.ax.toFixed(2)} Ay:${p.stage2_unbiased.ay.toFixed(2)}</td>
        <td style="${p.stage3_effective.deadzoneSuppressed ? 'color:#64748B' : 'color:#00E5FF'}">
          ${p.stage3_effective.deadzoneSuppressed ? '0.00 (deadzone)' : `Ax:${p.stage3_effective.ax.toFixed(2)}`}
        </td>
        <td>Vx:${p.stage4_integratedVel.vx.toFixed(2)} Vy:${p.stage4_integratedVel.vy.toFixed(2)}</td>
        <td style="${p.stage5_zuptClamped.isClamped ? 'color:#A855F7' : ''}">
          ${p.stage5_zuptClamped.isClamped ? 'LOCKED (0.00)' : p.stage5_zuptClamped.speed.toFixed(2)}
        </td>
        <td style="color: #00E676;">dX:${p.stage6_cursorOutput.dx.toFixed(1)} dY:${p.stage6_cursorOutput.dy.toFixed(1)}</td>
      `;
      pipelineTbody.appendChild(tr);
    }
  }

  // ============================================================
  // ALGORITHM EXPERIMENTATION SANDBOX
  // ============================================================
  sliderDeadzone.addEventListener('input', (e) => valExpDeadzone.textContent = parseFloat(e.target.value).toFixed(3));
  sliderDamping.addEventListener('input', (e) => valExpDamping.textContent = parseFloat(e.target.value).toFixed(3));
  sliderSens.addEventListener('input', (e) => valExpSens.textContent = `${parseFloat(e.target.value).toFixed(1)}x`);
  sliderZuptVar.addEventListener('input', (e) => valExpZuptVar.textContent = parseFloat(e.target.value).toFixed(3));
  sliderZuptDur.addEventListener('input', (e) => valExpZuptDur.textContent = e.target.value);

  btnRunExperiment.addEventListener('click', () => {
    const testRecord = loadedSuite.tests ? loadedSuite.tests[activeDirection] : null;
    if (!testRecord) {
      alert(`No test record loaded for ${activeDirection}. Please import or load a test first.`);
      return;
    }

    const customParams = {
      deadzone: parseFloat(sliderDeadzone.value),
      frictionHalfLife: parseFloat(sliderDamping.value),
      sensitivity: parseFloat(sliderSens.value),
      zuptAccelVarThreshold: parseFloat(sliderZuptVar.value),
      zuptMinStationaryMs: parseFloat(sliderZuptDur.value),
      wristPivotAssist: true,
      ballisticsEnabled: true
    };

    const reprocessed = AdvancedAnalyticsEngine.reprocessTest(testRecord, customParams);
    if (!reprocessed) return;

    reprocessedRecord = reprocessed;

    // Re-render 2D trajectory showing BOTH paths!
    AdvancedChartRenderer.renderTrajectory(trajectoryCanvas, testRecord, {
      reprocessedRecord,
      hoverTimeMs: currentScrubTimeMs
    });

    // Update Comparison Delta Badges
    comparisonRow.classList.remove('hidden');
    const comp = reprocessed.comparison;

    // Displacement delta
    const dispSign = comp.deltaDisplacementPct >= 0 ? '+' : '';
    badgeDeltaDisp.textContent = `Displacement: ${dispSign}${comp.deltaDisplacementPct.toFixed(1)}%`;
    badgeDeltaDisp.className = `delta-badge ${comp.deltaDisplacementPct > 0 ? 'delta-good' : 'delta-neutral'}`;

    // Recoil reduction delta
    const recoilSign = comp.deltaRecoilPct >= 0 ? '+' : '';
    badgeDeltaRecoil.textContent = `Recoil: ${recoilSign}${comp.deltaRecoilPct.toFixed(1)}%`;
    badgeDeltaRecoil.className = `delta-badge ${comp.deltaRecoilPct <= 0 ? 'delta-good' : 'delta-bad'}`;

    // Angle error delta
    const angleSign = comp.deltaAngleErrorDeg <= 0 ? '' : '+';
    badgeDeltaAngle.textContent = `Angle Error: ${angleSign}${comp.deltaAngleErrorDeg.toFixed(1)}°`;
    badgeDeltaAngle.className = `delta-badge ${comp.deltaAngleErrorDeg <= 0 ? 'delta-good' : 'delta-bad'}`;
  });

  // ============================================================
  // PAIRWISE DIRECTIONAL COMPARISON VIEW
  // ============================================================
  function renderPairwiseComparison() {
    const upRecord = loadedSuite.tests ? loadedSuite.tests.UP : null;
    const downRecord = loadedSuite.tests ? loadedSuite.tests.DOWN : null;
    const leftRecord = loadedSuite.tests ? loadedSuite.tests.LEFT : null;
    const rightRecord = loadedSuite.tests ? loadedSuite.tests.RIGHT : null;

    const elUpDown = document.getElementById('pair-results-up-down');
    const elLeftRight = document.getElementById('pair-results-left-right');

    if (upRecord && downRecord) {
      const comp = AdvancedAnalyticsEngine.compareDirectionalPair(upRecord, downRecord);
      elUpDown.innerHTML = `
        <p><strong>Displacement Symmetry:</strong> UP: ${comp.testA.stats.intendedDisplacement.toFixed(1)}px | DOWN: ${comp.testB.stats.intendedDisplacement.toFixed(1)}px (Ratio: ${comp.displacementRatio.toFixed(2)})</p>
        <p><strong>Recoil Comparison:</strong> UP: ${comp.testA.stats.maxRecoilVel.toFixed(2)} m/s | DOWN: ${comp.testB.stats.maxRecoilVel.toFixed(2)} m/s</p>
        <div style="margin-top: 8px;">${comp.findings.map(f => `<div style="padding: 4px; background: rgba(0,229,255,0.1); border-left: 2px solid #00E5FF; margin-bottom: 4px;">${f}</div>`).join('')}</div>
      `;
    } else {
      elUpDown.textContent = 'Both UP and DOWN tests must be loaded to evaluate vertical symmetry.';
    }

    if (leftRecord && rightRecord) {
      const comp = AdvancedAnalyticsEngine.compareDirectionalPair(leftRecord, rightRecord);
      elLeftRight.innerHTML = `
        <p><strong>Displacement Symmetry:</strong> LEFT: ${comp.testA.stats.intendedDisplacement.toFixed(1)}px | RIGHT: ${comp.testB.stats.intendedDisplacement.toFixed(1)}px (Ratio: ${comp.displacementRatio.toFixed(2)})</p>
        <p><strong>Recoil Comparison:</strong> LEFT: ${comp.testA.stats.maxRecoilVel.toFixed(2)} m/s | RIGHT: ${comp.testB.stats.maxRecoilVel.toFixed(2)} m/s</p>
        <div style="margin-top: 8px;">${comp.findings.map(f => `<div style="padding: 4px; background: rgba(0,229,255,0.1); border-left: 2px solid #00E5FF; margin-bottom: 4px;">${f}</div>`).join('')}</div>
      `;
    } else {
      elLeftRight.textContent = 'Both LEFT and RIGHT tests must be loaded to evaluate horizontal symmetry.';
    }
  }

  // ============================================================
  // EXPORT REPORT
  // ============================================================
  btnExportReport.addEventListener('click', () => {
    if (!loadedSuite.tests || Object.keys(loadedSuite.tests).length === 0) {
      alert('No test data loaded to generate a report.');
      return;
    }

    const allAnalysis = {};
    for (const dir of Object.keys(loadedSuite.tests)) {
      allAnalysis[dir] = AdvancedAnalyticsEngine.analyzeTest(loadedSuite.tests[dir]);
    }

    const report = AdvancedAnalyticsEngine.generateComprehensiveReport(allAnalysis);
    const blob = new Blob([report.markdown], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `AirPointer_Diagnostics_Report_${new Date().toISOString().slice(0, 10)}.md`;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }, 100);
  });
});
