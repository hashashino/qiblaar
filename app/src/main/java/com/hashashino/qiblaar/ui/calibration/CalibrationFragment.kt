package com.hashashino.qiblaar.ui.calibration

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hashashino.qiblaar.MainActivity
import com.hashashino.qiblaar.R
import com.hashashino.qiblaar.databinding.FragmentCalibrationBinding
import com.hashashino.qiblaar.sensor.CompassAccuracy
import com.hashashino.qiblaar.sensor.DevicePose
import com.hashashino.qiblaar.sensor.OrientationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CalibrationFragment : Fragment() {

    private var _binding: FragmentCalibrationBinding? = null
    private val binding get() = _binding!!

    private lateinit var orientationManager: OrientationManager

    /** True when this screen was opened as the mandatory first-launch calibration. */
    private var forcedFirstLaunch = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalibrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orientationManager = OrientationManager(requireContext(), DevicePose.FLAT)

        val activity = requireActivity() as MainActivity
        forcedFirstLaunch = !activity.isCalibrated()

        // During the mandatory first-launch calibration there is nothing valid to go
        // back to (the bottom nav is hidden), so send the app to the background instead
        // of popping to an empty AR screen.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(forcedFirstLaunch) {
                override fun handleOnBackPressed() {
                    requireActivity().moveTaskToBack(true)
                }
            }
        )

        binding.btnDone.setOnClickListener {
            if (forcedFirstLaunch) {
                activity.markCalibrated()
                activity.setBottomNavVisible(true)
                findNavController().popBackStack(R.id.arFragment, false)
            } else {
                findNavController().navigateUp()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            orientationManager.state.collectLatest { state ->
                updateAccuracyUi(state.accuracy)
            }
        }
    }

    private fun updateAccuracyUi(accuracy: CompassAccuracy) {
        val density = resources.displayMetrics.density

        val (progress, statusLabel, chipLabel, bgArgb, textArgb) = when (accuracy) {
            CompassAccuracy.UNRELIABLE -> tuple(15, "Low accuracy", "Needs calibration",
                0x24F59E0B.toInt(), 0xFFFCD34D.toInt())
            CompassAccuracy.LOW -> tuple(55, "Strength medium", "Low accuracy",
                0x24F59E0B.toInt(), 0xFFFCD34D.toInt())
            CompassAccuracy.HIGH -> tuple(100, "Good accuracy ✓", "±5°–±10°",
                0x2922C55E.toInt(), 0xFF86EFAC.toInt())
        }

        binding.progressCalibration.progress = progress
        binding.tvAccuracyStatus.text = statusLabel
        binding.tvAccuracyStatus.setTextColor(
            if (accuracy == CompassAccuracy.HIGH)
                ContextCompat.getColor(requireContext(), R.color.green)
            else ContextCompat.getColor(requireContext(), R.color.amber)
        )

        // Accuracy chip in header
        binding.tvAccuracyChip.text = chipLabel
        binding.tvAccuracyChip.setTextColor(textArgb)
        binding.tvAccuracyChip.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f * density
            setColor(bgArgb)
        }

        binding.btnDone.isEnabled = accuracy == CompassAccuracy.HIGH
    }

    override fun onResume() {
        super.onResume()
        orientationManager.start()
    }

    override fun onPause() {
        super.onPause()
        orientationManager.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orientationManager.stop(force = true)
        _binding = null
    }

    // Kotlin doesn't have built-in 5-tuple destructuring — use a data class helper
    private data class AccuracyUiTuple(
        val progress: Int,
        val statusLabel: String,
        val chipLabel: String,
        val bgArgb: Int,
        val textArgb: Int
    )

    private fun tuple(p: Int, s: String, c: String, bg: Int, text: Int) =
        AccuracyUiTuple(p, s, c, bg, text)
}
