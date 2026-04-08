package com.decathlon.smartnutristock.di

import com.decathlon.smartnutristock.data.export.CsvExporterImpl
import com.decathlon.smartnutristock.data.export.PdfExporterImpl
import com.decathlon.smartnutristock.domain.export.DocumentExporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for export functionality.
 *
 * This module provides the export implementations (CSV and PDF) and binds them
 * to the DocumentExporter interface using @Named qualifiers.
 *
 * The ExportInventoryUseCase can inject the exporters with @Named("csv") and
 * @Named("pdf") qualifiers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    /**
     * Bind CSV exporter implementation to DocumentExporter interface.
     *
     * @param csvExporter The CSV exporter implementation
     * @return DocumentExporter instance for CSV format
     */
    @Binds
    @Named("csv")
    abstract fun bindCsvExporter(csvExporter: CsvExporterImpl): DocumentExporter

    /**
     * Bind PDF exporter implementation to DocumentExporter interface.
     *
     * @param pdfExporter The PDF exporter implementation
     * @return DocumentExporter instance for PDF format
     */
    @Binds
    @Named("pdf")
    abstract fun bindPdfExporter(pdfExporter: PdfExporterImpl): DocumentExporter
}
