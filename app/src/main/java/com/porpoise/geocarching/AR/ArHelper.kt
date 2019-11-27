package com.porpoise.geocarching.AR

import android.net.Uri
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.porpoise.geocarching.Util.Constants

enum class AppAnchorState {
    NONE,
    HOSTING,
    HOSTED,
    RESOLVING,
    RESOLVED
}

class ArHelper(view_in: View) {
    lateinit var arFragment: CloudAnchorFragment
    var anchor: Anchor? = null
    var appAnchorState = AppAnchorState.NONE
    var view: View = view_in
    private var cacheAnchorNode: AnchorNode? = null
    var isCacheInScene: Boolean = false

    fun setArFragment(fragment: Fragment?) {
        arFragment = fragment as CloudAnchorFragment
    }

    fun setOnUpdate(updateFunction: () -> Unit) {
        arFragment.arSceneView.scene.addOnUpdateListener{ frameTime ->
            arFragment.onUpdate(frameTime)

            updateFunction()
        }
    }

    fun setOnTap(updateFunction: (hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) -> Unit) {
        arFragment.setOnTapArPlaneListener{ hitResult, plane, motionEvent ->
            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                return@setOnTapArPlaneListener
            }

            updateFunction(hitResult, plane, motionEvent)
        }
    }

    fun placeCache(model: Int) {
        placeCache(model){} // call placeCache with no onTap function
    }

    fun placeCache(model: Int, onTapCache: (motionEvent: MotionEvent) -> Unit) {
        ModelRenderable.builder()
            .setSource(arFragment.context, Uri.parse(Constants.MODEL_MAP[model] ?: Constants.DEFAULT_MODEL))
            .build()
            .thenAccept { addCacheToScene(it, onTapCache) }
            .exceptionally {
                Snackbar.make(view, it.message.toString(), Snackbar.LENGTH_LONG).show()
                return@exceptionally null
            }
    }

    fun hostCloudAnchor(newAnchor: Anchor) {
        cloudAnchor(arFragment.arSceneView.session?.hostCloudAnchor(newAnchor))
        appAnchorState = AppAnchorState.HOSTING
        setCacheAnchorNode()
    }

    fun resolveCloudAnchor(cloudAnchorId: String) {
        cloudAnchor(arFragment.arSceneView.session?.resolveCloudAnchor(cloudAnchorId))
        appAnchorState = AppAnchorState.RESOLVING
        setCacheAnchorNode()
    }

    fun cloudAnchor(newAnchor: Anchor?) {
        anchor?.detach()
        anchor = newAnchor
        appAnchorState = AppAnchorState.NONE
    }

    fun setCacheAnchorNode() {
        cacheAnchorNode = AnchorNode(anchor)
        arFragment.arSceneView.scene.addChild(cacheAnchorNode)
    }

    private fun addCacheToScene(modelRenderable: ModelRenderable, onTapCache: (motionEvent: MotionEvent) -> Unit) {
        var transformableNode = TransformableNode(arFragment.transformationSystem)
        transformableNode.renderable = modelRenderable
        transformableNode.setParent(cacheAnchorNode)
        transformableNode.setOnTapListener { _, motionEvent -> onTapCache(motionEvent) }
        transformableNode.select()

        isCacheInScene = true
    }
}