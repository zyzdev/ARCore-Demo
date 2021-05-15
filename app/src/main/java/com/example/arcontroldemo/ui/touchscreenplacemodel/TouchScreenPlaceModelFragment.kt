package com.example.arcontroldemo.ui.touchscreenplacemodel

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.arcontroldemo.R
import com.example.arcontroldemo.databinding.TouchScreenPlaceModelFragmentBinding
import com.example.arcontroldemo.helpers.TapHelper
import com.example.arpositiontool.helpers.CameraPermissionHelper
import com.example.arpositiontool.helpers.SnackbarHelper
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment

@RequiresApi(Build.VERSION_CODES.N)
class TouchScreenPlaceModelFragment : Fragment() {

    companion object {
        fun newInstance() = TouchScreenPlaceModelFragment()
    }

    private val dTag = javaClass.simpleName
    private val modelName = "andy_dance"
    private lateinit var binding: TouchScreenPlaceModelFragmentBinding
    private var installRequested = false
    private lateinit var session: Session
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private lateinit var modelRenderable: ModelRenderable
    private var shouldConfigureSession = false
    private lateinit var arFragment: ArFragment
    private val tapHelper by lazy {
        TapHelper(requireContext())
    }

    private val allNode = arrayListOf<Node>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.touch_screen_place_model_fragment,
            container,
            false
        )
        arFragment = (childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment)
        arFragment.arSceneView.setOnTouchListener(tapHelper)
        binding.cleanAll.apply {
            setOnClickListener {
                allNode.forEach {
                    it.setParent(null)
                }
                allNode.clear()
                binding.cleanAll.visibility = View.GONE
            }
        }
        loadModel()
        initializeSceneView()
        return binding.root
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
        }
    }

    private fun loadModel() {
        ModelRenderable.builder().setRegistryId("modelFuture")
            .setSource(context, Uri.parse("$modelName.sfb"))
            .build()
            .thenApply {
                modelRenderable = it
            }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun onUpdateFrame(frameTime: FrameTime) {
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

    private fun addNode(point: InstantPlacementPoint) {
        val anchor = point.createAnchor(point.pose)
        val node = AnchorNode()
        node.anchor = anchor
        node.renderable = modelRenderable
        node.setParent(arFragment.arSceneView.scene)
        allNode.add(node)
        binding.cleanAll.visibility = View.VISIBLE
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
    }
}