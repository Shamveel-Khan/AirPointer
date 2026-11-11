package com.airpointer.app.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.airpointer.app.bluetooth.BluetoothHidManager
import com.airpointer.app.data.InputMode
import com.airpointer.app.data.SettingsRepository
import com.airpointer.app.ui.components.HapticHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AirMouseManager(
    private val context: Context,
    private val hidManager: BluetoothHidManager,
    private val settingsRepository: SettingsRepository,
    private val hapticHelper: HapticHelper
) : SensorEventListener {

    companion object {
        private const val TAG = "AirMouseManager"
        // 0.82f smoothing gives instantaneous response while eliminating hand micro-tremor
        private const val GYRO_SMOOTHING = 0.82f
        // Throttle UI StateFlow updates to ~30 FPS to prevent Compose recomposition frame drops
        private const val UI_UPDATE_INTERVAL_NS = 33_000_000L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val linearAccel = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isGyroAvailable: Boolean = (gyroscope != null)
    val isAccelAvailable: Boolean = (linearAccel != null)
    val hasMotionSensors: Boolean = isGyroAvailable || isAccelAvailable

    // Dedicated background thread for sensor processing to prevent UI thread lag
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    // Live Tracking State
    private val _isTracking = MutableStateFlow(true)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // Real-time Motion Orientation Feedback for UI (Throttled to 30 FPS)
    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _yaw = MutableStateFlow(0f)
    val yaw: StateFlow<Float> = _yaw.asStateFlow()

    private val _deskSpeed = MutableStateFlow(0f)
    val deskSpeed: StateFlow<Float> = _deskSpeed.asStateFlow()

    private var lastUiUpdateNs = 0L

    // Gyroscope Biases
    private var biasGyroX = 0f
    private var biasGyroY = 0f
    private var biasGyroZ = 0f

    // Desk Motion Dynamic DC Blocker & Velocity Integrator
    private var dcBiasX = 0f
    private var dcBiasY = 0f
    private var velDeskX = 0f
    private var velDeskY = 0f
    private var lastDeskTimestamp = 0L
    private var deskPivotZ = 0f

    // Air Motion Smoothing Output
    private var filteredAirDx = 0f
    private var filteredAirDy = 0f

    private var isSensorRegistered = false

    fun start() {
        if (!hasMotionSensors || isSensorRegistered) return

        try {
            val thread = HandlerThread("AirPointerSensorThread").apply { start() }
            sensorThread = thread
            val handler = Handler(thread.looper)
            sensorHandler = handler

            gyroscope?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, handler)
            }
            linearAccel?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, handler)
            }

            isSensorRegistered = true
            _isTracking.value = true
            dcBiasX = 0f
            dcBiasY = 0f
            velDeskX = 0f
            velDeskY = 0f
            deskPivotZ = 0f
            lastDeskTimestamp = 0L
            Log.i(TAG, "Motion sensors registered on dedicated background thread")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting motion sensors", e)
        }
    }

    fun stop() {
        if (isSensorRegistered) {
            try {
                sensorManager?.unregisterListener(this)
                sensorThread?.quitSafely()
                sensorThread = null
                sensorHandler = null
                isSensorRegistered = false
                filteredAirDx = 0f
                filteredAirDy = 0f
                velDeskX = 0f
                velDeskY = 0f
                deskPivotZ = 0f
                lastDeskTimestamp = 0L
                _deskSpeed.value = 0f
                Log.i(TAG, "Motion sensors unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping motion sensors", e)
            }
        }
    }

    fun setTrackingActive(active: Boolean) {
        if (_isTracking.value != active) {
            _isTracking.value = active
            if (settingsRepository.settings.value.hapticFeedback) {
                hapticHelper.tick(true)
            }
            if (!active) {
                filteredAirDx = 0f
                filteredAirDy = 0f
                velDeskX = 0f
                velDeskY = 0f
                deskPivotZ = 0f
                lastDeskTimestamp = 0L
                _deskSpeed.value = 0f
            }
        }
    }

    fun toggleTracking(): Boolean {
        val newState = !_isTracking.value
        setTrackingActive(newState)
        return newState
    }

    fun calibrate() {
        biasGyroX = _pitch.value
        biasGyroZ = _yaw.value
        dcBiasX = 0f
        dcBiasY = 0f
        velDeskX = 0f
        velDeskY = 0f
        deskPivotZ = 0f
        lastDeskTimestamp = 0L
        filteredAirDx = 0f
        filteredAirDy = 0f
        _deskSpeed.value = 0f
        if (settingsRepository.settings.value.hapticFeedback) {
            hapticHelper.click(true)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event)
            Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ACCELEROMETER -> handleLinearAcceleration(event)
        }
    }

    /**
     * AIR MOUSE HANDLER: Translates 3D air rotation into cursor movement.
     */
    private fun handleGyroscope(event: SensorEvent) {
        val settings = settingsRepository.settings.value
        val rawX = event.values[0] - biasGyroX
        val rawY = event.values[1] - biasGyroY
        val rawZ = event.values[2] - biasGyroZ

        // Throttle UI orientation updates to 30 FPS to save Compose rendering overhead
        val now = event.timestamp
        if (now - lastUiUpdateNs >= UI_UPDATE_INTERVAL_NS) {
            _pitch.value = rawX
            _yaw.value = rawZ
            lastUiUpdateNs = now
        }

        // In Desk Mouse mode, Gyroscope Z assists with natural wrist arc pivoting
        if (settings.inputMode == InputMode.DESK_MOUSE) {
            deskPivotZ = if (abs(rawZ) > 0.05f) rawZ else 0f
            return
        }

        // In Air Mouse mode: Full 3D Gyro pointer
        if (settings.inputMode != InputMode.AIR_MOUSE || !_isTracking.value) return
        val target = hidManager.connectedDevice.value ?: return

        // Anti-tremor deadzone
        val deadzone = settings.gyroDeadzone
        val effectiveX = if (abs(rawX) > deadzone) (rawX - Math.signum(rawX) * deadzone) else 0f
        val effectiveY = if (abs(rawY) > deadzone) (rawY - Math.signum(rawY) * deadzone) else 0f
        val effectiveZ = if (abs(rawZ) > deadzone) (rawZ - Math.signum(rawZ) * deadzone) else 0f

        if (effectiveX == 0f && effectiveY == 0f && effectiveZ == 0f) {
            filteredAirDx = 0f
            filteredAirDy = 0f
            return
        }

        // Project angular velocities:
        // Swiveling right: -Z and +Y reinforce each other
        var gyroDx = (-effectiveZ * 1.15f + effectiveY * 0.95f) * settings.gyroSensitivity * 46f
        // Tilting nose up: -X yields negative dy (moves UP on PC screen)
        var gyroDy = -effectiveX * settings.gyroSensitivity * 46f

        if (settings.gyroInvertX) gyroDx = -gyroDx
        if (settings.gyroInvertY) gyroDy = -gyroDy

        // Instant smoothing: 82% new value removes lag while smoothing tremor
        filteredAirDx = GYRO_SMOOTHING * gyroDx + (1f - GYRO_SMOOTHING) * filteredAirDx
        filteredAirDy = GYRO_SMOOTHING * gyroDy + (1f - GYRO_SMOOTHING) * filteredAirDy

        hidManager.mouseSender.moveCursorFloat(device = target, dx = filteredAirDx, dy = filteredAirDy)
    }

    /**
     * DESK MOUSE HANDLER: High-speed zero-lag kinematic tracker
     * for sliding phone flat across a table, desk, or mousepad.
     */
    private fun handleLinearAcceleration(event: SensorEvent) {
        val settings = settingsRepository.settings.value
        if (settings.inputMode != InputMode.DESK_MOUSE || !_isTracking.value) return
        val target = hidManager.connectedDevice.value ?: return

        val rawAx = event.values[0]
        val rawAy = event.values[1]

        // Dynamic DC bias rejection: adapts continuously to any desk tilt or surface angle
        dcBiasX = dcBiasX * 0.95f + rawAx * 0.05f
        dcBiasY = dcBiasY * 0.95f + rawAy * 0.05f

        val dynAx = rawAx - dcBiasX
        val dynAy = rawAy - dcBiasY

        // Threshold to eliminate resting surface micro-vibrations
        val deadzone = 0.07f
        val effAx = if (abs(dynAx) > deadzone) (dynAx - Math.signum(dynAx) * deadzone) else 0f
        val effAy = if (abs(dynAy) > deadzone) (dynAy - Math.signum(dynAy) * deadzone) else 0f

        val mag = hypot(effAx, effAy)

        // Throttle UI speed gauge updates to 30 FPS
        val now = event.timestamp
        if (now - lastUiUpdateNs >= UI_UPDATE_INTERVAL_NS) {
            _deskSpeed.value = mag
            lastUiUpdateNs = now
        }

        val dt = if (lastDeskTimestamp != 0L) {
            min(0.035f, max(0.005f, (event.timestamp - lastDeskTimestamp) / 1_000_000_000f))
        } else {
            0.016f
        }
        lastDeskTimestamp = event.timestamp

        // High-friction desk physics model with instant braking:
        if (effAx == 0f && effAy == 0f) {
            velDeskX *= 0.5f
            velDeskY *= 0.5f
            if (abs(velDeskX) < 0.03f) velDeskX = 0f
            if (abs(velDeskY) < 0.03f) velDeskY = 0f
        } else {
            val prevVx = velDeskX
            val prevVy = velDeskY

            velDeskX += effAx * dt * 15f
            velDeskY += effAy * dt * 15f

            // Bounce prevention (braking clamp):
            // When braking (acceleration opposes prior velocity), if velocity crosses zero, clamp it
            if (prevVx > 0f && velDeskX < 0f && effAx < 0f) velDeskX = 0f
            if (prevVx < 0f && velDeskX > 0f && effAx > 0f) velDeskX = 0f
            if (prevVy > 0f && velDeskY < 0f && effAy < 0f) velDeskY = 0f
            if (prevVy < 0f && velDeskY > 0f && effAy > 0f) velDeskY = 0f

            // Natural desk surface friction decay
            velDeskX *= 0.86f
            velDeskY *= 0.86f
        }

        val sensitivityMultiplier = settings.deskSensitivity * 32f

        // Ballistics curve: slow movements are 1:1, fast flick traverses screen
        val speedFactor = when {
            mag < 0.25f -> 0.85f
            mag > 1.6f -> min(2.5f, 1.3f + (mag - 1.6f) * 0.45f)
            else -> 1.0f + (mag - 0.25f) * 0.22f
        }

        // Add wrist arc pivot assist from Gyroscope Z
        val pivotDx = if (abs(deskPivotZ) > 0.05f) {
            (-deskPivotZ * settings.deskSensitivity * 26f)
        } else {
            0f
        }

        var targetDx = (velDeskX * sensitivityMultiplier * speedFactor) + pivotDx
        var targetDy = (-velDeskY * sensitivityMultiplier * speedFactor)

        if (settings.gyroInvertX) targetDx = -targetDx
        if (settings.gyroInvertY) targetDy = -targetDy

        // Direct feed into rate-limited accumulator for instant, zero-lag movement
        hidManager.mouseSender.moveCursorFloat(device = target, dx = targetDx, dy = targetDy)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
