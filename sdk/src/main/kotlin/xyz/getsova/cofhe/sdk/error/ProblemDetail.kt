package xyz.getsova.cofhe.sdk.error

import kotlinx.serialization.Serializable

@Serializable
internal data class InvalidParam(
    val name: String,
    val reason: String,
)

@Serializable
internal data class ProblemDetail(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String = "",
    val invalidParams: List<InvalidParam>? = null,
)
