package com.decathlon.smartnutristock.presentation.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.util.Log
import kotlinx.coroutines.launch
import com.decathlon.smartnutristock.domain.usecase.DateExtractorUseCase
import com.decathlon.smartnutristock.presentation.ui.components.PremiumButton
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

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
    val batchInputState by viewModel.batchInputState.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val currentProductInfo by viewModel.currentProductInfo.collectAsState()

    // Haptic feedback for scan confirmation
    val view = LocalView.current

    // Trigger haptic feedback when EAN is detected (new scan)
    LaunchedEffect(currentEan) {
        val ean = currentEan
        if (ean != null && ean.isNotEmpty()) {
            // CONFIRM on valid EAN detection
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    // Trigger haptic feedback on error
    LaunchedEffect(errorMessage) {
        val errorMsg = errorMessage
        if (errorMsg != null && errorMsg.isNotEmpty()) {
            // REJECT on error/duplicate
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }

    // Quantity input state (local)
    var quantityInput by remember { mutableStateOf("") }
    var quantityError by remember { mutableStateOf<String?>(null) }

    // OCR state
    var showOcrCamera by remember { mutableStateOf(false) }
    var isCameraReleased by remember { mutableStateOf(false) }
    val dateExtractorUseCase = remember { DateExtractorUseCase() }

    // Camera permission for OCR
    var showPermissionDenied by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showOcrCamera = true
        } else {
            showPermissionDenied = true
        }
    }

    // Camera controller
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_ANALYSIS or
                LifecycleCameraController.IMAGE_CAPTURE
            )
        }
    }

    // ML Kit barcode scanner (EAN-13 and EAN-8 formats)
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13 or Barcode.FORMAT_EAN_8
            )
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



    // Setup image analysis - connect to LifecycleCameraController
    LaunchedEffect(cameraController) {
        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            Log.d("Scanner", "Procesando frame...")
            processImageProxy(
                imageProxy = imageProxy,
                barcodeScanner = barcodeScanner,
                viewModel = viewModel
            )
        }
    }

    // Release/rebind camera hardware when OCR overlay opens/closes
    LaunchedEffect(showOcrCamera) {
        if (showOcrCamera) {
            cameraController.unbind()
            isCameraReleased = true
        } else {
            isCameraReleased = false
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
                Log.d("Scanner", "Procesando frame...")
                processImageProxy(
                    imageProxy = imageProxy,
                    barcodeScanner = barcodeScanner,
                    viewModel = viewModel
                )
            }
        }
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
            // Camera Preview with Immersive Blur Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCameraActive) {
                    // Camera preview
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                controller = cameraController
                                cameraController.bindToLifecycle(lifecycleOwner)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Blur Guardrail — REQUIRED
                    // API 31+ (Android 12+): Use Modifier.blur()
                    // API < 31: Fallback to solid dark overlay
                    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(20.dp)
                    } else {
                        Modifier.background(Color.Black.copy(alpha = 0.4f))
                    }

                    // Blur overlay with clear center window for barcode scanning
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top edge blur (above center)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(150.dp)
                                .then(blurModifier)
                        )

                        // Bottom edge blur (below center)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(150.dp)
                                .then(blurModifier)
                        )

                        // Left edge blur (left of center window - ~15%)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(60.dp)
                                .fillMaxHeight()
                                .then(blurModifier)
                        )

                        // Right edge blur (right of center window - ~15%)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(60.dp)
                                .fillMaxHeight()
                                .then(blurModifier)
                        )

                        // Center scan window (70% of screen width) - clear for barcode scanning
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(280.dp) // 70% of 400dp screen width
                                .fillMaxHeight()
                                .background(Color.Transparent)
                        )
                    }
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
                mode = BottomSheetMode.CREATE,
                existingBatch = null,
                ean = currentEanVal,
                onDismiss = { viewModel.dismissBottomSheet() },
                onRegister = { productName, packSize ->
                    viewModel.onRegisterProduct(productName, packSize)
                }
            )
        }
    }

    // Batch Input Dialogs
    val productInfoVal = currentProductInfo

    // DatePickerDialog for expiry date
    if (batchInputState is BatchInputStep.SelectExpiryDate && productInfoVal != null && !showOcrCamera) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= System.currentTimeMillis()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { viewModel.onCancelBatchInput() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.ofEpochMilli(millis)
                            viewModel.onExpiryDateSelected(instant)
                        } ?: viewModel.onCancelBatchInput()
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                Row {
                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                showOcrCamera = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Escanear Fecha",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    TextButton(onClick = { viewModel.onCancelBatchInput() }) {
                        Text("Cancelar")
                    }
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.testTag("date_picker")
            )
        }
    }

    // OCR Camera Overlay for SelectExpiryDate step (gated on camera release)
    if (showOcrCamera && isCameraReleased && batchInputState is BatchInputStep.SelectExpiryDate) {
        OcrCameraOverlay(
            onDateDetected = { date: LocalDate ->
                val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                viewModel.onExpiryDateSelected(instant)
                showOcrCamera = false
            },
            onDismiss = { showOcrCamera = false },
            dateExtractorUseCase = dateExtractorUseCase,
            onManualInput = {
                showOcrCamera = false
            }
        )
    }

    // Permission denied dialog
    if (showPermissionDenied) {
        AlertDialog(
            onDismissRequest = { showPermissionDenied = false },
            title = { Text("Permiso de cámara necesario") },
            text = { Text("El escaneo OCR requiere acceso a la cámara. Puede ingresar la fecha manualmente.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDenied = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Quantity Input Dialog
    if (batchInputState is BatchInputStep.EnterQuantity && productInfoVal != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onCancelBatchInput() },
            modifier = Modifier.testTag("quantity_dialog"),
            title = {
                Text(text = "Ingresar Cantidad")
            },
            text = {
                Column {
                    Text(
                        text = "Producto: ${productInfoVal.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vencimiento: ${expiryDate?.let { 
                            ZonedDateTime.ofInstant(it, ZoneId.systemDefault()).toLocalDate()
                        }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = {
                            quantityInput = it
                            quantityError = null
                        },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = quantityError != null,
                        supportingText = quantityError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quantity_input_field")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val qty = quantityInput.toIntOrNull()
                        if (qty != null && qty > 0) {
                            viewModel.onQuantityEntered(qty)
                            quantityInput = ""
                        } else {
                            quantityError = "Ingrese una cantidad válida"
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onCancelBatchInput()
                    quantityInput = ""
                    quantityError = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmation Dialog
    if (batchInputState is BatchInputStep.Confirming && productInfoVal != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onCancelBatchInput() },
            title = {
                Text(text = "Confirmar Lote")
            },
            text = {
                Column {
                    Text(text = "Producto: ${productInfoVal.name}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "EAN: ${productInfoVal.ean}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Vencimiento: ${expiryDate?.let { 
                        ZonedDateTime.ofInstant(it, ZoneId.systemDefault()).toLocalDate()
                    }}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Cantidad: $quantity")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onConfirmBatch() }) {
                    Text("Guardar Lote")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCancelBatchInput() }) {
                    Text("Cancelar")
                }
            }
        )
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
                    if (!ean.isNullOrEmpty() && (ean.length == 13 || ean.length == 8)) {
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
            containerColor = MaterialTheme.colorScheme.primary
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
            containerColor = MaterialTheme.colorScheme.primary
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
