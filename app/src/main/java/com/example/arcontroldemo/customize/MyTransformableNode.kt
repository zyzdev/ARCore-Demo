package com.example.arcontroldemo.customize

import android.util.Log
import com.google.ar.sceneform.ux.BaseTransformableNode
import com.google.ar.sceneform.ux.ScaleController
import com.google.ar.sceneform.ux.TransformationSystem
import com.google.ar.sceneform.ux.TranslationController

class MyTransformableNode(transformationSystem: TransformationSystem) : BaseTransformableNode(
    transformationSystem
) {
    private val dTag = javaClass.simpleName

    companion object {
        private var twoDragGestureRecognizer: TwoPointDragGestureRecognizer? = null
    }

    init {
        addTransformationController(
            TranslationController(
                this,
                transformationSystem.dragRecognizer
            )
        )
        addTransformationController(
            ScaleController(
                this,
                transformationSystem.pinchRecognizer
            ).apply {
                minScale = 0.1f
                maxScale = 2f
            })
        localScale
        if (twoDragGestureRecognizer == null) {
            twoDragGestureRecognizer =
                TwoPointDragGestureRecognizer(transformationSystem.gesturePointersUtility)
        }
        transformationSystem.addGestureRecognizer(twoDragGestureRecognizer)
        addTransformationController(
            TwoPointDragRotationController(
                this,
                twoDragGestureRecognizer!!
            )
        )
    }

    override fun onDeactivate() {
        Log.d(dTag, "onDeactivate")

        super.onDeactivate()
    }
}


