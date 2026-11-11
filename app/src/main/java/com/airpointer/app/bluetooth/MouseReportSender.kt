package com.airpointer.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MouseReportSender(
    private val getHidDevice: () -> BluetoothHidDevice?
) {
    companion object {
        private const val TAG = "MouseReportSender"
        // 11ms interval = ~90 Hz, perfectly matches Bluetooth HID host polling rate.
        // Prevents bufferbloat and queue delay in Android Bluetooth L2CAP stack.
        private const val REPORT_INTERVAL_MS = 11L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val lock = Any()

    // Pre-allocated report buffer to eliminate runtime GC allocations
    private val reportBuffer = ByteArray(5)
    private var currentButtons: Byte = HidReportDescriptor.MOUSE_BUTTON_NONE

    // Accumulators for sub-interval movements
    private var pendingDx = 0f
    private var pendingDy = 0f
    private var pendingWheel = 0
    private var pendingPan = 0
    private var activeDevice: BluetoothDevice? = null

    private var lastSendTimeMs = 0L
    private var isFlushScheduled = false

    private val flushRunnable = Runnable {
        synchronized(lock) {
            isFlushScheduled = false
            flushAccumulatedReport()
        }
    }

    /**
     * Send raw mouse report to the target Bluetooth HID host.
     * Report payload (5 bytes):
     * [0] = Button mask (Bit 0: Left, 1: Right, 2: Middle, 3: Back, 4: Forward)
     * [1] = dX (-127 to 127)
     * [2] = dY (-127 to 127)
     * [3] = dWheel (-127 to 127, positive = up, negative = down)
     * [4] = dPan (-127 to 127, positive = right, negative = left)
     */
    @SuppressLint("MissingPermission")
    private fun sendRawReport(
        device: BluetoothDevice,
        buttons: Byte,
        dx: Byte,
        dy: Byte,
        wheel: Byte,
        pan: Byte
    ): Boolean {
        val hidDevice = getHidDevice() ?: return false
        reportBuffer[0] = buttons
        reportBuffer[1] = dx
        reportBuffer[2] = dy
        reportBuffer[3] = wheel
        reportBuffer[4] = pan

        return try {
            hidDevice.sendReport(device, HidReportDescriptor.REPORT_ID_MOUSE.toInt(), reportBuffer)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending mouse report", e)
            false
        }
    }

    /**
     * Relative cursor movement with optional button state preserved.
     * Uses rate-limited accumulator to match Bluetooth HID polling rate without bufferbloat.
     */
    fun moveCursor(device: BluetoothDevice?, dx: Int, dy: Int, buttons: Byte = currentButtons): Boolean {
        return moveCursorFloat(device, dx.toFloat(), dy.toFloat(), buttons)
    }

    fun moveCursorFloat(device: BluetoothDevice?, dx: Float, dy: Float, buttons: Byte = currentButtons): Boolean {
        if (device == null) return false

        synchronized(lock) {
            activeDevice = device
            currentButtons = buttons
            pendingDx += dx
            pendingDy += dy

            val now = System.currentTimeMillis()
            val timeSinceLast = now - lastSendTimeMs

            if (timeSinceLast >= REPORT_INTERVAL_MS) {
                handler.removeCallbacks(flushRunnable)
                isFlushScheduled = false
                return flushAccumulatedReport()
            } else {
                if (!isFlushScheduled) {
                    val delayMs = REPORT_INTERVAL_MS - timeSinceLast
                    handler.postDelayed(flushRunnable, max(1L, delayMs))
                    isFlushScheduled = true
                }
                return true
            }
        }
    }

    private fun flushAccumulatedReport(): Boolean {
        val device = activeDevice ?: return false
        val now = System.currentTimeMillis()

        val sendDx = max(-127, min(127, pendingDx.roundToInt()))
        val sendDy = max(-127, min(127, pendingDy.roundToInt()))
        val sendWheel = max(-127, min(127, pendingWheel))
        val sendPan = max(-127, min(127, pendingPan))

        if (sendDx == 0 && sendDy == 0 && sendWheel == 0 && sendPan == 0) {
            return true
        }

        val success = sendRawReport(
            device = device,
            buttons = currentButtons,
            dx = sendDx.toByte(),
            dy = sendDy.toByte(),
            wheel = sendWheel.toByte(),
            pan = sendPan.toByte()
        )

        if (success) {
            pendingDx -= sendDx
            pendingDy -= sendDy
            pendingWheel -= sendWheel
            pendingPan -= sendPan
            lastSendTimeMs = now
        } else {
            // If Bluetooth stack buffer is full, back off slightly to let buffer drain
            lastSendTimeMs = now + 4L
        }

        // If there's still movement remaining that exceeded -127..127, schedule immediate follow-up
        if ((pendingDx.roundToInt() != 0 || pendingDy.roundToInt() != 0) && !isFlushScheduled) {
            handler.postDelayed(flushRunnable, REPORT_INTERVAL_MS)
            isFlushScheduled = true
        }

        return success
    }

    /**
     * Vertical and horizontal scroll.
     */
    fun scroll(device: BluetoothDevice?, verticalSteps: Int, horizontalSteps: Int = 0): Boolean {
        if (device == null) return false
        synchronized(lock) {
            activeDevice = device
            pendingWheel += verticalSteps
            pendingPan += horizontalSteps

            val now = System.currentTimeMillis()
            if (now - lastSendTimeMs >= REPORT_INTERVAL_MS) {
                handler.removeCallbacks(flushRunnable)
                isFlushScheduled = false
                return flushAccumulatedReport()
            } else {
                if (!isFlushScheduled) {
                    handler.postDelayed(flushRunnable, max(1L, REPORT_INTERVAL_MS - (now - lastSendTimeMs)))
                    isFlushScheduled = true
                }
                return true
            }
        }
    }

    /**
     * Set button down or up (immediate priority delivery).
     */
    fun setButton(device: BluetoothDevice?, button: Byte, isPressed: Boolean): Boolean {
        if (device == null) return false
        synchronized(lock) {
            activeDevice = device
            currentButtons = if (isPressed) {
                (currentButtons.toInt() or button.toInt()).toByte()
            } else {
                (currentButtons.toInt() and button.toInt().inv()).toByte()
            }
            // Flush any pending movement along with the button state immediately
            val sendDx = max(-127, min(127, pendingDx.roundToInt()))
            val sendDy = max(-127, min(127, pendingDy.roundToInt()))
            pendingDx -= sendDx
            pendingDy -= sendDy
            lastSendTimeMs = System.currentTimeMillis()
            return sendRawReport(device, currentButtons, sendDx.toByte(), sendDy.toByte(), 0, 0)
        }
    }

    /**
     * Perform a complete click (press -> brief delay -> release).
     */
    fun sendClick(
        device: BluetoothDevice?,
        button: Byte,
        scope: CoroutineScope,
        clickHoldMs: Long = 20L
    ) {
        if (device == null) return
        scope.launch(Dispatchers.IO) {
            setButton(device, button, true)
            delay(clickHoldMs)
            setButton(device, button, false)
        }
    }

    /**
     * Perform a double-click.
     */
    fun sendDoubleClick(
        device: BluetoothDevice?,
        button: Byte,
        scope: CoroutineScope,
        clickHoldMs: Long = 20L,
        intervalMs: Long = 40L
    ) {
        if (device == null) return
        scope.launch(Dispatchers.IO) {
            setButton(device, button, true)
            delay(clickHoldMs)
            setButton(device, button, false)
            delay(intervalMs)
            setButton(device, button, true)
            delay(clickHoldMs)
            setButton(device, button, false)
        }
    }

    /**
     * Reset all buttons to unpressed state.
     */
    fun releaseAllButtons(device: BluetoothDevice?): Boolean {
        synchronized(lock) {
            currentButtons = HidReportDescriptor.MOUSE_BUTTON_NONE
            pendingDx = 0f
            pendingDy = 0f
            pendingWheel = 0
            pendingPan = 0
            handler.removeCallbacks(flushRunnable)
            isFlushScheduled = false
            return if (device != null) sendRawReport(device, HidReportDescriptor.MOUSE_BUTTON_NONE, 0, 0, 0, 0) else false
        }
    }

    fun getCurrentButtons(): Byte = currentButtons
}
