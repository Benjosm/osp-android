package com.example.ospandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ospandroid.databinding.FragmentCameraCaptureBinding\nimport com.example.ospandroid.metadata.MetadataCollectionTask
import com.example.ospandroid.managers.LocationPermissionsManager
import com.example.ospandroid.upload.UploadQueueTask
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCaptureFragment : Fragment() {

    private var _binding: FragmentCameraCaptureBinding? = null
    private val binding get() = _binding!!

    private var cameraExecutor: ExecutorService? = null
    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    
        // First check location permissions
        if (LocationPermissionsManager.isLocationPermissionGranted(requireContext())) {
            // Then check camera permissions
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                requestPermissions()
            }
        } else {
            // Request location permissions first
            LocationPermissionsManager.requestLocationPermissions(this)
        }
    
        // Setup capture button click listener
        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        // Setup flash button click listener
        binding.flashButton.setOnClickListener {
            cycleFlashMode()
        }

        // Setup camera switch button click listener
        binding.cameraSwitchButton.setOnClickListener {
            switchCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )

                // Update flash icon
                updateFlashIcon()

            } catch (exc: Exception) {
                Log.e("CameraCapture", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            1001
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1001 -> {
                if (allPermissionsGranted()) {
                    startCamera()
                } else {
                    // Handle camera permission denied
                }
            }
            1002 -> {
                if (LocationPermissionsManager.isLocationPermissionGranted(requireContext())) {
                    // Now check camera permissions
                    if (allPermissionsGranted()) {
                        startCamera()
                    } else {
                        requestPermissions()
                    }
                } else {
                    // Handle location permission denied
                }
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Collect metadata at the exact moment of capture\n        val metadata = try {\n            MetadataCollectionTask(requireContext()).collect()\n        } catch (e: Exception) {\n            Log.e(\"CameraCapture\", \"Metadata collection failed\", e)\n            null\n        }\n\n        // Create temporary file in app cache directory
        val photoFile = File(
            requireContext().cacheDir,
            "IMG_${System.currentTimeMillis()}.jpg"
        )

        // Create output options object
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set the flash mode
        imageCapture.flashMode = flashMode

        // Take the photo
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraCapture", "Photo saved: ${photoFile.absolutePath}")
                    // Associate metadata with captured photo\n                    metadata?.let { md ->\n                        try {\n                            // Save metadata to sidecar file\n                            val metadataFile = File(\n                                photoFile.parent,\n                                \"${photoFile.nameWithoutExtension}.metadata\"\n                            )\n                            metadataFile.writeText(\n                                \"timestamp=${md.timestamp}\\n\" +\n                                \"location=${md.location?.latitude},${md.location?.longitude}\"\n                            )\n                        } catch (e: Exception) {\n                            Log.e(\"CameraCapture\", \"Failed to save metadata\", e)\n                        }\n                    }\n                    // Add media to upload queue
try {
    UploadQueueTask.enqueue(requireContext(), photoFile.absolutePath, metadata)
    Log.d("CameraCapture", "Media enqueued for upload: ${photoFile.absolutePath}")
} catch (e: Exception) {
    Log.e("CameraCapture", "Failed to enqueue media for upload", e)
    // TODO: Consider user feedback for upload failures
}
// Further handling (e.g., show confirmation) could go here
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraCapture", "Photo capture failed: $exception")
                }
            }
        )
    }

    private fun cycleFlashMode() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                ImageCapture.FLASH_MODE_ON
            }
            ImageCapture.FLASH_MODE_ON -> {
                ImageCapture.FLASH_MODE_AUTO
            }
            ImageCapture.FLASH_MODE_AUTO -> {
                ImageCapture.FLASH_MODE_OFF
            }
            else -> ImageCapture.FLASH_MODE_OFF
        }
        updateFlashIcon()
        // Update the image capture instance if it exists
        imageCapture?.flashMode = flashMode
    }

    private fun updateFlashIcon() {
        val iconRes = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> android.R.drawable.btn_star_big_off
            ImageCapture.FLASH_MODE_ON -> android.R.drawable.btn_star_big_on
            ImageCapture.FLASH_MODE_AUTO -> android.R.drawable.checkbox_on_background
            else -> android.R.drawable.btn_star_big_off
        }
        binding.flashButton.setImageResource(iconRes)
    }

    private fun switchCamera() {
        // Toggle between front and back cameras
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        // Restart the camera with the new selector
        startCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor?.shutdown()
        _binding = null
    }
}
