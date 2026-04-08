package com.decathlon.smartnutristock.data.export

import android.content.Context
import com.decathlon.smartnutristock.domain.export.DocumentExporter
import com.decathlon.smartnutristock.domain.model.Batch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * CSV exporter implementation following RFC 4180 standard.
 *
 * This exporter generates RFC 4180 compliant CSV files with:
 * - Semicolon separator (; for European locale)
 * - UTF-8 encoding with BOM for Excel compatibility
 * - Proper escaping of special characters (commas, quotes, semicolons, newlines)
 * - Headers in Spanish
 * - Date format: dd/MM/yyyy
 *
 * @constructor Creates a new CSV exporter instance
 */
class CsvExporterImpl @Inject constructor() : DocumentExporter {

    companion object {
        private const val CSV_SEPARATOR = ';'
        private const val BOM = '\uFEFF'
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("UTC"))
        private val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.of("UTC"))
    }

    override suspend fun export(data: List<Batch>, context: Context): File {
        // Create exports subdirectory in cache
        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }

        // Generate filename with timestamp
        val timestamp = TIMESTAMP_FORMATTER.format(Instant.now())
        val file = File(exportsDir, "smart_nutri_stock_report_${timestamp}.csv")

        // Write CSV file with UTF-8 BOM and proper escaping
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            // Write UTF-8 BOM for Excel compatibility
            writer.write(BOM.toString())

            // Write header row in Spanish
            writer.write(buildHeaderRow())

            // Write data rows
            data.forEach { batch ->
                writer.write(buildDataRow(batch))
                writer.newLine()
            }
        }

        return file
    }

    /**
     * Build the CSV header row with Spanish column names.
     */
    private fun buildHeaderRow(): String {
        return listOf(
            escapeCsvField("ID"),
            escapeCsvField("EAN"),
            escapeCsvField("Nombre"),
            escapeCsvField("Pack Size (g)"),
            escapeCsvField("Cantidad"),
            escapeCsvField("Fecha Vencimiento"),
            escapeCsvField("Estado"),
            escapeCsvField("Acción Tomada")
        ).joinToString(CSV_SEPARATOR.toString()) + "\n"
    }

    /**
     * Build a CSV data row for a batch.
     */
    private fun buildDataRow(batch: Batch): String {
        return listOf(
            escapeCsvField(batch.id),
            escapeCsvField(batch.ean),
            escapeCsvField(batch.name ?: "N/A"),
            escapeCsvField(batch.packSize?.toString() ?: "N/A"),
            escapeCsvField(batch.quantity.toString()),
            escapeCsvField(formatDate(batch.expiryDate)),
            escapeCsvField(batch.status.name),
            escapeCsvField(batch.actionTaken.name)
        ).joinToString(CSV_SEPARATOR.toString())
    }

    /**
     * Escape a CSV field according to RFC 4180.
     *
     * Rules:
     * - Wrap field in double quotes if it contains: separator, quote, or newline
     * - Escape internal double quotes by doubling them ("")
     *
     * @param field The field value to escape
     * @return Escaped field value
     */
    private fun escapeCsvField(field: String): String {
        val needsQuoting = field.contains(CSV_SEPARATOR) ||
                          field.contains('"') ||
                          field.contains('\n') ||
                          field.contains('\r')

        return if (needsQuoting) {
            // Wrap in quotes and escape internal quotes
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    /**
     * Format an Instant as dd/MM/yyyy string.
     */
    private fun formatDate(instant: Instant): String {
        return DATE_FORMATTER.format(instant)
    }
}
