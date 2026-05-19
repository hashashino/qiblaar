package com.hashashino.qiblaar.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

enum class CompassAccuracy { UNRELIABLE, LOW, HIGH }
enum class MagneticInterference { NONE, MILD, SEVERE }
enum class LockSource { GPS, MANUAL }

/** Physical orientation of the phone while in use. */
enum class DevicePose {
    FLAT,    // phone laid flat on a surface (compass-on-table mode)
    UPRIGHT  // phone held vertically with camera pointing forward (AR mode)
}

data class OrientationState(
    val azimuthDegrees: Float = 0f,
    val accuracy: CompassAccuracy = CompassAccuracy.UNRELIABLE,
    val accuracyDegrees: Int = 45,
    val magneticFieldMicroTesla: Float = 0f,
    val interference: MagneticInterference = MagneticInterference.NONE,
    val isLocked: Boolean = false,
    val lockSource: LockSource? = null,
    val pitchDegrees: Float = 0f,
    val isPoseValid: Boolean = true
)

class OrientationManager(
    context: Context,
    val pose: DevicePose
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gameRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _state = MutableStateFlow(OrientationState())
    val state: StateFlow<OrientationState> = _state.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Magnetometer-free rotation matrices for gyro-lock tracking
    private val gameRotationMatrix = FloatArray(9)
    private val gameRemappedMatrix = FloatArray(9)
    private val gameOrientationAngles = FloatArray(3)

    private var declinationDegrees: Float = 0f

    private val ALPHA = 0.15f
    private var filteredAzimuth = 0f

    // Gyro-lock state
    var isLocked = false
        private set
    var lockedAzimuth = 0f      // true-north anchor captured at lock time
        private set
    var lockedGameAzimuth = 0f  // game-rotation baseline at lock time (for delta tracking)
        private set
    private var currentGameAzimuth = 0f // live game-rotation azimuth (delta source)

    // True once we have at least one TYPE_GAME_ROTATION_VECTOR reading. Locking
    // before this would freeze the gyro baseline at 0°, which is wrong.
    private var hasGameReading = false

    // Pending lock: applied on the next processGameRotation tick (sensors may not
    // have fired yet when lockWhenReady is called from onResume).
    private var pendingLockBearing: Float? = null
    // If non-null, the saved gyro baseline is restored directly; if null, we capture fresh.
    // Cross-fragment lock restore passes the saved value when poses match.
    private var pendingLockGameAzimuth: Float? = null
    private var pendingLockSource: LockSource = LockSource.GPS
    private var running = false

    fun start() {
        if (running) return
        running = true
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gameRotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magneticFieldSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /** Stop sensors. If [force] is false (default), sensors stay running while locked
     *  so the gyro frame is not reset across tab switches. */
    fun stop(force: Boolean = false) {
        if (!force && isLocked) return
        running = false
        sensorManager.unregisterListener(this)
    }

    fun updateDeclination(location: Location) {
        val geo = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        )
        declinationDegrees = geo.declination
    }

    /**
     * Capture the current compass bearing as a gyro-only anchor.
     * After this call [OrientationState.azimuthDegrees] is driven by
     * TYPE_GAME_ROTATION_VECTOR (accelerometer + gyro, no magnetometer),
     * so magnetic interference at the prayer spot has no effect.
     */
    fun lock(source: LockSource = LockSource.GPS): Boolean {
        // Refuse to lock if the gyro hasn't produced a reading yet — otherwise
        // we'd capture 0° as the baseline and tracking would be off by however
        // much the phone is currently rotated relative to the gyro's frame zero.
        if (!hasGameReading) return false
        lockedAzimuth = filteredAzimuth
        lockedGameAzimuth = currentGameAzimuth
        isLocked = true
        _state.value = _state.value.copy(isLocked = true, lockSource = source)
        return true
    }

    /**
     * Release the gyro lock and resume normal magnetometer-fused tracking.
     */
    fun unlock() {
        isLocked = false
        pendingLockBearing = null
        pendingLockGameAzimuth = null
        _state.value = _state.value.copy(isLocked = false, lockSource = null, azimuthDegrees = filteredAzimuth)
    }

    /**
     * Restore a lock from another fragment's session. Pass [savedGameAzimuth] from
     * the previous lock to preserve continuity through fragment recreation; pass
     * null when the saved baseline is from a different pose (it would be in a
     * different remap frame, so we capture fresh from the next gyro tick instead).
     */
    fun lockWhenReady(bearing: Float, savedGameAzimuth: Float?, source: LockSource) {
        pendingLockBearing = bearing
        pendingLockGameAzimuth = savedGameAzimuth
        pendingLockSource = source
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> processRotation(event)
            Sensor.TYPE_GAME_ROTATION_VECTOR -> processGameRotation(event)
            Sensor.TYPE_MAGNETIC_FIELD -> processMagneticField(event)
        }
    }

    private fun processRotation(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val matrixToUse = when (pose) {
            DevicePose.FLAT -> rotationMatrix
            DevicePose.UPRIGHT -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedMatrix
                )
                remappedMatrix
            }
        }

        SensorManager.getOrientation(matrixToUse, orientationAngles)

        val rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val trueAzimuth = (rawAzimuth + declinationDegrees + 360) % 360

        var delta = trueAzimuth - filteredAzimuth
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        filteredAzimuth = (filteredAzimuth + ALPHA * delta + 360) % 360

        // Pose check moved to processGameRotation — that matrix is magnetometer-
        // independent, so tilt detection works even in interference.
        if (!isLocked) {
            _state.value = _state.value.copy(azimuthDegrees = filteredAzimuth)
        }
        // When locked, azimuth comes from processGameRotation; we just keep
        // filteredAzimuth current so a future unlock has a sensible value.
    }

    private fun processGameRotation(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(gameRotationMatrix, event.values)

        val matrixToUse = when (pose) {
            DevicePose.FLAT -> gameRotationMatrix
            DevicePose.UPRIGHT -> {
                SensorManager.remapCoordinateSystem(
                    gameRotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    gameRemappedMatrix
                )
                gameRemappedMatrix
            }
        }

        SensorManager.getOrientation(matrixToUse, gameOrientationAngles)
        currentGameAzimuth = (Math.toDegrees(gameOrientationAngles[0].toDouble()).toFloat() + 360) % 360
        hasGameReading = true

        // Tilt check using the magnetometer-free matrix. Pitch from the un-remapped
        // gameRotationMatrix is the device's physical tilt regardless of pose.
        val poseAngles = FloatArray(3)
        SensorManager.getOrientation(gameRotationMatrix, poseAngles)
        val pitchDeg = Math.toDegrees(poseAngles[1].toDouble()).toFloat()
        val isPoseValid = when (pose) {
            DevicePose.FLAT -> Math.abs(pitchDeg) < 70f      // not pointing at ceiling/floor
            DevicePose.UPRIGHT -> pitchDeg < -20f             // phone sufficiently tilted forward
        }

        // Apply a cross-fragment lock restore on the first reading after start().
        // If the previous fragment shared a saved gyro baseline (same pose), we
        // restore it directly to preserve continuity through fragment recreation.
        pendingLockBearing?.let { bearing ->
            lockedAzimuth = bearing
            lockedGameAzimuth = pendingLockGameAzimuth ?: currentGameAzimuth
            isLocked = true
            _state.value = _state.value.copy(isLocked = true, lockSource = pendingLockSource)
            pendingLockBearing = null
            pendingLockGameAzimuth = null
        }

        if (isLocked) {
            // Signed delta from the game-rotation baseline captured at lock time
            var delta = currentGameAzimuth - lockedGameAzimuth
            if (delta > 180) delta -= 360
            if (delta < -180) delta += 360
            val trackedAzimuth = (lockedAzimuth + delta + 360) % 360
            _state.value = _state.value.copy(
                azimuthDegrees = trackedAzimuth,
                pitchDegrees = pitchDeg,
                isPoseValid = isPoseValid
            )
        } else {
            _state.value = _state.value.copy(
                pitchDegrees = pitchDeg,
                isPoseValid = isPoseValid
            )
        }
    }

    private fun processMagneticField(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        // Earth's magnetic field is ~25–65 µT (lower at equator, higher at poles).
        // Anything well outside this band indicates nearby ferrous metal,
        // electronics, or magnets — the bearing cannot be trusted.
        // Earth's natural field varies from ~25 µT (equator) to ~65 µT (poles).
        // Wider tolerance bands avoid false positives at high latitudes.
        val interference = when {
            magnitude < 20f || magnitude > 80f -> MagneticInterference.SEVERE
            magnitude < 25f || magnitude > 65f -> MagneticInterference.MILD
            else -> MagneticInterference.NONE
        }

        _state.value = _state.value.copy(
            magneticFieldMicroTesla = magnitude,
            interference = interference
        )
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val (compassAccuracy, degrees) = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH to 5
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.HIGH to 10
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW to 25
            else -> CompassAccuracy.UNRELIABLE to 45
        }
        _state.value = _state.value.copy(
            accuracy = compassAccuracy,
            accuracyDegrees = degrees
        )
    }
}
