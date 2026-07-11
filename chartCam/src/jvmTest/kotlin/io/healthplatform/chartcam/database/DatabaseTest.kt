package io.healthplatform.chartcam.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {
    @Test
    fun testInsertAndRetrievePractitioner() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            ChartCamDatabase.Schema.synchronous().create(driver)
            val database = ChartCamDatabase(driver)
            val repo = FhirRepository(database)

            val practitioner =
                Practitioner
                    .Builder()
                    .apply {
                        id = "prac-1"
                    }.build()

            repo.savePractitioner(practitioner)

            val practitioners = repo.getAllPractitioners()
            assertEquals(1, practitioners.size)
            assertEquals("prac-1", practitioners[0].id)
        }
}
