package xyz.getsova.cofhe.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(val status: String) {
    val isHealthy: Boolean get() = status == "ok"
}
