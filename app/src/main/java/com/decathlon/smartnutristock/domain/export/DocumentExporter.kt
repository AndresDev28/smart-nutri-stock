package com.decathlon.smartnutristock.domain.export

import android.content.Context
import com.decathlon.smartnutristock.domain.model.Batch
import java.io.File

/**
 * Interface for document exporters (CSV, PDF).
 *
 * This contract allows different export implementations to be injected
 * via Hilt, following the Dependency Inversion Principle.
 *
 * Implementations must handle file generation in the app's cache directory,
 * requiring no storage permissions due to Scoped Storage (API 34).
 */
interface DocumentExporter {
    /**
     * Export batches to a document file.
     *
     * This is a suspend function because file I/O operations should
     * be performed off the main thread using coroutines.
     *
     * @param data List of batches to export
     * @param context Android context for accessing cache directory
     * @return File containing the exported document (CSV or PDF)
     * @throws Exception if file generation fails
     */
    suspend fun export(data: List<Batch>, context: Context): File
}
