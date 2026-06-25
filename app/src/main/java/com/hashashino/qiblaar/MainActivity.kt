package com.hashashino.qiblaar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.hashashino.qiblaar.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val REQUEST_PERMISSIONS = 100
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        private const val PREFS_NAME = "qibla_prefs"
        private const val KEY_CALIBRATED = "calibration_done"
    }

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    /** True once the user has completed the one-time calibration on first launch. */
    fun isCalibrated() = prefs.getBoolean(KEY_CALIBRATED, false)

    fun markCalibrated() = prefs.edit().putBoolean(KEY_CALIBRATED, true).apply()

    fun setBottomNavVisible(visible: Boolean) {
        binding.bottomNav.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opt out of Android 15 forced edge-to-edge so status bar doesn't overlap content
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // Force a one-time compass calibration before the app can be used. Hide the
        // bottom nav and route to the calibration screen; CalibrationFragment releases
        // back to the AR view once HIGH accuracy is reached and "Done" is tapped.
        if (!isCalibrated()) {
            binding.bottomNav.visibility = View.GONE
            navController.navigate(R.id.calibrationFragment)
        }

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS)
        }
    }

    fun hasRequiredPermissions() = REQUIRED_PERMISSIONS.all {
        ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}
