package com.airpointer.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.airpointer.app.AirPointerApplication
import com.airpointer.app.bluetooth.HidReportDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AirPointerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AirPointerA11y"

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        var interceptVolumeKeys: Boolean = true
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AirPointerAccessibilityService connected")
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used - service does not inspect screen content
    }

    override fun onInterrupt() {
        Log.w(TAG, "AirPointerAccessibilityService interrupted")
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "AirPointerAccessibilityService destroyed")
        _isServiceActive.value = false
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || !interceptVolumeKeys) {
            return super.onKeyEvent(event)
        }

        val app = applicationContext as? AirPointerApplication ?: return super.onKeyEvent(event)
        val target = app.hidManager.connectedDevice.value ?: return super.onKeyEvent(event)
        val settings = app.settingsRepository.settings.value

        if (!settings.volumeButtonsAsMouse) {
            return super.onKeyEvent(event)
        }

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Volume Down -> Left Click
                val leftBtn = if (settings.swapLeftRight) {
                    HidReportDescriptor.MOUSE_BUTTON_RIGHT
                } else {
                    HidReportDescriptor.MOUSE_BUTTON_LEFT
                }

                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    app.hapticHelper.click(settings.hapticFeedback)
                    serviceScope.launch {
                        app.hidManager.mouseSender.setButton(target, leftBtn, true)
                    }
                } else if (event.action == KeyEvent.ACTION_UP) {
                    serviceScope.launch {
                        app.hidManager.mouseSender.setButton(target, leftBtn, false)
                    }
                }
                return true // Consume event
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                // Volume Up -> Right Click
                val rightBtn = if (settings.swapLeftRight) {
                    HidReportDescriptor.MOUSE_BUTTON_LEFT
                } else {
                    HidReportDescriptor.MOUSE_BUTTON_RIGHT
                }

                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    app.hapticHelper.click(settings.hapticFeedback)
                    serviceScope.launch {
                        app.hidManager.mouseSender.setButton(target, rightBtn, true)
                    }
                } else if (event.action == KeyEvent.ACTION_UP) {
                    serviceScope.launch {
                        app.hidManager.mouseSender.setButton(target, rightBtn, false)
                    }
                }
                return true // Consume event
            }
        }

        return super.onKeyEvent(event)
    }

    @Deprecated("Deprecated in Android API")
    @Suppress("DEPRECATION")
    override fun onGesture(gestureId: Int): Boolean {
        val app = applicationContext as? AirPointerApplication ?: return super.onGesture(gestureId)
        val target = app.hidManager.connectedDevice.value ?: return super.onGesture(gestureId)
        val settings = app.settingsRepository.settings.value

        when (gestureId) {
            GESTURE_SWIPE_UP -> {
                serviceScope.launch {
                    app.hidManager.mouseSender.scroll(target, verticalSteps = 5)
                }
                return true
            }
            GESTURE_SWIPE_DOWN -> {
                serviceScope.launch {
                    app.hidManager.mouseSender.scroll(target, verticalSteps = -5)
                }
                return true
            }
            GESTURE_SWIPE_LEFT -> {
                serviceScope.launch {
                    app.hidManager.mouseSender.scroll(target, verticalSteps = 0, horizontalSteps = -5)
                }
                return true
            }
            GESTURE_SWIPE_RIGHT -> {
                serviceScope.launch {
                    app.hidManager.mouseSender.scroll(target, verticalSteps = 0, horizontalSteps = 5)
                }
                return true
            }
        }

        return super.onGesture(gestureId)
    }
}
