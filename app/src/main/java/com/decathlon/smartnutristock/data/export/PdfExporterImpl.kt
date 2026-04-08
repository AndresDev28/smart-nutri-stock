package com.decathlon.smartnutristock.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.decathlon.smartnutristock.domain.export.DocumentExporter
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * PDF exporter implementation using Android's native PdfDocument and Canvas API.
 *
 * This exporter generates A4 portrait PDF documents with:
 * - Professional tabular layout
 * - Status color coding (GREEN/YELLOW/EXPIRED)
 * - Automatic pagination after 30 rows
 * - Report header with timestamp
 * - Reasonable file size for small datasets
 *
 * @constructor Creates a new PDF exporter instance
 */
class PdfExporterImpl @Inject constructor() : DocumentExporter {

    companion object {
        // A4 dimensions in points (1/72 inch)
        private const val PAGE_WIDTH = 595f
        private const val PAGE_HEIGHT = 842f

        // Layout constants
        private const val MARGIN = 40f
        private const val LINE_HEIGHT = 20f
        private const val HEADER_HEIGHT = 60f
        private const val CONTENT_START_Y = MARGIN + HEADER_HEIGHT
        private const val ROWS_PER_PAGE = 30

        // Column widths
        private const val COL_ID = 40f
        private const val COL_EAN = 80f
        private const val COL_NAME = 150f
        private const val COL_PACK = 40f
        private const val COL_QTY = 40f
        private const val COL_EXPIRY = 60f
        private const val COL_STATUS = 50f
        private const val COL_ACTION = 55f

        // Date formatters
        private val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("UTC"))
        private val EXPIRY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("UTC"))
        private val FILENAME_TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.of("UTC"))
    }

    override suspend fun export(data: List<Batch>, context: Context): File {
        // Create exports subdirectory in cache
        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }

        // Generate filename with timestamp
        val timestamp = FILENAME_TIMESTAMP_FORMATTER.format(Instant.now())
        val file = File(exportsDir, "smart_nutri_stock_report_${timestamp}.pdf")

        // Create PDF document
        val document = PdfDocument()

        // Setup first page
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = CONTENT_START_Y
        var currentPage = 1

        // Paint objects
        val headerPaint = createHeaderPaint()
        val normalPaint = createNormalPaint()
        val gridPaint = createGridPaint()
        val backgroundPaint = createBackgroundPaint()

        // Draw header
        drawHeader(canvas, currentPage, headerPaint, normalPaint)

        // Draw table headers
        drawTableHeaders(canvas, currentY, headerPaint)
        currentY += LINE_HEIGHT * 1.5f

        // Separator line
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, gridPaint)
        currentY += 10f

        // Draw data rows
        data.forEachIndexed { index, batch ->
            // Check if we need a new page
            if (index > 0 && index % ROWS_PER_PAGE == 0) {
                // Finish current page
                document.finishPage(page)

                // Start new page
                currentPage++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = CONTENT_START_Y

                // Draw header on new page
                drawHeader(canvas, currentPage, headerPaint, normalPaint)

                // Draw table headers again
                drawTableHeaders(canvas, currentY, headerPaint)
                currentY += LINE_HEIGHT * 1.5f
                canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, gridPaint)
                currentY += 10f
            }

            // Alternating row background
            if ((index % ROWS_PER_PAGE) % 2 == 0) {
                canvas.drawRect(
                    RectF(MARGIN, currentY - 5f, PAGE_WIDTH - MARGIN, currentY + 10f),
                    backgroundPaint
                )
            }

            // Draw row data
            drawDataRow(canvas, batch, currentY, normalPaint)
            currentY += LINE_HEIGHT
        }

        // Finish last page
        document.finishPage(page)

        // Write to file
        val outputStream = FileOutputStream(file)
        document.writeTo(outputStream)
        document.close()
        outputStream.close()

        return file
    }

    private fun createHeaderPaint(): Paint {
        return Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }
    }

    private fun createNormalPaint(): Paint {
        return Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    private fun createGridPaint(): Paint {
        return Paint().apply {
            color = Color.GRAY
            strokeWidth = 0.5f
        }
    }

    private fun createBackgroundPaint(): Paint {
        return Paint().apply {
            color = Color.parseColor("#F5F5F5")
        }
    }

    private fun drawHeader(canvas: android.graphics.Canvas, pageNum: Int, headerPaint: Paint, normalPaint: Paint) {
        // Header background
        canvas.drawRect(
            RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + HEADER_HEIGHT),
            Paint().apply { color = Color.parseColor("#E3F2FD") }
        )

        // Store name + timestamp
        val timestampText = TIMESTAMP_FORMATTER.format(Instant.now())
        canvas.drawText(
            "Smart Nutri-Stock - Reporte de Inventario",
            MARGIN,
            MARGIN + 20f,
            headerPaint
        )
        canvas.drawText(
            "Generado: $timestampText | Página $pageNum",
            MARGIN,
            MARGIN + 45f,
            normalPaint
        )
    }

    private fun drawTableHeaders(canvas: android.graphics.Canvas, y: Float, headerPaint: Paint) {
        val adjustedPaint = Paint(headerPaint).apply { textSize = 11f }

        canvas.drawText("ID", MARGIN, y, adjustedPaint)
        canvas.drawText("EAN", MARGIN + COL_ID, y, adjustedPaint)
        canvas.drawText("Nombre", MARGIN + COL_ID + COL_EAN, y, adjustedPaint)
        canvas.drawText("Pack", MARGIN + COL_ID + COL_EAN + COL_NAME, y, adjustedPaint)
        canvas.drawText("Cant.", MARGIN + COL_ID + COL_EAN + COL_NAME + COL_PACK, y, adjustedPaint)
        canvas.drawText("Vence", MARGIN + COL_ID + COL_EAN + COL_NAME + COL_PACK + COL_QTY, y, adjustedPaint)
        canvas.drawText("Estado", MARGIN + COL_ID + COL_EAN + COL_NAME + COL_PACK + COL_QTY + COL_EXPIRY, y, adjustedPaint)
        canvas.drawText("Acción", MARGIN + COL_ID + COL_EAN + COL_NAME + COL_PACK + COL_QTY + COL_EXPIRY + COL_STATUS, y, adjustedPaint)
    }

    private fun drawDataRow(canvas: android.graphics.Canvas, batch: Batch, y: Float, normalPaint: Paint) {
        // ID (truncated)
        val idText = batch.id.take(8) + "..."
        canvas.drawText(idText, MARGIN, y, normalPaint)

        // EAN
        canvas.drawText(batch.ean, MARGIN + COL_ID, y, normalPaint)

        // Name (truncated if too long)
        val nameText = (batch.name ?: "N/A").take(20) + if (batch.name?.length ?: 0 > 20) "..." else ""
        canvas.drawText(nameText, MARGIN + COL_ID + COL_EAN, y, normalPaint)

        // Pack size
        val packText = "${batch.packSize ?: "-"}g"
        canvas.drawText(packText, MARGIN + COL_ID + COL_EAN + COL_NAME, y, normalPaint)

        // Quantity
        canvas.drawText(batch.quantity.toString(), MARGIN + COL_ID + COL_EAN + COL_NAME + COL_PACK, y, normalPaint)

        // Expiry date
        val expiryText = EXPIRY_FORMATTER.format(batch.expiryDate)
        canvas.drawText(expiryText, MARGIN + COL_ID + COL_EAN + COL_NAME + COL_PACK + COL_QTY, y, normalPaint)

        // Status with color coding
        val statusColor = getStatusColor(batch.status)
        canvas.drawText(
            batch.status.name,
            MARGIN + COL_ID + COL_EAN + COL_NAME + COL_PACK + COL_QTY + COL_EXPIRY,
            y,
            normalPaint.apply { color = statusColor }
        )

        // Reset paint color for next column
        normalPaint.color = Color.BLACK

        // Action
        canvas.drawText(
            batch.actionTaken.name,
            MARGIN + COL_ID + COL_EAN + COL_NAME + COL_PACK + COL_QTY + COL_EXPIRY + COL_STATUS,
            y,
            normalPaint
        )
    }

    private fun getStatusColor(status: SemaphoreStatus): Int {
        return when (status) {
            SemaphoreStatus.GREEN -> Color.parseColor("#4CAF50") // Green
            SemaphoreStatus.YELLOW -> Color.parseColor("#FFC107") // Yellow
            SemaphoreStatus.EXPIRED -> Color.parseColor("#F44336") // Red
        }
    }
}
