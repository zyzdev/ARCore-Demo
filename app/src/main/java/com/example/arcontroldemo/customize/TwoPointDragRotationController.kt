package com.example.arcontroldemo.customize

import android.util.Log
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.*
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.withSign

class TwoPointDragRotationController(transformableNode: BaseTransformableNode, gestureRecognizer: TwoPointDragGestureRecognizer) :
    BaseTransformationController<TwoPointDragGesture>(transformableNode, gestureRecognizer) {

    private val dTag = javaClass.simpleName
    private val rotationRateDegrees = 0.5f

    override fun onGestureStarted(gesture: TwoPointDragGesture) {
        super.onGestureStarted(gesture)
    }

    override fun canStartTransformation(gesture: TwoPointDragGesture): Boolean {
        return transformableNode.isSelected
    }

    override fun onContinueTransformation(gesture: TwoPointDragGesture) {
        var localRotation = transformableNode.localRotation

        val rotationAmountX = gesture.delta.x * rotationRateDegrees
        val rotationDeltaX = Quaternion(Vector3.up(), rotationAmountX)
        val rotationAmountY = gesture.delta.y * rotationRateDegrees
        val rotationDeltaY = Quaternion(Vector3.right(), rotationAmountY)
        val rotateDelta = Quaternion.multiply(rotationDeltaX, rotationDeltaY)
        localRotation =  Quaternion.multiply(localRotation, rotateDelta)
        transformableNode.localRotation = Quaternion.multiply(localRotation, rotateDelta)
    }

    override fun onEndTransformation(gesture: TwoPointDragGesture) {

    }

    private fun toEulerAngles(q:Quaternion):Vector3 = Vector3().apply {
        // roll (x-axis rotation)
        val sinrCosp = 2 * (q.w * q.x + q.y * q.z).toDouble()
        val cosrCosp = 1 - 2 * (q.x * q.x + q.y * q.y).toDouble()
        val roll = atan2(sinrCosp, cosrCosp).let { Math.toDegrees(it) }.toFloat()

        // pitch (y-axis rotation)
        val sinp = 2 * (q.w * q.y - q.z * q.x).toDouble()
        val pitch = (if (abs(sinp) >= 1)
            (Math.PI / 2).withSign(sinp) // use 90 degrees if out of range
        else
            asin(sinp)).let { Math.toDegrees(it) }.toFloat()

        // yaw (z-axis rotation)
        val sinyCosp = 2 * (q.w * q.z + q.x * q.y).toDouble()
        val cosyCosp = 1 - 2 * (q.y * q.y + q.z * q.z).toDouble()
        val yaw = atan2(sinyCosp, cosyCosp).let { Math.toDegrees(it) }.toFloat()
        x = roll
        y = pitch
        z = yaw
    }
}