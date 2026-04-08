package com.decathlon.smartnutristock.domain.usecase

import android.content.Context
import com.decathlon.smartnutristock.domain.export.DocumentExporter
import com.decathlon.smartnutristock.domain.export.ExportFormat
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.repository.StockRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import javax.inject.Inject
import javax.inject.Named

/**
 * Use case for exporting inventory to CSV or PDF format.
 *
 * This use case orchestrates the export process by:
 * 1. Fetching all non-deleted batches from the repository
 * 2. Delegating to the appropriate exporter based on the selected format
 * 3. Returning a Result containing the generated file or an exception
 *
 * The export is performed on Dispatchers.IO to avoid blocking the main thread.
 *
 * @property stockRepository Repository for fetching batch data
 * @property csvExporter CSV exporter implementation
 * @property pdfExporter PDF exporter implementation
 * @property context Android context for accessing cache directory
 */
class ExportInventoryUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    @Named("csv") private val csvExporter: DocumentExporter,
    @Named("pdf") private val pdfExporter: DocumentExporter,
    @ApplicationContext private val context: Context
) {
    /**
     * Export inventory to the specified format.
     *
     * This method fetches all batches (including soft-deleted ones) and delegates
     * to the appropriate exporter. The exporter is responsible for filtering out
     * deleted batches if needed.
     *
     * @param format The export format (CSV or PDF)
     * @return Result containing the generated file, or a failure with exception
     */
    suspend operator fun invoke(format: ExportFormat): Result<java.io.File> {
        return try {
            // Collect all batches from the repository Flow
            val batches = stockRepository.findAllWithProductInfo().toList()

            // Get the appropriate exporter for the requested format
            val exporter = when (format) {
                ExportFormat.CSV -> csvExporter
                ExportFormat.PDF -> pdfExporter
            }

            // Delegate to the exporter to generate the file
            val file = exporter.export(batches, context)

            Result.success(file)
        } catch (e: Exception) {
            // Catch any exception and return as failure
            Result.failure(e)
        }
    }
}
