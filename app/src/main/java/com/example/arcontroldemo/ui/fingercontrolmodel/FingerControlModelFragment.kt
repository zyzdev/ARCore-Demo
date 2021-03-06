package com.example.arcontroldemo.ui.fingercontrolmodel

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.arcontroldemo.R
import com.example.arcontroldemo.customize.BaseFragment
import com.example.arcontroldemo.customize.MyTransformableNode
import com.example.arcontroldemo.databinding.FingerControlModelFramgentBinding
import com.example.arpositiontool.helpers.CameraPermissionHelper
import com.example.arcontroldemo.helpers.SnackbarHelper
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.*

@RequiresApi(Build.VERSION_CODES.N)
class FingerControlModelFragment : BaseFragment() {

    companion object {
        fun newInstance() = FingerControlModelFragment()
    }

    private val dTag = javaClass.simpleName
    private val modelName = "andy_dance"
    private lateinit var binding: FingerControlModelFramgentBinding
    private var installRequested = false
    private lateinit var session: Session
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private var modelRenderable: ModelRenderable? = null
    private var shouldConfigureSession = false
    private var node:AnchorNode? = null
    private lateinit var arFragment: ArFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.finger_control_model_framgent,
            container,
            false
        )
        binding.isTracking = false
        binding.reset.setOnClickListener {
            node?.apply {
                val tmp = arrayListOf<Node>().apply {
                    addAll(children)
                }
                MyTransformableNode(arFragment.transformationSystem).apply {
                    renderable = modelRenderable
                    setParent(node)
                }
                tmp.forEach {
                    removeChild(it)
                }
            }
        }
        binding.hint.setOnClickListener {
            showHint(binding, getString(R.string.string_hint_gesture_control))
        }
        arFragment = (childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment)
        loadModel()

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (modelRenderable == null) {
                return@setOnTapArPlaneListener
            }
            node?.apply {
                anchor?.detach()
                arFragment.arSceneView.scene.removeChild(this)
            }
            node = null
            // Create the Anchor.
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)
            node = anchorNode
            // Create the transformable andy and add it to the anchor.
            MyTransformableNode(arFragment.transformationSystem).apply {
                renderable = modelRenderable
                setParent(anchorNode)
            }
            if(binding.isTracking != true) {
                binding.isTracking = true
                hideHint(binding)
            }
        }
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
            if(node == null) showHint(binding, getString(R.string.string_hint_scan_plane), false)
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

    private fun loadModel() {
        ModelRenderable.builder().setRegistryId("modelFuture")
            .setSource(context, Uri.parse("$modelName.sfb"))
            .build()
            .thenApply {
                modelRenderable = it
            }
    }

    private fun configureSession() {
        val config = Config(session)
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        session.configure(config)
    }
}