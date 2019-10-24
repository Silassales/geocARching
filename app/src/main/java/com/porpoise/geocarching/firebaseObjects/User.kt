package com.porpoise.geocarching.firebaseObjects

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.porpoise.geocarching.Util.Constants.DEFAULT_LAT
import com.porpoise.geocarching.Util.Constants.DEFAULT_LONG

@IgnoreExtraProperties
data class User(val uid: String,
                val email: String = "",
                val experience: Int = 0,
                val level: Int = 0,
                val username: String = "")