package com.airpointer.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "airpointer_preferences"
        private const val KEY_CURSOR_SENSITIVITY = "cursor_sensitivity"
        private const val KEY_POINTER_ACCEL = "pointer_accel"
        private const val KEY_SCROLL_SENSITIVITY = "scroll_sensitivity"
        private const val KEY_NATURAL_SCROLL = "natural_scroll"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SWAP_BUTTONS = "swap_buttons"
        private const val KEY_SHOW_BUTTONS = "show_buttons"
        // Motion keys
        private const val KEY_INPUT_MODE = "input_mode"
        private const val KEY_DESK_SENSITIVITY = "desk_sensitivity"
        private const val KEY_GYRO_SENSITIVITY = "gyro_sensitivity"
        private const val KEY_GYRO_DEADZONE = "gyro_deadzone"
        private const val KEY_GYRO_INVERT_X = "gyro_invert_x"
        private const val KEY_GYRO_INVERT_Y = "gyro_invert_y"
        private const val KEY_GYRO_CONTINUOUS = "gyro_continuous"
        private const val KEY_VOL_BUTTONS = "vol_buttons_mouse"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val inputModeOrdinal = prefs.getInt(KEY_INPUT_MODE, InputMode.DESK_MOUSE.ordinal)
        val mode = when (inputModeOrdinal) {
            InputMode.TOUCHPAD.ordinal -> InputMode.TOUCHPAD
            InputMode.AIR_MOUSE.ordinal -> InputMode.AIR_MOUSE
            else -> InputMode.DESK_MOUSE
        }

        return UserSettings(
            cursorSensitivity = prefs.getFloat(KEY_CURSOR_SENSITIVITY, 1.2f),
            pointerAcceleration = prefs.getBoolean(KEY_POINTER_ACCEL, true),
            scrollSensitivity = prefs.getFloat(KEY_SCROLL_SENSITIVITY, 1.0f),
            naturalScrolling = prefs.getBoolean(KEY_NATURAL_SCROLL, false),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            swapLeftRight = prefs.getBoolean(KEY_SWAP_BUTTONS, false),
            showClickButtons = prefs.getBoolean(KEY_SHOW_BUTTONS, true),
            inputMode = mode,
            deskSensitivity = prefs.getFloat(KEY_DESK_SENSITIVITY, 1.4f),
            gyroSensitivity = prefs.getFloat(KEY_GYRO_SENSITIVITY, 1.2f),
            gyroDeadzone = prefs.getFloat(KEY_GYRO_DEADZONE, 0.018f),
            gyroInvertX = prefs.getBoolean(KEY_GYRO_INVERT_X, false),
            gyroInvertY = prefs.getBoolean(KEY_GYRO_INVERT_Y, false),
            gyroContinuous = prefs.getBoolean(KEY_GYRO_CONTINUOUS, true),
            volumeButtonsAsMouse = prefs.getBoolean(KEY_VOL_BUTTONS, true)
        )
    }

    fun updateCursorSensitivity(value: Float) {
        val clamped = value.coerceIn(0.5f, 3.0f)
        prefs.edit().putFloat(KEY_CURSOR_SENSITIVITY, clamped).apply()
        _settings.value = _settings.value.copy(cursorSensitivity = clamped)
    }

    fun updatePointerAcceleration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_POINTER_ACCEL, enabled).apply()
        _settings.value = _settings.value.copy(pointerAcceleration = enabled)
    }

    fun updateScrollSensitivity(value: Float) {
        val clamped = value.coerceIn(0.5f, 3.0f)
        prefs.edit().putFloat(KEY_SCROLL_SENSITIVITY, clamped).apply()
        _settings.value = _settings.value.copy(scrollSensitivity = clamped)
    }

    fun updateNaturalScrolling(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NATURAL_SCROLL, enabled).apply()
        _settings.value = _settings.value.copy(naturalScrolling = enabled)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _settings.value = _settings.value.copy(hapticFeedback = enabled)
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _settings.value = _settings.value.copy(keepScreenOn = enabled)
    }

    fun updateSwapLeftRight(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWAP_BUTTONS, enabled).apply()
        _settings.value = _settings.value.copy(swapLeftRight = enabled)
    }

    fun updateShowClickButtons(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BUTTONS, enabled).apply()
        _settings.value = _settings.value.copy(showClickButtons = enabled)
    }

    fun updateInputMode(mode: InputMode) {
        prefs.edit().putInt(KEY_INPUT_MODE, mode.ordinal).apply()
        _settings.value = _settings.value.copy(inputMode = mode)
    }

    fun updateDeskSensitivity(value: Float) {
        val clamped = value.coerceIn(0.5f, 3.0f)
        prefs.edit().putFloat(KEY_DESK_SENSITIVITY, clamped).apply()
        _settings.value = _settings.value.copy(deskSensitivity = clamped)
    }

    fun updateGyroSensitivity(value: Float) {
        val clamped = value.coerceIn(0.5f, 3.0f)
        prefs.edit().putFloat(KEY_GYRO_SENSITIVITY, clamped).apply()
        _settings.value = _settings.value.copy(gyroSensitivity = clamped)
    }

    fun updateGyroDeadzone(value: Float) {
        val clamped = value.coerceIn(0.005f, 0.08f)
        prefs.edit().putFloat(KEY_GYRO_DEADZONE, clamped).apply()
        _settings.value = _settings.value.copy(gyroDeadzone = clamped)
    }

    fun updateGyroInvertX(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GYRO_INVERT_X, enabled).apply()
        _settings.value = _settings.value.copy(gyroInvertX = enabled)
    }

    fun updateGyroInvertY(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GYRO_INVERT_Y, enabled).apply()
        _settings.value = _settings.value.copy(gyroInvertY = enabled)
    }

    fun updateGyroContinuous(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GYRO_CONTINUOUS, enabled).apply()
        _settings.value = _settings.value.copy(gyroContinuous = enabled)
    }

    fun updateVolumeButtonsAsMouse(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOL_BUTTONS, enabled).apply()
        _settings.value = _settings.value.copy(volumeButtonsAsMouse = enabled)
    }
}
