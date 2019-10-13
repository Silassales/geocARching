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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_maps, container, false)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(view.context)

        val mapsFragment = childFragmentManager.findFragmentById(R.id.main_map_fragment) as? SupportMapFragment ?: throw IllegalStateException("Map Fragment null onCreateView")

        mapsFragment.getMapAsync(this)

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
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.run {
            mMap = googleMap
            mMap?.setMinZoomPreference(17.0f)

            startLocationTracking()
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
