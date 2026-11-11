/**
 * AirPointer Advanced Chart & Trajectory Visualizer
 *
 * Provides:
 * - Multi-Track Synchronized Canvas Time-Series Charts with Hover Crosshairs
 * - 2D Trajectory Visualization with Expected Vector & What-If Comparison Overlay
 * - Anomaly Interval Highlighting (Recoil, Tilt Leakage, ZUPT locks)
 */

import { TestDirection } from './recorder.js';

export class AdvancedChartRenderer {
  /**
   * Render the 2D Trajectory Canvas.
   * Overlays:
   * 1. Expected Ground-Truth Direction Vector (Green Arrow)
   * 2. Original Recorded Cursor Trajectory (Cyan line with Start/End markers)
   * 3. Optional Reprocessed / What-If Trajectory (Bright Green line for comparison)
   * 4. Current Hover / Playhead Cursor (Amber dot)
   */
  static renderTrajectory(canvas, originalRecord, options = {}) {
    if (!canvas || !originalRecord || !originalRecord.samples || originalRecord.samples.length === 0) return;

    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const w = canvas.clientWidth || 360;
    const h = canvas.clientHeight || 280;

    if (canvas.width !== Math.floor(w * dpr) || canvas.height !== Math.floor(h * dpr)) {
      canvas.width = Math.floor(w * dpr);
      canvas.height = Math.floor(h * dpr);
    }

    ctx.save();
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, w, h);

    const reprocessedRecord = options.reprocessedRecord || null;
    const hoverTimeMs = options.hoverTimeMs ?? null;

    // 1. Desktop background
    ctx.fillStyle = '#0F172A';
    ctx.fillRect(0, 0, w, h);

    // Grid lines
    ctx.strokeStyle = '#1E293B';
    ctx.lineWidth = 1;
    const step = 32;
    for (let x = 0; x < w; x += step) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
    }
    for (let y = 0; y < h; y += step) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
    }

    const originX = w / 2;
    const originY = h / 2;

    // Center crosshair
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(originX - 12, originY); ctx.lineTo(originX + 12, originY);
    ctx.moveTo(originX, originY - 12); ctx.lineTo(originX, originY + 12);
    ctx.stroke();

    // 2. Expected Direction Vector
    this._drawExpectedArrow(ctx, originalRecord.direction, originX, originY, Math.min(w, h) * 0.38);

    // Compute coordinate points & bounds for auto-scaling
    let maxExtent = 12;
    let tempX = 0, tempY = 0;
    for (const s of originalRecord.samples) {
      tempX += s.dx;
      tempY += s.dy;
      const dist = Math.hypot(tempX, tempY);
      if (dist > maxExtent) maxExtent = dist;
    }

    if (reprocessedRecord && reprocessedRecord.samples) {
      tempX = 0; tempY = 0;
      for (const s of reprocessedRecord.samples) {
        tempX += s.dx;
        tempY += s.dy;
        const dist = Math.hypot(tempX, tempY);
        if (dist > maxExtent) maxExtent = dist;
      }
    }

    const availableRadius = Math.min(w, h) * 0.42;
    const scale = availableRadius / Math.max(availableRadius, maxExtent);

    // 3. Draw Original Trajectory
    const origPoints = this._buildTrajectoryPoints(originalRecord.samples, originX, originY, scale, originalRecord.direction);
    this._drawPath(ctx, origPoints, '#00E5FF', 2.5);

    // 4. Draw Reprocessed / What-If Trajectory if available
    let modPoints = null;
    if (reprocessedRecord && reprocessedRecord.samples) {
      modPoints = this._buildTrajectoryPoints(reprocessedRecord.samples, originX, originY, scale, originalRecord.direction);
      this._drawPath(ctx, modPoints, '#00E676', 2.0, true);
    }

    // 5. Origin / Start Marker (Green circle)
    ctx.fillStyle = '#00E676';
    ctx.beginPath();
    ctx.arc(originX, originY, 5, 0, Math.PI * 2);
    ctx.fill();

    // 6. End Marker for Original (Blue circle)
    const lastOrig = origPoints[origPoints.length - 1];
    if (lastOrig) {
      ctx.fillStyle = '#2979FF';
      ctx.beginPath();
      ctx.arc(lastOrig.x, lastOrig.y, 5, 0, Math.PI * 2);
      ctx.fill();
    }

    // 7. Hover / Playhead Point (Amber ring)
    if (hoverTimeMs !== null && origPoints.length > 0) {
      let targetPt = origPoints[0];
      for (let i = 0; i < origPoints.length; i++) {
        if (origPoints[i].t >= hoverTimeMs) {
          targetPt = origPoints[i];
          break;
        }
      }
      ctx.fillStyle = '#FFB300';
      ctx.beginPath();
      ctx.arc(targetPt.x, targetPt.y, 6.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 2;
      ctx.stroke();
    }

    // Legend
    ctx.font = '10px sans-serif';
    ctx.fillStyle = '#00E5FF';
    ctx.fillRect(10, h - 22, 10, 3);
    ctx.fillText('Original Path', 24, h - 17);

    if (modPoints) {
      ctx.fillStyle = '#00E676';
      ctx.fillRect(105, h - 22, 10, 3);
      ctx.fillText('Experiment Path', 119, h - 17);
    }

    ctx.restore();
  }

  static _buildTrajectoryPoints(samples, originX, originY, scale, direction) {
    let curX = originX;
    let curY = originY;
    const points = [{ x: curX, y: curY, t: 0, isRecoil: false }];

    for (const s of samples) {
      curX += s.dx * scale;
      curY += s.dy * scale;

      const isRecoil = (direction === TestDirection.UP && s.velY < -0.04) ||
                       (direction === TestDirection.DOWN && s.velY > 0.04) ||
                       (direction === TestDirection.LEFT && s.velX > 0.04) ||
                       (direction === TestDirection.RIGHT && s.velX < -0.04);

      points.push({ x: curX, y: curY, t: s.t, isRecoil });
    }
    return points;
  }

  static _drawPath(ctx, points, defaultColor, lineWidth = 2.5, isDashed = false) {
    if (points.length < 2) return;

    ctx.save();
    ctx.lineWidth = lineWidth;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    if (isDashed) ctx.setLineDash([4, 4]);

    for (let i = 1; i < points.length; i++) {
      const p0 = points[i - 1];
      const p1 = points[i];
      ctx.strokeStyle = p1.isRecoil ? '#EF4444' : defaultColor;
      ctx.beginPath();
      ctx.moveTo(p0.x, p0.y);
      ctx.lineTo(p1.x, p1.y);
      ctx.stroke();
    }
    ctx.restore();
  }

  static _drawExpectedArrow(ctx, direction, cx, cy, length) {
    let dx = 0, dy = 0, label = '';
    switch (direction) {
      case TestDirection.UP: dy = -length; label = 'EXPECTED UP'; break;
      case TestDirection.DOWN: dy = length; label = 'EXPECTED DOWN'; break;
      case TestDirection.LEFT: dx = -length; label = 'EXPECTED LEFT'; break;
      case TestDirection.RIGHT: dx = length; label = 'EXPECTED RIGHT'; break;
      default: return;
    }

    ctx.save();
    ctx.strokeStyle = 'rgba(0, 230, 118, 0.45)';
    ctx.lineWidth = 5;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + dx, cy + dy);
    ctx.stroke();

    const angle = Math.atan2(dy, dx);
    const headLen = 12;
    ctx.fillStyle = 'rgba(0, 230, 118, 0.65)';
    ctx.beginPath();
    ctx.moveTo(cx + dx, cy + dy);
    ctx.lineTo(cx + dx - headLen * Math.cos(angle - Math.PI / 6), cy + dy - headLen * Math.sin(angle - Math.PI / 6));
    ctx.lineTo(cx + dx - headLen * Math.cos(angle + Math.PI / 6), cy + dy - headLen * Math.sin(angle + Math.PI / 6));
    ctx.closePath();
    ctx.fill();

    ctx.fillStyle = '#00E676';
    ctx.font = 'bold 11px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(label, cx + (dx !== 0 ? dx / 2 : 0), cy + (dy !== 0 ? dy / 2 + (dy > 0 ? 16 : -10) : -8));
    ctx.restore();
  }

  /**
   * Render Synchronized Time-Series Chart with Anomaly Highlighting
   *
   * @param {HTMLCanvasElement} canvas
   * @param {Object} testRecord
   * @param {Object} options - { trackType, hoverTimeMs, anomalies: [...] }
   */
  static renderTimeSeriesTrack(canvas, testRecord, options = {}) {
    if (!canvas || !testRecord || !testRecord.samples || testRecord.samples.length === 0) return;

    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const w = canvas.clientWidth || 360;
    const h = canvas.clientHeight || 140;

    if (canvas.width !== Math.floor(w * dpr) || canvas.height !== Math.floor(h * dpr)) {
      canvas.width = Math.floor(w * dpr);
      canvas.height = Math.floor(h * dpr);
    }

    ctx.save();
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, w, h);

    const trackType = options.trackType || 'accel'; // 'accel', 'gyro', 'velocity', 'cursor'
    const hoverTimeMs = options.hoverTimeMs ?? null;
    const anomalies = options.anomalies || [];

    // Background
    ctx.fillStyle = '#111827';
    ctx.fillRect(0, 0, w, h);

    const pad = { top: 16, bottom: 20, left: 40, right: 12 };
    const plotW = w - pad.left - pad.right;
    const plotH = h - pad.top - pad.bottom;

    const samples = testRecord.samples;
    const totalDuration = testRecord.durationMs || 3000;

    // Compute min/max Y for scaling
    let minY = -0.2;
    let maxY = 0.2;

    for (const s of samples) {
      if (trackType === 'accel') {
        minY = Math.min(minY, s.unbiasedAx, s.unbiasedAy, s.rawAx, s.rawAy);
        maxY = Math.max(maxY, s.unbiasedAx, s.unbiasedAy, s.rawAx, s.rawAy);
      } else if (trackType === 'gyro') {
        const gz = (s.rotationRateZ || s.gyroZ || 0);
        minY = Math.min(minY, gz);
        maxY = Math.max(maxY, gz);
      } else if (trackType === 'velocity') {
        minY = Math.min(minY, s.velX, s.velY);
        maxY = Math.max(maxY, s.velX, s.velY);
      } else if (trackType === 'cursor') {
        minY = Math.min(minY, s.dx, s.dy);
        maxY = Math.max(maxY, s.dx, s.dy);
      }
    }

    const yMargin = Math.max(0.1, (maxY - minY) * 0.15);
    minY -= yMargin;
    maxY += yMargin;

    const mapX = (t) => pad.left + (t / totalDuration) * plotW;
    const mapY = (val) => pad.top + plotH - ((val - minY) / (maxY - minY)) * plotH;

    // 1. Draw Anomaly Shading Regions
    if (anomalies && anomalies.length > 0) {
      for (const a of anomalies) {
        if (a.startT !== undefined && a.endT !== undefined) {
          const x0 = mapX(a.startT);
          const x1 = mapX(a.endT);
          ctx.fillStyle = a.color || 'rgba(239, 68, 68, 0.15)'; // Red tint for recoil
          ctx.fillRect(x0, pad.top, Math.max(2, x1 - x0), plotH);
        }
      }
    }

    // 2. Zero baseline
    const zeroY = mapY(0);
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(pad.left, zeroY);
    ctx.lineTo(w - pad.right, zeroY);
    ctx.stroke();

    // 3. Grid labels
    ctx.fillStyle = '#64748B';
    ctx.font = '9px ui-monospace, monospace';
    ctx.textAlign = 'right';
    ctx.fillText(`${maxY.toFixed(1)}`, pad.left - 4, pad.top + 8);
    ctx.fillText('0.0', pad.left - 4, zeroY + 3);
    ctx.fillText(`${minY.toFixed(1)}`, pad.left - 4, h - pad.bottom);

    ctx.textAlign = 'center';
    ctx.fillText('0s', mapX(0), h - 6);
    ctx.fillText(`${(totalDuration / 2000).toFixed(1)}s`, mapX(totalDuration / 2), h - 6);
    ctx.fillText(`${(totalDuration / 1000).toFixed(1)}s`, mapX(totalDuration), h - 6);

    // 4. Draw Series Lines
    if (trackType === 'accel') {
      // Raw Ax, Ay (faint dashed)
      this._drawLine(ctx, samples, s => s.t, s => s.rawAx, mapX, mapY, 'rgba(251, 146, 60, 0.3)', 1, true);
      this._drawLine(ctx, samples, s => s.t, s => s.rawAy, mapX, mapY, 'rgba(56, 189, 248, 0.3)', 1, true);
      // Filtered Unbiased Ax (orange), Ay (cyan)
      this._drawLine(ctx, samples, s => s.t, s => s.unbiasedAx, mapX, mapY, '#FB923C', 1.8);
      this._drawLine(ctx, samples, s => s.t, s => s.unbiasedAy, mapX, mapY, '#00E5FF', 1.8);

      this._drawLegend(ctx, [
        { label: 'Unbiased Ax', color: '#FB923C' },
        { label: 'Unbiased Ay', color: '#00E5FF' },
        { label: 'Raw (dashed)', color: '#64748B' }
      ], pad.left, 10);
    } else if (trackType === 'gyro') {
      this._drawLine(ctx, samples, s => s.t, s => (s.rotationRateZ || s.gyroZ || 0), mapX, mapY, '#A855F7', 2);
      this._drawLegend(ctx, [{ label: 'Gyroscope Z (rad/s or deg/s)', color: '#A855F7' }], pad.left, 10);
    } else if (trackType === 'velocity') {
      this._drawLine(ctx, samples, s => s.t, s => s.velX, mapX, mapY, '#FB923C', 2);
      this._drawLine(ctx, samples, s => s.t, s => s.velY, mapX, mapY, '#00E5FF', 2);
      this._drawLegend(ctx, [
        { label: 'Velocity X (m/s)', color: '#FB923C' },
        { label: 'Velocity Y (m/s)', color: '#00E5FF' }
      ], pad.left, 10);
    } else if (trackType === 'cursor') {
      this._drawLine(ctx, samples, s => s.t, s => s.dx, mapX, mapY, '#FB923C', 1.5);
      this._drawLine(ctx, samples, s => s.t, s => s.dy, mapX, mapY, '#00E5FF', 1.5);
      this._drawLegend(ctx, [
        { label: 'Cursor dX (px)', color: '#FB923C' },
        { label: 'Cursor dY (px)', color: '#00E5FF' }
      ], pad.left, 10);
    }

    // 5. Synchronized Hover Crosshair Needle & Value Tooltip
    if (hoverTimeMs !== null && hoverTimeMs >= 0 && hoverTimeMs <= totalDuration) {
      const needleX = mapX(hoverTimeMs);
      ctx.strokeStyle = '#FFB300';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(needleX, pad.top);
      ctx.lineTo(needleX, h - pad.bottom);
      ctx.stroke();

      // Find sample closest to hoverTime
      const hovered = samples.reduce((prev, curr) =>
        Math.abs(curr.t - hoverTimeMs) < Math.abs(prev.t - hoverTimeMs) ? curr : prev, samples[0]);

      if (hovered) {
        ctx.fillStyle = '#FFB300';
        ctx.beginPath();
        let val = 0;
        if (trackType === 'accel') val = hovered.unbiasedAy;
        else if (trackType === 'gyro') val = (hovered.rotationRateZ || hovered.gyroZ || 0);
        else if (trackType === 'velocity') val = hovered.velY;
        else if (trackType === 'cursor') val = hovered.dy;

        ctx.arc(needleX, mapY(val), 4, 0, Math.PI * 2);
        ctx.fill();

        // Tooltip text
        ctx.fillStyle = '#CBD5E1';
        ctx.font = 'bold 9px ui-monospace, monospace';
        ctx.textAlign = needleX > w - 80 ? 'right' : 'left';
        ctx.fillText(`t:${hovered.t.toFixed(0)}ms val:${val.toFixed(2)}`, needleX > w - 80 ? needleX - 6 : needleX + 6, pad.top + 10);
      }
    }

    ctx.restore();
  }

  static _drawLine(ctx, samples, getT, getVal, mapX, mapY, color, lineWidth = 1.5, isDashed = false) {
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
      curX += ctx.measureText(item.label).width + 10;
    }
    ctx.restore();
  }
}
