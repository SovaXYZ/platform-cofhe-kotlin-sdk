package xyz.getsova.cofhe.sdk

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for [CoFheClient].
 *
 * @property baseUrl Base URL of the CoFHE service (without trailing slash)
 * @property requestTimeout Maximum time to wait for a complete request
 * @property connectTimeout Maximum time to establish a connection
 * @property socketTimeout Maximum time between data packets
 * @property enableLogging Enable HTTP request/response logging
 * @property engine Custom Ktor engine (for testing with MockEngine)
 * @property httpClient Bring your own preconfigured HttpClient
 */
data class CoFheClientConfig(
    val baseUrl: String,
    val requestTimeout: Duration = 60.seconds,
    val connectTimeout: Duration = 10.seconds,
    val socketTimeout: Duration = 60.seconds,
    val enableLogging: Boolean = false,
    val engine: HttpClientEngine? = null,
    val httpClient: HttpClient? = null,
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
    }

    val normalizedBaseUrl: String = baseUrl.trimEnd('/')

    class Builder {
        var baseUrl: String = ""
        var requestTimeout: Duration = 60.seconds
        var connectTimeout: Duration = 10.seconds
        var socketTimeout: Duration = 60.seconds
        var enableLogging: Boolean = false
        var engine: HttpClientEngine? = null
        var httpClient: HttpClient? = null

        fun build(): CoFheClientConfig = CoFheClientConfig(
            baseUrl = baseUrl,
            requestTimeout = requestTimeout,
            connectTimeout = connectTimeout,
            socketTimeout = socketTimeout,
            enableLogging = enableLogging,
            engine = engine,
            httpClient = httpClient,
        )
    }
}

fun CoFheClientConfig(block: CoFheClientConfig.Builder.() -> Unit): CoFheClientConfig =
    CoFheClientConfig.Builder().apply(block).build()
