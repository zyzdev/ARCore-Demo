package com.example.arcontroldemo.customize

import android.util.Log
import android.view.MotionEvent
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.BaseGesture
import com.google.ar.sceneform.ux.GesturePointersUtility
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("JoinDeclarationAndAssignment")
class TwoPointDragGesture(
    gesturePointersUtility: GesturePointersUtility,
    hitTestResult: HitTestResult,
    motionEvent: MotionEvent,
    private val pointerId2: Int
) : BaseGesture<TwoPointDragGesture>(gesturePointersUtility) {
    /** Interface definition for callbacks to be invoked by a [TwoPointDragGesture].  */
    interface OnGestureEventListener : BaseGesture.OnGestureEventListener<TwoPointDragGesture>

    private val dTag = javaClass.simpleName
    private val pointerId1: Int
    val delta: Vector3
    private val position1: Vector3
    private val position2: Vector3
    private val startPosition1: Vector3
    private val startPosition2: Vector3
    private var previousPosition1: Vector3? = null
    private var previousPosition2: Vector3? = null

    init {
        pointerId1 = motionEvent.getPointerId(motionEvent.actionIndex)
        delta = Vector3.zero()
        position1 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId1)
        position2 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId2)
        startPosition1 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId1)
        startPosition2 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId2)
        targetNode = hitTestResult.node
        debugLog("Created: $pointerId1")
    }

    override fun canStart(hitTestResult: HitTestResult, motionEvent: MotionEvent): Boolean {

        if (gesturePointersUtility.isPointerIdRetained(pointerId1)
            || gesturePointersUtility.isPointerIdRetained(pointerId2)
        ) {
            cancel()
            return false
        }

        val actionId = motionEvent.getPointerId(motionEvent.actionIndex)
        val action = motionEvent.actionMasked

        if (action == MotionEvent.ACTION_CANCEL) {
            cancel()
            return false
        }

        val touchEnded = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
        if (touchEnded && (actionId == pointerId1 || actionId == pointerId2)) {
            cancel()
            return false
        }

        if (action != MotionEvent.ACTION_MOVE) {
            return false
        }

        val newPosition1 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId1)
        val newPosition2 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId2)
        if (previousPosition1 == null || previousPosition2 == null) {
            previousPosition1 = newPosition1
            previousPosition2 = newPosition2
            debugLog("just tap screen.")
            return false
        }
        val deltaPosition1 = Vector3.subtract(newPosition1, previousPosition1)
        val deltaPosition2 = Vector3.subtract(newPosition2, previousPosition2)
        val slope1 = atan2(deltaPosition1.x, deltaPosition1.y).toDouble()
        val slope2 = atan2(deltaPosition2.x, deltaPosition2.y).toDouble()
        val angle1 = Math.toDegrees(slope1)
        val angle2 = Math.toDegrees(slope2)
        val angleDis = abs(angle1.toFloat() - angle2.toFloat())
        debugLog("angle1:$angle1, angle2:$angle2, angleDis:$angleDis")
        if (angleDis > 45f) {
            cancel()
            return false
        }
        // Check that both fingers are moving.
        if (Vector3.equals(deltaPosition1, Vector3.zero())
            || Vector3.equals(deltaPosition2, Vector3.zero())
        ) {
            return false
        }
        previousPosition1 = newPosition1
        previousPosition2 = newPosition2
        return true
    }

    override fun onStart(hitTestResult: HitTestResult, motionEvent: MotionEvent) {
        debugLog("Started: $pointerId1")
        gesturePointersUtility.retainPointerId(pointerId1)
        gesturePointersUtility.retainPointerId(pointerId2)
    }

    override fun updateGesture(hitTestResult: HitTestResult, motionEvent: MotionEvent): Boolean {
        val actionId = motionEvent.getPointerId(motionEvent.actionIndex)
        val action = motionEvent.actionMasked
        if (action == MotionEvent.ACTION_CANCEL) {
            cancel()
            return false
        }
        val touchEnded = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
        if (touchEnded && (actionId == pointerId1 || actionId == pointerId2)) {
            complete()
            return false
        }
        if (action != MotionEvent.ACTION_MOVE) {
            return false
        }
        // Two drag gestures require at least two fingers to be touching.
        if (motionEvent.pointerCount == 2) {
            val newPosition1 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId1)
            val newPosition2 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId2)
            val deltaPosition1 = Vector3.subtract(newPosition1, previousPosition1)
            val deltaPosition2 = Vector3.subtract(newPosition2, previousPosition2)
            val slope1 = atan2(deltaPosition1.x, deltaPosition1.y).toDouble()
            val slope2 = atan2(deltaPosition2.x, deltaPosition2.y).toDouble()
            val angle1 = Math.toDegrees(slope1)
            val angle2 = Math.toDegrees(slope2)
            val angleDis = abs(angle1.toFloat() - angle2.toFloat())
            debugLog("angle1:$angle1, angle2:$angle2, angleDis:$angleDis")
            if (angleDis > 45f) {
                return false
            }
            if (!Vector3.equals(newPosition1, position1) && !Vector3.equals(
                    newPosition2,
                    position2
                )
            ) {
                val angleAvgAbs = abs((angle1 + angle2) / 2)
                when {
                    angleAvgAbs < 30 || angleAvgAbs > 150 -> {
                        debugLog("1 angleAvgAbs:$angleAvgAbs")
                        val avgX = (deltaPosition1.y + deltaPosition2.y) / 2
                        delta.set(Vector3(0f, avgX, 0f))
                    }
                    angleAvgAbs > 60 && angleAvgAbs < 120 -> {
                        debugLog("2 angleAvgAbs:$angleAvgAbs")
                        val avgY = (deltaPosition1.x + deltaPosition2.x) / 2
                        delta.set(Vector3(avgY, 0f, 0f))
                    }
                    else -> {
                        debugLog("3 angleAvgAbs:$angleAvgAbs")
                        val avgX = (deltaPosition1.y + deltaPosition2.y) / 2
                        val avgY = (deltaPosition1.x + deltaPosition2.x) / 2
                        delta.set(Vector3(avgX, avgY, 0f))
                    }
                }
                previousPosition1 = newPosition1
                previousPosition2 = newPosition2
                position1.set(newPosition1)
                position2.set(newPosition2)
                debugLog("Updated: $delta")
            }
        }
        return true
    }

    override fun onCancel() {
        debugLog("Cancelled")
    }

    override fun onFinish() {
        debugLog("Finished")
        gesturePointersUtility.releasePointerId(pointerId1)
        gesturePointersUtility.releasePointerId(pointerId2)
        previousPosition1 = null
        previousPosition2 = null
    }

    override fun getSelf(): TwoPointDragGesture {
        return this
    }

    companion object {
        private val TAG = TwoPointDragGesture::class.java.simpleName
        private const val DRAG_GESTURE_DEBUG = true
        private fun debugLog(log: String) {
            if (DRAG_GESTURE_DEBUG) {
                Log.d(TAG, "TwoPointDragGesture:[$log]")
            }
        }
    }
}