package com.porpoise.geocarching.AR

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.porpoise.geocarching.Dialogs.AddMarkerFragment
import com.porpoise.geocarching.R
import com.porpoise.geocarching.firebaseObjects.Cache
import com.porpoise.geocarching.firebaseObjects.UserPlacedCache
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.extension.setLocation

class CachePlacer : Fragment(), AddMarkerFragment.AddMarkerDialogListener {
    private lateinit var arHelper: ArHelper

    private lateinit var cacheName: String
    private lateinit var cacheDesc: String
    private var cacheModel: Int = 1
    private lateinit var latLng: LatLng

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_ar_cacheplacer, container, false)
        arHelper = ArHelper(view)

        val dialog = AddMarkerFragment()
        dialog.setTargetFragment(this, 0)
        fragmentManager?.let { dialog.show(it, "place_cache_dialog") }

        return view
    }

    private fun createCloudCache(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (arHelper.appAnchorState != AppAnchorState.NONE) return

        Snackbar.make(arHelper.view, getString(R.string.ar_cloud_host_attempt), Snackbar.LENGTH_LONG).show()

        arHelper.hostCloudAnchor(hitResult.createAnchor())
    }

    @Synchronized
    private fun checkUpdatedAnchor() {
        if (arHelper.appAnchorState != AppAnchorState.HOSTING && arHelper.appAnchorState != AppAnchorState.RESOLVING) return

        arHelper.anchor?.let {
            val cloudState = it.cloudAnchorState

            if (arHelper.appAnchorState == AppAnchorState.HOSTING) {
                if (cloudState.isError) {
                    Snackbar.make(arHelper.view, getString(R.string.ar_cloud_host_error), Snackbar.LENGTH_LONG).show()

                    arHelper.appAnchorState = AppAnchorState.NONE
                } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                    addPlacedCacheToFirebase()

                    arHelper.appAnchorState = AppAnchorState.HOSTED
                }
            }
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
        fragmentManager?.popBackStackImmediate()
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, cacheName_in: String, cacheDesc_in: String, cacheModel_in: String, latLng_in: LatLng) {
        cacheName = cacheName_in
        cacheDesc = cacheDesc_in
        latLng = latLng_in

        when(cacheModel_in){
            "Model 1" -> cacheModel = 1
            "Model 2" -> cacheModel = 2
            "Model 3" -> cacheModel = 3
            "Model 4" -> cacheModel = 4
        }

        arHelper.setArFragment(childFragmentManager.findFragmentById(R.id.cacheplacer_ar_fragment))

        arHelper.setOnUpdate(::checkUpdatedAnchor)
        arHelper.setOnTap(::createCloudCache)

        Snackbar.make(arHelper.view, getString(R.string.ar_cacheplacer_tapprompt), Snackbar.LENGTH_LONG).show()
    }

    private fun addPlacedCacheToFirebase() {
        arHelper.anchor?.let { safeAnchor ->
            val newCache = Cache(GeoPoint(latLng.latitude, latLng.longitude),
                    date_placed = Timestamp.now(),
                    model = cacheModel,
                    name = cacheName,
                    description = cacheDesc,
                    cloudAnchorId = safeAnchor.cloudAnchorId)

            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(getString(R.string.firebase_collection_caches)).add(newCache).addOnSuccessListener { addedCache ->
                addPlacedCacheToCurrentUser(addedCache.id)
                val geoFirestore = GeoFirestore(FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches)))

                geoFirestore.setLocation(addedCache.id, GeoPoint(latLng.latitude, latLng.longitude)) { e ->
                    if(e != null) Log.d("addPlacedCacheToFirebase", "failed to add location to cache ${addedCache.id}, exception: ${e.message}")
                }
            }
        }
    }

    private fun addPlacedCacheToCurrentUser(placedCacheId: String) {
        // TODO this could be cleaned up and separated into parts
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        auth.currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).document(currentAuthUser.uid).get().addOnSuccessListener { currentUser ->
                currentUser?.let { safeCurrentUser ->
                    firestore.collection(getString(R.string.firebase_collection_caches)).document(placedCacheId).get().addOnSuccessListener { placedSnapshot ->
                        val placedCache = placedSnapshot.toObject(Cache::class.java)
                        placedCache ?: Log.d("addPlacedCacheToCurrentUser", "Couldn't populate fields from snapshot: {${placedSnapshot.id}")
                        placedCache?.let { safePlacedCache ->
                            Log.d("addPlacedCacheToCurrentUser", "user: $safeCurrentUser placed cache: ${safePlacedCache.name}")
                            val userPlacedCache = UserPlacedCache(placedCache.name, placedCache.l, placedCache.g)
                            firestore.collection(getString(R.string.firebase_collection_users))
                                    .document(currentUser.id)
                                    .collection(getString(R.string.firebase_collection_users_placed_caches))
                                    .document(placedCacheId)
                                    .set(userPlacedCache)

                            arHelper.placeCache(cacheModel)

                            Snackbar.make(arHelper.view, getString(R.string.ar_cacheplacer_placesuccess_message), Snackbar.LENGTH_LONG).setAction(getString(R.string.ar_cacheplacer_placesuccess_option)){
                                fragmentManager?.popBackStackImmediate()
                            }.show()
                        }
                    }
                }
            }
        }
    }
}