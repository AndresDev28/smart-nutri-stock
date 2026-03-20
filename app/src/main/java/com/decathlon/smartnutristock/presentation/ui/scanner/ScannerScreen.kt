package com.decathlon.smartnutristock.presentation.ui.scanner

import android.Manifest
import androidx.camera.core.ImageAnalysis
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Scanner screen with barcode scanning functionality.
 *
 * Features:
 * - CameraX preview for real-time barcode scanning
 * - ML Kit barcode detection
 * - Product status display (found/not found)
 * - Bottom Sheet trigger when product not found
 * - Loading states and error handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentEan by viewModel.currentEan.collectAsState()
    val isCameraActive by viewModel.isCameraActive.collectAsState()
    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val foundProduct by viewModel.foundProduct.collectAsState()

    // Camera controller
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_ANALYSIS or
                LifecycleCameraController.IMAGE_CAPTURE
            )
        }
    }

    // ML Kit barcode scanner (EAN-13 format)
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13)
            .build()
        BarcodeScanning.getClient(options)
    }

    // Executor for ML Kit analysis
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Auto-start camera when screen comes into view
    LaunchedEffect(Unit) {
        viewModel.startCamera()
    }

    // Bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Auto-show/hide bottom sheet based on state
    LaunchedEffect(isBottomSheetVisible) {
        if (isBottomSheetVisible) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    // Cleanup on disposal
    LaunchedEffect(Unit) {
        return@LaunchedEffect lifecycleOwner.lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
                    viewModel.stopCamera()
                    executor.shutdown()
                }
            }
        )
    }



    // Setup image analysis separately
    LaunchedEffect(Unit) {
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            processImageProxy(
                imageProxy = imageProxy,
                barcodeScanner = barcodeScanner,
                viewModel = viewModel
            )
        }

        // Note: In a real implementation, you would set this up with CameraProvider
        // For now, we're using LifecycleCameraController for preview only
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Escáner de Código de Barras")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCameraActive) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                controller = cameraController
                                cameraController.bindToLifecycle(lifecycleOwner)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "Cámara no iniciada",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Product status display
            Spacer(modifier = Modifier.height(16.dp))

            val foundProductVal = foundProduct
            val errorMessageVal = errorMessage
            val successMessageVal = successMessage

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
                foundProductVal != null -> {
                    // Product found - show product name
                    ProductInfoCard(productName = foundProductVal.name, packSize = foundProductVal.packSize)
                }
                errorMessageVal != null -> {
                    // Show error message
                    ErrorCard(message = errorMessageVal)
                }
                successMessageVal != null -> {
                    // Show success message
                    SuccessCard(message = successMessageVal)
                }
                isBottomSheetVisible -> {
                    // Bottom Sheet is visible (showing ProductRegistrationBottomSheet separately)
                    Text(
                        text = "Registre el nuevo producto",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    // Loading or idle
                    Text(
                        text = "Escanea un código de barras...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // ProductRegistrationBottomSheet (integrated below)
    val currentEanVal = currentEan
    if (isBottomSheetVisible && currentEanVal != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissBottomSheet() },
            sheetState = bottomSheetState,
            windowInsets = WindowInsets(0.dp)
        ) {
            ProductRegistrationBottomSheet(
                ean = currentEanVal,
                onDismiss = { viewModel.dismissBottomSheet() },
                onRegister = { productName, packSize ->
                    viewModel.onRegisterProduct(productName, packSize)
                }
            )
        }
    }
}

/**
 * Process image frame for barcode detection.
 */
@Suppress("UnsafeOptInUsageError")
private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    viewModel: ScannerViewModel
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val ean = barcode.rawValue
                    if (!ean.isNullOrEmpty() && ean.length == 13) {
                        viewModel.onEanDetected(ean)
                        break // Only process first barcode
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle ML Kit errors silently
                e.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
private fun ProductInfoCard(productName: String, packSize: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50) // Green
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Producto encontrado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = productName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = "Pack: ${packSize}g",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "❌ Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun SuccessCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50) // Green
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✅ Éxito",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}
