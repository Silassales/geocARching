package com.porpoise.geocarching

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.porpoise.geocarching.Util.Constants.DEFAULT_CACHE_MARKER_SEARCH_RADIUS
import com.porpoise.geocarching.Util.Constants.DEFAULT_LAT
import com.porpoise.geocarching.Util.Constants.DEFAULT_LONG
import com.porpoise.geocarching.firebaseObjects.Cache
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.GeoQuery
import org.imperiumlabs.geofirestore.listeners.GeoQueryEventListener

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapsFragment : Fragment(), OnMapReadyCallback {


    private var listener: OnFragmentInteractionListener? = null
    private val locationUpdateInterval = 5 * 1000 // 5 secs
    private val locationUpdateFastestInterval = 1000 // 1 sec
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var mMap: GoogleMap? = null
    private var geoQuery: GeoQuery? = null
    private lateinit var markerMap: MutableMap<String, Marker>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_maps, container, false)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(view.context)

        val mapsFragment = childFragmentManager.findFragmentById(R.id.main_map_fragment) as? SupportMapFragment ?: throw IllegalStateException("Map Fragment null onCreateView")

        mapsFragment.getMapAsync(this)

        markerMap = mutableMapOf()

        return view
    }

    override fun onResume() {
        super.onResume()

        startLocationTracking()
    }

    override fun onPause() {
        super.onPause()

        // we don't want to be update the user location if they close the app
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geoQuery?.removeAllListeners()
        markerMap.clear()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.run {
            mMap = googleMap

            // set the map style
            mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.style_json))
            mMap?.setMinZoomPreference(17.0f)

            // set map UI settings
            val uiSettings = mMap!!.uiSettings
            uiSettings.isCompassEnabled = false
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isScrollGesturesEnabled = false

            addCacheMarkerListeners()

            startLocationTracking()
        }
    }

    private fun addCacheMarkerListeners() {
        mMap ?: Log.e("addCacheMarkerListeners", "null mMap")
        mMap?.run {
            // setup firebase references
            val ref = FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches))
            val geoFire = GeoFirestore(ref)

            geoQuery = geoFire.queryAtLocation(GeoPoint(DEFAULT_LAT, DEFAULT_LONG), DEFAULT_CACHE_MARKER_SEARCH_RADIUS)

            geoQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {

                override fun onKeyMoved(documentID: String, location: GeoPoint) {
                    mMap?.run {
                        Log.d("cacheOnKeyMoved", "cache with ID $documentID  moved to $location ")
                        if(markerMap.containsKey(documentID)) {
                            val marker = markerMap[documentID] as Marker
                            marker.position = LatLng(location.latitude, location.longitude)
                        } else {
                            // aren't currently tracking this cache, lets add it
                            mMap!!.addMarker(MarkerOptions().position(LatLng(location.longitude, location.latitude)))
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
                    mMap?.run {
                        Log.d("cacheOnKeyEntered", "new cache found at $location with ID $documentID")
                        val marker = mMap!!.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude)))
                        markerMap[documentID] = marker
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
        locationRequest.interval = locationUpdateInterval.toLong()
        locationRequest.fastestInterval = locationUpdateFastestInterval.toLong()

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun updateMapLocation(location: Location) {
        mMap ?: Log.e("UpdateMapLocation", "null mMap")
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
        updateCacheMarkerListeners(location)
    }

    private fun updateCacheMarkerListeners(location: Location) {
        mMap ?: Log.e("addCacheMarkerListeners", "null mMap")
        mMap?.run {
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

}
