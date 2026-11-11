/**
 * AirPointer Visual Debugger & Chart Renderer
 *
 * Provides:
 * 1. 2D Trajectory Replay: Expected Direction Vector vs Actual Cursor Trajectory
 * 2. Synchronized Time-Series Sensor Charts:
 *    - Acceleration (Raw vs Filtered Ax, Ay)
 *    - Estimated Velocity (Vx, Vy) with braking & recoil indicators
 * 3. Scrubbable Timeline & Synchronized Playback
 */

import { TestDirection } from './recorder.js';

export class ChartRenderer {
  /**
   * Render the 2D Trajectory comparison canvas.
   *
   * @param {HTMLCanvasElement} canvas
   * @param {Object} testRecord - The recorded test data
   * @param {number|null} playheadTimeMs - Current playback time, or null for full path
   */
  static renderTrajectory(canvas, testRecord, playheadTimeMs = null) {
    if (!canvas || !testRecord || !testRecord.samples || testRecord.samples.length === 0) return;

    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const w = canvas.clientWidth || 320;
    const h = canvas.clientHeight || 240;

    if (canvas.width !== Math.floor(w * dpr) || canvas.height !== Math.floor(h * dpr)) {
      canvas.width = Math.floor(w * dpr);
      canvas.height = Math.floor(h * dpr);
    }

    ctx.save();
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, w, h);

    // Dark background
    ctx.fillStyle = '#0F172A';
    ctx.fillRect(0, 0, w, h);

    // Subtle grid
    ctx.strokeStyle = '#1E293B';
    ctx.lineWidth = 1;
    const step = 30;
    for (let x = 0; x < w; x += step) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
    }
    for (let y = 0; y < h; y += step) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
    }

    // Origin (Start point) in center of canvas
    const originX = w / 2;
    const originY = h / 2;

    // Center crosshair
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(originX - 10, originY); ctx.lineTo(originX + 10, originY);
    ctx.moveTo(originX, originY - 10); ctx.lineTo(originX, originY + 10);
    ctx.stroke();

    // 1. Draw Expected Direction Arrow
    this._drawExpectedArrow(ctx, testRecord.direction, originX, originY, Math.min(w, h) * 0.38);

    // 2. Compute cumulative trajectory coordinates relative to origin
    let curX = originX;
    let curY = originY;
    const points = [{ x: curX, y: curY, t: 0, recoil: false }];

    // Auto-scale path so it fits comfortably within canvas
    let maxExtent = 10;
    let tempX = 0, tempY = 0;
    for (const s of testRecord.samples) {
      tempX += s.dx;
      tempY += s.dy;
      const ext = Math.hypot(tempX, tempY);
      if (ext > maxExtent) maxExtent = ext;
    }

    const availableRadius = Math.min(w, h) * 0.42;
    const scale = maxExtent > 0 ? (availableRadius / Math.max(availableRadius, maxExtent)) : 1.0;

    for (const s of testRecord.samples) {
      curX += s.dx * scale;
      curY += s.dy * scale;

      // Recoil detection flag for color coding
      const isRecoil = (testRecord.direction === TestDirection.UP && s.velY < -0.05) ||
                       (testRecord.direction === TestDirection.DOWN && s.velY > 0.05) ||
                       (testRecord.direction === TestDirection.LEFT && s.velX > 0.05) ||
                       (testRecord.direction === TestDirection.RIGHT && s.velX < -0.05);

      points.push({ x: curX, y: curY, t: s.t, recoil: isRecoil });
    }

    // 3. Draw Actual Trajectory Path
    const maxIndex = playheadTimeMs !== null
      ? points.findIndex(p => p.t >= playheadTimeMs)
      : points.length - 1;
    const renderCount = maxIndex >= 0 ? maxIndex : points.length - 1;

    if (renderCount > 0) {
      for (let i = 1; i <= renderCount; i++) {
        const p0 = points[i - 1];
        const p1 = points[i];
        ctx.beginPath();
        ctx.moveTo(p0.x, p0.y);
        ctx.lineTo(p1.x, p1.y);
        ctx.strokeStyle = p1.recoil ? '#EF4444' : '#00E5FF'; // Red for recoil, Cyan for forward
        ctx.lineWidth = 2.5;
        ctx.lineCap = 'round';
        ctx.stroke();
      }
    }

    // 4. Start Marker (Green circle)
    ctx.fillStyle = '#00E676';
    ctx.beginPath();
    ctx.arc(originX, originY, 4.5, 0, Math.PI * 2);
    ctx.fill();

    // 5. Current Playhead or End Marker
    const headPoint = points[Math.min(renderCount, points.length - 1)];
    if (headPoint) {
      ctx.fillStyle = playheadTimeMs !== null ? '#FFB300' : '#2979FF';
      ctx.beginPath();
      ctx.arc(headPoint.x, headPoint.y, 5.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 1.5;
      ctx.stroke();
    }

    // Legend
    ctx.fillStyle = '#94A3B8';
    ctx.font = '10px ui-monospace, monospace';
    ctx.fillText(`Scale: ${(scale).toFixed(2)}x | Pts: ${renderCount}/${points.length - 1}`, 10, h - 10);

    ctx.restore();
  }

  static _drawExpectedArrow(ctx, direction, cx, cy, length) {
    let dx = 0;
    let dy = 0;
    let label = '';

    switch (direction) {
      case TestDirection.UP:
        dy = -length;
        label = 'EXPECTED UP (Forward)';
        break;
      case TestDirection.DOWN:
        dy = length;
        label = 'EXPECTED DOWN (Back)';
        break;
      case TestDirection.LEFT:
        dx = -length;
        label = 'EXPECTED LEFT';
        break;
      case TestDirection.RIGHT:
        dx = length;
        label = 'EXPECTED RIGHT';
        break;
      default:
        return;
    }

    ctx.save();
    ctx.strokeStyle = 'rgba(0, 230, 118, 0.4)'; // Faint green
    ctx.lineWidth = 6;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + dx, cy + dy);
    ctx.stroke();

    // Arrowhead
    const angle = Math.atan2(dy, dx);
    const headLen = 12;
    ctx.fillStyle = 'rgba(0, 230, 118, 0.6)';
    ctx.beginPath();
    ctx.moveTo(cx + dx, cy + dy);
    ctx.lineTo(
      cx + dx - headLen * Math.cos(angle - Math.PI / 6),
      cy + dy - headLen * Math.sin(angle - Math.PI / 6)
    );
    ctx.lineTo(
      cx + dx - headLen * Math.cos(angle + Math.PI / 6),
      cy + dy - headLen * Math.sin(angle + Math.PI / 6)
    );
    ctx.closePath();
    ctx.fill();

    // Label
    ctx.fillStyle = '#00E676';
    ctx.font = 'bold 11px sans-serif';
    ctx.textAlign = 'center';
    const textOffset = 18;
    ctx.fillText(label, cx + (dx !== 0 ? dx / 2 : 0), cy + (dy !== 0 ? dy / 2 + (dy > 0 ? textOffset : -textOffset) : -8));
    ctx.restore();
  }

  /**
   * Render synchronized multi-series time chart (Accel or Velocity vs Time)
   */
  static renderTimeSeries(canvas, testRecord, options = {}) {
    if (!canvas || !testRecord || !testRecord.samples || testRecord.samples.length === 0) return;

    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const w = canvas.clientWidth || 320;
    const h = canvas.clientHeight || 140;

    if (canvas.width !== Math.floor(w * dpr) || canvas.height !== Math.floor(h * dpr)) {
      canvas.width = Math.floor(w * dpr);
      canvas.height = Math.floor(h * dpr);
    }

    ctx.save();
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, w, h);

    const type = options.type || 'accel'; // 'accel' or 'velocity'
    const playheadTimeMs = options.playheadTimeMs ?? null;

    // Background
    ctx.fillStyle = '#111827';
    ctx.fillRect(0, 0, w, h);

    const padding = { top: 18, bottom: 20, left: 35, right: 12 };
    const plotW = w - padding.left - padding.right;
    const plotH = h - padding.top - padding.bottom;

    const samples = testRecord.samples;
    const totalDuration = testRecord.durationMs || 3000;

    // Find min and max value for vertical scaling
    let minY = -0.5;
    let maxY = 0.5;

    for (const s of samples) {
      if (type === 'accel') {
        const ax = s.unbiasedAx;
        const ay = s.unbiasedAy;
        const rawAx = s.rawAx;
        const rawAy = s.rawAy;
        minY = Math.min(minY, ax, ay, rawAx, rawAy);
        maxY = Math.max(maxY, ax, ay, rawAx, rawAy);
      } else {
        minY = Math.min(minY, s.velX, s.velY);
        maxY = Math.max(maxY, s.velX, s.velY);
      }
    }

    // Add padding to Y bounds
    const yMargin = Math.max(0.2, (maxY - minY) * 0.15);
    minY -= yMargin;
    maxY += yMargin;

    // Helper functions
    const mapX = (t) => padding.left + (t / totalDuration) * plotW;
    const mapY = (val) => padding.top + plotH - ((val - minY) / (maxY - minY)) * plotH;

    // Draw zero reference line
    const zeroY = mapY(0);
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padding.left, zeroY);
    ctx.lineTo(w - padding.right, zeroY);
    ctx.stroke();

    // Axis labels
    ctx.fillStyle = '#64748B';
    ctx.font = '9px ui-monospace, monospace';
    ctx.textAlign = 'right';
    ctx.fillText(`${maxY.toFixed(1)}`, padding.left - 4, padding.top + 8);
    ctx.fillText('0.0', padding.left - 4, zeroY + 3);
    ctx.fillText(`${minY.toFixed(1)}`, padding.left - 4, h - padding.bottom);

    // Time markings
    ctx.textAlign = 'center';
    ctx.fillText('0s', mapX(0), h - 6);
    ctx.fillText('1.5s', mapX(1500), h - 6);
    ctx.fillText('3.0s', mapX(3000), h - 6);

    // Draw series
    if (type === 'accel') {
      // 1. Raw Ax (faint orange dashed)
      this._drawPlotLine(ctx, samples, s => s.t, s => s.rawAx, mapX, mapY, 'rgba(251, 146, 60, 0.35)', 1, true);
      // 2. Raw Ay (faint cyan dashed)
      this._drawPlotLine(ctx, samples, s => s.t, s => s.rawAy, mapX, mapY, 'rgba(56, 189, 248, 0.35)', 1, true);
      // 3. Filtered/Unbiased Ax (solid orange)
      this._drawPlotLine(ctx, samples, s => s.t, s => s.unbiasedAx, mapX, mapY, '#FB923C', 2);
      // 4. Filtered/Unbiased Ay (solid cyan)
      this._drawPlotLine(ctx, samples, s => s.t, s => s.unbiasedAy, mapX, mapY, '#00E5FF', 2);

      // Title & Legend
      this._drawLegend(ctx, [
        { label: 'Unbiased Ax', color: '#FB923C' },
        { label: 'Unbiased Ay', color: '#00E5FF' },
        { label: 'Raw Ax/Ay (dashed)', color: '#64748B' }
      ], padding.left, 12);
    } else {
      // Velocity series
      // Vel X (solid orange)
      this._drawPlotLine(ctx, samples, s => s.t, s => s.velX, mapX, mapY, '#FB923C', 2);
      // Vel Y (solid cyan)
      this._drawPlotLine(ctx, samples, s => s.t, s => s.velY, mapX, mapY, '#00E5FF', 2);

      this._drawLegend(ctx, [
        { label: 'Velocity X (m/s)', color: '#FB923C' },
        { label: 'Velocity Y (m/s)', color: '#00E5FF' }
      ], padding.left, 12);
    }

    // Playhead vertical scrubber needle
    if (playheadTimeMs !== null) {
      const needleX = mapX(playheadTimeMs);
      ctx.strokeStyle = '#FFB300'; // Amber needle
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(needleX, padding.top);
      ctx.lineTo(needleX, h - padding.bottom);
      ctx.stroke();
    }

    ctx.restore();
  }

  static _drawPlotLine(ctx, samples, getT, getVal, mapX, mapY, color, lineWidth = 1.5, isDashed = false) {
    if (samples.length < 2) return;

    ctx.save();
    ctx.strokeStyle = color;
    ctx.lineWidth = lineWidth;
    if (isDashed) ctx.setLineDash([3, 3]);

    ctx.beginPath();
    ctx.moveTo(mapX(getT(samples[0])), mapY(getVal(samples[0])));
    for (let i = 1; i < samples.length; i++) {
      ctx.lineTo(mapX(getT(samples[i])), mapY(getVal(samples[i])));
    }
    ctx.stroke();
    ctx.restore();
  }

  static _drawLegend(ctx, items, startX, y) {
    ctx.save();
    ctx.font = '10px sans-serif';
    let curX = startX;
    for (const item of items) {
      ctx.fillStyle = item.color;
      ctx.fillRect(curX, y - 7, 8, 8);
      curX += 12;
      ctx.fillStyle = '#CBD5E1';
      ctx.fillText(item.label, curX, y);
      curX += ctx.measureText(item.label).width + 12;
    }
    ctx.restore();
  }
}
