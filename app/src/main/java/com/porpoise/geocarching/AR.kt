package com.porpoise.geocarching

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
import kotlinx.android.synthetic.main.fragment_ar.view.*

import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.firebase.auth.FirebaseAuth
import com.porpoise.geocarching.firebaseObjects.Cache
import com.porpoise.geocarching.firebaseObjects.User
import com.porpoise.geocarching.firebaseObjects.UserVisit

class AR : Fragment() {
    private lateinit var arFragment: ArFragment
    private lateinit var cacheRenderable: ModelRenderable
    private var cacheAnchorNode: AnchorNode? = null
    private var isCacheVisited: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_ar, container, false)

        // set the arFragment
        arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as? ArFragment ?: throw IllegalStateException("AR Fragment null onCreateView")

        // set the cache model
        ModelRenderable.builder()
            .setSource(arFragment.context, R.raw.andy)
            .build()
            .thenAccept { cacheRenderable = it }
            .exceptionally {
                Snackbar.make(view, it.message.toString(), Snackbar.LENGTH_LONG).show()
                return@exceptionally null
            }

        // we want to try and place the cache at the centre on each update until it happens
        arFragment.arSceneView.scene.addOnUpdateListener(this::placeCacheAtScreenCentre)

        getIsCacheVisited()

        view.cache_details_fab.setOnClickListener{
            MapsFragment.nearbyCacheId?.let {
                val cache = FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches)).document(it)
                cache.get().addOnSuccessListener { result ->
                    this.context?.let { context ->
                        val builder = AlertDialog.Builder(context)
                        builder.setMessage(result.get("description").toString())
                            .setTitle(result.get("name").toString())
                        val dialog = builder.create()
                        dialog.show()
                    }
                }
            }
        }

        return view
    }

    private fun getIsCacheVisited() {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).whereEqualTo(getString(R.string.firebase_users_uid), currentAuthUser.uid).get().addOnSuccessListener { currentUserSnapshots ->
                var currentUserId = ""
                for (currentUserSnapshot in currentUserSnapshots) {
                    currentUserId = currentUserSnapshot.id
                }
                firestore.collection(getString(R.string.firebase_collection_users))
                    .document(currentUserId).collection(getString(R.string.firebase_collection_users_visits)).get().addOnSuccessListener { visitedCacheSnapshots ->
                        MapsFragment.nearbyCacheId?.let { nearbyCacheId ->
                            val cache = FirebaseFirestore.getInstance().collection(getString(R.string.firebase_collection_caches)).document(nearbyCacheId)

                            // if we can't find a cache
                            if (visitedCacheSnapshots.find { it.id == cache.id } == null) {
                                isCacheVisited = false
                            }
                        }
                    }
            }
        }
    }

    private fun placeCacheAtScreenCentre(frameTime: FrameTime) {
        // Let the fragment update its state first.
        arFragment.onUpdate(frameTime)

        arFragment.arSceneView.arFrame?.let { arFrame ->
            // If ARCore is not tracking yet, then don't process anything
            // attempt to place the anchor at the centre of the screen only if no anchor has been set
            if (arFrame.camera.trackingState == TrackingState.TRACKING && cacheAnchorNode == null) {
                activity?.run {
                    val screenSize = Point()
                    windowManager.defaultDisplay.getSize(screenSize)

                    // perform hit tests and look for a hit that hits the plane
                    for (hit in arFrame.hitTest(screenSize.x.toFloat() / 2f, screenSize.y.toFloat() / 2f)) {
                        val trackable = hit.trackable

                        if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                            setCacheAnchorNode(hit.createAnchor())
                            placeCache()

                            break
                        }
                    }
                }
            }
        }
    }

    private fun setCacheAnchorNode(anchor: Anchor) {
        cacheAnchorNode = AnchorNode(anchor)
        arFragment.arSceneView.scene.addChild(cacheAnchorNode)
    }

    private fun placeCache() {
        var transformableNode = TransformableNode(arFragment.transformationSystem)
        transformableNode.renderable = cacheRenderable
        transformableNode.setParent(cacheAnchorNode)
        transformableNode.setOnTapListener { _, motionEvent -> onTapCache(motionEvent) }
        transformableNode.select()
    }

    private fun onTapCache(motionEvent: MotionEvent) {
        if (!isCacheVisited) {
            addVisitToCurrentUser(motionEvent)

            return
        }

        this.view?.let {
            Snackbar.make(it, R.string.ar_cache_dialog_visited, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun addVisitToCurrentUser(motionEvent: MotionEvent) {
        // TODO this could be cleaned up and separated into parts

        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).whereEqualTo(getString(R.string.firebase_users_uid), currentAuthUser.uid).get().addOnSuccessListener { currentUserSnapshots ->
                val currentUserSnapshot = currentUserSnapshots.last()

                val currentUser = currentUserSnapshot.toObject(User::class.java)
                val currentUserId = currentUserSnapshot.id

                currentUser?.let {
                    MapsFragment.nearbyCacheId?.let {nearbyCacheId ->
                        firestore.collection(getString(R.string.firebase_collection_caches)).document(nearbyCacheId).get().addOnSuccessListener { visitedCacheSnapshot ->
                            val visitedCache = visitedCacheSnapshot.toObject(Cache::class.java)
                            val visitedCacheId = visitedCacheSnapshot.id

                            visitedCache ?: Log.e("addVisitToCurrentUser", "Couldn't populate fields from snapshot: {${visitedCacheSnapshot.id}")
                            visitedCache?.let {
                                val visit = UserVisit(visitedCache.name, visitedCache.l, visitedCache.g)

                                firestore.collection(getString(R.string.firebase_collection_users))
                                    .document(currentUserId)
                                    .collection(getString(R.string.firebase_collection_users_visits))
                                    .document(visitedCacheId)
                                    .set(visit)
                                    .addOnSuccessListener {
                                        triggerConfetti(motionEvent)

                                        this.view?.let {
                                            Snackbar.make(it, R.string.ar_cache_dialog_unvisited, Snackbar.LENGTH_LONG).show()
                                        }

                                        isCacheVisited = true
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
}