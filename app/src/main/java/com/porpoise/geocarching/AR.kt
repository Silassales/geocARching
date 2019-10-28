package com.porpoise.geocarching

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.graphics.Point
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_ar.view.*

import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.rendering.ModelRenderable

class AR : Fragment() {
    private lateinit var arFragment: ArFragment
    private lateinit var cacheRenderable: ModelRenderable
    private var cacheAnchorNode: AnchorNode? = null

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
        transformableNode.select()
    }
}