package xyz.getsova.cofhe.sdk

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import xyz.getsova.cofhe.sdk.error.CoFheException
import xyz.getsova.cofhe.sdk.error.ProblemDetail
import xyz.getsova.cofhe.sdk.internal.ApiPaths
import xyz.getsova.cofhe.sdk.internal.HttpClientFactory
import xyz.getsova.cofhe.sdk.model.BatchEncryptResult
import xyz.getsova.cofhe.sdk.model.BatchItem
import xyz.getsova.cofhe.sdk.model.EncryptedValue
import xyz.getsova.cofhe.sdk.model.EncryptionType
import xyz.getsova.cofhe.sdk.model.HealthStatus
import java.io.Closeable
import java.io.IOException
import java.math.BigInteger

/**
 * Client for CoFHE (Confidential FHE) encryption service.
 *
 * Provides type-safe encryption methods for all supported FHE types:
 * - Unsigned integers: uint8, uint16, uint32, uint64, uint128, uint256
 * - Ethereum addresses
 * - Booleans
 * - Batch encryption (up to 10 items)
 *
 * Example:
 * ```kotlin
 * val client = CoFheClient {
 *     baseUrl = "https://cofhe.getsova.xyz"
 * }
 *
 * val encrypted = client.encryptUint64(
 *     value = BigInteger("1000000"),
 *     userAddress = "0x1234567890123456789012345678901234567890"
 * )
 *
 * // Use encrypted.data and encrypted.inputProof in smart contract calls
 * client.close()
 * ```
 *
 * All methods throw [CoFheException] on errors.
 */
class CoFheClient private constructor(
    private val config: CoFheClientConfig,
    private val client: HttpClient,
    private val ownsClient: Boolean,
) : Closeable {

    constructor(config: CoFheClientConfig) : this(
        config = config,
        client = config.httpClient ?: HttpClientFactory.create(config),
        ownsClient = config.httpClient == null,
    )

    constructor(block: CoFheClientConfig.Builder.() -> Unit) : this(CoFheClientConfig(block))

    private val baseUrl = config.normalizedBaseUrl

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    // region Typed endpoints

    suspend fun encryptUint8(value: Int, userAddress: String): EncryptedValue {
        require(value in 0..255) { "uint8 value must be in range 0..255, got $value" }
        requireValidAddress(userAddress)
        return postEncrypt(ApiPaths.ENCRYPT_UINT8, TypedRequest(value, userAddress))
    }

    suspend fun encryptUint16(value: Int, userAddress: String): EncryptedValue {
        require(value in 0..65535) { "uint16 value must be in range 0..65535, got $value" }
        requireValidAddress(userAddress)
        return postEncrypt(ApiPaths.ENCRYPT_UINT16, TypedRequest(value, userAddress))
    }

    suspend fun encryptUint32(value: Long, userAddress: String): EncryptedValue {
        require(value in 0..4294967295) { "uint32 value must be in range 0..4294967295, got $value" }
        requireValidAddress(userAddress)
        return postEncrypt(ApiPaths.ENCRYPT_UINT32, TypedRequest(value.toString(), userAddress))
    }

    suspend fun encryptUint64(value: BigInteger, userAddress: String): EncryptedValue {
        requireUintRange(value, 64)
        requireValidAddress(userAddress)
        return postEncrypt(ApiPaths.ENCRYPT_UINT64, TypedRequest(value.toString(), userAddress))
    }

    suspend fun encryptUint128(value: BigInteger, userAddress: String): EncryptedValue {
        requireUintRange(value, 128)
        requireValidAddress(userAddress)
        return postEncrypt(ApiPaths.ENCRYPT_UINT128, TypedRequest(value.toString(), userAddress))
    }

    suspend fun encryptUint256(value: BigInteger, userAddress: String): EncryptedValue {
        requireUintRange(value, 256)
        requireValidAddress(userAddress)
        return postEncrypt(ApiPaths.ENCRYPT_UINT256, TypedRequest(value.toString(), userAddress))
    }

    suspend fun encryptAddress(value: String, userAddress: String): EncryptedValue {
        require(ADDRESS_REGEX.matches(value)) {
            "address must match 0x followed by 40 hex characters, got $value"
        }
        requireValidAddress(userAddress)
        return postEncrypt(ApiPaths.ENCRYPT_ADDRESS, TypedRequest(value, userAddress))
    }

    suspend fun encryptBool(value: Boolean, userAddress: String): EncryptedValue {
        requireValidAddress(userAddress)
        return postEncrypt(ApiPaths.ENCRYPT_BOOL, TypedRequest(value, userAddress))
    }

    suspend fun encrypt(type: EncryptionType, value: Any, userAddress: String): EncryptedValue {
        requireValidAddress(userAddress)
        val jsonValue = toJsonPrimitive(type, value)
        return postEncrypt(ApiPaths.ENCRYPT, GenericRequest(type, jsonValue, userAddress))
    }

    suspend fun encryptBatch(userAddress: String, items: List<BatchItem>): BatchEncryptResult {
        require(items.isNotEmpty()) { "batch items must not be empty" }
        require(items.size <= 10) { "batch items must not exceed 10, got ${items.size}" }
        requireValidAddress(userAddress)

        val batchItems = items.map { item ->
            BatchRequestItem(item.type, toJsonPrimitive(item.type, item.value))
        }

        val response = executePost(ApiPaths.ENCRYPT_BATCH, BatchRequest(userAddress, batchItems))
        return parseResponse(response)
    }

    suspend fun isAlive(): Boolean {
        return try {
            val response = client.get("$baseUrl${ApiPaths.HEALTH}")
            response.status.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isReady(): Boolean {
        return try {
            val result = healthReady()
            result.isHealthy
        } catch (_: Exception) {
            false
        }
    }

    suspend fun healthReady(): HealthStatus {
        val response = try {
            client.get("$baseUrl${ApiPaths.HEALTH_READY}")
        } catch (e: IOException) {
            throw CoFheException.NetworkError("Failed to connect: ${e.message}", e)
        }
        return parseResponse(response)
    }

    // endregion

    override fun close() {
        if (ownsClient) {
            client.close()
        }
    }

    // region Internal request/response types

    @Serializable
    private data class TypedRequest<T>(
        val value: T,
        val userAddress: String,
    )

    @Serializable
    private data class GenericRequest(
        val type: EncryptionType,
        val value: JsonElement,
        val userAddress: String,
    )

    @Serializable
    private data class BatchRequestItem(
        val type: EncryptionType,
        val value: JsonElement,
    )

    @Serializable
    private data class BatchRequest(
        val userAddress: String,
        val items: List<BatchRequestItem>,
    )

    // endregion

    // region Private helpers

    private suspend inline fun <reified T> postEncrypt(path: String, body: T): EncryptedValue {
        val response = executePost(path, body)
        return parseResponse(response)
    }

    private suspend inline fun <reified T> executePost(path: String, body: T): HttpResponse {
        return try {
            client.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (e: IOException) {
            throw CoFheException.NetworkError("Failed to connect: ${e.message}", e)
        }
    }

    private suspend inline fun <reified T> parseResponse(response: HttpResponse): T {
        if (response.status.isSuccess()) {
            return try {
                response.body<T>()
            } catch (e: Exception) {
                // Catch both SerializationException and Ktor's JsonConvertException
                when (e) {
                    is SerializationException,
                    is JsonConvertException ->
                        throw CoFheException.SerializationError("Failed to parse response: ${e.message}", e)
                    else -> throw e
                }
            }
        }

        val bodyText = try {
            response.bodyAsText()
        } catch (_: Exception) {
            ""
        }

        val problem = try {
            json.decodeFromString<ProblemDetail>(bodyText)
        } catch (_: Exception) {
            ProblemDetail(
                type = "urn:fhe:error:unknown",
                title = "HTTP ${response.status.value}",
                status = response.status.value,
                detail = bodyText.ifEmpty { "No response body" },
            )
        }

        throw CoFheException.fromProblemDetail(problem)
    }

    private fun toJsonPrimitive(type: EncryptionType, value: Any): JsonPrimitive {
        return when (type) {
            EncryptionType.BOOL -> JsonPrimitive(value as Boolean)
            EncryptionType.UINT8, EncryptionType.UINT16 -> {
                when (value) {
                    is Int -> JsonPrimitive(value)
                    is Long -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value.toInt())
                    else -> JsonPrimitive(value.toString())
                }
            }
            EncryptionType.ADDRESS -> JsonPrimitive(value as String)
            else -> {
                when (value) {
                    is BigInteger -> JsonPrimitive(value.toString())
                    is Long -> JsonPrimitive(value.toString())
                    is Int -> JsonPrimitive(value)
                    is String -> JsonPrimitive(value)
                    else -> JsonPrimitive(value.toString())
                }
            }
        }
    }

    private fun requireValidAddress(address: String) {
        require(ADDRESS_REGEX.matches(address)) {
            "userAddress must be a valid Ethereum address (0x + 40 hex chars), got $address"
        }
    }

    private fun requireUintRange(value: BigInteger, bits: Int) {
        val max = BigInteger.TWO.pow(bits).subtract(BigInteger.ONE)
        require(value >= BigInteger.ZERO && value <= max) {
            "uint$bits value must be in range 0..2^$bits-1, got $value"
        }
    }

    companion object {
        private val ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
    }

    // endregion
}
