package com.porpoise.geocarching.Util

object Constants {
    // Guelph
    const val DEFAULT_LAT = 43.544804
    const val DEFAULT_LONG = -80.248169

    const val DEFAULT_CACHE_MARKER_SEARCH_RADIUS = 1.0 // km

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