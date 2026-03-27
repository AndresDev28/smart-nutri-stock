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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * Bottom Sheet for quick product registration when product not found.
 *
 * Features:
 * - imePadding() for keyboard handling
 * - Auto-focus on Product Name field (first field)
 * - Tap targets 56dp+ (thumb zone optimized for XCover7)
 * - Validation: Name (3-100 chars), Pack Size (positive)
 * - Buttons: Cancel (bottom-left), Save (bottom-right)
 * - Keyboard-driven UX (Done action submits)
 */
@Composable
fun ProductRegistrationBottomSheet(
    ean: String,
    onDismiss: () -> Unit,
    onRegister: (productName: String, packSize: Int) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var packSize by remember { mutableStateOf("") }

    // Focus request for auto-focus
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Auto-focus on Product Name when Bottom Sheet opens
        focusRequester.requestFocus()
    }

    // Validation states
    var nameError by remember { mutableStateOf<String?>(null) }
    var packSizeError by remember { mutableStateOf<String?>(null) }

    // Validate name in real-time
    LaunchedEffect(productName) {
        nameError = when {
            productName.isEmpty() -> null
            productName.length < 3 -> "Mínimo 3 caracteres"
            productName.length > 100 -> "Máximo 100 caracteres"
            else -> null
        }
    }

    // Validate packSize in real-time
    LaunchedEffect(packSize) {
        packSizeError = when {
            packSize.isEmpty() -> null
            packSize.toIntOrNull() == null -> "Debe ser un número"
            packSize.toInt() <= 0 -> "Debe ser positivo"
            else -> null
        }
    }

    // Check if form is valid
    val isFormValid = nameError == null &&
            packSizeError == null &&
            productName.isNotBlank() &&
            packSize.isNotBlank()

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
            // Title
            Text(
                text = "Registrar Nuevo Producto",
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

            // Product Name (auto-focused)
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Nombre del producto") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
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

            // Pack Size
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
                    imeAction = ImeAction.Done
                )
            )

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
                            onRegister(productName, packSize.toInt())
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp) // Thumb zone!
                        .testTag("save_button"),
                    enabled = isFormValid
                ) {
                    Text(
                        text = "Guardar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
