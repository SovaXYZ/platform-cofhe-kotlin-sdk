package xyz.getsova.cofhe.sdk

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class CoFheClientConfigTest {

    @Test
    fun `should create config with defaults`() {
        val config = CoFheClientConfig { baseUrl = "http://localhost:3000" }
        assertEquals("http://localhost:3000", config.normalizedBaseUrl)
        assertEquals(60.seconds, config.requestTimeout)
        assertEquals(10.seconds, config.connectTimeout)
        assertEquals(60.seconds, config.socketTimeout)
        assertEquals(false, config.enableLogging)
    }

    @Test
    fun `should strip trailing slash from baseUrl`() {
        val config = CoFheClientConfig { baseUrl = "http://localhost:3000/" }
        assertEquals("http://localhost:3000", config.normalizedBaseUrl)
    }

    @Test
    fun `should strip multiple trailing slashes`() {
        val config = CoFheClientConfig { baseUrl = "http://localhost:3000///" }
        assertEquals("http://localhost:3000", config.normalizedBaseUrl)
    }

    @Test
    fun `should throw on blank baseUrl`() {
        assertThrows<IllegalArgumentException> {
            CoFheClientConfig { baseUrl = "" }
        }
    }

    @Test
    fun `should throw on whitespace-only baseUrl`() {
        assertThrows<IllegalArgumentException> {
            CoFheClientConfig { baseUrl = "   " }
        }
    }

    @Test
    fun `should accept custom timeouts`() {
        val config = CoFheClientConfig {
            baseUrl = "http://localhost:3000"
            requestTimeout = 120.seconds
            connectTimeout = 5.seconds
            socketTimeout = 30.seconds
        }
        assertEquals(120.seconds, config.requestTimeout)
        assertEquals(5.seconds, config.connectTimeout)
        assertEquals(30.seconds, config.socketTimeout)
    }
}
