package io.healthplatform.chartcam.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertNotNull

class NetworkClientTest {
    @Test
    fun testCreateNetworkClient() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respondOk("OK")
                }
            val client = NetworkClient.create(mockEngine)

            assertNotNull(client)
            val response = client.get("https://fhir.healthplatform.io")
            assertNotNull(response)

            client.close()
        }
}
