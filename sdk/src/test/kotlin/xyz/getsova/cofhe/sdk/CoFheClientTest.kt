package xyz.getsova.cofhe.sdk

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.getsova.cofhe.sdk.error.CoFheException
import xyz.getsova.cofhe.sdk.model.BatchItem
import xyz.getsova.cofhe.sdk.model.EncryptionType
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CoFheClientTest {

    private val validAddress = "0xabcdef0123456789abcdef0123456789abcdef01"
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val encryptedValueJson = """
        {
            "type": "euint64",
            "data": "0xdata123",
            "securityZone": 0,
            "utype": 5,
            "inputProof": "0xinputproof456",
            "encryptionTimeMs": 1000
        }
    """.trimIndent()

    private fun encryptedValueJson(type: String, utype: Int) = """
        {
            "type": "$type",
            "data": "0xdata123",
            "securityZone": 0,
            "utype": $utype,
            "inputProof": "0xinputproof456",
            "encryptionTimeMs": 1000
        }
    """.trimIndent()

    private val batchResponseJson = """
        {
            "results": [
                {
                    "type": "euint64",
                    "data": "0xdata123",
                    "securityZone": 0,
                    "utype": 5,
                    "inputProof": "0xinputproof456",
                    "encryptionTimeMs": 1000
                },
                {
                    "type": "ebool",
                    "data": "0xdata456",
                    "securityZone": 0,
                    "utype": 13,
                    "inputProof": "0xinputproof789",
                    "encryptionTimeMs": 500
                }
            ],
            "totalEncryptionTimeMs": 1500
        }
    """.trimIndent()

    private fun createClient(engine: MockEngine): CoFheClient {
        return CoFheClient {
            baseUrl = "http://localhost:3000"
            this.engine = engine
        }
    }

    // region Successful responses

    @Test
    fun `encryptUint8 should return encrypted value`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/encrypt/uint8", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            respond(encryptedValueJson(type = "euint8", utype = 0), headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encryptUint8(42, validAddress)
        assertEquals(EncryptionType.UINT8, result.type)
        assertEquals("0xdata123", result.data)
        client.close()
    }

    @Test
    fun `encryptUint16 should return encrypted value`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/encrypt/uint16", request.url.encodedPath)
            respond(encryptedValueJson(type = "euint16", utype = 1), headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encryptUint16(1000, validAddress)
        assertEquals(EncryptionType.UINT16, result.type)
        client.close()
    }

    @Test
    fun `encryptUint32 should return encrypted value`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/encrypt/uint32", request.url.encodedPath)
            respond(encryptedValueJson(type = "euint32", utype = 2), headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encryptUint32(4294967295, validAddress)
        assertEquals(EncryptionType.UINT32, result.type)
        client.close()
    }

    @Test
    fun `encryptUint64 should return encrypted value`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/encrypt/uint64", request.url.encodedPath)
            respond(encryptedValueJson, headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encryptUint64(BigInteger("1000000"), validAddress)
        assertEquals(EncryptionType.UINT64, result.type)
        assertEquals("0xdata123", result.data)
        assertEquals(0, result.securityZone)
        assertEquals(5, result.utype)
        assertEquals("0xinputproof456", result.inputProof)
        assertEquals(1000, result.encryptionTimeMs)
        client.close()
    }

    @Test
    fun `encryptUint128 should return encrypted value`() = runTest {
        val engine = MockEngine {
            respond(encryptedValueJson(type = "euint128", utype = 6), headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encryptUint128(BigInteger("340282366920938463463374607431768211455"), validAddress)
        assertEquals(EncryptionType.UINT128, result.type)
        client.close()
    }

    @Test
    fun `encryptUint256 should return encrypted value`() = runTest {
        val engine = MockEngine {
            respond(encryptedValueJson(type = "euint256", utype = 8), headers = jsonHeaders)
        }
        val client = createClient(engine)
        val max256 = BigInteger.TWO.pow(256).subtract(BigInteger.ONE)
        val result = client.encryptUint256(max256, validAddress)
        assertEquals(EncryptionType.UINT256, result.type)
        client.close()
    }

    @Test
    fun `encryptAddress should return encrypted value`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/encrypt/address", request.url.encodedPath)
            respond(encryptedValueJson(type = "eaddress", utype = 7), headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encryptAddress(validAddress, validAddress)
        assertEquals(EncryptionType.ADDRESS, result.type)
        client.close()
    }

    @Test
    fun `encryptBool should return encrypted value`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/encrypt/bool", request.url.encodedPath)
            val body = Json.decodeFromString<JsonObject>(request.body.toByteArray().decodeToString())
            assertEquals(true, body["value"]?.jsonPrimitive?.boolean)
            respond(encryptedValueJson(type = "ebool", utype = 13), headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encryptBool(true, validAddress)
        assertEquals(EncryptionType.BOOL, result.type)
        client.close()
    }

    @Test
    fun `encrypt generic should return encrypted value`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/encrypt", request.url.encodedPath)
            respond(encryptedValueJson, headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encrypt(EncryptionType.UINT64, BigInteger("1000000"), validAddress)
        assertEquals(EncryptionType.UINT64, result.type)
        client.close()
    }

    @Test
    fun `encryptBatch should return batch result`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/encrypt/batch", request.url.encodedPath)
            respond(batchResponseJson, headers = jsonHeaders)
        }
        val client = createClient(engine)
        val result = client.encryptBatch(
            userAddress = validAddress,
            items = listOf(
                BatchItem.uint64(BigInteger("1000000")),
                BatchItem.bool(true),
            ),
        )
        assertEquals(2, result.results.size)
        assertEquals(EncryptionType.UINT64, result.results[0].type)
        assertEquals(EncryptionType.BOOL, result.results[1].type)
        assertEquals(1500, result.totalEncryptionTimeMs)
        client.close()
    }

    // endregion

    // region Client-side validation

    @Test
    fun `encryptUint8 should reject value above 255`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptUint8(256, validAddress)
        }
        client.close()
    }

    @Test
    fun `encryptUint8 should reject negative value`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptUint8(-1, validAddress)
        }
        client.close()
    }

    @Test
    fun `encryptUint16 should reject value above 65535`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptUint16(65536, validAddress)
        }
        client.close()
    }

    @Test
    fun `encryptUint32 should reject value above max`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptUint32(4294967296, validAddress)
        }
        client.close()
    }

    @Test
    fun `encryptUint64 should reject negative BigInteger`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptUint64(BigInteger("-1"), validAddress)
        }
        client.close()
    }

    @Test
    fun `encryptUint64 should reject value above max`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        val overMax = BigInteger("18446744073709551616")
        assertThrows<IllegalArgumentException> {
            client.encryptUint64(overMax, validAddress)
        }
        client.close()
    }

    @Test
    fun `encryptAddress should reject invalid address`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptAddress("invalid", validAddress)
        }
        client.close()
    }

    @Test
    fun `encryptAddress should reject address without 0x prefix`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptAddress("abcdef0123456789abcdef0123456789abcdef01", validAddress)
        }
        client.close()
    }

    @Test
    fun `should reject invalid userAddress`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptUint64(BigInteger.ONE, "not-an-address")
        }
        client.close()
    }

    @Test
    fun `encryptBatch should reject empty items`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        assertThrows<IllegalArgumentException> {
            client.encryptBatch(validAddress, emptyList())
        }
        client.close()
    }

    @Test
    fun `encryptBatch should reject more than 10 items`() = runTest {
        val engine = MockEngine { respond("") }
        val client = createClient(engine)
        val items = (1..11).map { BatchItem.uint8(it % 256) }
        assertThrows<IllegalArgumentException> {
            client.encryptBatch(validAddress, items)
        }
        client.close()
    }

    // endregion

    // region Error mapping

    @Test
    fun `should map 400 validation error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:validation",
                        "title": "Validation Failed",
                        "status": 400,
                        "detail": "Request validation failed",
                        "instance": "/api/v1/encrypt",
                        "invalidParams": [{"name": "value", "reason": "value is required"}]
                    }
                """.trimIndent(),
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        val ex = assertThrows<CoFheException.ValidationFailed> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        assertEquals(1, ex.invalidParams.size)
        client.close()
    }

    @Test
    fun `should map 422 invalid-address error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:invalid-address",
                        "title": "Invalid Address",
                        "status": 422,
                        "detail": "Invalid user address"
                    }
                """.trimIndent(),
                status = HttpStatusCode.UnprocessableEntity,
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        val ex = assertThrows<CoFheException.InvalidAddress> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        assertEquals(422, ex.statusCode)
        client.close()
    }

    @Test
    fun `should map 422 invalid-value error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:invalid-value",
                        "title": "Invalid Value",
                        "status": 422,
                        "detail": "Invalid encryption value"
                    }
                """.trimIndent(),
                status = HttpStatusCode.UnprocessableEntity,
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        assertThrows<CoFheException.InvalidValue> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        client.close()
    }

    @Test
    fun `should map 500 encryption-failed error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:encryption-failed",
                        "title": "Encryption Failed",
                        "status": 500,
                        "detail": "Failed to encrypt uint64"
                    }
                """.trimIndent(),
                status = HttpStatusCode.InternalServerError,
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        assertThrows<CoFheException.EncryptionFailed> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        client.close()
    }

    @Test
    fun `should map 503 not-initialized error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:not-initialized",
                        "title": "Service Not Ready",
                        "status": 503,
                        "detail": "FHEVM not initialized"
                    }
                """.trimIndent(),
                status = HttpStatusCode.ServiceUnavailable,
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        assertThrows<CoFheException.NotInitialized> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        client.close()
    }

    @Test
    fun `should map 503 pool-exhausted error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:pool-exhausted",
                        "title": "Service Overloaded",
                        "status": 503,
                        "detail": "Worker pool exhausted"
                    }
                """.trimIndent(),
                status = HttpStatusCode.ServiceUnavailable,
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        assertThrows<CoFheException.PoolExhausted> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        client.close()
    }

    @Test
    fun `should map 504 timeout error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:timeout",
                        "title": "Encryption Timeout",
                        "status": 504,
                        "detail": "Encryption timeout after 30000ms"
                    }
                """.trimIndent(),
                status = HttpStatusCode.GatewayTimeout,
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        assertThrows<CoFheException.Timeout> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        client.close()
    }

    @Test
    fun `should map 500 internal error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:internal",
                        "title": "Internal Server Error",
                        "status": 500,
                        "detail": "Unknown error"
                    }
                """.trimIndent(),
                status = HttpStatusCode.InternalServerError,
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        assertThrows<CoFheException.InternalServerError> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        client.close()
    }

    @Test
    fun `should map unknown URN to UnknownServerError`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "type": "urn:fhe:error:some-new-error",
                        "title": "New Error",
                        "status": 418,
                        "detail": "Something unexpected"
                    }
                """.trimIndent(),
                status = HttpStatusCode(418, "I'm a teapot"),
                headers = jsonHeaders,
            )
        }
        val client = createClient(engine)
        assertThrows<CoFheException.UnknownServerError> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        client.close()
    }

    // endregion

    // region Edge cases

    @Test
    fun `should handle non-JSON error body`() = runTest {
        val engine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
            )
        }
        val client = createClient(engine)
        val ex = assertThrows<CoFheException.UnknownServerError> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        assertTrue(ex.message!!.contains("Internal Server Error"))
        client.close()
    }

    @Test
    fun `should handle empty error body`() = runTest {
        val engine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.InternalServerError,
            )
        }
        val client = createClient(engine)
        val ex = assertThrows<CoFheException.UnknownServerError> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        assertTrue(ex.message!!.contains("No response body"))
        client.close()
    }

    // endregion

    // region Health endpoints

    @Test
    fun `isAlive should return true when healthy`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/health", request.url.encodedPath)
            respond("""{"status":"ok","info":{},"error":{},"details":{}}""", headers = jsonHeaders)
        }
        val client = createClient(engine)
        assertTrue(client.isAlive())
        client.close()
    }

    @Test
    fun `isAlive should return false on error`() = runTest {
        val engine = MockEngine {
            respond("", status = HttpStatusCode.ServiceUnavailable)
        }
        val client = createClient(engine)
        assertFalse(client.isAlive())
        client.close()
    }

    @Test
    fun `isReady should return true when ready`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/health/ready", request.url.encodedPath)
            respond("""{"status":"ok","info":{"fhe":{"status":"up"}},"error":{},"details":{}}""", headers = jsonHeaders)
        }
        val client = createClient(engine)
        assertTrue(client.isReady())
        client.close()
    }

    @Test
    fun `isReady should return false when not ready`() = runTest {
        val engine = MockEngine {
            respond("""{"status":"error","info":{},"error":{"fhe":{"status":"down"}},"details":{}}""", headers = jsonHeaders)
        }
        val client = createClient(engine)
        assertFalse(client.isReady())
        client.close()
    }

    @Test
    fun `healthReady should return HealthStatus`() = runTest {
        val engine = MockEngine {
            respond("""{"status":"ok","info":{},"error":{},"details":{}}""", headers = jsonHeaders)
        }
        val client = createClient(engine)
        val status = client.healthReady()
        assertTrue(status.isHealthy)
        client.close()
    }

    // endregion

    // region Request body validation

    @Test
    fun `encryptUint64 should send correct request body`() = runTest {
        val engine = MockEngine { request ->
            val body = Json.decodeFromString<JsonObject>(request.body.toByteArray().decodeToString())
            assertEquals("1000000", body["value"]?.jsonPrimitive?.content)
            assertEquals(validAddress, body["userAddress"]?.jsonPrimitive?.content)
            respond(encryptedValueJson, headers = jsonHeaders)
        }
        val client = createClient(engine)
        client.encryptUint64(BigInteger("1000000"), validAddress)
        client.close()
    }

    @Test
    fun `encryptBatch should send correct request body`() = runTest {
        val engine = MockEngine { request ->
            val body = Json.decodeFromString<JsonObject>(request.body.toByteArray().decodeToString())
            assertEquals(validAddress, body["userAddress"]?.jsonPrimitive?.content)
            respond(batchResponseJson, headers = jsonHeaders)
        }
        val client = createClient(engine)
        client.encryptBatch(
            userAddress = validAddress,
            items = listOf(
                BatchItem.uint64(BigInteger("1000000")),
                BatchItem.bool(true),
            ),
        )
        client.close()
    }

    // endregion
}
