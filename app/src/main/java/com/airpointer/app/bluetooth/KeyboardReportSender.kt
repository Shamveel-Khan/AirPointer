package com.airpointer.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KeyboardReportSender(
    private val getHidDevice: () -> BluetoothHidDevice?
) {
    companion object {
        private const val TAG = "KeyboardReportSender"
    }

    private var currentModifiers: Byte = HidReportDescriptor.MODIFIER_NONE
    private val activeKeys = mutableListOf<Byte>()

    /**
     * Send standard 8-byte keyboard report.
     * [0] = Modifiers (bitfield: LCtrl, LShift, LAlt, LGUI, RCtrl, RShift, RAlt, RGUI)
     * [1] = Reserved (0x00)
     * [2..7] = 6 active key scancodes
     */
    @SuppressLint("MissingPermission")
    fun sendKeyboardReport(
        device: BluetoothDevice?,
        modifiers: Byte = currentModifiers,
        keys: List<Byte> = activeKeys
    ): Boolean {
        if (device == null) return false
        val hidDevice = getHidDevice() ?: return false

        currentModifiers = modifiers
        val report = ByteArray(8)
        report[0] = modifiers
        report[1] = 0x00
        for (i in 0 until 6) {
            report[2 + i] = if (i < keys.size) keys[i] else 0x00
        }

        return try {
            hidDevice.sendReport(device, HidReportDescriptor.REPORT_ID_KEYBOARD.toInt(), report)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending keyboard report", e)
            false
        }
    }

    /**
     * Send Consumer Control Report (Report ID 3, 2 bytes for 16-bit usage).
     */
    @SuppressLint("MissingPermission")
    fun sendConsumerReport(
        device: BluetoothDevice?,
        usage: Short
    ): Boolean {
        if (device == null) return false
        val hidDevice = getHidDevice() ?: return false

        val report = byteArrayOf(
            (usage.toInt() and 0xFF).toByte(),
            ((usage.toInt() shr 8) and 0xFF).toByte()
        )
        return try {
            hidDevice.sendReport(device, HidReportDescriptor.REPORT_ID_CONSUMER.toInt(), report)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending consumer report", e)
            false
        }
    }

    /**
     * Trigger a consumer action with instant press-and-release.
     */
    fun tapConsumerKey(
        device: BluetoothDevice?,
        usage: Short,
        scope: CoroutineScope
    ) {
        if (device == null) return
        scope.launch(Dispatchers.IO) {
            sendConsumerReport(device, usage)
            delay(30L)
            sendConsumerReport(device, 0)
        }
    }

    /**
     * Tap a key (press -> delay -> release) with optional modifier.
     */
    fun tapKey(
        device: BluetoothDevice?,
        key: Byte,
        modifier: Byte = currentModifiers,
        scope: CoroutineScope,
        holdMs: Long = 25L
    ) {
        if (device == null) return
        scope.launch(Dispatchers.IO) {
            sendKeyboardReport(device, modifiers = modifier, keys = listOf(key))
            delay(holdMs)
            sendKeyboardReport(device, modifiers = currentModifiers, keys = emptyList())
        }
    }

    /**
     * Tap shortcut combination (e.g. Ctrl + C).
     */
    fun tapShortcut(
        device: BluetoothDevice?,
        modifier: Byte,
        key: Byte,
        scope: CoroutineScope
    ) {
        tapKey(device, key, modifier = modifier, scope = scope)
    }

    /**
     * Toggle modifier latch state (e.g. user taps Ctrl button on UI).
     */
    fun toggleModifier(modifier: Byte): Byte {
        currentModifiers = (currentModifiers.toInt() xor modifier.toInt()).toByte()
        return currentModifiers
    }

    fun isModifierActive(modifier: Byte): Boolean {
        return (currentModifiers.toInt() and modifier.toInt()) != 0
    }

    fun clearModifiers(device: BluetoothDevice?) {
        currentModifiers = HidReportDescriptor.MODIFIER_NONE
        sendKeyboardReport(device, modifiers = 0, keys = emptyList())
    }

    fun getActiveModifiers(): Byte = currentModifiers

    /**
     * Type an entire string of characters by sequential HID keycodes.
     */
    fun typeText(
        device: BluetoothDevice?,
        text: String,
        scope: CoroutineScope,
        delayBetweenCharsMs: Long = 18L
    ) {
        if (device == null || text.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            for (char in text) {
                val mapping = charToHid(char)
                if (mapping != null) {
                    val (key, requiresShift) = mapping
                    val mod = if (requiresShift) {
                        (currentModifiers.toInt() or HidReportDescriptor.MODIFIER_LEFT_SHIFT.toInt()).toByte()
                    } else {
                        currentModifiers
                    }
                    sendKeyboardReport(device, modifiers = mod, keys = listOf(key))
                    delay(delayBetweenCharsMs)
                    sendKeyboardReport(device, modifiers = currentModifiers, keys = emptyList())
                    delay(delayBetweenCharsMs)
                }
            }
        }
    }

    data class HidKeyMapping(val key: Byte, val shift: Boolean)

    /**
     * Map ASCII character to standard US keyboard HID keycode + shift flag.
     */
    fun charToHid(c: Char): HidKeyMapping? {
        return when (c) {
            in 'a'..'z' -> HidKeyMapping(
                (HidReportDescriptor.KEY_A + (c - 'a')).toByte(),
                shift = false
            )
            in 'A'..'Z' -> HidKeyMapping(
                (HidReportDescriptor.KEY_A + (c - 'A')).toByte(),
                shift = true
            )
            in '1'..'9' -> HidKeyMapping(
                (HidReportDescriptor.KEY_1 + (c - '1')).toByte(),
                shift = false
            )
            '0' -> HidKeyMapping(HidReportDescriptor.KEY_0, shift = false)
            '!' -> HidKeyMapping(HidReportDescriptor.KEY_1, shift = true)
            '@' -> HidKeyMapping(HidReportDescriptor.KEY_2, shift = true)
            '#' -> HidKeyMapping(HidReportDescriptor.KEY_3, shift = true)
            '$' -> HidKeyMapping(HidReportDescriptor.KEY_4, shift = true)
            '%' -> HidKeyMapping(HidReportDescriptor.KEY_5, shift = true)
            '^' -> HidKeyMapping(HidReportDescriptor.KEY_6, shift = true)
            '&' -> HidKeyMapping(HidReportDescriptor.KEY_7, shift = true)
            '*' -> HidKeyMapping(HidReportDescriptor.KEY_8, shift = true)
            '(' -> HidKeyMapping(HidReportDescriptor.KEY_9, shift = true)
            ')' -> HidKeyMapping(HidReportDescriptor.KEY_0, shift = true)
            ' ' -> HidKeyMapping(HidReportDescriptor.KEY_SPACE, shift = false)
            '\n', '\r' -> HidKeyMapping(HidReportDescriptor.KEY_ENTER, shift = false)
            '\t' -> HidKeyMapping(HidReportDescriptor.KEY_TAB, shift = false)
            '\b' -> HidKeyMapping(HidReportDescriptor.KEY_BACKSPACE, shift = false)
            '-' -> HidKeyMapping(HidReportDescriptor.KEY_MINUS, shift = false)
            '_' -> HidKeyMapping(HidReportDescriptor.KEY_MINUS, shift = true)
            '=' -> HidKeyMapping(HidReportDescriptor.KEY_EQUAL, shift = false)
            '+' -> HidKeyMapping(HidReportDescriptor.KEY_EQUAL, shift = true)
            '[' -> HidKeyMapping(HidReportDescriptor.KEY_LEFTBRACE, shift = false)
            '{' -> HidKeyMapping(HidReportDescriptor.KEY_LEFTBRACE, shift = true)
            ']' -> HidKeyMapping(HidReportDescriptor.KEY_RIGHTBRACE, shift = false)
            '}' -> HidKeyMapping(HidReportDescriptor.KEY_RIGHTBRACE, shift = true)
            '\\' -> HidKeyMapping(HidReportDescriptor.KEY_BACKSLASH, shift = false)
            '|' -> HidKeyMapping(HidReportDescriptor.KEY_BACKSLASH, shift = true)
            ';' -> HidKeyMapping(HidReportDescriptor.KEY_SEMICOLON, shift = false)
            ':' -> HidKeyMapping(HidReportDescriptor.KEY_SEMICOLON, shift = true)
            '\'' -> HidKeyMapping(HidReportDescriptor.KEY_APOSTROPHE, shift = false)
            '"' -> HidKeyMapping(HidReportDescriptor.KEY_APOSTROPHE, shift = true)
            '`' -> HidKeyMapping(HidReportDescriptor.KEY_GRAVE, shift = false)
            '~' -> HidKeyMapping(HidReportDescriptor.KEY_GRAVE, shift = true)
            ',' -> HidKeyMapping(HidReportDescriptor.KEY_COMMA, shift = false)
            '<' -> HidKeyMapping(HidReportDescriptor.KEY_COMMA, shift = true)
            '.' -> HidKeyMapping(HidReportDescriptor.KEY_DOT, shift = false)
            '>' -> HidKeyMapping(HidReportDescriptor.KEY_DOT, shift = true)
            '/' -> HidKeyMapping(HidReportDescriptor.KEY_SLASH, shift = false)
            '?' -> HidKeyMapping(HidReportDescriptor.KEY_SLASH, shift = true)
            else -> null
        }
    }
}
