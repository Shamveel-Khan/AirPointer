package com.airpointer.app.ui.viewmodels

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airpointer.app.bluetooth.BluetoothHidManager
import com.airpointer.app.bluetooth.HidConnectionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val hidManager: BluetoothHidManager
) : ViewModel() {

    val isBluetoothEnabled: StateFlow<Boolean> = hidManager.isBluetoothEnabled
    val isHidSupported: StateFlow<Boolean> = hidManager.isHidSupported
    val isAppRegistered: StateFlow<Boolean> = hidManager.isAppRegistered
    val connectionState: StateFlow<HidConnectionState> = hidManager.connectionState
    val connectedDevice: StateFlow<BluetoothDevice?> = hidManager.connectedDevice
    val pairedDevices: StateFlow<List<BluetoothDevice>> = hidManager.pairedDevices
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = hidManager.discoveredDevices
    val isDiscovering: StateFlow<Boolean> = hidManager.isDiscovering

    fun refreshPaired() {
        hidManager.refreshPairedDevices()
    }

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            hidManager.connect(device)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            hidManager.disconnect()
        }
    }

    fun pair(device: BluetoothDevice) {
        viewModelScope.launch {
            hidManager.pairDevice(device)
        }
    }

    fun startDiscovery() {
        hidManager.startDiscovery()
    }

    fun cancelDiscovery() {
        hidManager.cancelDiscovery()
    }

    fun makeDiscoverable(activity: Activity, durationSeconds: Int = 180) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, durationSeconds)
        }
        activity.startActivity(intent)
    }

    fun isComputer(device: BluetoothDevice): Boolean {
        return hidManager.isComputer(device)
    }
}
