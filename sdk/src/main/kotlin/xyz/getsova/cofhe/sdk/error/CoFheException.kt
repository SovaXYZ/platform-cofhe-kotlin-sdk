package xyz.getsova.cofhe.sdk.error

/**
 * Base exception class for all CoFHE SDK errors.
 *
 * @property statusCode HTTP status code associated with this error (0 for client-side errors)
 */
sealed class CoFheException(
    message: String,
    val statusCode: Int,
    cause: Throwable? = null,
) : Exception(message, cause) {

    data class InvalidParam(val name: String, val reason: String)

    class ValidationFailed(
        message: String,
        val invalidParams: List<InvalidParam>,
    ) : CoFheException(message, 400)

    class InvalidAddress(message: String) : CoFheException(message, 422)
    class InvalidValue(message: String) : CoFheException(message, 422)
    class UnsupportedType(message: String) : CoFheException(message, 422)
    class EncryptionFailed(message: String) : CoFheException(message, 500)
    class InternalServerError(message: String) : CoFheException(message, 500)
    class NotInitialized(message: String) : CoFheException(message, 503)
    class InitializationFailed(message: String) : CoFheException(message, 503)
    class PoolExhausted(message: String) : CoFheException(message, 503)
    class Timeout(message: String) : CoFheException(message, 504)
    class UnknownServerError(message: String, statusCode: Int) : CoFheException(message, statusCode)
    class NetworkError(message: String, cause: Throwable) : CoFheException(message, 0, cause)
    class SerializationError(message: String, cause: Throwable) : CoFheException(message, 0, cause)

    internal companion object {
        fun fromProblemDetail(problem: ProblemDetail): CoFheException {
            val detail = problem.detail
            return when (problem.type) {
                "urn:fhe:error:validation" -> ValidationFailed(
                    message = detail,
                    invalidParams = problem.invalidParams?.map {
                        InvalidParam(it.name, it.reason)
                    } ?: emptyList(),
                )
                "urn:fhe:error:invalid-address" -> InvalidAddress(detail)
                "urn:fhe:error:invalid-value" -> InvalidValue(detail)
                "urn:fhe:error:unsupported-type" -> UnsupportedType(detail)
                "urn:fhe:error:encryption-failed" -> EncryptionFailed(detail)
                "urn:fhe:error:internal" -> InternalServerError(detail)
                "urn:fhe:error:not-initialized" -> NotInitialized(detail)
                "urn:fhe:error:initialization-failed" -> InitializationFailed(detail)
                "urn:fhe:error:pool-exhausted" -> PoolExhausted(detail)
                "urn:fhe:error:timeout" -> Timeout(detail)
                else -> UnknownServerError(detail, problem.status)
            }
        }
    }
}
