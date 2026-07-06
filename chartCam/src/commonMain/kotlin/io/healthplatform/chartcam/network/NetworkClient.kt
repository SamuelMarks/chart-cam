/**
 * Contains the network client configuration and factory methods for Ktor.
 */
package io.healthplatform.chartcam.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory singleton object for creating the Ktor [HttpClient].
 * Configures JSON serialization for FHIR-compliant REST communication.
 */
object NetworkClient {
    /**
     * Creates a configured Ktor [HttpClient].
     * Installs [ContentNegotiation] with Kotlinx Serialization tuned for FHIR JSON parsing
     * (lenient, pretty-printed, ignoring unknown keys).
     *
     * @param engine An optional specific engine (useful for testing with MockEngine or specifying a platform-specific engine).
     *               If null, it uses the platform default engine resolved via ServiceLoader.
     * @return A fully configured [HttpClient] ready to make network requests.
     */
    fun create(engine: io.ktor.client.engine.HttpClientEngine? = null): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
        }

        return if (engine != null) {
            HttpClient(engine, config)
        } else {
            HttpClient(config)
        }
    }
}
