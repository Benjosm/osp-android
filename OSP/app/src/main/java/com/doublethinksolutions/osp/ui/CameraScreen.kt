package com.doublethinksolutions.osp.ui

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doublethinksolutions.osp.R
import com.doublethinksolutions.osp.data.PhotoMetadata
import com.doublethinksolutions.osp.tasks.MetadataCollectionTask
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(uploadViewModel: UploadViewModel = viewModel()) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (permissionsState.allPermissionsGranted) {
        CameraView(context = context, viewModel = uploadViewModel)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera and Location permissions are required.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

@Composable
private fun CameraView(context: Context, viewModel: UploadViewModel) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

    // 1. Build the use cases and PreviewView, remembering them across recompositions.
    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(flashMode) // Set initial flash mode
            .build()
    }
    val previewView = remember { PreviewView(context) }

    // Collect state from the ViewModel
    val uploadQueue by viewModel.uploadQueue.collectAsState()
    val uploadHistory by viewModel.uploadHistory.collectAsState()

    // 2. Use LaunchedEffect to handle the side effect of binding the camera.
    //    This will re-run whenever cameraSelector or lifecycleOwner changes.
    LaunchedEffect(cameraSelector, lifecycleOwner) {
        val cameraProvider = getCameraProvider(context)
        try {
            // Unbind existing use cases before re-binding
            cameraProvider.unbindAll()
            // Bind the use cases to the camera
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e("CameraScreen", "Use case binding failed", exc)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // 3. The AndroidView is now much simpler. It just displays the view.
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // UI Controls overlaid on top of the preview
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                UploadStatusIndicator(
                    uploadQueue = uploadQueue,
                    uploadHistory = uploadHistory
                )
            }

            // Bottom controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            takePhoto(context, imageCapture, viewModel)
                        }
                    },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(id = R.string.capture_photo),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.large)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                        // This direct mutation is fine and more efficient than re-binding
                        imageCapture.flashMode = flashMode
                    }) {
                        val flashIcon = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            else -> Icons.Default.FlashAuto
                        }
                        Icon(imageVector = flashIcon, contentDescription = "Toggle Flash", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                        // This state change will now correctly trigger the LaunchedEffect
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

private suspend fun getCameraProvider(context: Context): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(context).also { future ->
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(context))
    }
}

private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    previewView: PreviewView,
    imageCapture: ImageCapture
) {
    try {
        cameraProvider.unbindAll()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
    } catch (exc: Exception) {
        Log.e("CameraScreen", "Use case binding failed", exc)
    }
}

private suspend fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    viewModel: UploadViewModel
) {
    val metadata: PhotoMetadata? = try {
        MetadataCollectionTask(context).collect()
    } catch (e: Exception) {
        Log.e("CameraScreen", "Metadata collection failed", e)
        null
    }

    val photoFile = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            Log.d("CameraScreen", "Photo saved: ${photoFile.absolutePath}")
            // The magic happens here: just tell the ViewModel to start the upload.
            viewModel.startUpload(context, photoFile, metadata)
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("CameraScreen", "Photo capture failed: $exception")
            // Optionally, you could add a failed capture to a state in the ViewModel to show in the UI
        }
    })
}
