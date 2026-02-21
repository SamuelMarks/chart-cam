package io.healthplatform.chartcam.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory class for creating the Ktor [HttpClient].
 * Configures JSON serialization for FHIR-compliant communication.
 */
object NetworkClient {
    /**
     * Creates a configured HttpClient.
     *
     * @param engine An optional specific engine (useful for testing with MockEngine).
     * If null, it uses the platform default engine via service loading.
     */
    fun create(engine: io.ktor.client.engine.HttpClientEngine? = null): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        return if (engine != null) {
            HttpClient(engine, config)
        } else {
            HttpClient(config)
        }
    }
}