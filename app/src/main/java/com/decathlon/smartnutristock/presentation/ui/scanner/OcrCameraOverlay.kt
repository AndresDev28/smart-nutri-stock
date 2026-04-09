package com.decathlon.smartnutristock.presentation.ui.scanner

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.decathlon.smartnutristock.domain.usecase.DateExtractorUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val FRAME_SKIP = 3
private const val DEBOUNCE_MS = 500L
private const val TIMEOUT_MS = 5000L

@Composable
fun OcrCameraOverlay(
    onDateDetected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    dateExtractorUseCase: DateExtractorUseCase,
    onManualInput: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var detectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var showTimeout by remember { mutableStateOf(false) }
    var isModelLoading by remember { mutableStateOf(false) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
        }
    }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val frameCount = remember { AtomicInteger(0) }
    val lastProcessTime = remember { AtomicLong(0L) }

    LaunchedEffect(Unit) {
        delay(TIMEOUT_MS)
        if (detectedDate == null) {
            showTimeout = true
            isScanning = false
        }
    }

    DisposableEffect(key1 = Unit) {
        cameraController.bindToLifecycle(lifecycleOwner)

        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            val now = System.currentTimeMillis()
            val count = frameCount.incrementAndGet()

            if (count % FRAME_SKIP != 0 || (now - lastProcessTime.get()) < DEBOUNCE_MS) {
                imageProxy.close()
                return@setImageAnalysisAnalyzer
            }
            lastProcessTime.set(now)

            processOcrFrame(
                imageProxy = imageProxy,
                textRecognizer = textRecognizer,
                dateExtractorUseCase = dateExtractorUseCase,
                onDateDetected = { date ->
                    mainHandler.post {
                        if (detectedDate == null) {
                            detectedDate = date
                            isScanning = false
                            showTimeout = false
                        }
                    }
                },
                onModelLoading = {
                    mainHandler.post { isModelLoading = true }
                },
                onModelReady = {
                    mainHandler.post { isModelLoading = false }
                }
            )
        }

        onDispose {
            try {
                cameraController.unbind()
            } catch (e: Exception) {
                Log.e("OcrCameraOverlay", "Error unbinding camera controller", e)
            }
            try {
                cameraController.clearImageAnalysisAnalyzer()
            } catch (e: Exception) {
                Log.e("OcrCameraOverlay", "Error clearing analyzer", e)
            }
            try {
                textRecognizer.close()
            } catch (e: Exception) {
                Log.e("OcrCameraOverlay", "Error closing text recognizer", e)
            }
            try {
                executor.shutdown()
            } catch (e: Exception) {
                Log.e("OcrCameraOverlay", "Error shutting down executor", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .align(Alignment.TopCenter)
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 8.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Escanear fecha",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isModelLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Descargando modelo de OCR...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                isScanning && detectedDate == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Buscando fecha...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                showTimeout && detectedDate == null -> {
                    Text(
                        text = "No se detect\u00f3 fecha. Ingrese manualmente.",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onManualInput?.invoke() ?: onDismiss() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ingresar fecha manualmente")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cerrar", color = Color.White)
                    }
                }
                detectedDate != null -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Fecha detectada",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DateTimeFormatter
                                    .ofPattern("dd/MM/yyyy")
                                    .format(detectedDate),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { detectedDate?.let(onDateDetected) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Confirmar fecha")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UnsafeOptInUsageError")
private fun processOcrFrame(
    imageProxy: ImageProxy,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    dateExtractorUseCase: DateExtractorUseCase,
    onDateDetected: (LocalDate) -> Unit,
    onModelLoading: () -> Unit,
    onModelReady: () -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onModelReady()
                val rawText = visionText.textBlocks.joinToString(" ") { it.text }
                if (rawText.isNotBlank()) {
                    val date = dateExtractorUseCase(rawText)
                    if (date != null) {
                        onDateDetected(date)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("OcrCameraOverlay", "OCR processing failed", e)
                if (e.message?.contains("download", ignoreCase = true) == true) {
                    onModelLoading()
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
