package xyz.getsova.cofhe.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedValue(
    val type: EncryptionType,
    val data: String,
    val securityZone: Int,
    val utype: Int,
    val inputProof: String,
    val encryptionTimeMs: Long,
)
