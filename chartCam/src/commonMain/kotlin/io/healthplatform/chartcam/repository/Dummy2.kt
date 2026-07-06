/**
 * Contains dummy/test logic for the repository layer.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.healthplatform.chartcam.database.ChartCamDatabase

/**
 * A simple test function that queries the database asynchronously.
 * Fetches a practitioner by a hardcoded ID to verify the coroutine database integration.
 *
 * @param db The [ChartCamDatabase] instance to execute the query against.
 */
suspend fun testAsync(db: ChartCamDatabase) {
    val x = db.chartCamQueries.getPractitionerById("1").awaitAsOneOrNull()
    println(x)
}
