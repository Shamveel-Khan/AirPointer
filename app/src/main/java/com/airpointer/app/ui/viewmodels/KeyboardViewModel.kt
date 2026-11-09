package com.airpointer.app.ui.viewmodels

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airpointer.app.bluetooth.BluetoothHidManager
import com.airpointer.app.bluetooth.HidConnectionState
import com.airpointer.app.bluetooth.HidReportDescriptor
import com.airpointer.app.data.SettingsRepository
import com.airpointer.app.ui.components.HapticHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KeyboardViewModel(
    private val hidManager: BluetoothHidManager,
    private val settingsRepository: SettingsRepository,
    private val hapticHelper: HapticHelper
) : ViewModel() {

    val connectionState: StateFlow<HidConnectionState> = hidManager.connectionState
    val connectedDevice: StateFlow<BluetoothDevice?> = hidManager.connectedDevice
    val capsLockActive: StateFlow<Boolean> = hidManager.capsLockActive

    // Modifiers latch state
    private val _ctrlActive = MutableStateFlow(false)
    val ctrlActive: StateFlow<Boolean> = _ctrlActive.asStateFlow()

    private val _altActive = MutableStateFlow(false)
    val altActive: StateFlow<Boolean> = _altActive.asStateFlow()

    private val _shiftActive = MutableStateFlow(false)
    val shiftActive: StateFlow<Boolean> = _shiftActive.asStateFlow()

    private val _guiActive = MutableStateFlow(false)
    val guiActive: StateFlow<Boolean> = _guiActive.asStateFlow()

    private fun getModifierMask(): Byte {
        var mask = HidReportDescriptor.MODIFIER_NONE.toInt()
        if (_ctrlActive.value) mask = mask or HidReportDescriptor.MODIFIER_LEFT_CTRL.toInt()
        if (_shiftActive.value) mask = mask or HidReportDescriptor.MODIFIER_LEFT_SHIFT.toInt()
        if (_altActive.value) mask = mask or HidReportDescriptor.MODIFIER_LEFT_ALT.toInt()
        if (_guiActive.value) mask = mask or HidReportDescriptor.MODIFIER_LEFT_GUI.toInt()
        return mask.toByte()
    }

    private fun triggerHaptic() {
        if (settingsRepository.settings.value.hapticFeedback) {
            hapticHelper.click(true)
        }
    }

    fun toggleCtrl() {
        triggerHaptic()
        _ctrlActive.value = !_ctrlActive.value
        updateModifiersOnHost()
    }

    fun toggleAlt() {
        triggerHaptic()
        _altActive.value = !_altActive.value
        updateModifiersOnHost()
    }

    fun toggleShift() {
        triggerHaptic()
        _shiftActive.value = !_shiftActive.value
        updateModifiersOnHost()
    }

    fun toggleGui() {
        triggerHaptic()
        _guiActive.value = !_guiActive.value
        updateModifiersOnHost()
    }

    fun clearAllModifiers() {
        triggerHaptic()
        _ctrlActive.value = false
        _altActive.value = false
        _shiftActive.value = false
        _guiActive.value = false
        updateModifiersOnHost()
    }

    private fun updateModifiersOnHost() {
        val target = connectedDevice.value ?: return
        hidManager.keyboardSender.sendKeyboardReport(
            device = target,
            modifiers = getModifierMask(),
            keys = emptyList()
        )
    }

    fun sendKey(key: Byte) {
        val target = connectedDevice.value ?: return
        triggerHaptic()
        val mod = getModifierMask()
        hidManager.keyboardSender.tapKey(target, key, mod, viewModelScope)
    }

    fun sendShortcut(modifier: Byte, key: Byte) {
        val target = connectedDevice.value ?: return
        triggerHaptic()
        hidManager.keyboardSender.tapShortcut(target, modifier, key, viewModelScope)
    }

    fun typeText(text: String) {
        val target = connectedDevice.value ?: return
        if (text.isEmpty()) return
        triggerHaptic()
        hidManager.keyboardSender.typeText(target, text, viewModelScope)
    }

    fun sendConsumerKey(usage: Short) {
        val target = connectedDevice.value ?: return
        triggerHaptic()
        hidManager.keyboardSender.tapConsumerKey(target, usage, viewModelScope)
    }
}
