package com.hashashino.qiblaar.qibla

import kotlin.math.*

object QiblaCalculator {
    // Kaaba center coordinates (refined from satellite imagery)
    private const val MECCA_LAT = 21.422487
    private const val MECCA_LON = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0

    fun calculateBearing(userLat: Double, userLon: Double): Float {
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(MECCA_LAT)
        val deltaLon = Math.toRadians(MECCA_LON - userLon)

        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)

        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    fun calculateDistanceKm(userLat: Double, userLon: Double): Double {
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(MECCA_LAT)
        val lon1 = Math.toRadians(userLon)
        val lon2 = Math.toRadians(MECCA_LON)

        val dlat = lat2 - lat1
        val dlon = lon2 - lon1
        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        return EARTH_RADIUS_KM * 2 * asin(sqrt(a))
    }
}
