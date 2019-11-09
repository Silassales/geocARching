package com.porpoise.geocarching.Util

import com.porpoise.geocarching.R

object Constants {
    // Guelph
    const val DEFAULT_LAT = 43.544804
    const val DEFAULT_LONG = -80.248169

    // map
    const val DEFAULT_CACHE_MARKER_SEARCH_RADIUS = 1.0 // km
    const val LOCATION_UPDATE_INTERVAL = 3 * 1000 // 3 secs
    const val LOCATION_UPDATE_FASTEST_INTERVAL = 1000 // 1 sec

    const val DEFAULT_MODEL = R.drawable.marker_1
    val MARKER_MODEL_MAP = hashMapOf(
            1 to R.drawable.marker_1,
            2 to R.drawable.marker_2,
            3 to R.drawable.marker_3,
            4 to R.drawable.marker_4
    )
    const val DEFAULT_NEARBY_MODEL = R.drawable.marker_1_nearby
    val MARKER_NEARBY_MODEL_MAP = hashMapOf(
            1 to R.drawable.marker_1_nearby,
            2 to R.drawable.marker_2_nearby,
            3 to R.drawable.marker_3_nearby,
            4 to R.drawable.marker_4_nearby
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
}