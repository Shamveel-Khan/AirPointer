package com.airpointer.app.data

enum class InputMode {
    TOUCHPAD,
    DESK_MOUSE,
    AIR_MOUSE
}

data class UserSettings(
    val cursorSensitivity: Float = 1.2f,
    val pointerAcceleration: Boolean = true,
    val scrollSensitivity: Float = 1.0f,
    val naturalScrolling: Boolean = false,
    val hapticFeedback: Boolean = true,
    val keepScreenOn: Boolean = true,
    val swapLeftRight: Boolean = false,
    val showClickButtons: Boolean = true,
    // Motion Mode
    val inputMode: InputMode = InputMode.DESK_MOUSE,
    // Desk Mouse settings
    val deskSensitivity: Float = 1.4f,
    // Air Mouse settings
    val gyroSensitivity: Float = 1.2f,
    val gyroDeadzone: Float = 0.018f,
    val gyroInvertX: Boolean = false,
    val gyroInvertY: Boolean = false,
    val gyroContinuous: Boolean = true,
    val volumeButtonsAsMouse: Boolean = true
)
