package io.healthplatform.chartcam.repository
import io.healthplatform.chartcam.database.ChartCamDatabase
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull

suspend fun testAsync(db: ChartCamDatabase) {
    val x = db.chartCamQueries.getPractitionerById("1").awaitAsOneOrNull()
    println(x)
}
