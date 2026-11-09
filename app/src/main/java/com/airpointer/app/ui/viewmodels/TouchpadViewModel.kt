package com.airpointer.app.ui.viewmodels

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airpointer.app.bluetooth.BluetoothHidManager
import com.airpointer.app.bluetooth.HidConnectionState
import com.airpointer.app.bluetooth.HidReportDescriptor
import com.airpointer.app.data.SettingsRepository
import com.airpointer.app.data.UserSettings
import com.airpointer.app.gesture.HapticType
import com.airpointer.app.gesture.TouchpadGestureHandler
import com.airpointer.app.gesture.TouchpadListener
import com.airpointer.app.ui.components.HapticHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TouchpadViewModel(
    private val hidManager: BluetoothHidManager,
    private val settingsRepository: SettingsRepository,
    private val hapticHelper: HapticHelper
) : ViewModel(), TouchpadListener {

    val settings: StateFlow<UserSettings> = settingsRepository.settings

    val connectionState: StateFlow<HidConnectionState> = hidManager.connectionState
    val connectedDevice: StateFlow<BluetoothDevice?> = hidManager.connectedDevice

    private val _isDragLocked = MutableStateFlow(false)
    val isDragLocked: StateFlow<Boolean> = _isDragLocked.asStateFlow()

    val gestureHandler = TouchpadGestureHandler(this, settings.value)

    init {
        viewModelScope.launch {
            settings.collect { newSettings ->
                gestureHandler.settings = newSettings
            }
        }
    }

    override fun onMove(dx: Float, dy: Float, isDragging: Boolean) {
        val target = connectedDevice.value ?: return
        val currentSettings = settings.value

        val leftBtn = if (currentSettings.swapLeftRight) {
            HidReportDescriptor.MOUSE_BUTTON_RIGHT
        } else {
            HidReportDescriptor.MOUSE_BUTTON_LEFT
        }

        val buttonMask = if (isDragging || _isDragLocked.value) leftBtn else HidReportDescriptor.MOUSE_BUTTON_NONE
        hidManager.mouseSender.moveCursor(
            device = target,
            dx = dx.roundToInt(),
            dy = dy.roundToInt(),
            buttons = buttonMask
        )
    }

    override fun onLeftClick() {
        val target = connectedDevice.value ?: return
        val currentSettings = settings.value
        val button = if (currentSettings.swapLeftRight) {
            HidReportDescriptor.MOUSE_BUTTON_RIGHT
        } else {
            HidReportDescriptor.MOUSE_BUTTON_LEFT
        }
        hidManager.mouseSender.sendClick(target, button, viewModelScope)
    }

    override fun onRightClick() {
        val target = connectedDevice.value ?: return
        val currentSettings = settings.value
        val button = if (currentSettings.swapLeftRight) {
            HidReportDescriptor.MOUSE_BUTTON_LEFT
        } else {
            HidReportDescriptor.MOUSE_BUTTON_RIGHT
        }
        hidManager.mouseSender.sendClick(target, button, viewModelScope)
    }

    override fun onDoubleClick() {
        val target = connectedDevice.value ?: return
        val currentSettings = settings.value
        val button = if (currentSettings.swapLeftRight) {
            HidReportDescriptor.MOUSE_BUTTON_RIGHT
        } else {
            HidReportDescriptor.MOUSE_BUTTON_LEFT
        }
        hidManager.mouseSender.sendDoubleClick(target, button, viewModelScope)
    }

    fun onMiddleClick() {
        val target = connectedDevice.value ?: return
        hapticHelper.click(settings.value.hapticFeedback)
        hidManager.mouseSender.sendClick(target, HidReportDescriptor.MOUSE_BUTTON_MIDDLE, viewModelScope)
    }

    override fun onScroll(vertical: Float, horizontal: Float) {
        val target = connectedDevice.value ?: return
        hidManager.mouseSender.scroll(
            device = target,
            verticalSteps = vertical.roundToInt(),
            horizontalSteps = horizontal.roundToInt()
        )
    }

    override fun onDragStateChanged(isDragging: Boolean) {
        _isDragLocked.value = isDragging
        val target = connectedDevice.value ?: return
        val leftBtn = if (settings.value.swapLeftRight) {
            HidReportDescriptor.MOUSE_BUTTON_RIGHT
        } else {
            HidReportDescriptor.MOUSE_BUTTON_LEFT
        }
        hidManager.mouseSender.setButton(target, leftBtn, isDragging)
    }

    fun toggleDragLock() {
        val newState = !_isDragLocked.value
        _isDragLocked.value = newState
        gestureHandler.setDragLocked(newState)
        onDragStateChanged(newState)
    }

    override fun onHapticFeedback(type: HapticType) {
        if (!settings.value.hapticFeedback) return
        when (type) {
            HapticType.CLICK -> hapticHelper.click(true)
            HapticType.DOUBLE_CLICK -> hapticHelper.doubleClick(true)
            HapticType.HEAVY_CLICK -> hapticHelper.heavyClick(true)
            HapticType.TICK -> hapticHelper.tick(true)
        }
    }

    // Explicit button press methods (for on-screen click buttons)
    fun pressButton(button: Byte) {
        val target = connectedDevice.value ?: return
        val effectiveButton = mapButton(button)
        hapticHelper.click(settings.value.hapticFeedback)
        hidManager.mouseSender.setButton(target, effectiveButton, true)
    }

    fun releaseButton(button: Byte) {
        val target = connectedDevice.value ?: return
        val effectiveButton = mapButton(button)
        hidManager.mouseSender.setButton(target, effectiveButton, false)
    }

    private fun mapButton(button: Byte): Byte {
        if (!settings.value.swapLeftRight) return button
        return when (button) {
            HidReportDescriptor.MOUSE_BUTTON_LEFT -> HidReportDescriptor.MOUSE_BUTTON_RIGHT
            HidReportDescriptor.MOUSE_BUTTON_RIGHT -> HidReportDescriptor.MOUSE_BUTTON_LEFT
            else -> button
        }
    }
}
