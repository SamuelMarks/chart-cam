/**
 * @file NetworkClientTest.kt
 * Contains declarations for NetworkClientTest.kt.
 */
package io.healthplatform.chartcam.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Tests for the [NetworkClient] creation and basic usage.
 */
class NetworkClientTest {
    /**
     * Tests that the Ktor client can be created and queried using a MockEngine.
     */
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
