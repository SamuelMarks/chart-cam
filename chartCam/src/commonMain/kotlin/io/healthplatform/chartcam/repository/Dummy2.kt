package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.healthplatform.chartcam.database.ChartCamDatabase

suspend fun testAsync(db: ChartCamDatabase) {
    val x = db.chartCamQueries.getResourceById("Practitioner", "1").awaitAsOneOrNull()
    println(x)
}
