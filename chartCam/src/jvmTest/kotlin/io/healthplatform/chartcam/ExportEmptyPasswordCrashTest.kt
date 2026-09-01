package io.healthplatform.chartcam

import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.repository.ExportImportService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ExportEmptyPasswordCrashTest {
    @Test
    fun testExport() {
        runBlocking {
            val driver = DatabaseDriverFactory().createDriver()
            val db = ChartCamDatabase(driver)
            val storage = createFileStorage()
            val service = ExportImportService(db, storage)

            println("Exporting...")
            val result = service.exportData("", true, null)
            println("Exported size: ${result.length}")
        }
    }
}
