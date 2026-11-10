package com.airpointer.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.airpointer.app.accessibility.AirPointerAccessibilityService
import com.airpointer.app.bluetooth.BluetoothHidManager
import com.airpointer.app.data.InputMode
import com.airpointer.app.data.SettingsRepository
import com.airpointer.app.data.UserSettings
import com.airpointer.app.motion.AirMouseManager
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    val hidManager: BluetoothHidManager,
    val airMouseManager: AirMouseManager
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
    val isAccessibilityActive: StateFlow<Boolean> = AirPointerAccessibilityService.isServiceActive

    fun setCursorSensitivity(value: Float) = settingsRepository.updateCursorSensitivity(value)
    fun setPointerAcceleration(enabled: Boolean) = settingsRepository.updatePointerAcceleration(enabled)
    fun setScrollSensitivity(value: Float) = settingsRepository.updateScrollSensitivity(value)
    fun setNaturalScrolling(enabled: Boolean) = settingsRepository.updateNaturalScrolling(enabled)
    fun setHapticFeedback(enabled: Boolean) = settingsRepository.updateHapticFeedback(enabled)
    fun setKeepScreenOn(enabled: Boolean) = settingsRepository.updateKeepScreenOn(enabled)
    fun setSwapLeftRight(enabled: Boolean) = settingsRepository.updateSwapLeftRight(enabled)
    fun setShowClickButtons(enabled: Boolean) = settingsRepository.updateShowClickButtons(enabled)

    // Desk Mouse Settings
    fun setDeskSensitivity(value: Float) = settingsRepository.updateDeskSensitivity(value)

    // Air Mouse Settings
    fun setInputMode(mode: InputMode) = settingsRepository.updateInputMode(mode)
    fun setGyroSensitivity(value: Float) = settingsRepository.updateGyroSensitivity(value)
    fun setGyroDeadzone(value: Float) = settingsRepository.updateGyroDeadzone(value)
    fun setGyroInvertX(enabled: Boolean) = settingsRepository.updateGyroInvertX(enabled)
    fun setGyroInvertY(enabled: Boolean) = settingsRepository.updateGyroInvertY(enabled)
    fun setGyroContinuous(enabled: Boolean) = settingsRepository.updateGyroContinuous(enabled)
    fun setVolumeButtonsAsMouse(enabled: Boolean) {
        settingsRepository.updateVolumeButtonsAsMouse(enabled)
        AirPointerAccessibilityService.interceptVolumeKeys = enabled
    }
}
