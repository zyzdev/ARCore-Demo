package com.example.arcontroldemo.ui.modelindicator

import android.app.Activity
import android.graphics.Point
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.example.arcontroldemo.R
import com.example.arcontroldemo.databinding.ModelIndicatorFragmentBinding
import com.example.arpositiontool.helpers.CameraPermissionHelper
import com.example.arpositiontool.helpers.SnackbarHelper
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import kotlin.math.atan

@RequiresApi(Build.VERSION_CODES.N)
class ModelIndicatorFragment : Fragment() {

    companion object {
        fun newInstance() = ModelIndicatorFragment()
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

    private var node: Node? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.model_indicator_fragment, container, false)
        arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ModelIndicatorViewModel::class.java)
        // TODO: Use the ViewModel
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
        arFragment.arSceneView.scene.addOnUpdateListener { frameTime: FrameTime ->
            onUpdateFrame(
                frameTime
            )
            if (node == null) addNodeToScreen()
            node?.also {
                indicatorHandler.post{
                    indicatorNode(it)
                }
            }
        }
    }

    private fun configureSession() {
        val config = Config(session)
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        session.configure(config)
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
        initializeSceneView()
    }

    private fun addNodeToScreen() {
        ModelRenderable.builder().setRegistryId("modelFuture")
            .setSource(context, Uri.parse("andy_dance.sfb"))
            .build()
            .thenApply {
                val scene = arFragment.arSceneView.scene

                Pose.makeTranslation(0.0f, -0.25f, -5f).also { it1 ->
                    AnchorNode().apply {
                        anchor = session.createAnchor(it1)
                        //setParent(scene)
                        //localPosition = Vector3(0f, 0f, -2f)
                        //localScale = Vector3(0.6f, 0.6f,0.6f)
                        renderable = it
                        scene.addChild(this)
                        this@ModelIndicatorFragment.node = this
                    }
                }
            }
    }

    private fun onUpdateFrame(frameTime: FrameTime) {
        arFragment.arSceneView.arFrame?.also { frame ->

        }
    }

    private val height by lazy { binding.root.height }
    private val width by lazy { binding.root.width }
    private val center by lazy { Point(width / 2, height/ 2)}

    private fun indicatorNode(node: Node) {
        val camera = arFragment.arSceneView.scene.camera
        val worldPos = node.worldPosition
        val visibility = camera.isWorldPositionVisible(worldPos)
        val screenPos = camera.worldToScreenPoint(worldPos)
        val modelX = screenPos.x
        val modelY = screenPos.y
        //y = ax + b
        val a = getSlope(center, PointF(modelX, modelY))
        val b = modelY- a * modelX
        val angle = Math.toDegrees(atan(a).toDouble()).toFloat().let {
            if(modelX < width / 2) it +180f else it
        }
        //detect model is at horizontal side or vertical de of screen
        if(!visibility) {
            var x = 0f
            var y = 0f
            if(modelX < 0) {
                x = 0f
                y = b
            } else if(modelX > width) {
                //right
                x = width.toFloat()
                y = a * x + b
            } else {
                //should be up or below
                if(modelY < 0) {
                    //up
                    x = -b / a
                    y = 0f
                } else if(modelY > height) {
                    //below
                    x = -b / a
                    y = height.toFloat()
                }
            }
            if(x >= width) x = (width -binding.indicator.width).toFloat() else if(x < 0) x = 0f
            if(y >= height) y = (height - binding.indicator.height).toFloat() else if(y < 0) y = 0f
            binding.indicator.apply {
                post {
                    this.visibility = if (visibility) View.GONE else View.VISIBLE
                    rotation = angle
                    this.x = x
                    this.y = y
                }
            }
            Log.d(dTag, "${node.name}, sp:$screenPos, vis:$visibility, a:$a, angle:$angle, x:$x,y")
        } else {
            binding.indicator.apply {
                if(this.visibility == View.VISIBLE) post { this.visibility = View.GONE }
            }
        }
    }

    fun getSlope(a: Point, b: PointF): Float = (b.y - a.y) / (b.x - a.x)
}

fun Camera.isWorldPositionVisible(worldPosition: Vector3): Boolean {
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
    var9.x = var5 * var2.data[0] + var6 * var2.data[4] + var7 * var2.data[8] + 1.0f * var2.data[12]
    var9.x = var9.x / var8
    if (var9.x !in -1f..1f) {
        return false
    }

    var9.y = var5 * var2.data[1] + var6 * var2.data[5] + var7 * var2.data[9] + 1.0f * var2.data[13]
    var9.y = var9.y / var8
    return var9.y in -1f..1f
}