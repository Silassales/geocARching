package com.porpoise.geocarching.firebaseObjects

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.porpoise.geocarching.Util.Constants.DEFAULT_LAT
import com.porpoise.geocarching.Util.Constants.DEFAULT_LONG

@IgnoreExtraProperties
data class User(val username: String = "",
                val email: String = "",
                val uid: String = "",
                val experience: Int = 0,
                val level: Int = 0
                )

@IgnoreExtraProperties
data class UserVisit(val name: String = "",
                     val l: GeoPoint = GeoPoint(DEFAULT_LAT, DEFAULT_LONG),
                     val g: String = "")

@IgnoreExtraProperties
data class UserPlacedCache(val name: String = "",
                     val l: GeoPoint = GeoPoint(DEFAULT_LAT, DEFAULT_LONG),
                     val g: String = "")

@IgnoreExtraProperties
data class LeaderboardUser(val username: String = "",
                           val level: Int = 0)