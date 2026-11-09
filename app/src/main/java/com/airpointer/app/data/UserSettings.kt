package com.airpointer.app.data

data class UserSettings(
    val cursorSensitivity: Float = 1.2f,
    val pointerAcceleration: Boolean = true,
    val scrollSensitivity: Float = 1.0f,
    val naturalScrolling: Boolean = false,
    val hapticFeedback: Boolean = true,
    val keepScreenOn: Boolean = true,
    val swapLeftRight: Boolean = false,
    val showClickButtons: Boolean = true
)
