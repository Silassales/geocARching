package com.porpoise.geocarching

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.graphics.Point
import android.net.Uri
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions.merge
import com.porpoise.geocarching.Util.Constants
import com.porpoise.geocarching.Util.Constants.DEFAULT_MODEL
import com.porpoise.geocarching.Util.Constants.MODEL_MAP
import com.porpoise.geocarching.Util.Leveling
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
        val firestore = FirebaseFirestore.getInstance()

        // set the arFragment
        arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as? ArFragment ?: throw IllegalStateException("AR Fragment null onCreateView")

        MapsFragment.nearbyCacheId?.let { documentId ->
            firestore.collection(getString(R.string.firebase_collection_caches)).document(documentId).get().addOnSuccessListener { cache ->
                cache.toObject(Cache::class.java)?.let {safeCache ->
                    // set the cache model
                    ModelRenderable.builder()
                        .setSource(arFragment.context, Uri.parse(MODEL_MAP[safeCache.model] ?: DEFAULT_MODEL))
                        .build()
                        .thenAccept { cacheRenderable = it }
                        .exceptionally {
                            Snackbar.make(view, it.message.toString(), Snackbar.LENGTH_LONG).show()
                            return@exceptionally null
                        }
                }
            }
        }

        // we want to try and place the cache at the centre on each update until it happens
        arFragment.arSceneView.scene.addOnUpdateListener(this::placeCacheAtScreenCentre)

        getIsCacheVisited()

        view.cache_details_fab.setOnClickListener{
            MapsFragment.nearbyCacheId?.let {
                firestore.collection(getString(R.string.firebase_collection_caches)).document(it).get().addOnSuccessListener { result ->
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
            firestore.collection(getString(R.string.firebase_collection_users))
                .document(currentAuthUser.uid)
                .collection(getString(R.string.firebase_collection_users_visits))
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
            addVisitToCurrentCache(motionEvent)

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
            firestore.collection(getString(R.string.firebase_collection_users)).document(currentAuthUser.uid).get().addOnSuccessListener { currentUserSnapshot ->
                val currentUser = currentUserSnapshot.toObject(User::class.java)
                currentUser.let {
                    MapsFragment.nearbyCacheId?.let {nearbyCacheId ->
                        firestore.collection(getString(R.string.firebase_collection_caches)).document(nearbyCacheId).get().addOnSuccessListener { visitedCacheSnapshot ->
                            val visitedCache = visitedCacheSnapshot.toObject(Cache::class.java)
                            val visitedCacheId = visitedCacheSnapshot.id

                            visitedCache ?: Log.e("addVisitToCurrentUser", "Couldn't populate fields from snapshot: {${visitedCacheSnapshot.id}")
                            visitedCache?.let {
                                val visit = UserVisit(visitedCache.name, visitedCache.l, visitedCache.g)
                                firestore.collection(getString(R.string.firebase_collection_users))
                                    .document(currentAuthUser.uid)
                                    .collection(getString(R.string.firebase_collection_users_visits))
                                    .document(visitedCacheId)
                                    .set(visit)
                                    .addOnSuccessListener {
                                        triggerConfetti(motionEvent)

                                        this.view?.let {
                                            Snackbar.make(it, R.string.ar_cache_dialog_unvisited, Snackbar.LENGTH_LONG).show()
                                        }

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
                                            view?.let{ safeView -> Snackbar.make(safeView, getString(R.string.level_up_snacker_bar_message, newLevel), Snackbar.LENGTH_LONG).show() }
                                        }
                            }
                        }
            }
        }
    }

    private fun addVisitToCurrentCache(motionEvent: MotionEvent) {
        val firestore = FirebaseFirestore.getInstance()

        FirebaseAuth.getInstance().currentUser?.let { currentAuthUser ->
            firestore.collection(getString(R.string.firebase_collection_users)).document(currentAuthUser.uid).get().addOnSuccessListener { currentUser ->
                currentUser?.let {
                    MapsFragment.nearbyCacheId?.let { nearbyCacheId ->
                        firestore.collection(getString(R.string.firebase_collection_caches))
                                .document(nearbyCacheId)
                                .collection(getString(R.string.firebase_collection_found_caches))
                                .document(currentUser.id)
                                .set(hashMapOf(getString(R.string.default_username) to currentUser.getString(getString(R.string.default_username))))
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