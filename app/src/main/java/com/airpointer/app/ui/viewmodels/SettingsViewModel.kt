package com.airpointer.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.airpointer.app.bluetooth.BluetoothHidManager
import com.airpointer.app.data.SettingsRepository
import com.airpointer.app.data.UserSettings
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    val hidManager: BluetoothHidManager
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings

    fun setCursorSensitivity(value: Float) = settingsRepository.updateCursorSensitivity(value)
    fun setPointerAcceleration(enabled: Boolean) = settingsRepository.updatePointerAcceleration(enabled)
    fun setScrollSensitivity(value: Float) = settingsRepository.updateScrollSensitivity(value)
    fun setNaturalScrolling(enabled: Boolean) = settingsRepository.updateNaturalScrolling(enabled)
    fun setHapticFeedback(enabled: Boolean) = settingsRepository.updateHapticFeedback(enabled)
    fun setKeepScreenOn(enabled: Boolean) = settingsRepository.updateKeepScreenOn(enabled)
    fun setSwapLeftRight(enabled: Boolean) = settingsRepository.updateSwapLeftRight(enabled)
    fun setShowClickButtons(enabled: Boolean) = settingsRepository.updateShowClickButtons(enabled)
}
