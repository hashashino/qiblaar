package com.hashashino.qiblaar.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SharedLockState(
    val isLocked: Boolean = false,
    val lockedBearing: Float = 0f,         // true-north bearing captured at lock time
    val lockedGameAzimuth: Float = 0f,     // raw gyro azimuth at lock (for delta tracking)
    val lockedPose: DevicePose? = null,    // pose at lock — gyro baseline only valid in same pose
    val lockSource: LockSource? = null,
    val lockTimestampMs: Long = 0L,
    val manualQiblaBearing: Float? = null  // non-null only for MANUAL source
)

/**
 * Process-scoped singleton that keeps lock state in sync across
 * ArFragment and CompassFragment, which each own a separate OrientationManager.
 *
 * When a fragment is recreated (e.g. user navigates AR → Calibration → AR),
 * the new OrientationManager re-applies the saved gyro baseline directly,
 * preventing the "fresh-capture-assumes-no-rotation" bug.
 */
object QiblaLockHolder {
    private val _state = MutableStateFlow(SharedLockState())
    val state: StateFlow<SharedLockState> = _state.asStateFlow()

    fun lock(
        bearing: Float,
        gameAzimuth: Float,
        pose: DevicePose,
        source: LockSource,
        manualQiblaBearing: Float? = null
    ) {
        _state.value = SharedLockState(
            isLocked = true,
            lockedBearing = bearing,
            lockedGameAzimuth = gameAzimuth,
            lockedPose = pose,
            lockSource = source,
            lockTimestampMs = System.currentTimeMillis(),
            manualQiblaBearing = manualQiblaBearing
        )
    }

    fun unlock() {
        _state.value = SharedLockState()
    }
}
