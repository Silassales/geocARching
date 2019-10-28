package com.porpoise.geocarching.Util

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.ln
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.tan
import kotlin.math.atan
import kotlin.math.PI

// For converting between deg (lat/lon) and UTM coordinates
// credit: https://stackoverflow.com/a/28224544
class DegToUTM(latitude: Double, longitude: Double) {
    private var easting: Double = 0.toDouble()
    private var northing: Double = 0.toDouble()
    private var zone: Int = 0
    private var letter: Char = ' '

    init {
        zone = floor(longitude / 6 + 31).toInt()

        when {
            latitude < -72 -> letter = 'C'
            latitude < -64 -> letter = 'D'
            latitude < -56 -> letter = 'E'
            latitude < -48 -> letter = 'F'
            latitude < -40 -> letter = 'G'
            latitude < -32 -> letter = 'H'
            latitude < -24 -> letter = 'J'
            latitude < -16 -> letter = 'K'
            latitude < -8 -> letter = 'L'
            latitude < 0 -> letter = 'M'
            latitude < 8 -> letter = 'N'
            latitude < 16 -> letter = 'P'
            latitude < 24 -> letter = 'Q'
            latitude < 32 -> letter = 'R'
            latitude < 40 -> letter = 'S'
            latitude < 48 -> letter = 'T'
            latitude < 56 -> letter = 'U'
            latitude < 64 -> letter = 'V'
            latitude < 72 -> letter = 'W'
            else -> letter = 'X'
        }

        easting = 0.5 * ln((1 + cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180)) / (1 - cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180))) * 0.9996 * 6399593.62 / (1 + 0.0820944379.pow(2) * sqrt(cos(latitude * PI / 180).pow(2)) * (1 + 0.0820944379.pow(2)) / 2 * (0.5 * ln((1 + cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180)) / (1 - cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180)))).pow(2) * cos(latitude * PI / 180).pow(2) / 3) + 500000
        easting = round(easting * 100) * 0.01
        northing = (atan(tan(latitude * PI / 180) / cos(longitude * PI / 180 - (6 * zone - 183) * PI / 180)) - latitude * PI / 180) * 0.9996 * 6399593.625 / sqrt(1 + 0.006739496742 * cos(latitude * PI / 180).pow(2)) * (1 + 0.006739496742 / 2 * (0.5 * ln((1 + cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180)) / (1 - cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180)))).pow(2) * cos(latitude * PI / 180).pow(2)) + 0.9996 * 6399593.625 * (latitude * PI / 180 - 0.005054622556 * (latitude * PI / 180 + sin(2.0 * latitude * PI / 180) / 2) + 4.258201531e-05 * (3 * (latitude * PI / 180 + sin(2.0 * latitude * PI / 180) / 2) + sin(2.0 * latitude * PI / 180) * cos(latitude * PI / 180).pow(2)) / 4 - 1.674057895e-07 * (5 * (3 * (latitude * PI / 180 + sin(2.0 * latitude * PI / 180) / 2) + sin(2.0 * latitude * PI / 180) * cos(latitude * PI / 180).pow(2)) / 4 + sin(2.0 * latitude * PI / 180) * cos(latitude * PI / 180).pow(2) * cos(latitude * PI / 180).pow(2)) / 3)
        if (letter < 'M')
            northing += 10000000
        northing = round(northing * 100) * 0.01
    }

    companion object {
        // Computes the distance in metres between two deg (lat/lon) coordinates
        fun distanceBetweenDeg(latitudeA: Double, longitudeA: Double, latitudeB: Double, longitudeB: Double): Double {
            // convert to utm
            val utmA = DegToUTM(latitudeA, longitudeA)
            val utmB = DegToUTM(latitudeB, longitudeB)

            // compute distance
            return sqrt((utmA.northing - utmB.northing).pow(2) + (utmA.easting- utmB.easting).pow(2))
        }
    }
}