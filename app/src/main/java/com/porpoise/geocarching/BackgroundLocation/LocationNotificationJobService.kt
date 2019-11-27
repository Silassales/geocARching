package com.porpoise.geocarching.BackgroundLocation

import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.porpoise.geocarching.MainActivity
import com.porpoise.geocarching.R
import com.porpoise.geocarching.Util.Constants
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.listeners.GeoQueryEventListener

class LocationNotificationJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        LocationServices.getFusedLocationProviderClient(applicationContext).lastLocation.addOnSuccessListener {location ->
            val ref = FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches))
            val geoFire = GeoFirestore(ref)
            val cachesNearby = mutableListOf<String>()

            val geoQuery = geoFire.queryAtLocation(GeoPoint(location.latitude, location.longitude), Constants.DEFAULT_CACHE_MARKER_SEARCH_RADIUS)
            geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {

                override fun onKeyMoved(documentID: String, location: GeoPoint) {
                }

                override fun onKeyExited(documentID: String) {
                }

                override fun onKeyEntered(documentID: String, location: GeoPoint) {
                    // called everytime a key enters even on init aka this is how you get all the queries at the start one time
                    cachesNearby.add(documentID)
                }

                override fun onGeoQueryReady() {
                    // All current data has been loaded from the server and all initial events have been fired.
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)
                    val builder = NotificationCompat.Builder(applicationContext, getString(R.string.notif_channel_name))
                            .setSmallIcon(R.drawable.icon)
                            .setContentTitle(getString(R.string.background_location_notif_title))
                            .setContentText(getString(R.string.background_location_notif_content, cachesNearby.size.toString()))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)

                    with(NotificationManagerCompat.from(applicationContext)) {
                        notify(0, builder.build())
                    }

                    geoQuery.removeAllListeners()
                }

                override fun onGeoQueryError(exception: Exception) {
                    Log.e("GeoQueryEventListener", "Error with this query: $exception")
                }
            })
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}