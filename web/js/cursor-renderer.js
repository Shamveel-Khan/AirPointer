/**
 * AirPointer Cursor & Desktop Canvas Renderer
 *
 * Renders a simulated computer desktop display with:
 * - High-DPI canvas resolution
 * - Floating-point subpixel cursor positioning
 * - Motion trail visualization to observe smoothness, bounce, and deceleration
 * - Aspect ratio modes (16:9 laptop monitor vs full phone viewport)
 * - Boundary clamping
 */

export class CursorRenderer {
  constructor(canvasElement, options = {}) {
    this.canvas = canvasElement;
    this.ctx = canvasElement.getContext('2d');

    this.aspectRatioMode = options.aspectRatioMode ?? '16:9'; // '16:9' or 'fill'
    this.showTrails = options.showTrails ?? true;

    // Viewport dimensions in CSS pixels
    this.viewWidth = 0;
    this.viewHeight = 0;

    // Cursor position in canvas coordinates (exact floating point)
    this.cursorX = 0;
    this.cursorY = 0;

    // Motion trail history
    this.trail = [];
    this.maxTrailLength = 40;

    // Animation frame
    this.isRendering = false;
    this._rafId = null;

    this._resizeHandler = this.resize.bind(this);
    window.addEventListener('resize', this._resizeHandler);
    this.resize();
    this.resetToCenter();
  }

  resize() {
    const dpr = window.devicePixelRatio || 1;
    const parent = this.canvas.parentElement;
    const availableWidth = parent ? parent.clientWidth : window.innerWidth;
    const availableHeight = parent ? parent.clientHeight : window.innerHeight;

    let targetWidth = availableWidth;
    let targetHeight = availableHeight;

    if (this.aspectRatioMode === '16:9') {
      const targetRatio = 16 / 9;
      const currentRatio = availableWidth / availableHeight;

      if (currentRatio > targetRatio) {
        // Limited by height
        targetHeight = availableHeight;
        targetWidth = targetHeight * targetRatio;
      } else {
        // Limited by width
        targetWidth = availableWidth;
        targetHeight = targetWidth / targetRatio;
      }
    }

    this.viewWidth = Math.floor(targetWidth);
    this.viewHeight = Math.floor(targetHeight);

    this.canvas.style.width = `${this.viewWidth}px`;
    this.canvas.style.height = `${this.viewHeight}px`;

    this.canvas.width = Math.floor(this.viewWidth * dpr);
    this.canvas.height = Math.floor(this.viewHeight * dpr);

    this.ctx.scale(dpr, dpr);

    // Keep cursor within bounds after resize
    this.cursorX = Math.max(0, Math.min(this.viewWidth, this.cursorX));
    this.cursorY = Math.max(0, Math.min(this.viewHeight, this.cursorY));

    this.renderFrame();
  }

  setAspectRatioMode(mode) {
    if (this.aspectRatioMode !== mode) {
      this.aspectRatioMode = mode;
      this.resize();
      this.resetToCenter();
    }
  }

  resetToCenter() {
    this.cursorX = this.viewWidth / 2.0;
    this.cursorY = this.viewHeight / 2.0;
    this.trail = [];
    this.renderFrame();
  }

  /**
   * Apply displacement delta in floating-point pixels
   *
   * @param {number} dx - Horizontal movement
   * @param {number} dy - Vertical movement
   */
  moveCursor(dx, dy) {
    if (dx === 0 && dy === 0) return;

    this.cursorX += dx;
    this.cursorY += dy;

    // Clamping within desktop display bounds
    this.cursorX = Math.max(0, Math.min(this.viewWidth, this.cursorX));
    this.cursorY = Math.max(0, Math.min(this.viewHeight, this.cursorY));

    if (this.showTrails) {
      this.trail.push({ x: this.cursorX, y: this.cursorY, age: 1.0 });
      if (this.trail.length > this.maxTrailLength) {
        this.trail.shift();
      }
    }

    this.renderFrame();
  }

  renderFrame() {
    const ctx = this.ctx;
    const w = this.viewWidth;
    const h = this.viewHeight;

    ctx.clearRect(0, 0, w, h);

    // 1. Draw desktop grid background
    ctx.fillStyle = '#0F172A'; // Deep navy desktop background
    ctx.fillRect(0, 0, w, h);

    // Grid lines
    ctx.strokeStyle = '#1E293B';
    ctx.lineWidth = 1;
    const gridSize = 40;
    ctx.beginPath();
    for (let x = 0; x < w; x += gridSize) {
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
    }
    for (let y = 0; y < h; y += gridSize) {
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
    }
    ctx.stroke();

    // Center crosshair marker
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(w / 2 - 12, h / 2);
    ctx.lineTo(w / 2 + 12, h / 2);
    ctx.moveTo(w / 2, h / 2 - 12);
    ctx.lineTo(w / 2, h / 2 + 12);
    ctx.stroke();

    // 2. Draw motion trails
    if (this.showTrails && this.trail.length > 1) {
      ctx.beginPath();
      ctx.moveTo(this.trail[0].x, this.trail[0].y);
      for (let i = 1; i < this.trail.length; i++) {
        ctx.lineTo(this.trail[i].x, this.trail[i].y);
      }
      ctx.strokeStyle = 'rgba(0, 229, 255, 0.35)'; // Cyan trail
      ctx.lineWidth = 2.5;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.stroke();
    }

    // 3. Draw mouse cursor
    this._drawMousePointer(ctx, this.cursorX, this.cursorY);
  }

  _drawMousePointer(ctx, x, y) {
    ctx.save();
    ctx.translate(x, y);

    // Drop shadow
    ctx.shadowColor = 'rgba(0, 0, 0, 0.6)';
    ctx.shadowBlur = 6;
    ctx.shadowOffsetX = 2;
    ctx.shadowOffsetY = 3;

    // Draw realistic classic OS pointer
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(0, 19);
    ctx.lineTo(4.5, 14.5);
    ctx.lineTo(8.5, 22);
    ctx.lineTo(11.5, 20.5);
    ctx.lineTo(7.5, 13);
    ctx.lineTo(13.5, 13);
    ctx.closePath();

    // White body with dark outline
    ctx.fillStyle = '#FFFFFF';
    ctx.fill();

    ctx.shadowColor = 'transparent';
    ctx.strokeStyle = '#0F172A';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    // Center target dot for precision visualization
    ctx.fillStyle = '#00E5FF'; // Cyan accent dot
    ctx.beginPath();
    ctx.arc(0, 0, 2, 0, Math.PI * 2);
    ctx.fill();

    ctx.restore();
  }

  getCursorCoordinates() {
    return {
      x: this.cursorX,
      y: this.cursorY,
      normalizedX: this.viewWidth > 0 ? this.cursorX / this.viewWidth : 0,
      normalizedY: this.viewHeight > 0 ? this.cursorY / this.viewHeight : 0,
      viewWidth: this.viewWidth,
      viewHeight: this.viewHeight
    };
  }

  destroy() {
    window.removeEventListener('resize', this._resizeHandler);
  }
}
