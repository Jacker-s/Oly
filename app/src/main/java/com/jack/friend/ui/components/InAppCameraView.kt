package com.jack.friend.ui.components

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jack.friend.ui.theme.MessengerBlue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

@Composable
fun InAppCameraView(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit,
    onVideoCaptured: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    
    var showFlashEffect by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lensFacing, flashMode) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        
        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
            
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture, videoCapture)
        } catch (e: Exception) {
            Log.e("Camera", "Binding failed", e)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val start = System.currentTimeMillis()
            while (isRecording) {
                recordingDuration = System.currentTimeMillis() - start
                delay(100)
            }
        } else {
            recordingDuration = 0L
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Flash Effect Overlay
        AnimatedVisibility(
            visible = showFlashEffect,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(150))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        }

        // Top Controls (Glassmorphism inspired)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 20.dp, end = 20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ImageCapture.FLASH_MODE_OFF to Icons.Rounded.FlashOff,
                    ImageCapture.FLASH_MODE_ON to Icons.Rounded.FlashOn,
                    ImageCapture.FLASH_MODE_AUTO to Icons.Rounded.FlashAuto
                ).forEach { (mode, icon) ->
                    val isSelected = flashMode == mode
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MessengerBlue else Color.Transparent)
                            .clickable { flashMode = mode },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = if (isSelected) Color.White else Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Bottom UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Red.copy(0.8f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", (recordingDuration / 60000), (recordingDuration % 60000) / 1000),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.height(30.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Shortcut (Placeholder)
                Box(modifier = Modifier.size(56.dp))

                // Shutter Button (Modern Design)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showFlashEffect = true
                                    
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, "WAPPI_IMG_${System.currentTimeMillis()}")
                                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                    }
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                                        context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                                    ).build()

                                    imageCapture?.takePicture(outputOptions, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                            scope.launch {
                                                delay(100)
                                                showFlashEffect = false
                                                out.savedUri?.let { onPhotoCaptured(it) }
                                                onDismiss()
                                            }
                                        }
                                        override fun onError(e: ImageCaptureException) { 
                                            showFlashEffect = false
                                            Log.e("Camera", "Photo error", e) 
                                        }
                                    })
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    val name = "WAPPI_VID_${System.currentTimeMillis()}"
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.Video.Media.DISPLAY_NAME, name)
                                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                    }
                                    val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                                        context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                    ).setContentValues(contentValues).build()

                                    recording = videoCapture?.output?.prepareRecording(context, mediaStoreOutput)
                                        ?.start(mainExecutor) { event ->
                                            if (event is VideoRecordEvent.Finalize) {
                                                if (!event.hasError()) onVideoCaptured(event.outputResults.outputUri)
                                            }
                                        }
                                    isRecording = true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val scale by animateFloatAsState(if (isRecording) 1.2f else 1f)
                    val strokeWidth by animateDpAsState(if (isRecording) 4.dp else 6.dp)
                    
                    // Outer Ring
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(scale)
                            .border(strokeWidth, Color.White, CircleShape)
                    )
                    
                    // Inner Button
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .scale(if (isRecording) 0.8f else 1f)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Red else Color.White)
                    )
                }

                // Flip Camera
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Cached, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Text(
                "Toque para foto, segure para vídeo",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 40.dp, bottom = 120.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MessengerBlue)
                    .clickable { 
                        recording?.stop()
                        recording = null
                        isRecording = false
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, null, tint = Color.White)
            }
        }
    }
}
