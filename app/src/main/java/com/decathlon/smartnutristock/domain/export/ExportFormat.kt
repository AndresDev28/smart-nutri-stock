package com.decathlon.smartnutristock.domain.export

/**
 * Export format enum for inventory reports.
 *
 * @property CSV Comma-separated values format (RFC 4180 compliant)
 * @property PDF Portable Document Format (A4 portrait, Canvas API)
 */
enum class ExportFormat {
    CSV,
    PDF
}
