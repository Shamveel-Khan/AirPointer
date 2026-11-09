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
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            cursorSensitivity = prefs.getFloat(KEY_CURSOR_SENSITIVITY, 1.2f),
            pointerAcceleration = prefs.getBoolean(KEY_POINTER_ACCEL, true),
            scrollSensitivity = prefs.getFloat(KEY_SCROLL_SENSITIVITY, 1.0f),
            naturalScrolling = prefs.getBoolean(KEY_NATURAL_SCROLL, false),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            swapLeftRight = prefs.getBoolean(KEY_SWAP_BUTTONS, false),
            showClickButtons = prefs.getBoolean(KEY_SHOW_BUTTONS, true)
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
}
