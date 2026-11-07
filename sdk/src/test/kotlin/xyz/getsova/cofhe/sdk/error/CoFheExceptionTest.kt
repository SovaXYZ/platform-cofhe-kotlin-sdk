package xyz.getsova.cofhe.sdk.error

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CoFheExceptionTest {

    @Test
    fun `should map validation error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:validation",
            title = "Validation Failed",
            status = 400,
            detail = "Request validation failed",
            invalidParams = listOf(
                InvalidParam("value", "value must be a non-empty string"),
            ),
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.ValidationFailed>(ex)
        assertEquals(400, ex.statusCode)
        assertEquals(1, ex.invalidParams.size)
        assertEquals("value", ex.invalidParams[0].name)
    }

    @Test
    fun `should map invalid-address error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:invalid-address",
            title = "Invalid Address",
            status = 422,
            detail = "Invalid user address: 0xinvalid",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.InvalidAddress>(ex)
        assertEquals(422, ex.statusCode)
    }

    @Test
    fun `should map invalid-value error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:invalid-value",
            title = "Invalid Value",
            status = 422,
            detail = "Invalid encryption value",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.InvalidValue>(ex)
        assertEquals(422, ex.statusCode)
    }

    @Test
    fun `should map unsupported-type error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:unsupported-type",
            title = "Unsupported Type",
            status = 422,
            detail = "Unsupported encryption type: efloat",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.UnsupportedType>(ex)
    }

    @Test
    fun `should map encryption-failed error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:encryption-failed",
            title = "Encryption Failed",
            status = 500,
            detail = "Failed to encrypt uint64",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.EncryptionFailed>(ex)
        assertEquals(500, ex.statusCode)
    }

    @Test
    fun `should map internal error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:internal",
            title = "Internal Server Error",
            status = 500,
            detail = "Unknown error",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.InternalServerError>(ex)
    }

    @Test
    fun `should map not-initialized error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:not-initialized",
            title = "Service Not Ready",
            status = 503,
            detail = "FHEVM not initialized",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.NotInitialized>(ex)
        assertEquals(503, ex.statusCode)
    }

    @Test
    fun `should map initialization-failed error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:initialization-failed",
            title = "Initialization Failed",
            status = 503,
            detail = "FHEVM initialization failed",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.InitializationFailed>(ex)
    }

    @Test
    fun `should map pool-exhausted error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:pool-exhausted",
            title = "Service Overloaded",
            status = 503,
            detail = "Worker pool exhausted",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.PoolExhausted>(ex)
    }

    @Test
    fun `should map timeout error`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:timeout",
            title = "Encryption Timeout",
            status = 504,
            detail = "Encryption timeout after 30000ms",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.Timeout>(ex)
        assertEquals(504, ex.statusCode)
    }

    @Test
    fun `should map unknown URN to UnknownServerError`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:some-future-error",
            title = "Future Error",
            status = 418,
            detail = "Something new",
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.UnknownServerError>(ex)
        assertEquals(418, ex.statusCode)
    }

    @Test
    fun `should handle validation error with no invalidParams`() {
        val problem = ProblemDetail(
            type = "urn:fhe:error:validation",
            title = "Validation Failed",
            status = 400,
            detail = "Request validation failed",
            invalidParams = null,
        )
        val ex = CoFheException.fromProblemDetail(problem)
        assertIs<CoFheException.ValidationFailed>(ex)
        assertTrue(ex.invalidParams.isEmpty())
    }
}
