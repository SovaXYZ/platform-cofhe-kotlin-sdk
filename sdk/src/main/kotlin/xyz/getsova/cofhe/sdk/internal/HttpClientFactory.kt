package xyz.getsova.cofhe.sdk.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import xyz.getsova.cofhe.sdk.CoFheClientConfig

internal object HttpClientFactory {

    fun create(config: CoFheClientConfig): HttpClient {
        return if (config.engine != null) {
            HttpClient(config.engine) { configureClient(config) }
        } else {
            HttpClient(OkHttp) { configureClient(config) }
        }
    }

    private fun io.ktor.client.HttpClientConfig<*>.configureClient(config: CoFheClientConfig) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = false
                encodeDefaults = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeout.inWholeMilliseconds
            connectTimeoutMillis = config.connectTimeout.inWholeMilliseconds
            socketTimeoutMillis = config.socketTimeout.inWholeMilliseconds
        }

        if (config.enableLogging) {
            install(Logging) {
                level = LogLevel.BODY
            }
        }

        expectSuccess = false
    }
}
