package com.porpoise.geocarching.firebaseObjects

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.porpoise.geocarching.Util.Constants.DEFAULT_LAT
import com.porpoise.geocarching.Util.Constants.DEFAULT_LONG

@IgnoreExtraProperties
data class Cache(val ar_cord: GeoPoint =  GeoPoint(DEFAULT_LAT, DEFAULT_LONG),
                 val date_placed: Timestamp = Timestamp.now(),
                 val description: String = "",
                 val model: Int = 1,
                 val name: String = "",
                 val l: GeoPoint =  GeoPoint(DEFAULT_LAT, DEFAULT_LONG),
                 val g: String = "")