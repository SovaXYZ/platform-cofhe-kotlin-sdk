package xyz.getsova.cofhe.sdk.integration

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import xyz.getsova.cofhe.sdk.CoFheClient
import xyz.getsova.cofhe.sdk.error.CoFheException
import xyz.getsova.cofhe.sdk.model.BatchItem
import xyz.getsova.cofhe.sdk.model.EncryptionType
import java.math.BigInteger
import kotlin.test.*

/**
 * Integration tests using WireMock for HTTP communication testing.
 * Tests real HTTP requests/responses without requiring a running CoFHE service.
 * Run with: ./gradlew :sdk:integrationTest
 */
@Tag("integration")
class CoFheClientIntegrationTest {

    companion object {
        @JvmField
        @RegisterExtension
        val wireMock: WireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()
    }

    private val validAddress = "0xabcdef0123456789abcdef0123456789abcdef01"

    private fun createClient(): CoFheClient {
        return CoFheClient {
            baseUrl = wireMock.baseUrl()
        }
    }

    @Test
    fun `should encrypt uint64 with real HTTP communication`() = runTest {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/encrypt/uint64"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.value"))
                .withRequestBody(matchingJsonPath("$.userAddress"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "type": "euint64",
                                "data": "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
                                "securityZone": 0,
                                "utype": 5,
                                "inputProof": "0x9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba",
                                "encryptionTimeMs": 1234
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val client = createClient()
        val result = client.encryptUint64(BigInteger("1000000"), validAddress)

        assertEquals(EncryptionType.UINT64, result.type)
        assertEquals("0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", result.data)
        assertEquals(0, result.securityZone)
        assertEquals(5, result.utype)
        assertEquals("0x9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba", result.inputProof)
        assertEquals(1234, result.encryptionTimeMs)

        wireMock.verify(
            postRequestedFor(urlEqualTo("/api/v1/encrypt/uint64"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(containing("1000000"))
                .withRequestBody(containing(validAddress)),
        )
        client.close()
    }

    @Test
    fun `should encrypt batch with multiple items`() = runTest {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/encrypt/batch"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "results": [
                                    {
                                        "type": "euint64",
                                        "data": "0xdata1",
                                        "securityZone": 0,
                                        "utype": 5,
                                        "inputProof": "0xproof1",
                                        "encryptionTimeMs": 100
                                    },
                                    {
                                        "type": "ebool",
                                        "data": "0xdata2",
                                        "securityZone": 0,
                                        "utype": 13,
                                        "inputProof": "0xproof2",
                                        "encryptionTimeMs": 50
                                    }
                                ],
                                "totalEncryptionTimeMs": 150
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val client = createClient()
        val result = client.encryptBatch(
            userAddress = validAddress,
            items = listOf(
                BatchItem.uint64(BigInteger("42")),
                BatchItem.bool(true),
            ),
        )

        assertEquals(2, result.results.size)
        assertEquals(EncryptionType.UINT64, result.results[0].type)
        assertEquals(EncryptionType.BOOL, result.results[1].type)
        assertEquals(150, result.totalEncryptionTimeMs)

        wireMock.verify(postRequestedFor(urlEqualTo("/api/v1/encrypt/batch")))
        client.close()
    }

    @Test
    fun `should handle 400 validation error with invalid params`() = runTest {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/encrypt/uint64"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "type": "urn:fhe:error:validation",
                                "title": "Validation Failed",
                                "status": 400,
                                "detail": "Request validation failed",
                                "instance": "/api/v1/encrypt/uint64",
                                "invalidParams": [
                                    {"name": "value", "reason": "value must be a non-empty string"}
                                ]
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val client = createClient()
        val exception = assertFailsWith<CoFheException.ValidationFailed> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }

        assertEquals(400, exception.statusCode)
        assertEquals(1, exception.invalidParams.size)
        assertEquals("value", exception.invalidParams[0].name)
        assertEquals("value must be a non-empty string", exception.invalidParams[0].reason)
        client.close()
    }

    @Test
    fun `should handle 503 service unavailable`() = runTest {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/encrypt/uint64"))
                .willReturn(
                    aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "type": "urn:fhe:error:not-initialized",
                                "title": "Service Not Ready",
                                "status": 503,
                                "detail": "FHEVM not initialized. Service is not ready.",
                                "instance": "/api/v1/encrypt/uint64"
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val client = createClient()
        val exception = assertFailsWith<CoFheException.NotInitialized> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }

        assertEquals(503, exception.statusCode)
        assertTrue(exception.message!!.contains("FHEVM not initialized"))
        client.close()
    }

    @Test
    fun `should handle 504 timeout`() = runTest {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/encrypt/uint64"))
                .willReturn(
                    aResponse()
                        .withStatus(504)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "type": "urn:fhe:error:timeout",
                                "title": "Encryption Timeout",
                                "status": 504,
                                "detail": "Encryption timeout after 30000ms",
                                "instance": "/api/v1/encrypt/uint64"
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val client = createClient()
        val exception = assertFailsWith<CoFheException.Timeout> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }

        assertEquals(504, exception.statusCode)
        client.close()
    }

    @Test
    fun `should verify Content-Type header is sent`() = runTest {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/encrypt/bool"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "type": "ebool",
                                "data": "0xdata",
                                "securityZone": 0,
                                "utype": 13,
                                "inputProof": "0xproof",
                                "encryptionTimeMs": 100
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val client = createClient()
        client.encryptBool(true, validAddress)

        wireMock.verify(
            postRequestedFor(urlEqualTo("/api/v1/encrypt/bool"))
                .withHeader("Content-Type", equalTo("application/json")),
        )
        client.close()
    }

    @Test
    fun `should handle health check endpoints`() = runTest {
        wireMock.stubFor(
            get(urlEqualTo("/health"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"status":"ok","info":{},"error":{},"details":{}}"""),
                ),
        )

        wireMock.stubFor(
            get(urlEqualTo("/health/ready"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"status":"ok","info":{"fhe":{"status":"up"}},"error":{},"details":{}}"""),
                ),
        )

        val client = createClient()
        assertTrue(client.isAlive())
        assertTrue(client.isReady())

        val healthStatus = client.healthReady()
        assertTrue(healthStatus.isHealthy)

        wireMock.verify(getRequestedFor(urlEqualTo("/health")))
        wireMock.verify(getRequestedFor(urlEqualTo("/health/ready")))
        client.close()
    }

    @Test
    fun `should handle malformed JSON response`() = runTest {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/encrypt/uint64"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("invalid json"),
                ),
        )

        val client = createClient()
        assertFailsWith<CoFheException.SerializationError> {
            client.encryptUint64(BigInteger.ONE, validAddress)
        }
        client.close()
    }

    @Test
    fun `should verify request body for generic encrypt`() = runTest {
        wireMock.stubFor(
            post(urlEqualTo("/api/v1/encrypt"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "type": "euint64",
                                "data": "0xdata",
                                "securityZone": 0,
                                "utype": 5,
                                "inputProof": "0xproof",
                                "encryptionTimeMs": 100
                            }
                            """.trimIndent(),
                        ),
                ),
        )

        val client = createClient()
        client.encrypt(EncryptionType.UINT64, BigInteger("999"), validAddress)

        wireMock.verify(
            postRequestedFor(urlEqualTo("/api/v1/encrypt"))
                .withRequestBody(matchingJsonPath("$.type", equalTo("euint64")))
                .withRequestBody(matchingJsonPath("$.value"))
                .withRequestBody(matchingJsonPath("$.userAddress", equalTo(validAddress))),
        )
        client.close()
    }
}
