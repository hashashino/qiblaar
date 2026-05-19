package com.hashashino.qiblaar.ui.compass

import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hashashino.qiblaar.R
import com.hashashino.qiblaar.databinding.FragmentCompassBinding
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

class CompassFragment : Fragment() {

    private var _binding: FragmentCompassBinding? = null
    private val binding get() = _binding!!

    private lateinit var orientationManager: OrientationManager
    private lateinit var locationProvider: LocationProvider

    private var lastLocation: Location? = null
    private var gpsBearing = 0f
    private var distanceKm = 0.0
    private var lockTimestampMs = 0L
    private var lockAgeJob: Job? = null

    private val activeBearing: Float
        get() {
            val shared = QiblaLockHolder.state.value
            return if (shared.isLocked && shared.lockedPose == DevicePose.FLAT) shared.lockedBearing else gpsBearing
        }

    // ── Priority-ordered UI state ─────────────────────────────────────────────

    private sealed class UiState {
        object PoseHint : UiState()
        object FindingLocation : UiState()
        data class InterferenceSevere(val microTesla: Float) : UiState()
        data class NeedsCalibration(
            val bearing: Float, val distKm: Double,
            val microTesla: Float, val gpsAccuracyM: Float?
        ) : UiState()
        data class Locked(
            val bearing: Float, val distKm: Double,
            val lockSource: LockSource, val lockAgeMin: Int,
            val accuracy: CompassAccuracy, val microTesla: Float,
            val gpsAccuracyM: Float?
        ) : UiState()
        data class Ready(
            val bearing: Float, val distKm: Double,
            val accuracy: CompassAccuracy,
            val interference: MagneticInterference,
            val microTesla: Float, val gpsAccuracyM: Float?
        ) : UiState()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orientationManager = OrientationManager(requireContext(), DevicePose.FLAT)
        locationProvider = LocationProvider(requireContext())

        setupClickListeners()

        locationProvider.getLastKnown { loc ->
            loc?.let { applyLocation(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationProvider.locationFlow().collectLatest { loc ->
                applyLocation(loc)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            orientationManager.state.collectLatest { state ->
                val az = state.azimuthDegrees
                binding.compassView.azimuth = az
                binding.compassViewFinding.azimuth = az
                binding.compassViewSevere.azimuth = az
                val bearing = activeBearing
                binding.compassView.qiblaBearing = bearing
                binding.compassViewFinding.qiblaBearing = bearing
                binding.compassViewSevere.qiblaBearing = bearing
                render(buildUiState())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        orientationManager.start()
        val shared = QiblaLockHolder.state.value
        // Only restore a lock that was saved from compass (FLAT) pose — locks from AR stay in AR.
        if (shared.isLocked && shared.lockedPose == DevicePose.FLAT && shared.lockSource != null) {
            lockTimestampMs = shared.lockTimestampMs
            if (!orientationManager.isLocked) {
                orientationManager.lockWhenReady(shared.lockedBearing, shared.lockedGameAzimuth, shared.lockSource!!)
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

    // ── State building ────────────────────────────────────────────────────────

    private fun applyLocation(loc: Location) {
        lastLocation = loc
        orientationManager.updateDeclination(loc)
        gpsBearing = QiblaCalculator.calculateBearing(loc.latitude, loc.longitude)
        distanceKm = QiblaCalculator.calculateDistanceKm(loc.latitude, loc.longitude)
        val bearing = activeBearing
        binding.compassView.qiblaBearing = bearing
        binding.compassViewFinding.qiblaBearing = bearing
        binding.compassViewSevere.qiblaBearing = bearing
        // Rough city placeholder until reverse geocoding is added
        binding.tvLocationLabel.text = "%.3f°, %.3f°".format(loc.latitude, loc.longitude)
        render(buildUiState())
    }

    private fun buildUiState(): UiState {
        val orientation = orientationManager.state.value
        val shared = QiblaLockHolder.state.value
        val location = lastLocation
        val gpsAcquired = location != null && location.accuracy <= 50f
        val gpsAccuracyM = location?.accuracy

        if (!orientation.isPoseValid) return UiState.PoseHint

        if (orientation.interference == MagneticInterference.SEVERE) {
            return UiState.InterferenceSevere(orientation.magneticFieldMicroTesla)
        }

        if (orientation.accuracy == CompassAccuracy.UNRELIABLE) {
            return UiState.NeedsCalibration(
                bearing = if (gpsAcquired) gpsBearing else orientation.azimuthDegrees,
                distKm = distanceKm,
                microTesla = orientation.magneticFieldMicroTesla,
                gpsAccuracyM = gpsAccuracyM
            )
        }

        // Locked state: only shown when this pose's own lock is active.
        if (orientation.isLocked && orientation.lockSource != null) {
            val ageMin = ((System.currentTimeMillis() - lockTimestampMs) / 60_000).toInt()
            return UiState.Locked(
                bearing = shared.lockedBearing,
                distKm = distanceKm,
                lockSource = orientation.lockSource,
                lockAgeMin = ageMin,
                accuracy = orientation.accuracy,
                microTesla = orientation.magneticFieldMicroTesla,
                gpsAccuracyM = gpsAccuracyM
            )
        }

        if (!gpsAcquired) return UiState.FindingLocation

        return UiState.Ready(
            bearing = gpsBearing,
            distKm = distanceKm,
            accuracy = orientation.accuracy,
            interference = orientation.interference,
            microTesla = orientation.magneticFieldMicroTesla,
            gpsAccuracyM = gpsAccuracyM
        )
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private fun render(state: UiState) {
        val showPose = state is UiState.PoseHint
        val showFinding = state is UiState.FindingLocation
        val showSevere = state is UiState.InterferenceSevere
        val showMain = !showPose && !showFinding && !showSevere

        binding.llPoseHint.visibility = if (showPose) View.VISIBLE else View.GONE
        binding.llFindingLocation.visibility = if (showFinding) View.VISIBLE else View.GONE
        binding.llInterferenceSevere.visibility = if (showSevere) View.VISIBLE else View.GONE
        binding.llCompassMain.visibility = if (showMain) View.VISIBLE else View.GONE

        // Clear all action groups
        binding.llActionsMain.visibility = View.GONE
        binding.llActionsLocked.visibility = View.GONE
        binding.llActionsSevere.visibility = View.GONE
        binding.btnUsePresetFinding.visibility = View.GONE

        when (state) {
            UiState.PoseHint -> renderPoseHint()
            UiState.FindingLocation -> renderFindingLocation()
            is UiState.InterferenceSevere -> renderInterferenceSevere(state)
            is UiState.NeedsCalibration -> renderNeedsCalibration(state)
            is UiState.Locked -> renderLocked(state)
            is UiState.Ready -> renderReady(state)
        }
    }

    private fun renderPoseHint() {
        val pitch = abs(orientationManager.state.value.pitchDegrees).roundToInt()
        binding.tvTiltDegrees.text = "Tilted ${pitch}° — needs to be under 15°"
        updateAccuracyChip(orientationManager.state.value.accuracy)
    }

    private fun renderFindingLocation() {
        binding.compassViewFinding.dimmed = true
        binding.compassViewFinding.accuracyLow = true
        // Show pre-set link only if user already has a saved bearing
        binding.btnUsePresetFinding.visibility =
            if (QiblaLockHolder.state.value.isLocked) View.VISIBLE else View.GONE
        updateAccuracyChip(orientationManager.state.value.accuracy)
    }

    private fun renderInterferenceSevere(state: UiState.InterferenceSevere) {
        binding.compassViewSevere.dimmed = true
        binding.tvSevereBody.text = getString(R.string.interference_severe_body, state.microTesla)
        binding.llActionsSevere.visibility = View.VISIBLE
        updateAccuracyChip(orientationManager.state.value.accuracy)
    }

    private fun renderNeedsCalibration(state: UiState.NeedsCalibration) {
        binding.compassView.dimmed = true
        binding.compassView.accuracyLow = true
        binding.compassView.locked = false
        binding.tvBearingBig.text = "—°"
        binding.tvDistance.text = if (state.distKm > 0) "%.0f km away".format(state.distKm) else "— km away"
        binding.llMildBanner.visibility = View.GONE
        binding.llLockedBanner.visibility = View.GONE
        binding.llMagDetail.visibility = View.GONE
        updateSignalTrio(state.gpsAccuracyM, CompassAccuracy.UNRELIABLE, state.microTesla, MagneticInterference.NONE)
        binding.llActionsSevere.visibility = View.VISIBLE
        updateAccuracyChip(CompassAccuracy.UNRELIABLE)
    }

    private fun renderLocked(state: UiState.Locked) {
        binding.tvLocationLabel.text = getString(R.string.preset_location_label)
        renderCompassMain(
            bearing = state.bearing, distKm = state.distKm,
            accuracy = state.accuracy,
            interference = MagneticInterference.NONE,
            microTesla = state.microTesla, gpsAccuracyM = state.gpsAccuracyM,
            isLocked = true, lockSource = state.lockSource, lockAgeMin = state.lockAgeMin
        )
        binding.llActionsLocked.visibility = View.VISIBLE
        updateAccuracyChip(state.accuracy)
    }

    private fun renderReady(state: UiState.Ready) {
        renderCompassMain(
            bearing = state.bearing, distKm = state.distKm,
            accuracy = state.accuracy, interference = state.interference,
            microTesla = state.microTesla, gpsAccuracyM = state.gpsAccuracyM,
            isLocked = false, lockSource = null, lockAgeMin = 0
        )
        binding.llActionsMain.visibility = View.VISIBLE
        updateAccuracyChip(state.accuracy)
    }

    private fun renderCompassMain(
        bearing: Float, distKm: Double,
        accuracy: CompassAccuracy, interference: MagneticInterference,
        microTesla: Float, gpsAccuracyM: Float?,
        isLocked: Boolean, lockSource: LockSource?, lockAgeMin: Int
    ) {
        binding.compassView.dimmed = false
        binding.compassView.accuracyLow = accuracy != CompassAccuracy.HIGH
        binding.compassView.locked = isLocked

        binding.tvBearingBig.text = "%.0f°".format(bearing)
        binding.tvDistance.text = if (distKm > 0) "%.0f km away".format(distKm) else "— km away"

        // Mild banner
        val showMild = interference == MagneticInterference.MILD
        binding.llMildBanner.visibility = if (showMild) View.VISIBLE else View.GONE
        if (showMild) {
            val deg = orientationManager.state.value.accuracyDegrees
            binding.tvMildBanner.text = getString(R.string.interference_mild_body, deg)
        }

        // Locked banner
        binding.llLockedBanner.visibility = if (isLocked) View.VISIBLE else View.GONE
        if (isLocked) {
            binding.tvLockedBannerBody.text = getString(R.string.preset_banner)
            binding.tvLockedAge.text = when {
                lockAgeMin < 1 -> "Just now"
                lockAgeMin < 60 -> "~${lockAgeMin}m ago"
                else -> "~${lockAgeMin / 60}h ago"
            }
        }

        updateSignalTrio(gpsAccuracyM, accuracy, microTesla, interference)

        // Magnetic detail row (shown when mild)
        binding.llMagDetail.visibility = if (showMild) View.VISIBLE else View.GONE
        if (showMild) {
            binding.tvMagDetailValue.text = "%.0f µT (normal 25–65)".format(microTesla)
        }
    }

    private fun updateSignalTrio(
        gpsAccuracyM: Float?,
        compassAccuracy: CompassAccuracy,
        microTesla: Float,
        interference: MagneticInterference
    ) {
        val ctx = requireContext()

        val gpsText = gpsAccuracyM?.let { "±%.0fm".format(it) } ?: "…"
        val gpsColor = when {
            gpsAccuracyM == null || gpsAccuracyM > 500f -> R.color.red
            gpsAccuracyM > 50f -> R.color.amber
            else -> R.color.green
        }
        binding.tvGpsValue.text = gpsText
        binding.tvGpsValue.setTextColor(ContextCompat.getColor(ctx, gpsColor))

        val compassText = "±${orientationManager.state.value.accuracyDegrees}°"
        val compassColor = when (compassAccuracy) {
            CompassAccuracy.HIGH -> R.color.green
            CompassAccuracy.LOW -> R.color.amber
            CompassAccuracy.UNRELIABLE -> R.color.red
        }
        binding.tvCompassValue.text = compassText
        binding.tvCompassValue.setTextColor(ContextCompat.getColor(ctx, compassColor))

        val magText = if (microTesla > 0f) "%.0f µT".format(microTesla) else "…"
        val magColor = when (interference) {
            MagneticInterference.NONE -> R.color.green
            MagneticInterference.MILD -> R.color.amber
            MagneticInterference.SEVERE -> R.color.red
        }
        binding.tvMagneticValue.text = magText
        binding.tvMagneticValue.setTextColor(ContextCompat.getColor(ctx, magColor))
    }

    private fun updateAccuracyChip(accuracy: CompassAccuracy) {
        val deg = orientationManager.state.value.accuracyDegrees
        val density = resources.displayMetrics.density
        val (label, bgArgb, textArgb) = when (accuracy) {
            CompassAccuracy.HIGH ->
                Triple("±${deg}°", 0x2922C55E.toInt(), 0xFF86EFAC.toInt())
            CompassAccuracy.LOW ->
                Triple("±${deg}°", 0x24F59E0B.toInt(), 0xFFFCD34D.toInt())
            CompassAccuracy.UNRELIABLE ->
                Triple("Inaccurate", 0x24EF4444.toInt(), 0xFFFCA5A5.toInt())
        }
        binding.tvAccuracyChip.text = label
        binding.tvAccuracyChip.setTextColor(textArgb)
        binding.tvAccuracyChip.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f * density
            setColor(bgArgb)
        }
    }

    // ── Click handlers ────────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnPresetDirection.setOnClickListener { saveDirectionNow() }
        binding.btnPresetSevere.setOnClickListener { saveDirectionNow() }
        binding.btnUsePresetFinding.setOnClickListener { saveDirectionNow() }

        binding.btnUseLiveCompass.setOnClickListener {
            orientationManager.unlock()
            QiblaLockHolder.unlock()
            lockAgeJob?.cancel()
            lastLocation?.let { loc ->
                gpsBearing = QiblaCalculator.calculateBearing(loc.latitude, loc.longitude)
                binding.compassView.qiblaBearing = gpsBearing
                binding.tvLocationLabel.text = "%.3f°, %.3f°".format(loc.latitude, loc.longitude)
            }
        }

        binding.btnReAnchor.setOnClickListener {
            orientationManager.unlock()
            QiblaLockHolder.unlock()
            lockAgeJob?.cancel()
            saveDirectionNow()
        }

        binding.btnRecalibrateIcon.setOnClickListener { navigateToCalibrate() }
        binding.btnRecalibrateSevere.setOnClickListener { navigateToCalibrate() }
    }

    private fun navigateToCalibrate() {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            ?.selectedItemId = R.id.calibrationFragment
    }

    private fun saveDirectionNow() {
        if (lastLocation == null) {
            Toast.makeText(requireContext(), "Waiting for GPS — try outdoors first", Toast.LENGTH_SHORT).show()
            return
        }
        if (!orientationManager.lock(LockSource.GPS)) {
            Toast.makeText(requireContext(), "Sensors still warming up, try again", Toast.LENGTH_SHORT).show()
            return
        }
        lockTimestampMs = System.currentTimeMillis()
        QiblaLockHolder.lock(
            bearing = gpsBearing,
            gameAzimuth = orientationManager.lockedGameAzimuth,
            pose = orientationManager.pose,
            source = LockSource.GPS
        )
        startLockAgeTimer()
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
}
