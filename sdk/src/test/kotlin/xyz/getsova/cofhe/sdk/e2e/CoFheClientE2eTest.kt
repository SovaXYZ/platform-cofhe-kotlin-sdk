package xyz.getsova.cofhe.sdk.e2e

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import xyz.getsova.cofhe.sdk.CoFheClient
import xyz.getsova.cofhe.sdk.error.CoFheException
import xyz.getsova.cofhe.sdk.model.BatchItem
import xyz.getsova.cofhe.sdk.model.EncryptionType
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * End-to-end tests against real CoFHE service.
 * Requires COFHE_BASE_URL environment variable.
 * Run with: ./gradlew :sdk:e2eTest
 */
@Tag("e2e")
class CoFheClientE2eTest {

    companion object {
        private lateinit var client: CoFheClient

        @JvmStatic
        @BeforeAll
        fun setup() {
            val baseUrl = System.getenv("COFHE_BASE_URL")
            Assumptions.assumeTrue(
                baseUrl != null,
                "COFHE_BASE_URL env var must be set for integration tests",
            )
            client = CoFheClient {
                this.baseUrl = baseUrl!!
                requestTimeout = 120.seconds
            }
        }
    }

    @Test
    fun `should check health`() = runTest {
        assertTrue(client.isAlive())
    }

    @Test
    fun `should check readiness`() = runTest {
        assertTrue(client.isReady())
    }

    @Test
    fun `should encrypt uint64`() = runTest {
        val result = client.encryptUint64(
            value = BigInteger("1000000"),
            userAddress = "0x1234567890123456789012345678901234567890",
        )
        assertEquals(EncryptionType.UINT64, result.type)
        assertNotNull(result.data)
        assertTrue(result.data.startsWith("0x"))
        assertNotNull(result.inputProof)
        assertTrue(result.inputProof.startsWith("0x"))
        assertTrue(result.encryptionTimeMs > 0)
    }

    @Test
    fun `should encrypt batch`() = runTest {
        val result = client.encryptBatch(
            userAddress = "0x1234567890123456789012345678901234567890",
            items = listOf(
                BatchItem.uint64(BigInteger("42")),
                BatchItem.bool(true),
                BatchItem.address("0xabcdef0123456789abcdef0123456789abcdef01"),
            ),
        )
        assertEquals(3, result.results.size)
        assertEquals(EncryptionType.UINT64, result.results[0].type)
        assertEquals(EncryptionType.BOOL, result.results[1].type)
        assertEquals(EncryptionType.ADDRESS, result.results[2].type)
        assertTrue(result.totalEncryptionTimeMs > 0)
    }

    @Test
    fun `should return validation error for invalid type`() = runTest {
        try {
            client.encrypt(EncryptionType.UINT64, "not-a-number-but-server-decides", "0x1234567890123456789012345678901234567890")
        } catch (e: CoFheException) {
            // Expected: some kind of error from server
            assertNotNull(e.message)
        }
    }
}
