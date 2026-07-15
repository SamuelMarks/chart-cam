/**
 * Dummy file for testing coverage.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.healthplatform.chartcam.database.ChartCamDatabase

/**
 * Executes a dummy async test on the database.
 * @param db The [ChartCamDatabase] to test against.
 */
suspend fun testAsync(db: ChartCamDatabase) {
    val x = db.chartCamQueries.getResourceById("Practitioner", "1").awaitAsOneOrNull()
    println(x)
}
