package com.porpoise.geocarching

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

import com.porpoise.geocarching.Dialogs.AddMarkerFragment
import com.porpoise.geocarching.Util.Constants.DEFAULT_CACHE_MARKER_SEARCH_RADIUS
import com.porpoise.geocarching.Util.Constants.DEFAULT_LAT
import com.porpoise.geocarching.Util.Constants.DEFAULT_LONG
import com.porpoise.geocarching.Util.Constants.DEFAULT_MODEL
import com.porpoise.geocarching.Util.Constants.LOCATION_UPDATE_FASTEST_INTERVAL
import com.porpoise.geocarching.Util.Constants.LOCATION_UPDATE_INTERVAL
import com.porpoise.geocarching.Util.Constants.MARKER_MODEL_MAP
import com.porpoise.geocarching.Util.Constants.MY_PERMISSIONS_REQUEST_ACCESS_LOCATION
import com.porpoise.geocarching.Util.Constants.NEARBY_CACHE_DISTANCE
import com.porpoise.geocarching.Util.DegToUTM
import com.porpoise.geocarching.firebaseObjects.Cache
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.GeoQuery
import org.imperiumlabs.geofirestore.extension.setLocation
import org.imperiumlabs.geofirestore.listeners.GeoQueryEventListener
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseAuth
import com.porpoise.geocarching.Util.Constants.DEFAULT_NEARBY_MODEL
import com.porpoise.geocarching.Util.Constants.MARKER_NEARBY_MODEL_MAP
import com.porpoise.geocarching.Util.BitmapUtil.bitmapDescriptorFromVector
import com.porpoise.geocarching.firebaseObjects.User
import com.porpoise.geocarching.firebaseObjects.UserPlacedCache


class MapsFragment : Fragment(), OnMapReadyCallback, AddMarkerFragment.AddMarkerDialogListener {


    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var mMap: GoogleMap? = null
    private var geoQuery: GeoQuery? = null
    private var markerMap: MutableMap<String, Marker> = mutableMapOf()
    private var addMarkerLatLng: LatLng? = null

    // used to publicly access the cache nearbyCacheId
    companion object {
        var nearbyCacheId: String? = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_maps, container, false)

        // first thing we wanna do is check permissions access
        if(!checkPermissionsAccess()) {
            requestPermissionsAccess()
        } else {
            createViewWithPermissions()
        }

        return view
    }

    private fun createViewWithPermissions() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context
                ?: throw IllegalStateException("context null onCreateView"))

        val mapsFragment = childFragmentManager.findFragmentById(R.id.main_map_fragment) as? SupportMapFragment
                ?: throw IllegalStateException("Map Fragment null onCreateView")

        mapsFragment.getMapAsync(this)
    }

    override fun onPause() {
        super.onPause()

        // we don't want to be update the user location if they close the app
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        geoQuery?.removeAllListeners()
        markerMap.clear()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        // need this to call dialog
        val thisFragment = this
        googleMap?.let { safeGMap ->
            // set the map style
            safeGMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.style_json))
            safeGMap.setMinZoomPreference(17.0f)

            // set the click listener for adding user caches
            safeGMap.setOnMapLongClickListener { latLong ->
                // Create an instance of the dialog fragment and show it
                addMarkerLatLng = latLong
                val dialog = AddMarkerFragment()
                dialog.setTargetFragment(thisFragment, 0)
                fragmentManager?.let { fm -> dialog.show(fm, "add_friend_dialog")  }
            }

            safeGMap.setOnMarkerClickListener { marker ->
                markerMap.asIterable().find { it.value == marker }?.let {
                    FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches)).document(it.key).get().addOnSuccessListener { result ->
                        marker.title = result.getString("name")
                        marker.snippet = result.getString("description")
                        marker.showInfoWindow()
                    }
                }

                true
            }

            // set map UI settings
            val uiSettings = safeGMap.uiSettings
            uiSettings.isCompassEnabled = false
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isScrollGesturesEnabled = false
            uiSettings.isMapToolbarEnabled = false

            /* this NEEDS to be called before [addCacheMarkerListeners] and [startLocationTracking] */
            mMap = safeGMap

            addCacheMarkerListeners()

            startLocationTracking()
        }
    }

    private fun addCacheMarkerListeners() {
        mMap ?: Log.e("addCacheMarkerListeners", "null mMap")
        mMap?.let {
            // setup firebase references
            val ref = FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches))
            val geoFire = GeoFirestore(ref)

            geoQuery = geoFire.queryAtLocation(GeoPoint(DEFAULT_LAT, DEFAULT_LONG), DEFAULT_CACHE_MARKER_SEARCH_RADIUS)

            geoQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {

                override fun onKeyMoved(documentID: String, location: GeoPoint) {
                    mMap?.let { safeMap ->
                        Log.d("cacheOnKeyMoved", "cache with ID $documentID  moved to $location ")
                        if(markerMap.containsKey(documentID)) {
                            val marker = markerMap[documentID] as Marker
                            marker.position = LatLng(location.latitude, location.longitude)
                        } else {
                            // aren't currently tracking this cache, lets add it
                            val marker = safeMap.addMarker(MarkerOptions().position(LatLng(location.longitude, location.latitude)))
                            markerMap[documentID] = marker
                            updateMarkerIcon(documentID, marker, nearby = false)
                        }
                    }
                }

                override fun onKeyExited(documentID: String) {
                    Log.d("cacheOnKeyExited", "cache with ID $documentID  moved out of range")
                    if(markerMap.containsKey(documentID)) {
                        val marker = markerMap[documentID] as Marker
                        marker.remove()
                        markerMap.remove(documentID)
                    }
                }

                override fun onKeyEntered(documentID: String, location: GeoPoint) {
                    mMap?.let { safeMap ->
                        Log.d("cacheOnKeyEntered", "new cache found at $location with ID $documentID")
                        val marker = safeMap.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude)))
                        markerMap[documentID] = marker
                        updateMarkerIcon(documentID, marker, nearby = false)
                    }
                }

                override fun onGeoQueryReady() {
                    // All current data has been loaded from the server and all initial events have been fired.
                }

                override fun onGeoQueryError(exception: Exception) {
                    Log.e("GeoQueryEventListener", "Error with this query: $exception")
                }
            })
        }
    }

    private fun updateMarkerIcon(documentID: String, marker: Marker?, nearby: Boolean) {
        FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches)).document(documentID).get().addOnSuccessListener {cache ->
            cache.toObject(Cache::class.java)?.let { safeCache ->
                context?.let {safeContext ->
                    if(nearby) {
                        marker?.setIcon(bitmapDescriptorFromVector(safeContext, MARKER_NEARBY_MODEL_MAP[safeCache.model] ?: DEFAULT_NEARBY_MODEL))
                    } else {
                        marker?.setIcon(bitmapDescriptorFromVector(safeContext, MARKER_MODEL_MAP[safeCache.model] ?: DEFAULT_MODEL))
                    }
                }
            }
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, cacheName: String, cacheDesc: String, cacheModel: String) {
        // so if everything is in order.. we should have a latlng by now but ya idk maybe not
        addMarkerLatLng?.let {
            addPlacedCacheToFirebase(cacheName, cacheDesc, cacheModel, it)
        }
    }

    private fun addPlacedCacheToFirebase(cacheName: String, cacheDesc: String, cacheModel: String, latLng: LatLng) {
        var model = 1
        when(cacheModel){
            "Model 1" -> model = 1
            "Model 2" -> model = 2
            "Model 3" -> model = 3
            "Model 4" -> model = 4
        }

        val newCache = Cache(GeoPoint(latLng.latitude, latLng.longitude),
                date_placed = Timestamp.now(),
                model = model,
                name = cacheName,
                description = cacheDesc)

        FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches)).add(newCache).addOnSuccessListener {
            addPlacedCacheToCurrentUser(it.id)
            val geoFirestore = GeoFirestore(FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches)))

            geoFirestore.setLocation(it.id, GeoPoint(latLng.latitude, latLng.longitude)) { e ->
                if(e != null) Log.d("addPlacedCacheToFirebase", "failed to add location to cache ${it.id}, exception: ${e.message}")
            }
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
    }

    private fun startLocationTracking() {
        mMap ?: Log.e("UpdateMapLocation", "null mMap")
        mMap?.isMyLocationEnabled = true

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: Log.e("startLocationTracking", "null locationResult")
                locationResult ?: return

                updateMapLocation(locationResult.lastLocation)
            }
        }

        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = LOCATION_UPDATE_INTERVAL.toLong()
        locationRequest.fastestInterval = LOCATION_UPDATE_FASTEST_INTERVAL.toLong()

        fusedLocationProviderClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun updateMapLocation(location: Location) {
        mMap ?: Log.e("UpdateMapLocation", "null mMap")
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
        updateCacheMarkerListeners(location)

        setNearbyCache(location)
    }

    private fun setNearbyCache(location: Location) {
        // reset, as there isn't always a nearby cache
        nearbyCacheId = null

        var nearestMarker: Marker? = null
        var nearestMarkerId: String? = null
        var nearestDistance = NEARBY_CACHE_DISTANCE // must be at least nearer than NEARBY_CACHE_DISTANCE

        for(marker in markerMap) {
            val distance = DegToUTM.distanceBetweenDeg(location.latitude, location.longitude, marker.value.position.latitude, marker.value.position.longitude)

            // reset the icon colour
            updateMarkerIcon(marker.key, marker.value, nearby = false)

            if (distance < nearestDistance) {
                nearestMarker = marker.value
                nearestMarkerId = marker.key
                nearestDistance = distance
            }
        }

        // if there's a nearby cache, set it
        nearestMarker?.let {
            nearbyCacheId = nearestMarkerId

            // highlight the nearby cache
            nearestMarkerId?.let { updateMarkerIcon(nearestMarkerId, nearestMarker, nearby = true) }
        }
    }

    private fun updateCacheMarkerListeners(location: Location) {
        mMap ?: Log.e("addCacheMarkerListeners", "null mMap")
        mMap?.let {
            geoQuery?.setLocation(GeoPoint(location.latitude, location.longitude), DEFAULT_CACHE_MARKER_SEARCH_RADIUS)
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    private fun addPlacedCacheToCurrentUser(placedCacheId: String) {
        // TODO this could be cleaned up and separated into parts
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        auth.currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).whereEqualTo(getString(R.string.firebase_users_uid), currentAuthUser.uid).get().addOnSuccessListener { currentUserSnapshots ->
                var currentUser: User? = null
                var currentUserId = ""
                for (currentUserSnapshot in currentUserSnapshots) {
                    currentUser = currentUserSnapshot.toObject(User::class.java)
                    currentUserId = currentUserSnapshot.id
                }
                currentUser?.let { safeCurrentUser ->
                    firestore.collection(getString(R.string.firebase_collection_caches)).document(placedCacheId).get().addOnSuccessListener { placedSnapshot ->
                        val placedCache = placedSnapshot.toObject(Cache::class.java)
                        placedCache
                                ?: Log.d("addPlacedCacheToCurrentUser", "Couldn't populate fields from snapshot: {${placedSnapshot.id}")
                        placedCache?.let { safePlacedCache ->
                            Log.d("addPlacedCacheToCurrentUser", "user: $safeCurrentUser placed cache: ${safePlacedCache.name}")
                            val userPlacedCache = UserPlacedCache(placedCache.name, placedCache.l, placedCache.g)
                            firestore.collection(getString(R.string.firebase_collection_users))
                                    .document(currentUserId)
                                    .collection(getString(R.string.firebase_collection_users_placed_caches))
                                    .document(placedCacheId)
                                    .set(userPlacedCache)
                        }
                    }
                }
            }
        }
    }

    /*
   Permissions stuff
    */

    private fun checkPermissionsAccess() : Boolean {
        // check for fine location permission
        context?.let { return (ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
        // if context is null we have bigger issues than permissions
        return false
    }

    private fun requestPermissionsAccess() {
        // In the future we may want to add prompts for the users to know why we need these permissions
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_ACCESS_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_LOCATION -> {

                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED)) {
                    /* we could ask for permissions again, but in my testing this caused the app to crash and wasnt very user
                    friendly TODO add some feedback for the user if they decline
                    */

                    activity?.finishAndRemoveTask()
                } else {
                    createViewWithPermissions()
                }
            }
        }
    }

}

