package com.example.arcontroldemo.customize

import android.view.MotionEvent
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.ux.BaseGestureRecognizer
import com.google.ar.sceneform.ux.GesturePointersUtility

class TwoPointDragGestureRecognizer(gesturePointersUtility: GesturePointersUtility) :
    BaseGestureRecognizer<TwoPointDragGesture>(gesturePointersUtility) {

    private val dTag = javaClass.simpleName

    /** Interface definition for a callbacks to be invoked when a [TwoPointDragGesture] starts.  */
    interface OnGestureStartedListener :
        BaseGestureRecognizer.OnGestureStartedListener<TwoPointDragGesture>

    override fun tryCreateGestures(hitTestResult: HitTestResult, motionEvent: MotionEvent) {
        // Two point drag gestures require at least two fingers to be touching.
        if (motionEvent.pointerCount < 2) {
            return
        }
        val actionId = motionEvent.getPointerId(motionEvent.actionIndex)
        val action = motionEvent.actionMasked
        val touchBegan =
            action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
        if (!touchBegan || gesturePointersUtility.isPointerIdRetained(actionId)) {
            return
        }

        // Determine if there is another pointer Id that has not yet been retained.
        for (i in 0 until motionEvent.pointerCount) {
            val pointerId = motionEvent.getPointerId(i)
            if (pointerId == actionId) {
                continue
            }
            if (gesturePointersUtility.isPointerIdRetained(pointerId)) {
                continue
            }
            gestures.add(TwoPointDragGesture(gesturePointersUtility, hitTestResult, motionEvent, pointerId))
        }
    }
}