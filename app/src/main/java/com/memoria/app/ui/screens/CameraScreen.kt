package com.memoria.app.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.*
import com.memoria.app.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CameraScreen"

private fun Context.findLifecycleOwner(): LifecycleOwner {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is LifecycleOwner) return ctx
        ctx = ctx.baseContext
    }
    error("No LifecycleOwner found")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageCaptured: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cameraPermState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing   by remember { mutableStateOf(false) }
    var flashEnabled  by remember { mutableStateOf(false) }
    var lensFacing    by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var captureResult by remember { mutableStateOf<String?>(null) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }

    var shutterFlash by remember { mutableStateOf(false) }
    val shutterAlpha by animateFloatAsState(
        targetValue = if (shutterFlash) 0.6f else 0f,
        animationSpec = tween(150), label = "shutter",
        finishedListener = { shutterFlash = false }
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val captureRingScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring"
    )

    LaunchedEffect(cameraPermState.status) {
        if (!cameraPermState.status.isGranted) cameraPermState.launchPermissionRequest()
    }

    when {
        cameraPermState.status.isGranted -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        startCamera(ctx, ctx.findLifecycleOwner(), previewView, lensFacing, flashEnabled, imageCaptureRef)
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (shutterAlpha > 0f) {
                    Box(modifier = Modifier.fillMaxSize().alpha(shutterAlpha).background(Color.White))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(0.5f))) {
                        Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                    Text("MemorIA Cam", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        colors  = IconButtonDefaults.iconButtonColors(
                            containerColor = if (flashEnabled) MemorIAColors.GoldBright.copy(0.8f) else Color.Black.copy(0.5f)
                        )
                    ) { Icon(if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff, "Flash", tint = Color.White) }
                }

                captureResult?.let { path ->
                    Card(
                        onClick  = { onImageCaptured(path) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp).size(72.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(2.dp, MemorIAColors.GoldBright)
                    ) {
                        coil.compose.AsyncImage(model = path, contentDescription = "Preview", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (captureResult != null) "Foto lista — pulsa ✓ para continuar" else "Pulsa para capturar",
                        style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                        IconButton(
                            onClick = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK },
                            colors  = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(0.5f))
                        ) { Icon(Icons.Filled.FlipCameraAndroid, "Voltear", tint = Color.White) }

                        Box(contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(80.dp).scale(captureRingScale).border(2.dp, Color.White.copy(0.5f), CircleShape))
                            Button(
                                onClick = {
                                    if (!isCapturing && captureResult == null) {
                                        isCapturing  = true
                                        shutterFlash = true
                                        capturePhoto(context, imageCaptureRef.value,
                                            onSuccess = { path -> captureResult = path; isCapturing = false },
                                            onError   = { msg  -> errorMsg = msg;       isCapturing = false }
                                        )
                                    }
                                },
                                modifier       = Modifier.size(66.dp),
                                shape          = CircleShape,
                                enabled        = !isCapturing && captureResult == null,
                                colors         = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        captureResult != null -> MemorIAColors.Success
                                        isCapturing           -> MemorIAColors.IndigoAccent
                                        else                  -> Color.White
                                    }
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                when {
                                    isCapturing           -> CircularProgressIndicator(Modifier.size(24.dp), Color.White, 2.dp)
                                    captureResult != null -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    else                  -> Icon(Icons.Filled.Camera, "Capturar", tint = Color.Black, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        if (captureResult != null) {
                            IconButton(
                                onClick = { captureResult?.let { onImageCaptured(it) } },
                                colors  = IconButtonDefaults.iconButtonColors(containerColor = MemorIAColors.IndigoAccent)
                            ) { Icon(Icons.Filled.ArrowForward, "Continuar", tint = Color.White) }
                        } else {
                            Spacer(Modifier.size(48.dp))
                        }
                    }
                }

                errorMsg?.let { msg ->
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 180.dp),
                        action   = { TextButton(onClick = { errorMsg = null; isCapturing = false }) { Text("OK") } }
                    ) { Text(msg) }
                }
            }
        }
        cameraPermState.status.shouldShowRationale ->
            PermissionRationale("MemorIA necesita la cámara para capturar recuerdos.", { cameraPermState.launchPermissionRequest() }, onBack)
        else -> PermissionDenied(onBack)
    }
}

private fun startCamera(
    ctx: Context,
    owner: LifecycleOwner,
    previewView: PreviewView,
    lensFacing: Int,
    flashEnabled: Boolean,
    imageCaptureRef: MutableState<ImageCapture?>
) {
    val future = ProcessCameraProvider.getInstance(ctx)
    future.addListener({
        try {
            val provider = future.get()
            val preview  = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val capture  = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()
            imageCaptureRef.value = capture
            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.Builder().requireLensFacing(lensFacing).build(), preview, capture)
            Log.d(TAG, "Camera started OK facing=$lensFacing")
        } catch (e: Exception) {
            Log.e(TAG, "startCamera failed", e)
        }
    }, ContextCompat.getMainExecutor(ctx))
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    if (imageCapture == null) { onError("Cámara no lista, espera un momento"); return }
    val file = File(File(context.filesDir, "captures").apply { mkdirs() },
        "MEMORIA_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg")
    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(o: ImageCapture.OutputFileResults) = onSuccess(file.absolutePath)
            override fun onError(e: ImageCaptureException) = onError(when (e.imageCaptureError) {
                ImageCapture.ERROR_CAMERA_CLOSED  -> "Cámara cerrada, vuelve a intentarlo"
                ImageCapture.ERROR_FILE_IO         -> "Error al guardar la imagen"
                ImageCapture.ERROR_CAPTURE_FAILED  -> "Captura fallida, inténtalo de nuevo"
                else                               -> "Error: ${e.message}"
            })
        }
    )
}

@Composable
fun PermissionRationale(message: String, onRequest: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Card(Modifier.padding(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", fontSize = 48.sp); Spacer(Modifier.height(16.dp))
                Text("Permiso de Cámara", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp))
                Text(message); Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onBack) { Text("Cancelar") }
                    Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = MemorIAColors.IndigoAccent)) { Text("Permitir") }
                }
            }
        }
    }
}

@Composable
fun PermissionDenied(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚫", fontSize = 48.sp); Spacer(Modifier.height(16.dp))
            Text("Permiso denegado", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp))
            Text("Activa el permiso de cámara en Ajustes del dispositivo.")
            Spacer(Modifier.height(20.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MemorIAColors.IndigoAccent)) { Text("Volver") }
        }
    }
}