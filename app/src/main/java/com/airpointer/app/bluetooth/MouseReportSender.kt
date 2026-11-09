package com.airpointer.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class MouseReportSender(
    private val getHidDevice: () -> BluetoothHidDevice?
) {
    companion object {
        private const val TAG = "MouseReportSender"
    }

    private var currentButtons: Byte = HidReportDescriptor.MOUSE_BUTTON_NONE

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
    fun sendMouseReport(
        device: BluetoothDevice?,
        buttons: Byte = currentButtons,
        dx: Byte = 0,
        dy: Byte = 0,
        wheel: Byte = 0,
        pan: Byte = 0
    ): Boolean {
        if (device == null) return false
        val hidDevice = getHidDevice() ?: return false

        currentButtons = buttons
        val report = byteArrayOf(buttons, dx, dy, wheel, pan)
        return try {
            hidDevice.sendReport(device, HidReportDescriptor.REPORT_ID_MOUSE.toInt(), report)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending mouse report", e)
            false
        }
    }

    /**
     * Relative cursor movement with optional button state preserved.
     */
    fun moveCursor(device: BluetoothDevice?, dx: Int, dy: Int, buttons: Byte = currentButtons): Boolean {
        val clampedDx = max(-127, min(127, dx)).toByte()
        val clampedDy = max(-127, min(127, dy)).toByte()
        return sendMouseReport(device, buttons = buttons, dx = clampedDx, dy = clampedDy)
    }

    /**
     * Vertical and horizontal scroll.
     */
    fun scroll(device: BluetoothDevice?, verticalSteps: Int, horizontalSteps: Int = 0): Boolean {
        val clampedWheel = max(-127, min(127, verticalSteps)).toByte()
        val clampedPan = max(-127, min(127, horizontalSteps)).toByte()
        return sendMouseReport(device, buttons = currentButtons, dx = 0, dy = 0, wheel = clampedWheel, pan = clampedPan)
    }

    /**
     * Set button down or up (useful for drag lock or pressing button).
     */
    fun setButton(device: BluetoothDevice?, button: Byte, isPressed: Boolean): Boolean {
        val newButtons = if (isPressed) {
            (currentButtons.toInt() or button.toInt()).toByte()
        } else {
            (currentButtons.toInt() and button.toInt().inv()).toByte()
        }
        return sendMouseReport(device, buttons = newButtons, dx = 0, dy = 0)
    }

    /**
     * Perform a complete click (press -> brief delay -> release).
     */
    fun sendClick(
        device: BluetoothDevice?,
        button: Byte,
        scope: CoroutineScope,
        clickHoldMs: Long = 25L
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
        clickHoldMs: Long = 25L,
        intervalMs: Long = 50L
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
        currentButtons = HidReportDescriptor.MOUSE_BUTTON_NONE
        return sendMouseReport(device, buttons = HidReportDescriptor.MOUSE_BUTTON_NONE, dx = 0, dy = 0)
    }

    fun getCurrentButtons(): Byte = currentButtons
}
