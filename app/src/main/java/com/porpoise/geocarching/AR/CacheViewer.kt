package com.porpoise.geocarching.AR

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_ar_cacheviewer.view.*

import com.google.ar.core.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.porpoise.geocarching.MapsFragment
import com.porpoise.geocarching.R
import com.porpoise.geocarching.Util.Constants
import com.porpoise.geocarching.Util.Leveling
import com.porpoise.geocarching.firebaseObjects.Cache
import com.porpoise.geocarching.firebaseObjects.User
import com.porpoise.geocarching.firebaseObjects.UserVisit

class CacheViewer : Fragment() {
    private var model: Int = 0
    private var isCacheVisited: Boolean = true
    private lateinit var arHelper: ArHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_ar_cacheviewer, container, false)
        arHelper = ArHelper(view)
        val firestore = FirebaseFirestore.getInstance()

        // set the arFragment
        arHelper.setArFragment(childFragmentManager.findFragmentById(R.id.cacheviewer_ar_fragment))

        MapsFragment.nearbyCacheId?.let { documentId ->
            firestore.collection(getString(R.string.firebase_collection_caches)).document(documentId).get().addOnSuccessListener { cache ->
                cache.toObject(Cache::class.java)?.let { safeCache ->
                    arHelper.resolveCloudAnchor(safeCache.cloudAnchorId)

                    model = safeCache.model

                    arHelper.setOnUpdate(::checkUpdatedAnchor)
                }
            }
        }

        getIsCacheVisited()

        view.cache_details_fab.setOnClickListener{ _ ->
            MapsFragment.nearbyCacheId?.let { nearbyCacheId ->
                firestore.collection(getString(R.string.firebase_collection_caches)).document(nearbyCacheId).get().addOnSuccessListener { result ->
                    result.toObject(Cache::class.java)?.let { safeCache ->
                        this.context?.let { AlertDialog.Builder(it).setMessage(safeCache.description).setTitle(safeCache.name).create().show() }
                    }
                }
            }
        }

        return view
    }

    @Synchronized
    private fun checkUpdatedAnchor() {
        if (arHelper.appAnchorState != AppAnchorState.HOSTING && arHelper.appAnchorState != AppAnchorState.RESOLVING) return

        arHelper.anchor?.let {
            val cloudState = it.cloudAnchorState

            if (arHelper.appAnchorState == AppAnchorState.RESOLVING) {
                if (cloudState.isError) {
                    Snackbar.make(arHelper.view, getString(R.string.ar_cloud_resolve_error), Snackbar.LENGTH_LONG).show()

                    arHelper.appAnchorState = AppAnchorState.NONE

                    arHelper.setOnUpdate(::placeCacheAtScreenCentre)
                } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                    arHelper.appAnchorState = AppAnchorState.RESOLVED

                    if (arHelper.isCacheInScene) return

                    arHelper.placeCache(model, ::onTapCache)
                }
            }
        }
    }

    private fun getIsCacheVisited() {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users))
                .document(currentAuthUser.uid)
                .collection(getString(R.string.firebase_collection_found_caches))
                .get()
                .addOnSuccessListener { visitedCacheSnapshots ->
                    MapsFragment.nearbyCacheId?.let { nearbyCacheId ->
                        val cache = firestore.collection(getString(R.string.firebase_collection_caches)).document(nearbyCacheId)

                        // if we can't find a cache
                        if (visitedCacheSnapshots.find { it.id == cache.id } == null) {
                            isCacheVisited = false
                        }
                    }
                }
        }
    }

    private fun placeCacheAtScreenCentre() {
        arHelper.arFragment.arSceneView.arFrame?.let { arFrame ->
            // If ARCore is not tracking yet, then don't process anything
            // attempt to place the anchor at the centre of the screen only if no anchor has been set
            if (arFrame.camera.trackingState == TrackingState.TRACKING && !arHelper.isCacheInScene) {
                activity?.run {
                    val screenSize = Point()
                    windowManager.defaultDisplay.getSize(screenSize)

                    // perform hit tests and look for a hit that hits the plane
                    for (hit in arFrame.hitTest(screenSize.x.toFloat() / 2f, screenSize.y.toFloat() / 2f)) {
                        val trackable = hit.trackable

                        if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                            arHelper.cloudAnchor(hit.createAnchor())
                            arHelper.setCacheAnchorNode()
                            arHelper.placeCache(model, ::onTapCache)

                            break
                        }
                    }
                }
            }
        }
    }

    private fun onTapCache(motionEvent: MotionEvent) {
        if (!isCacheVisited) {
            addVisitToCurrentUser(motionEvent)
            addVisitToCurrentCache()

            return
        }

        this.view?.let { Snackbar.make(it, getString(R.string.ar_cache_dialog_visited), Snackbar.LENGTH_LONG).show() }
    }

    private fun addVisitToCurrentUser(motionEvent: MotionEvent) {
        // TODO this could be cleaned up and separated into parts

        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).document(currentAuthUser.uid).get().addOnSuccessListener { currentUserSnapshot ->
                val currentUser = currentUserSnapshot.toObject(User::class.java)
                currentUser.let {
                    MapsFragment.nearbyCacheId?.let { nearbyCacheId ->
                        firestore.collection(getString(R.string.firebase_collection_caches)).document(nearbyCacheId).get().addOnSuccessListener { visitedCacheSnapshot ->
                            val visitedCache = visitedCacheSnapshot.toObject(Cache::class.java)
                            val visitedCacheId = visitedCacheSnapshot.id

                            visitedCache ?: Log.e("addVisitToCurrentUser", "Couldn't populate fields from snapshot: {${visitedCacheSnapshot.id}")
                            visitedCache?.let {
                                val visit = UserVisit(visitedCache.name, visitedCache.l, visitedCache.g)
                                firestore.collection(getString(R.string.firebase_collection_users))
                                    .document(currentAuthUser.uid)
                                    .collection(getString(R.string.firebase_collection_found_caches))
                                    .document(visitedCacheId)
                                    .set(visit)
                                    .addOnSuccessListener {
                                        triggerConfetti(motionEvent)

                                        this.view?.let { Snackbar.make(it, getString(R.string.ar_cache_dialog_unvisited), Snackbar.LENGTH_LONG).show() }

                                        isCacheVisited = true

                                        addXPToCurrentUser(Constants.DEFAULT_CACHE_VISIT_XP)
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerConfetti(motionEvent: MotionEvent) {
        val explosion = CommonConfetti.explosion(
                view as ViewGroup,
                motionEvent.x.toInt(), motionEvent.y.toInt(),
                intArrayOf(Color.YELLOW, Color.LTGRAY, Color.GREEN, Color.MAGENTA))

        explosion.confettiManager
                .setTTL(2000)

        explosion.stream(1000)
    }

    private fun addXPToCurrentUser(amount: Long) {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).document(currentAuthUser.uid).get().addOnSuccessListener { currentUser ->
                val currentUserId = currentUser.id
                val currentLevel: Int = (currentUser[getString(R.string.firebase_collection_users_level)] as Long).toInt()
                var currentXP: Long = currentUser[getString(R.string.firebase_collection_users_experience)] as Long

                firestore.collection(getString(R.string.firebase_collection_users))
                        .document(currentUserId)
                        .update(getString(R.string.firebase_collection_users_experience), FieldValue.increment(amount)).addOnSuccessListener {
                            // see if we have levelled up!
                            currentXP += Constants.DEFAULT_CACHE_VISIT_XP
                            val newLevel = Leveling.getLevelFromXP(currentXP)

                            if(newLevel > currentLevel) {
                                firestore.collection(getString(R.string.firebase_collection_users))
                                        .document(currentUserId)
                                        .update(getString(R.string.firebase_collection_users_level), newLevel).addOnSuccessListener {
                                            Snackbar.make(arHelper.view, getString(R.string.level_up_snacker_bar_message, newLevel), Snackbar.LENGTH_LONG).show()
                                        }
                            }
                        }
            }
        }
    }

    private fun addVisitToCurrentCache() {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).document(currentAuthUser.uid).get().addOnSuccessListener { currentUser ->
                currentUser?.let {
                    MapsFragment.nearbyCacheId?.let { nearbyCacheId ->
                        firestore.collection(getString(R.string.firebase_collection_caches))
                            .document(nearbyCacheId)
                            .collection(getString(R.string.firebase_collection_cache_visits))
                            .document(currentUser.id)
                            .set(hashMapOf(getString(R.string.default_username) to currentUser.getString(getString(R.string.default_username))))
                    }
                }
            }
        }
    }
}