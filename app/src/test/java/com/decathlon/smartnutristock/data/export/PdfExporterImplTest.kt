package com.decathlon.smartnutristock.data.export

import org.junit.Ignore
import org.junit.Test

/**
 * PDF exporter tests.
 *
 * NOTE: These tests require Android runtime (PdfDocument class cannot be mocked in JVM tests).
 * Tests are skipped (@Ignore) and should be run as instrumentation tests on an Android device/emulator.
 *
 * TODO: Create instrumentation tests in app/src/androidTest/ for proper PDF validation.
 */
@Ignore("PdfExporterImpl requires Android runtime - create instrumentation tests in app/src/androidTest/")
class PdfExporterImplTest {

    // All tests skipped - implementation verified manually on device
    @Test
    fun placeholder() {
        // Placeholder to keep test class structure
    }
}
