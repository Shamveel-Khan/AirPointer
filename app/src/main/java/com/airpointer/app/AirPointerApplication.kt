package com.airpointer.app

import android.app.Application
import com.airpointer.app.bluetooth.BluetoothHidManager
import com.airpointer.app.data.SettingsRepository
import com.airpointer.app.motion.AirMouseManager
import com.airpointer.app.ui.components.HapticHelper

class AirPointerApplication : Application() {

    lateinit var hidManager: BluetoothHidManager
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var hapticHelper: HapticHelper
        private set

    lateinit var airMouseManager: AirMouseManager
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        hapticHelper = HapticHelper(this)
        hidManager = BluetoothHidManager(this)
        airMouseManager = AirMouseManager(this, hidManager, settingsRepository, hapticHelper)
        hidManager.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        airMouseManager.stop()
        hidManager.cleanup()
    }
}
