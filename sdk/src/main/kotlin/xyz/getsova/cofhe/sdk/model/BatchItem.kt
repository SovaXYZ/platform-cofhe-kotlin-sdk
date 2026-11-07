package xyz.getsova.cofhe.sdk.model

import java.math.BigInteger

/**
 * Represents a single item in a batch encryption request.
 *
 * Use factory methods from companion object to create instances with compile-time type safety
 * and automatic validation.
 *
 * Example:
 * ```kotlin
 * val items = listOf(
 *     BatchItem.uint64(BigInteger("1000000")),
 *     BatchItem.bool(true),
 *     BatchItem.address("0x1234...")
 * )
 * ```
 */
class BatchItem internal constructor(
    val type: EncryptionType,
    val value: Any,
) {
    companion object {
        private val UINT8_MAX = BigInteger.valueOf(255)
        private val UINT16_MAX = BigInteger.valueOf(65535)
        private val UINT32_MAX = BigInteger.valueOf(4294967295)
        private val UINT64_MAX = BigInteger("18446744073709551615")
        private val UINT128_MAX = BigInteger.TWO.pow(128).subtract(BigInteger.ONE)
        private val UINT256_MAX = BigInteger.TWO.pow(256).subtract(BigInteger.ONE)
        private val ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")

        fun uint8(value: Int): BatchItem {
            require(value in 0..255) { "uint8 value must be in range 0..255, got $value" }
            return BatchItem(EncryptionType.UINT8, value)
        }

        fun uint16(value: Int): BatchItem {
            require(value in 0..65535) { "uint16 value must be in range 0..65535, got $value" }
            return BatchItem(EncryptionType.UINT16, value)
        }

        fun uint32(value: Long): BatchItem {
            require(value in 0..4294967295) { "uint32 value must be in range 0..4294967295, got $value" }
            return BatchItem(EncryptionType.UINT32, value.toString())
        }

        fun uint64(value: BigInteger): BatchItem {
            require(value >= BigInteger.ZERO && value <= UINT64_MAX) {
                "uint64 value must be in range 0..2^64-1, got $value"
            }
            return BatchItem(EncryptionType.UINT64, value.toString())
        }

        fun uint128(value: BigInteger): BatchItem {
            require(value >= BigInteger.ZERO && value <= UINT128_MAX) {
                "uint128 value must be in range 0..2^128-1, got $value"
            }
            return BatchItem(EncryptionType.UINT128, value.toString())
        }

        fun uint256(value: BigInteger): BatchItem {
            require(value >= BigInteger.ZERO && value <= UINT256_MAX) {
                "uint256 value must be in range 0..2^256-1, got $value"
            }
            return BatchItem(EncryptionType.UINT256, value.toString())
        }

        fun address(value: String): BatchItem {
            require(ADDRESS_REGEX.matches(value)) {
                "address must match 0x followed by 40 hex characters, got $value"
            }
            return BatchItem(EncryptionType.ADDRESS, value)
        }

        fun bool(value: Boolean): BatchItem {
            return BatchItem(EncryptionType.BOOL, value)
        }
    }
}
