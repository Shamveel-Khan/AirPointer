package com.airpointer.app.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.airpointer.app.data.UserSettings
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

interface TouchpadListener {
    fun onMove(dx: Float, dy: Float, isDragging: Boolean)
    fun onLeftClick()
    fun onRightClick()
    fun onDoubleClick()
    fun onScroll(vertical: Float, horizontal: Float)
    fun onDragStateChanged(isDragging: Boolean)
    fun onHapticFeedback(type: HapticType)
}

enum class HapticType {
    CLICK,
    DOUBLE_CLICK,
    HEAVY_CLICK,
    TICK
}

class TouchpadGestureHandler(
    private val listener: TouchpadListener,
    var settings: UserSettings
) {
    companion object {
        private const val TAP_TIMEOUT_MS = 240L
        private const val DOUBLE_TAP_TIMEOUT_MS = 280L
        private const val LONG_PRESS_TIMEOUT_MS = 360L
        private const val TOUCH_SLOP_PX = 18f
        private const val SCROLL_SLOP_PX = 10f
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // 1-finger tracking
    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f
    private var downTimeMs = 0L
    private var hasMoved = false

    // Tap tracking
    private var lastTapTimeMs = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    // 2-finger tracking
    private var isTwoFingerGesture = false
    private var lastTwoFingerAvgX = 0f
    private var lastTwoFingerAvgY = 0f
    private var twoFingerDownTimeMs = 0L
    private var twoFingerHasMoved = false

    // Drag mode
    private var isDragging = false
    private var longPressTriggered = false

    private val longPressRunnable = Runnable {
        if (!hasMoved && !isTwoFingerGesture) {
            isDragging = true
            longPressTriggered = true
            listener.onDragStateChanged(true)
            listener.onHapticFeedback(HapticType.HEAVY_CLICK)
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                lastX = event.x
                lastY = event.y
                downTimeMs = System.currentTimeMillis()
                hasMoved = false
                longPressTriggered = false
                isTwoFingerGesture = false

                // Schedule long press check for dragging
                mainHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS)
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Second finger touched down
                mainHandler.removeCallbacks(longPressRunnable)
                if (pointerCount == 2) {
                    isTwoFingerGesture = true
                    twoFingerDownTimeMs = System.currentTimeMillis()
                    twoFingerHasMoved = false

                    val p0x = event.getX(0)
                    val p0y = event.getY(0)
                    val p1x = event.getX(1)
                    val p1y = event.getY(1)
                    lastTwoFingerAvgX = (p0x + p1x) / 2f
                    lastTwoFingerAvgY = (p0y + p1y) / 2f
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 1 && !isTwoFingerGesture) {
                    val currentX = event.x
                    val currentY = event.y
                    val distFromStart = hypot(currentX - startX, currentY - startY)

                    if (!hasMoved && distFromStart > TOUCH_SLOP_PX) {
                        hasMoved = true
                        mainHandler.removeCallbacks(longPressRunnable)
                    }

                    if (hasMoved || isDragging) {
                        val rawDx = currentX - lastX
                        val rawDy = currentY - lastY

                        var dx = rawDx * settings.cursorSensitivity
                        var dy = rawDy * settings.cursorSensitivity

                        if (settings.pointerAcceleration) {
                            val speed = hypot(rawDx, rawDy)
                            val accelFactor = when {
                                speed < 4f -> 0.85f
                                speed > 30f -> min(2.5f, 1.4f + (speed - 30f) * 0.035f)
                                else -> 1.0f + (speed - 4f) * 0.015f
                            }
                            dx *= accelFactor
                            dy *= accelFactor
                        }

                        listener.onMove(dx, dy, isDragging)
                    }

                    lastX = currentX
                    lastY = currentY
                } else if (pointerCount >= 2) {
                    mainHandler.removeCallbacks(longPressRunnable)
                    val p0x = event.getX(0)
                    val p0y = event.getY(0)
                    val p1x = event.getX(1)
                    val p1y = event.getY(1)
                    val avgX = (p0x + p1x) / 2f
                    val avgY = (p0y + p1y) / 2f

                    val rawDeltaX = avgX - lastTwoFingerAvgX
                    val rawDeltaY = avgY - lastTwoFingerAvgY

                    if (hypot(rawDeltaX, rawDeltaY) > SCROLL_SLOP_PX) {
                        twoFingerHasMoved = true
                    }

                    if (twoFingerHasMoved) {
                        val scrollMultiplier = settings.scrollSensitivity * 0.4f
                        val dir = if (settings.naturalScrolling) 1f else -1f
                        val scrollY = rawDeltaY * scrollMultiplier * dir
                        val scrollX = rawDeltaX * scrollMultiplier

                        listener.onScroll(scrollY, scrollX)
                    }

                    lastTwoFingerAvgX = avgX
                    lastTwoFingerAvgY = avgY
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (isTwoFingerGesture && pointerCount == 2) {
                    val duration = System.currentTimeMillis() - twoFingerDownTimeMs
                    if (!twoFingerHasMoved && duration < TAP_TIMEOUT_MS + 60L) {
                        // Two finger tap -> Right Click!
                        listener.onRightClick()
                        listener.onHapticFeedback(HapticType.CLICK)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(longPressRunnable)
                val duration = System.currentTimeMillis() - downTimeMs

                if (isDragging) {
                    isDragging = false
                    listener.onDragStateChanged(false)
                    listener.onHapticFeedback(HapticType.TICK)
                } else if (!hasMoved && !isTwoFingerGesture && duration < TAP_TIMEOUT_MS) {
                    val now = System.currentTimeMillis()
                    val tapDist = hypot(event.x - lastTapX, event.y - lastTapY)

                    if (now - lastTapTimeMs < DOUBLE_TAP_TIMEOUT_MS && tapDist < TOUCH_SLOP_PX * 2f) {
                        // Double tap
                        listener.onDoubleClick()
                        listener.onHapticFeedback(HapticType.DOUBLE_CLICK)
                        lastTapTimeMs = 0L // reset
                    } else {
                        // Single tap
                        listener.onLeftClick()
                        listener.onHapticFeedback(HapticType.CLICK)
                        lastTapTimeMs = now
                        lastTapX = event.x
                        lastTapY = event.y
                    }
                }

                isTwoFingerGesture = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                if (isDragging) {
                    isDragging = false
                    listener.onDragStateChanged(false)
                }
                isTwoFingerGesture = false
                return true
            }
        }
        return false
    }

    fun setDragLocked(locked: Boolean) {
        if (isDragging != locked) {
            isDragging = locked
            listener.onDragStateChanged(locked)
            listener.onHapticFeedback(if (locked) HapticType.HEAVY_CLICK else HapticType.TICK)
        }
    }

    fun isDragging(): Boolean = isDragging
}
