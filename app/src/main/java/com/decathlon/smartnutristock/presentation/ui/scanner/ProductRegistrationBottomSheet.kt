package com.decathlon.smartnutristock.presentation.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decathlon.smartnutristock.domain.model.Batch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Bottom Sheet for product registration and batch editing.
 *
 * Features:
 * - imePadding() for keyboard handling
 * - Auto-focus on first field based on mode
 * - Tap targets 56dp+ (thumb zone optimized for XCover7)
 * - CREATE mode: Validation - Name (3-100 chars), Pack Size (positive)
 * - EDIT mode: Validation - Quantity (positive), Expiry Date (future date)
 * - Buttons: Cancel (bottom-left), Save (bottom-right)
 * - Keyboard-driven UX (Done action submits)
 *
 * Modes:
 * - CREATE: Register a new product when not found in catalog
 * - EDIT: Edit an existing batch (from History screen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductRegistrationBottomSheet(
    mode: BottomSheetMode = BottomSheetMode.CREATE,
    existingBatch: Batch? = null,
    ean: String,
    onDismiss: () -> Unit,
    onRegister: ((productName: String, packSize: Int) -> Unit)? = null,
    onSave: ((barcode: String, quantity: Int, expiryDate: LocalDate, mode: BottomSheetMode, batchId: String?, productName: String?) -> Unit)? = null
) {
    // State for product registration (CREATE mode)
    var productName by remember { mutableStateOf("") }
    var packSize by remember { mutableStateOf("") }

    // State for batch editing (EDIT mode)
    var quantity by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf<LocalDate?>(null) }

    // Focus request for auto-focus
    val focusRequester = remember { FocusRequester() }

    // DatePicker state
    val showDatePicker = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = expiryDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Only allow future dates (no past dates for expiry)
                return utcTimeMillis >= System.currentTimeMillis()
            }
        }
    )

    // Pre-fill logic for EDIT mode
    LaunchedEffect(existingBatch) {
        if (mode == BottomSheetMode.EDIT && existingBatch != null) {
            // Pre-fill quantity field
            quantity = existingBatch.quantity.toString()

            // Pre-fill expiry date field (convert Instant to LocalDate)
            expiryDate = existingBatch.expiryDate
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()

            // Pre-fill product name (if available)
            existingBatch.name?.let { productName = it }

            // Note: EAN is passed as parameter, no need to pre-fill
            // Note: Pack Size is not used in EDIT mode
        }
    }

    LaunchedEffect(Unit) {
        // Auto-focus on first field when Bottom Sheet opens
        // In CREATE mode: focus on Product Name
        // In EDIT mode: focus on Quantity
        focusRequester.requestFocus()
    }

    // Validation states for CREATE mode
    var nameError by remember { mutableStateOf<String?>(null) }
    var packSizeError by remember { mutableStateOf<String?>(null) }

    // Validation states for EDIT mode
    var quantityError by remember { mutableStateOf<String?>(null) }
    var expiryDateError by remember { mutableStateOf<String?>(null) }

    // Validate name in real-time (both CREATE and EDIT modes)
    LaunchedEffect(productName) {
        nameError = when {
            productName.isEmpty() -> null
            productName.length < 3 -> "Mínimo 3 caracteres"
            productName.length > 100 -> "Máximo 100 caracteres"
            else -> null
        }
    }

    // Validate packSize in real-time (CREATE mode only)
    LaunchedEffect(packSize) {
        packSizeError = when {
            mode == BottomSheetMode.EDIT -> null // Not used in EDIT mode
            packSize.isEmpty() -> null
            packSize.toIntOrNull() == null -> "Debe ser un número"
            packSize.toInt() <= 0 -> "Debe ser positivo"
            else -> null
        }
    }

    // Validate quantity in real-time (EDIT mode only)
    LaunchedEffect(quantity) {
        quantityError = when {
            mode == BottomSheetMode.CREATE -> null // Not used in CREATE mode
            quantity.isEmpty() -> null
            quantity.toIntOrNull() == null -> "Debe ser un número"
            quantity.toInt() <= 0 -> "Debe ser positivo"
            else -> null
        }
    }

    // Validate expiry date in real-time (EDIT mode only)
    LaunchedEffect(expiryDate) {
        val date = expiryDate
        expiryDateError = when {
            mode == BottomSheetMode.CREATE -> null // Not used in CREATE mode
            date == null -> null
            date.isBefore(LocalDate.now()) -> "La fecha debe ser futura"
            else -> null
        }
    }

    // Check if form is valid
    val isFormValid = when (mode) {
        BottomSheetMode.CREATE -> {
            nameError == null &&
            packSizeError == null &&
            productName.isNotBlank() &&
            packSize.isNotBlank()
        }
        BottomSheetMode.EDIT -> {
            nameError == null &&
            quantityError == null &&
            expiryDateError == null &&
            productName.isNotBlank() &&
            quantity.isNotBlank() &&
            expiryDate != null
        }
    }

    Surface(
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title based on mode
            Text(
                text = if (mode == BottomSheetMode.CREATE) "Registrar Nuevo Producto" else "Editar Lote",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // EAN field (read-only)
            OutlinedTextField(
                value = ean,
                onValueChange = { },
                label = { Text("Código EAN") },
                enabled = false,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ean_field"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name (auto-focused in CREATE mode, editable in EDIT mode)
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Nombre del producto") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                enabled = mode == BottomSheetMode.CREATE || mode == BottomSheetMode.EDIT,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("name_field"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pack Size (CREATE mode only)
            if (mode == BottomSheetMode.CREATE) {
                OutlinedTextField(
                    value = packSize,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                            packSize = it
                        }
                    },
                    label = { Text("Tamaño del pack (gr)") },
                    isError = packSizeError != null,
                    supportingText = packSizeError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pack_size_field"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (mode == BottomSheetMode.CREATE) ImeAction.Next else ImeAction.Done
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quantity (EDIT mode only)
            if (mode == BottomSheetMode.EDIT) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                            quantity = it
                        }
                    },
                    label = { Text("Cantidad") },
                    isError = quantityError != null,
                    supportingText = quantityError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("quantity_field"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Expiry Date (EDIT mode only)
            if (mode == BottomSheetMode.EDIT) {
                val expiryDateText = expiryDate?.let { date ->
                    DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date)
                } ?: ""

                OutlinedTextField(
                    value = expiryDateText,
                    onValueChange = { },
                    label = { Text("Fecha de vencimiento") },
                    isError = expiryDateError != null,
                    supportingText = expiryDateError?.let { { Text(it) } },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expiry_date_field")
                        .clickable { showDatePicker.value = true },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons (thumb zone optimized - 56dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cancel button (bottom-left)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp), // Thumb zone!
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "Cancelar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Save button (bottom-right)
                Button(
                    onClick = {
                        if (isFormValid) {
                            when (mode) {
                                BottomSheetMode.CREATE -> {
                                    // Use onRegister callback for CREATE mode (backward compatibility)
                                    onRegister?.invoke(productName, packSize.toInt())
                                    // Also call onSave if provided
                                    onSave?.invoke(
                                        ean,
                                        packSize.toInt(),
                                        LocalDate.now().plusDays(30), // Default expiry
                                        mode,
                                        null,
                                        productName
                                    )
                                }
                                BottomSheetMode.EDIT -> {
                                    // Use onSave callback for EDIT mode
                                    val expiryDateVal = expiryDate ?: LocalDate.now()
                                    onSave?.invoke(
                                        ean,
                                        quantity.toInt(),
                                        expiryDateVal,
                                        mode,
                                        existingBatch?.id,
                                        productName
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp) // Thumb zone!
                        .testTag("save_button"),
                    enabled = isFormValid
                ) {
                    Text(
                        text = if (mode == BottomSheetMode.CREATE) "Registrar" else "Guardar cambios",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // DatePickerDialog for expiry date (EDIT mode only)
        if (showDatePicker.value && mode == BottomSheetMode.EDIT) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker.value = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val instant = Instant.ofEpochMilli(millis)
                                expiryDate = instant.atZone(ZoneId.of("UTC")).toLocalDate()
                            }
                            showDatePicker.value = false
                        }
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker.value = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                androidx.compose.material3.DatePicker(state = datePickerState)
            }
        }
    }
}
