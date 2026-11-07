package xyz.getsova.cofhe.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class BatchEncryptResult(
    val results: List<EncryptedValue>,
    val totalEncryptionTimeMs: Long,
)
