package com.airpointer.app.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

enum class HidConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

class BluetoothHidManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothHidManager"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private val executor = Executors.newSingleThreadExecutor()

    val mouseSender = MouseReportSender { hidDevice }
    val keyboardSender = KeyboardReportSender { hidDevice }

    // Reactive State
    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isHidSupported = MutableStateFlow(true)
    val isHidSupported: StateFlow<Boolean> = _isHidSupported.asStateFlow()

    private val _isAppRegistered = MutableStateFlow(false)
    val isAppRegistered: StateFlow<Boolean> = _isAppRegistered.asStateFlow()

    private val _connectionState = MutableStateFlow(HidConnectionState.DISCONNECTED)
    val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _capsLockActive = MutableStateFlow(false)
    val capsLockActive: StateFlow<Boolean> = _capsLockActive.asStateFlow()

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.i(TAG, "Bluetooth HID_DEVICE service connected")
                hidDevice = proxy as? BluetoothHidDevice
                _isHidSupported.value = (hidDevice != null)
                registerHidApp()
                refreshPairedDevices()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.w(TAG, "Bluetooth HID_DEVICE service disconnected")
                hidDevice = null
                _isAppRegistered.value = false
                _connectionState.value = HidConnectionState.DISCONNECTED
                _connectedDevice.value = null
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.i(TAG, "onAppStatusChanged: registered=$registered, device=${pluggedDevice?.name}")
            _isAppRegistered.value = registered
            if (registered && pluggedDevice != null) {
                _connectedDevice.value = pluggedDevice
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val deviceName = try {
                device?.name ?: device?.address ?: "Unknown"
            } catch (e: SecurityException) {
                device?.address ?: "Unknown"
            }
            Log.i(TAG, "onConnectionStateChanged: device=$deviceName, state=$state")

            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = HidConnectionState.CONNECTED
                    _connectedDevice.value = device
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = HidConnectionState.CONNECTING
                    _connectedDevice.value = device
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _connectionState.value = HidConnectionState.DISCONNECTING
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = HidConnectionState.DISCONNECTED
                    if (_connectedDevice.value?.address == device?.address) {
                        _connectedDevice.value = null
                    }
                    mouseSender.releaseAllButtons(device)
                    keyboardSender.clearModifiers(device)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport: type=$type, id=$id, bufferSize=$bufferSize")
            val target = device ?: return
            val currentHid = hidDevice ?: return

            when (id) {
                HidReportDescriptor.REPORT_ID_MOUSE -> {
                    val mouseData = byteArrayOf(mouseSender.getCurrentButtons(), 0, 0, 0, 0)
                    try {
                        currentHid.replyReport(target, type, id, mouseData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error replying to mouse report", e)
                    }
                }
                HidReportDescriptor.REPORT_ID_KEYBOARD -> {
                    val kbData = ByteArray(8)
                    kbData[0] = keyboardSender.getActiveModifiers()
                    try {
                        currentHid.replyReport(target, type, id, kbData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error replying to keyboard report", e)
                    }
                }
                HidReportDescriptor.REPORT_ID_CONSUMER -> {
                    try {
                        currentHid.replyReport(target, type, id, byteArrayOf(0, 0))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error replying to consumer report", e)
                    }
                }
                else -> {
                    try {
                        currentHid.reportError(target, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending reportError", e)
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            Log.d(TAG, "onSetReport: type=$type, id=$id, data=${data?.joinToString()}")
            val target = device ?: return
            val currentHid = hidDevice ?: return

            // Keyboard LEDs report (Bit 1 is Caps Lock: 0x02, Bit 0 is Num Lock: 0x01)
            if (id == HidReportDescriptor.REPORT_ID_KEYBOARD && data != null && data.isNotEmpty()) {
                val led = data[0].toInt()
                _capsLockActive.value = (led and 0x02) != 0
            }
            try {
                currentHid.reportError(target, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "Error acknowledging onSetReport", e)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            val target = device ?: return
            try {
                hidDevice?.reportError(target, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "Error acknowledging onSetProtocol", e)
            }
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            Log.i(TAG, "onVirtualCableUnplug: device=${device?.address}")
            _connectionState.value = HidConnectionState.DISCONNECTED
            _connectedDevice.value = null
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val isEnabled = (state == BluetoothAdapter.STATE_ON)
                    _isBluetoothEnabled.value = isEnabled
                    if (isEnabled) {
                        initHidProfile()
                        refreshPairedDevices()
                    } else {
                        _isAppRegistered.value = false
                        _connectionState.value = HidConnectionState.DISCONNECTED
                        _connectedDevice.value = null
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { addDiscoveredDevice(it) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isDiscovering.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPairedDevices()
                }
            }
        }
    }

    private var isReceiverRegistered = false

    fun initialize() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(bluetoothReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.e(TAG, "Error registering Bluetooth broadcast receiver", e)
            }
        }

        if (hasBluetoothPermissions()) {
            try {
                _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
                if (bluetoothAdapter?.isEnabled == true) {
                    initHidProfile()
                    refreshPairedDevices()
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException checking bluetooth state", e)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during initialize", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun initHidProfile() {
        if (!hasBluetoothPermissions()) return
        val adapter = bluetoothAdapter ?: return
        try {
            val supported = adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
            _isHidSupported.value = supported
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission for HID_DEVICE profile proxy", e)
            _isHidSupported.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error getting HID_DEVICE profile proxy", e)
            _isHidSupported.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun registerHidApp() {
        if (!hasBluetoothPermissions()) return
        val currentHid = hidDevice ?: return
        if (_isAppRegistered.value) return

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "AirPointer",
            "AirPointer Wireless Bluetooth Mouse and Keyboard",
            "AirPointer",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidReportDescriptor.DESCRIPTOR
        )

        val inQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            800,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        val outQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            800,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        try {
            val result = currentHid.registerApp(sdpSettings, inQos, outQos, executor, hidCallback)
            Log.i(TAG, "registerApp result: $result")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering HID app", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during registerApp", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun unregisterHidApp() {
        try {
            if (hasBluetoothPermissions()) {
                hidDevice?.unregisterApp()
            }
            _isAppRegistered.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering app", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermissions()) return false
        val currentHid = hidDevice ?: return false
        _connectionState.value = HidConnectionState.CONNECTING
        return try {
            currentHid.connect(device)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException connecting to ${device.address}", e)
            _connectionState.value = HidConnectionState.DISCONNECTED
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to ${device.address}", e)
            _connectionState.value = HidConnectionState.DISCONNECTED
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect(device: BluetoothDevice? = _connectedDevice.value): Boolean {
        if (!hasBluetoothPermissions()) return false
        val target = device ?: return false
        val currentHid = hidDevice ?: return false
        _connectionState.value = HidConnectionState.DISCONNECTING
        return try {
            currentHid.disconnect(target)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException disconnecting from ${target.address}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from ${target.address}", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!hasBluetoothPermissions()) return
        val adapter = bluetoothAdapter ?: return
        try {
            val bonded = adapter.bondedDevices?.toList() ?: emptyList()
            _pairedDevices.value = bonded
            _isBluetoothEnabled.value = adapter.isEnabled
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException refreshing paired devices", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing paired devices", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (!hasBluetoothPermissions()) return false
        val adapter = bluetoothAdapter ?: return false
        _discoveredDevices.value = emptyList()
        return try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting discovery", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun cancelDiscovery(): Boolean {
        if (!hasBluetoothPermissions()) return false
        val adapter = bluetoothAdapter ?: return false
        return try {
            adapter.cancelDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling discovery", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermissions()) return false
        return try {
            cancelDiscovery()
            device.createBond()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException creating bond", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error creating bond with ${device.address}", e)
            false
        }
    }

    private fun addDiscoveredDevice(device: BluetoothDevice) {
        val current = _discoveredDevices.value
        if (current.none { it.address == device.address }) {
            _discoveredDevices.value = current + device
        }
    }

    fun isComputer(device: BluetoothDevice): Boolean {
        return try {
            val major = device.bluetoothClass?.majorDeviceClass
            val devClass = device.bluetoothClass?.deviceClass
            major == BluetoothClass.Device.Major.COMPUTER ||
                    devClass == BluetoothClass.Device.COMPUTER_LAPTOP ||
                    devClass == BluetoothClass.Device.COMPUTER_DESKTOP ||
                    devClass == BluetoothClass.Device.COMPUTER_SERVER ||
                    devClass == BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA
        } catch (e: Exception) {
            false
        }
    }

    fun cleanup() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                // Already unregistered
            }
        }
        try {
            if (hasBluetoothPermissions() && bluetoothAdapter?.isEnabled == true && hidDevice != null) {
                unregisterHidApp()
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
        hidDevice = null
    }
}
