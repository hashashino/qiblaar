package com.hashashino.qiblaar.ui.ar

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hashashino.qiblaar.MainActivity
import com.hashashino.qiblaar.R
import com.hashashino.qiblaar.databinding.FragmentArBinding
import com.hashashino.qiblaar.location.LocationProvider
import com.hashashino.qiblaar.qibla.QiblaCalculator
import com.hashashino.qiblaar.sensor.CompassAccuracy
import com.hashashino.qiblaar.sensor.DevicePose
import com.hashashino.qiblaar.sensor.LockSource
import com.hashashino.qiblaar.sensor.MagneticInterference
import com.hashashino.qiblaar.sensor.OrientationManager
import com.hashashino.qiblaar.sensor.QiblaLockHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class ArFragment : Fragment() {

    private var _binding: FragmentArBinding? = null
    private val binding get() = _binding!!

    private lateinit var orientationManager: OrientationManager
    private lateinit var locationProvider: LocationProvider

    private var lastLocation: Location? = null
    private var gpsBearing = 0f
    private var distanceKm = 0.0
    private var lockTimestampMs = 0L
    private var lockAgeJob: Job? = null
    private var cameraVisible = true

    private val vibrator: Vibrator? by lazy {
        requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // ── Priority-ordered UI state ─────────────────────────────────────────────

    private sealed class UiState {
        object PoseHint : UiState()
        data class Interference(val microTesla: Float) : UiState()
        data class Locked(
            val bearing: Float, val lockAgeMin: Int, val lockSource: LockSource
        ) : UiState()
        data class Searching(val bearing: Float, val azimuth: Float) : UiState()
        data class Aligned(val bearing: Float, val accuracy: CompassAccuracy) : UiState()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orientationManager = OrientationManager(requireContext(), DevicePose.UPRIGHT)
        locationProvider = LocationProvider(requireContext())

        binding.arOverlay.onAlignmentChanged = { aligned ->
            if (aligned) vibrateAligned()
        }

        setupClickListeners()

        if ((requireActivity() as MainActivity).hasRequiredPermissions()) {
            startCamera()
            startLocationUpdates()
        } else {
            binding.tvSearchingFooter.text = getString(R.string.permission_required)
            binding.tvSearchingFooter.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            orientationManager.state.collectLatest { state ->
                binding.arOverlay.deviceAzimuth = state.azimuthDegrees
                render(buildUiState())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        orientationManager.start()
        val shared = QiblaLockHolder.state.value
        // Only restore a lock that was saved from AR (UPRIGHT) pose — locks from compass stay in compass.
        if (shared.isLocked && shared.lockedPose == DevicePose.UPRIGHT && shared.lockSource != null) {
            lockTimestampMs = shared.lockTimestampMs
            if (!orientationManager.isLocked) {
                orientationManager.lockWhenReady(shared.lockedBearing, shared.lockedGameAzimuth, shared.lockSource)
            }
            startLockAgeTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        orientationManager.stop()
        lockAgeJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orientationManager.stop(force = true)
        _binding = null
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            val cameraProvider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startLocationUpdates() {
        locationProvider.getLastKnown { loc ->
            loc?.let { applyLocation(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            locationProvider.locationFlow().collectLatest { loc ->
                applyLocation(loc)
            }
        }
    }

    private fun applyLocation(loc: Location) {
        lastLocation = loc
        orientationManager.updateDeclination(loc)
        gpsBearing = QiblaCalculator.calculateBearing(loc.latitude, loc.longitude)
        distanceKm = QiblaCalculator.calculateDistanceKm(loc.latitude, loc.longitude)
        binding.arOverlay.qiblaBearing = gpsBearing
        binding.arOverlay.distanceKm = distanceKm.toFloat()
        render(buildUiState())
    }

    // ── State building ────────────────────────────────────────────────────────

    private fun buildUiState(): UiState {
        val orientation = orientationManager.state.value

        if (!orientation.isPoseValid) return UiState.PoseHint

        if (orientation.interference == MagneticInterference.SEVERE && !orientation.isLocked) {
            return UiState.Interference(orientation.magneticFieldMicroTesla)
        }

        if (orientation.isLocked && orientation.lockSource != null) {
            val ageMin = ((System.currentTimeMillis() - lockTimestampMs) / 60_000).toInt()
            return UiState.Locked(
                bearing = orientation.azimuthDegrees,
                lockAgeMin = ageMin,
                lockSource = orientation.lockSource
            )
        }

        val delta = run {
            var d = gpsBearing - orientation.azimuthDegrees
            d = ((d + 540) % 360) - 180
            d
        }
        return if (abs(delta) > 5f) {
            UiState.Searching(gpsBearing, orientation.azimuthDegrees)
        } else {
            UiState.Aligned(gpsBearing, orientation.accuracy)
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private fun render(state: UiState) {
        // Defaults: hide all overlay panels
        binding.flPoseHint.visibility = View.GONE
        binding.flInterference.visibility = View.GONE
        binding.llAlignedBar.visibility = View.GONE
        binding.llLockedBar.visibility = View.GONE
        binding.tvSearchingFooter.visibility = View.GONE

        when (state) {
            UiState.PoseHint -> renderPoseHint()
            is UiState.Interference -> renderInterference(state)
            is UiState.Locked -> renderLocked(state)
            is UiState.Searching -> renderSearching(state)
            is UiState.Aligned -> renderAligned(state)
        }
    }

    private fun renderPoseHint() {
        val pitch = orientationManager.state.value.pitchDegrees.roundToInt()
        binding.tvTiltChipAr.text = "Currently tilted ${pitch}° — needs near vertical"
        binding.flPoseHint.visibility = View.VISIBLE
        setModeChip(false)
        setStatusChip("WRONG POSE", 0x24F59E0B.toInt(), 0xFFFCD34D.toInt())
    }

    private fun renderInterference(state: UiState.Interference) {
        binding.tvInterferenceBody.text = getString(R.string.ar_interference_body, state.microTesla)
        binding.flInterference.visibility = View.VISIBLE
        setModeChip(false)
        setStatusChip("COMPASS DISTURBED", 0x24EF4444.toInt(), 0xFFFCA5A5.toInt())
    }

    private fun renderLocked(state: UiState.Locked) {
        val ageText = when {
            state.lockAgeMin < 1 -> "Just now"
            state.lockAgeMin < 60 -> "~${state.lockAgeMin}m ago"
            else -> "~${state.lockAgeMin / 60}h ago"
        }
        binding.tvLockedSubtitle.text = "Saved $ageText · won't drift from metal here"
        binding.llLockedBar.visibility = View.VISIBLE
        setModeChip(true)
        setStatusChip("PRE-SET", 0x2922C55E.toInt(), 0xFF86EFAC.toInt())
    }

    private fun renderSearching(state: UiState.Searching) {
        val delta = run {
            var d = state.bearing - state.azimuth
            d = ((d + 540) % 360) - 180
            d
        }
        val text = if (delta > 0) getString(R.string.sweep_right) else getString(R.string.sweep_left)
        binding.tvSearchingFooter.text = text
        binding.tvSearchingFooter.visibility = View.VISIBLE
        setModeChip(false)
        val deg = abs(delta).roundToInt()
        setStatusChip("${deg}° OFF", 0x24F59E0B.toInt(), 0xFFFCD34D.toInt())
    }

    private fun renderAligned(state: UiState.Aligned) {
        binding.llAlignedBar.visibility = View.VISIBLE
        setModeChip(false)
        val deg = orientationManager.state.value.accuracyDegrees
        setStatusChip("ALIGNED · ±${deg}°", 0x2922C55E.toInt(), 0xFF86EFAC.toInt())
    }

    private fun setModeChip(isPreset: Boolean) {
        binding.tvModeChip.text = if (isPreset) "AR · pre-set" else "AR · live"
    }

    private fun setStatusChip(label: String, bgArgb: Int, textArgb: Int) {
        val density = resources.displayMetrics.density
        binding.tvStatusChip.text = label
        binding.tvStatusChip.setTextColor(textArgb)
        binding.tvStatusChip.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f * density
            setColor(bgArgb)
        }
    }

    // ── Click handlers ────────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnCalibrateHeader.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.calibrationFragment
        }

        binding.btnToggleCamera.setOnClickListener {
            cameraVisible = !cameraVisible
            binding.previewView.visibility = if (cameraVisible) View.VISIBLE else View.INVISIBLE
            binding.btnToggleCamera.alpha = if (cameraVisible) 1f else 0.4f
        }

        binding.btnUseCompassMode.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.compassFragment
        }

        binding.btnSaveDirection.setOnClickListener {
            val orientation = orientationManager.state.value
            if (!orientation.isPoseValid || orientation.interference == MagneticInterference.SEVERE) {
                Toast.makeText(
                    requireContext(),
                    "Move to a clear spot and hold phone upright first",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (!orientationManager.lock(LockSource.GPS)) {
                Toast.makeText(requireContext(), "Gyro not ready — try again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lockTimestampMs = System.currentTimeMillis()
            QiblaLockHolder.lock(
                orientation.azimuthDegrees,
                orientationManager.lockedGameAzimuth,
                orientationManager.pose,
                LockSource.GPS
            )
            startLockAgeTimer()
        }

        binding.btnUnlockAr.setOnClickListener {
            orientationManager.unlock()
            QiblaLockHolder.unlock()
            lockAgeJob?.cancel()
            lastLocation?.let { loc ->
                gpsBearing = QiblaCalculator.calculateBearing(loc.latitude, loc.longitude)
                binding.arOverlay.qiblaBearing = gpsBearing
            }
        }

        binding.btnSwitchPreset.setOnClickListener {
            // Navigate to compass and open preset flow from there
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.compassFragment
        }
    }

    // ── Lock age timer ────────────────────────────────────────────────────────

    private fun startLockAgeTimer() {
        lockAgeJob?.cancel()
        lockAgeJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(60_000)
                render(buildUiState())
            }
        }
    }

    // ── Haptics ───────────────────────────────────────────────────────────────

    private fun vibrateAligned() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(40)
        }
    }
}
