package com.example.arcontroldemo.ui.animatemodel

import android.animation.ValueAnimator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.arcontroldemo.R
import com.example.arcontroldemo.databinding.AnimateModelFragmentBinding
import com.example.arpositiontool.helpers.CameraPermissionHelper
import com.example.arpositiontool.helpers.SnackbarHelper
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.AnimationData
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment

@RequiresApi(Build.VERSION_CODES.N)
class AnimateModelFragment : Fragment() {

    companion object {
        fun newInstance() = AnimateModelFragment()
    }

    private val dTag = javaClass.simpleName
    private val defYPos = -0.6f
    private val defZPos = -2f
    private val modelName = "andy_dance"
    private lateinit var binding: AnimateModelFragmentBinding
    private var installRequested = false
    private lateinit var session: Session
    private val messageSnackbarHelper: SnackbarHelper = SnackbarHelper()
    private lateinit var modelRenderable: ModelRenderable
    private var shouldConfigureSession = false
    private lateinit var arFragment: ArFragment
    private lateinit var node: Node
    private var animator: ModelAnimator? = null
    private var nextAnimation = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.animate_model_fragment,
            container,
            false
        )
        initParam()
        binding.setting = true
        arFragment = (childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment)

        binding.xOffset.addTextChangedListener {
            val offset = it?.toString()?.toFloatOrNull() ?: 0f
            binding.xOffsetValue = offset
        }
        binding.yOffset.addTextChangedListener {
            val offset = it?.toString()?.toFloatOrNull() ?: 0f
            binding.yOffsetValue = offset
        }
        binding.zOffset.addTextChangedListener {
            val offset = it?.toString()?.toFloatOrNull() ?: 0f
            binding.zOffsetValue = offset
        }
        binding.xRotate.addTextChangedListener {
            val offset = it?.toString()?.toFloatOrNull() ?: 0f
            binding.xRotateValue = offset
        }
        binding.yRotate.addTextChangedListener {
            val offset = it?.toString()?.toFloatOrNull() ?: 0f
            binding.yRotateValue = offset
        }
        binding.zRotate.addTextChangedListener {
            val offset = it?.toString()?.toFloatOrNull() ?: 0f
            binding.zRotateValue = offset
        }
        binding.scaleOffset.addTextChangedListener {
            val offset = it?.toString()?.toFloatOrNull() ?: 0f
            binding.scaleValue = offset
        }
        binding.animateDuration.addTextChangedListener {
            val offset = it?.toString()?.toLongOrNull() ?: 0L
            binding.aniDurValue = offset
        }
        binding.settingSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            showHideSetting(isChecked)
        }
        binding.reset.setOnClickListener {
            node.localRotation = Quaternion.axisAngle(Vector3(1f, 1f, 1f), 0f)
            node.localPosition = Vector3(0f, defYPos, defZPos)
            node.localScale = Vector3.one()
            initParam()
        }
        binding.run.setOnClickListener {
            if (binding.settingSwitch.isChecked) binding.settingSwitch.isChecked = false
            ValueAnimator.ofFloat(0f, binding.xOffsetValue!!).apply {
                duration = binding.aniDurValue!!
                interpolator = FastOutSlowInInterpolator()
                var preValue = 0f
                addUpdateListener {
                    val dis = it.animatedValue as Float - preValue
                    moveBy(Vector3(dis, 0f, 0f))
                    preValue = it.animatedValue as Float
                }
                start()
            }
            ValueAnimator.ofFloat(0f, binding.yOffsetValue!!).apply {
                duration = binding.aniDurValue!!
                interpolator = FastOutSlowInInterpolator()
                var preValue = 0f
                addUpdateListener {
                    val dis = it.animatedValue as Float - preValue
                    moveBy(Vector3(0f, dis, 0f))
                    preValue = it.animatedValue as Float
                }
                start()
            }
            ValueAnimator.ofFloat(0f, binding.zOffsetValue!!).apply {
                duration = binding.aniDurValue!!
                interpolator = FastOutSlowInInterpolator()
                var preValue = 0f
                addUpdateListener {
                    val dis = it.animatedValue as Float - preValue
                    moveBy(Vector3(0f, 0f, dis))
                    preValue = it.animatedValue as Float
                }
                start()
            }
            ValueAnimator.ofFloat(0f, binding.xOffsetValue!!).apply {
                duration = binding.aniDurValue!!
                interpolator = FastOutSlowInInterpolator()
                var preValue = 0f
                addUpdateListener {
                    val dis = it.animatedValue as Float - preValue
                    moveBy(Vector3(dis, 0f, 0f))
                    preValue = it.animatedValue as Float
                }
                start()
            }
            ValueAnimator.ofFloat(0f, binding.xRotateValue!!).apply {
                duration = binding.aniDurValue!!
                interpolator = FastOutSlowInInterpolator()
                var preValue = 0f
                addUpdateListener {
                    val dis = it.animatedValue as Float - preValue
                    rotateBy(Quaternion.axisAngle(Vector3.right(), dis))
                    preValue = it.animatedValue as Float
                }
                start()
            }
            ValueAnimator.ofFloat(0f, binding.yRotateValue!!).apply {
                duration = binding.aniDurValue!!
                interpolator = FastOutSlowInInterpolator()
                var preValue = 0f
                addUpdateListener {
                    val dis = it.animatedValue as Float - preValue
                    rotateBy(Quaternion.axisAngle(Vector3.up(), dis))
                    preValue = it.animatedValue as Float
                }
                start()
            }
            ValueAnimator.ofFloat(0f, binding.zRotateValue!!).apply {
                duration = binding.aniDurValue!!
                interpolator = FastOutSlowInInterpolator()
                var preValue = 0f
                addUpdateListener {
                    val dis = it.animatedValue as Float - preValue
                    rotateBy(Quaternion.axisAngle(Vector3.forward(), dis))
                    preValue = it.animatedValue as Float
                }
                start()
            }
            ValueAnimator.ofFloat(0f, binding.scaleValue!!).apply {
                duration = binding.aniDurValue!!
                interpolator = FastOutSlowInInterpolator()
                var preValue = 0f
                addUpdateListener {
                    val dis = it.animatedValue as Float - preValue
                    scaleBy(dis)
                    preValue = it.animatedValue as Float
                }
                start()
            }
        }
        return binding.root
    }

    private fun initParam() {
        binding.xOffsetValue = 0f
        binding.yOffsetValue = 0f
        binding.zOffsetValue = 0f
        binding.xRotateValue = 0f
        binding.yRotateValue = 0f
        binding.zRotateValue = 0f
        binding.scaleValue = 0f
        binding.aniDurValue = 1500L
        binding.xOffset.setText("${binding.xOffsetValue}")
        binding.yOffset.setText("${binding.yOffsetValue}")
        binding.zOffset.setText("${binding.zOffsetValue}")
        binding.xRotate.setText("${binding.xRotateValue}")
        binding.yRotate.setText("${binding.yRotateValue}")
        binding.zRotate.setText("${binding.zRotateValue}")
        binding.scaleOffset.setText("${binding.scaleValue}")
        binding.animateDuration.setText("${binding.aniDurValue}")
    }

    private fun moveBy(pos: Vector3) {
        node.localPosition = Vector3.add(node.localPosition, pos)
    }

    private fun rotateBy(rotate: Quaternion) {
        node.localRotation = Quaternion.multiply(node.localRotation, rotate)
    }

    private fun scaleBy(scale: Float) {
        node.localScale = Vector3.add(node.localScale, Vector3(scale, scale, scale))
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

            addNodeToScreen()
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

    private fun configureSession() {
        val config = Config(session)
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        session.configure(config)
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
    }

    private fun addNodeToScreen() {
        ModelRenderable.builder().setRegistryId("modelFuture")
            .setSource(context, Uri.parse("$modelName.sfb"))
            .build()
            .thenApply {
                this.modelRenderable = it
                val scene = arFragment.arSceneView.scene
                Node().apply {
                    setParent(scene)
                    localPosition = Vector3(0f, defYPos, defZPos)
                    renderable = it
                    scene.addChild(this)
                    setOnTapListener { hitTestResult, motionEvent ->
                        playAnimation()
                    }
                    this@AnimateModelFragment.node = this
                }
                it
            }
    }

    private fun showHideSetting(show: Boolean) {
        binding.setting = show
    }

    private fun playAnimation() {
        val data: AnimationData = modelRenderable.getAnimationData(nextAnimation)
        nextAnimation = (nextAnimation + 1) % modelRenderable.animationDataCount
        animator?.also { it.cancel() }
        animator = ModelAnimator(data, modelRenderable)
        animator!!.start()
        val toast: Toast = Toast.makeText(requireContext(), data.name, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}