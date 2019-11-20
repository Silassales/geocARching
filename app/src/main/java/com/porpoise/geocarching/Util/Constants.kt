package com.porpoise.geocarching.Util

import com.google.common.math.IntMath.pow
import com.porpoise.geocarching.R
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sqrt

object Constants {
    // Guelph
    const val DEFAULT_LAT = 43.544804
    const val DEFAULT_LONG = -80.248169

    // map
    const val DEFAULT_CACHE_MARKER_SEARCH_RADIUS = 1.0 // km
    const val LOCATION_UPDATE_INTERVAL = 3 * 1000 // 3 secs
    const val LOCATION_UPDATE_FASTEST_INTERVAL = 1000 // 1 sec

    const val DEFAULT_MARKER = R.drawable.marker_1
    val MARKER_MAP = hashMapOf(
            1 to R.drawable.marker_1,
            2 to R.drawable.marker_2,
            3 to R.drawable.marker_3,
            4 to R.drawable.marker_4
    )
    const val DEFAULT_NEARBY_MARKER = R.drawable.marker_1_nearby
    val NEARBY_MARKER_MAP = hashMapOf(
            1 to R.drawable.marker_1_nearby,
            2 to R.drawable.marker_2_nearby,
            3 to R.drawable.marker_3_nearby,
            4 to R.drawable.marker_4_nearby
    )
    const val DEFAULT_MODEL = "andy.sfb"
    val MODEL_MAP = hashMapOf(
            1 to "andy.sfb",
            2 to "cactus.sfb",
            3 to "pepper.sfb",
            4 to "car.sfb"
    )

    // google sign in
    const val RC_SIGN_IN = 1
    const val SIGN_OUT_MESSAGE = 1
    const val DONT_SIGN_OUT_MESSAGE = 0
    const val DISCONNECT_MESSAGE = 2
    const val DONT_DISCONNECT_MESSAGE = 2

    //misc
    const val SPLASH_SCREEN_DELAY = 1000L

    // this is just to track which permission was request from onRequestPermissionsResult
    const val MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1

    const val NEARBY_CACHE_DISTANCE = 10.0

    /* ----------------------- Firebase --------------------------- */
    // Users
    const val STARTING_LEVEL = 0
    const val STARTING_EXPERIENCE = 0
    const val DEFAULT_CACHE_VISIT_XP = 50L
}

class Leveling {
    companion object {
        fun getLevelFromXP(XP: Long) : Int {
            /*
             Current formula for xp is an exponential/sqrt relationship so there is progression ->
             for ex. xp needed for level 1 -> 50xp - 1 cache
                     xp needed for level 2 -> 150xp - 2 more caches
                                         3 -> 300xp - 3 more
                                         4 -> 500xp - 4 more
                                         5 -> 750xp - 5 more

                Formula is: (sqrt(625 + 100XP) - 25) / 50
             */
            return floor(((sqrt(625 + 100*XP.toDouble()) - 25) / 50)).toInt()
        }

        /* returns a value between 0-100 representing the progress to go till the next level
        * Doesn't validate so if you give it bad data you will get bad data
        * */
        fun getProgressToNextLevel(currentLevel: Int, currentXP: Int) : Int {
            /*
                Rearranged for XP we get: ((50y + 25)^2 - 25) / 100 where y is the level
             */

            val nextLevelXp = (pow((50*(currentLevel + 1) + 25), 2) - 625) / 100
            val currentLevelXp = (pow((50*currentLevel + 25), 2) - 625) / 100

            val xpDelta = nextLevelXp - currentLevelXp
            val xpSinceLastLevelUp = currentXP - currentLevelXp

            // weird casts to avoid int division
            return ((xpSinceLastLevelUp.toDouble() / xpDelta.toDouble()) * 100).toInt()
        }
    }
}