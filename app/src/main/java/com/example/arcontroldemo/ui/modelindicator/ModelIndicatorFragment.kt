package com.example.arcontroldemo.ui.modelindicator

import android.graphics.Point
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.arcontroldemo.R
import com.example.arcontroldemo.databinding.ModelIndicatorFragmentBinding
import com.example.arcontroldemo.helpers.TapHelper
import com.example.arpositiontool.helpers.CameraPermissionHelper
import com.example.arpositiontool.helpers.SnackbarHelper
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import kotlin.math.atan

@RequiresApi(Build.VERSION_CODES.N)
class ModelIndicatorFragment : Fragment() {

    companion object {
        fun newInstance() = ModelIndicatorFragment()
        private const val CENTER_NODE_NAME = "center node"
        private const val BOX_NODE_PREFIX = "box node"

        //enable it to see "box nodes" of model
        private const val DEBUG = false
    }


    private val dTag = javaClass.simpleName
    private lateinit var binding: ModelIndicatorFragmentBinding
    private lateinit var viewModel: ModelIndicatorViewModel

    private var installRequested = false
    private lateinit var session: Session
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private var shouldConfigureSession = false
    private lateinit var arFragment: ArFragment


    private lateinit var indicatorHandlerThread: HandlerThread
    private lateinit var indicatorHandler: Handler

    private var node: AnchorNode? = null
    private val tapHelper by lazy {
        TapHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.model_indicator_fragment, container, false)
        arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        arFragment.arSceneView.setOnTouchListener(tapHelper)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ModelIndicatorViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        if (!::session.isInitialized) {
            var exception: Exception? = null
            var message: String? = null
            try {
                val installStatus =
                    ArCoreApk.getInstance().requestInstall(requireActivity(), !installRequested)
                installStatus?.also {
                    when (installStatus) {
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            installRequested = true
                            return
                        }
                        ArCoreApk.InstallStatus.INSTALLED -> {
                        }
                    }
                }
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
                    CameraPermissionHelper.requestCameraPermission(requireActivity())
                    return
                }
                session = Session( /* context = */requireContext())
            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: Exception) {
                message = "This device does not support AR"
                exception = e
            }
            if (message != null) {
                messageSnackbarHelper.showError(requireActivity(), message)
                Log.e(dTag, "Exception creating session", exception)
                return
            }
            shouldConfigureSession = true
        }

        indicatorHandlerThread = HandlerThread("indicatorHandlerThread").apply { start() }
        indicatorHandler = Handler(indicatorHandlerThread.looper)
        node = null
        if (shouldConfigureSession) {
            configureSession()
            shouldConfigureSession = false
            arFragment.arSceneView.setupSession(session)
        }
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume()
            arFragment.arSceneView.resume()
        } catch (e: CameraNotAvailableException) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(
                requireActivity(),
                "Camera not available. Please restart the app."
            )
            resetField(this, "session")
            return
        }
    }

    private fun resetField(target: Any, fieldName: String) {
        val field = target.javaClass.getDeclaredField(fieldName)

        with(field) {
            isAccessible = true
            set(target, null)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::session.isInitialized) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            arFragment.arSceneView.pause()
            session.pause()
        }
    }

    override fun onStop() {
        indicatorHandlerThread.quitSafely()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
            Toast.makeText(
                requireContext(),
                "Camera permissions are needed to run this application",
                Toast.LENGTH_LONG
            ).show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(requireActivity())) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(requireActivity())
            }
        } else super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initializeSceneView() {
        arFragment.arSceneView.scene.addOnUpdateListener {
            onUpdateFrame()
            if (node == null && !messageSnackbarHelper.isShowing) {
                messageSnackbarHelper.showMessage(requireActivity(), "Tap screen to place a model.")
            }
            node?.also {
                indicatorNode(it)
            }
        }
    }

    private fun configureSession() {
        val config = Config(session)
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        session.configure(config)
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
        initializeSceneView()
        loadModel()
    }

    private fun addNodeToScreen() {
        ModelRenderable.builder().setRegistryId("modelFuture")
            .setSource(context, Uri.parse("andy_dance.sfb"))
            .build()
            .thenApply {
                val scene = arFragment.arSceneView.scene

                Pose.makeTranslation(0.0f, -0.75f, -5f).also { it1 ->
                    AnchorNode().apply {
                        anchor = session.createAnchor(it1)
                        //setParent(scene)
                        //localPosition = Vector3(0f, 0f, -2f)
                        localScale = Vector3(0.6f, 0.6f, 0.6f)
                        renderable = it
                        scene.addChild(this)
                        this@ModelIndicatorFragment.node = this
                    }
                }
            }
    }

    private lateinit var modelRenderable: ModelRenderable
    private fun loadModel() {
        ModelRenderable.builder().setRegistryId("modelFuture")
            .setSource(context, Uri.parse("andy_dance.sfb"))
            .build()
            .thenApply {
                modelRenderable = it
            }
    }

    private fun addNode(point: InstantPlacementPoint) {
        val scene = arFragment.arSceneView.scene
        node?.also {
            it.setParent(null)
            it.anchor?.detach()
        }
        val anchor = point.createAnchor(point.pose)
        node = AnchorNode()
        node?.anchor = anchor
        node?.renderable = modelRenderable
        node.apply {

            //add box nodes for detecting model is visible/invisible
            val box = modelRenderable.collisionShape as Box
            val centerX = box.center.x
            val centerY = box.center.y
            val centerZ = box.center.z
            val offsetX = box.extents.x
            val offsetY = box.extents.y
            val offsetZ = box.extents.z
            val pos = arrayListOf<Vector3>().apply {
                //first position is center of model
                add(box.center)
                val sign = intArrayOf(1, -1)
                for (x in 0..1) {
                    for (y in 0..1) {
                        for (z in 0..1) {
                            val tmp = Vector3(
                                centerX + offsetX * sign[x],
                                centerY + offsetY * sign[y],
                                centerZ + offsetZ * sign[z]
                            )
                            add(tmp)
                        }
                    }
                }
            }
            MaterialFactory.makeTransparentWithColor(requireContext(), Color(0f, 0f, 0f, 1f))
                .thenAccept {
                    val r = ShapeFactory.makeSphere(0.01f, Vector3.zero(), it)
                    pos.forEachIndexed { index, c ->
                        val tmp = Node()
                        tmp.localPosition = c
                        //for debug, to see box nodes, set DEBUG to true
                        if(DEBUG) tmp.renderable = if (index == 0) r else r.makeCopy()
                        //naming each box node, CENTER_NODE_NAME is for indicator ref
                        tmp.name = if (index == 0) CENTER_NODE_NAME else "${BOX_NODE_PREFIX}_$index"
                        node?.addChild(tmp)
                    }
                }
        }
        node?.setParent(scene)
        if (messageSnackbarHelper.isShowing) messageSnackbarHelper.hide(requireActivity())
    }

    private fun onUpdateFrame() {
        arFragment.arSceneView.arFrame?.also { frame ->
            while (tapHelper.didTap() && ::modelRenderable.isInitialized) {
                // Use estimated distance from the user's device to the real world, based
                // on expected user interaction and behavior.
                val approximateDistanceMeters = 2.0f
                val motionEvent = tapHelper.poll()
                val results = frame.hitTestInstantPlacement(
                    motionEvent.x,
                    motionEvent.y,
                    approximateDistanceMeters
                )
                if (results.isNotEmpty()) {
                    results.forEach {
                        Log.d(dTag, "2,hitTestResult:$it")
                        val point = it.trackable as InstantPlacementPoint
                        addNode(point)
                    }
                }

            }
        }
    }

    private val height by lazy { binding.root.height }
    private val width by lazy { binding.root.width }
    private val center by lazy { Point(width / 2, height / 2) }

    /**
     * get angle of each corner from screen center to each screen corner
     * find Linear equation of screen center to each screen corner
     * and find the angle by arctan
     */
    private val upDownAngle by lazy {
        arrayListOf<Float>().apply {
            var targetX = 0f
            var targetY = 0f
            var screenPoint = PointF(targetX, targetY)
            //y = ax + b
            var a = getSlope(center, screenPoint)
            var angle = Math.toDegrees(atan(a).toDouble()).toFloat().let {
                ((if (targetX < width / 2) it + 180f else it) + 360) % 360
            }
            add(angle)

            targetX = width.toFloat()
            targetY = 0f
            screenPoint = PointF(targetX, targetY)
            //y = ax + b
            a = getSlope(center, screenPoint)
            angle = Math.toDegrees(atan(a).toDouble()).toFloat().let {
                ((if (targetX < width / 2) it + 180f else it) + 360) % 360
            }
            add(angle)

            targetX = 0f
            targetY = height.toFloat()
            screenPoint = PointF(targetX, targetY)
            //y = ax + b
            a = getSlope(center, screenPoint)
            angle = Math.toDegrees(atan(a).toDouble()).toFloat().let {
                ((if (targetX < width / 2) it + 180f else it) + 360) % 360
            }
            add(angle)

            targetX = width.toFloat()
            targetY = height.toFloat()
            screenPoint = PointF(targetX, targetY)
            //y = ax + b
            a = getSlope(center, screenPoint)
            angle = Math.toDegrees(atan(a).toDouble()).toFloat().let {
                ((if (targetX < width / 2) it + 180f else it) + 360) % 360
            }
            add(angle)
        }
    }

    private fun indicatorNode(node: Node) {
        val camera = arFragment.arSceneView.scene.camera
        val visibility = camera.isWorldPositionVisible(node)
        //find child node name "CENTER_NODE_NAME" be ref or using this node
        var targetNode: Node = node
        for (child in node.children) {
            if (child.name == CENTER_NODE_NAME) {
                targetNode = child
                break
            }
        }
        val worldPos = targetNode.worldPosition
        val screenPos = camera.worldToScreenPoint(worldPos)
        val modelX = screenPos.x
        val modelY = screenPos.y
        val screenPoint = PointF(modelX, modelY)
        //y = ax + b
        val a = getSlope(center, screenPoint)
        val b = modelY - a * modelX
        val angle = Math.toDegrees(atan(a).toDouble()).toFloat().let {
            ((if (modelX < width / 2) it + 180f else it) + 360) % 360
        }
        //detect model is at horizontal side or vertical de of screen
        if (!visibility) {
            val maxWidth = width - binding.indicator.width
            val maxHeight = height - binding.indicator.height
            var x: Float
            var y: Float
            //user [angle] to detect the model direction
            if ((angle >= upDownAngle[0] && angle < upDownAngle[1]) || (angle >= upDownAngle[2] && angle < upDownAngle[3])) {
                //screen top-left to top-right and screen bottom-left to bottom-right
                //up or below, consider y first then calculate x
                y = when {
                    modelY < 0 -> {
                        //up
                        0f
                    }
                    modelY >= 0 -> {
                        //bottom
                        maxHeight.toFloat()
                    }
                    else -> modelY
                }
                x = (y - b) / a
            } else {
                //screen top-left to bottom-left and screen top-right to bottom-right
                //right or left, consider x first then calculate y
                x = when {
                    modelX < 0 -> {
                        //left
                        0f
                    }
                    modelX > maxWidth -> {
                        //right
                        maxWidth.toFloat()
                    }
                    else -> modelX
                }
                y = a * x + b
            }
            if (x > maxWidth) x = maxWidth.toFloat() else if (x < 0) x = 0f
            if (y > maxHeight) y = maxHeight.toFloat() else if (y < 0) y = 0f
            Log.d(
                dTag,
                "${node.name}, sc:$center, sp:$screenPoint, a:$a, b:$b, angle:$angle, x:$x, y:$y"
            )
            binding.indicator.apply {
                post {
                    this.visibility = if (visibility) View.GONE else View.VISIBLE
                    rotation = angle
                    this.x = x
                    this.y = y
                }
            }
        } else {
            binding.indicator.apply {
                if (this.visibility == View.VISIBLE) post { this.visibility = View.GONE }
            }
        }
    }

    private fun getSlope(a: Point, b: PointF): Float = (b.y - a.y) / (b.x - a.x)
}

fun Camera.isWorldPositionVisible(node: Node): Boolean {
    var isVisible = false
    val worldPosition = node.worldPosition
    val pos = arrayListOf<Vector3>().apply {
        add(worldPosition)
        node.children.forEach {
            add(it.worldPosition)
        }
    }

    fun internalVisible(worldPosition: Vector3): Boolean {
        Log.d("Camera", "111111111111$worldPosition")
        val var2 = com.google.ar.sceneform.math.Matrix()
        com.google.ar.sceneform.math.Matrix.multiply(projectionMatrix, viewMatrix, var2)
        val var5: Float = worldPosition.x
        val var6: Float = worldPosition.y
        val var7: Float = worldPosition.z
        val var8 =
            var5 * var2.data[3] + var6 * var2.data[7] + var7 * var2.data[11] + 1.0f * var2.data[15]
        if (var8 < 0f) {
            return false
        }
        val var9 = Vector3()
        var9.x =
            var5 * var2.data[0] + var6 * var2.data[4] + var7 * var2.data[8] + 1.0f * var2.data[12]
        var9.x = var9.x / var8
        if (var9.x !in -1f..1f) {
            return false
        }

        var9.y =
            var5 * var2.data[1] + var6 * var2.data[5] + var7 * var2.data[9] + 1.0f * var2.data[13]
        var9.y = var9.y / var8
        return var9.y in -1f..1f
    }
    for (p in pos) {
        if (internalVisible(p)) {
            isVisible = true
            break
        }
    }
    return isVisible
}