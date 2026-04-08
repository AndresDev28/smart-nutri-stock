package com.decathlon.smartnutristock.presentation.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.domain.export.ExportFormat

/**
 * Modal bottom sheet for selecting export format.
 *
 * Displays two options for exporting inventory reports: CSV and PDF formats.
 * Optimized for Samsung XCover7 with 48dp minimum tap targets.
 *
 * @param onFormatSelected Callback when a format is selected (CSV or PDF)
 * @param onDismiss Callback when the sheet is dismissed or cancelled
 * @param modifier Modifier for the sheet content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFormatDialog(
    onFormatSelected: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = "Exportar Reporte",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Selecciona el formato de exportación",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CSV Option
            FilledTonalButton(
                onClick = { onFormatSelected(ExportFormat.CSV) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("📄 Exportar CSV")
            }

            // PDF Option
            FilledTonalButton(
                onClick = { onFormatSelected(ExportFormat.PDF) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("📑 Exportar PDF")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cancel Button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
