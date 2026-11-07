# CoFHE Kotlin SDK

[![CI](https://github.com/SovaXYZ/platform-cofhe-kotlin-sdk/actions/workflows/ci.yml/badge.svg)](https://github.com/SovaXYZ/platform-cofhe-kotlin-sdk/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Kotlin SDK for [Sova CoFHE](https://github.com/SovaXYZ/platform-cofhe-nodejs-worker) — Fully Homomorphic Encryption (FHE) service powered by Fhenix CoFHE.

## Features

- ✅ **Type-safe API** — Compile-time validation for all FHE types
- ✅ **Coroutines support** — All methods are `suspend` functions
- ✅ **Batch encryption** — Encrypt up to 10 values in a single request
- ✅ **Error handling** — RFC 7807 Problem Details with typed exceptions
- ✅ **Health checks** — Liveness and readiness probes
- ✅ **Pure JVM** — Works with any Kotlin/JVM project

## Installation

### GitHub Packages

GitHub Packages requires authentication. Create a [Personal Access Token](https://github.com/settings/tokens) with `read:packages` scope and add to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=ghp_YOUR_TOKEN
```

Then add to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/SovaXYZ/platform-cofhe-kotlin-sdk")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("xyz.getsova:cofhe-sdk:0.1.0")
}
```

## Quick Start

```kotlin
import xyz.getsova.cofhe.sdk.CoFheClient
import xyz.getsova.cofhe.sdk.model.BatchItem
import java.math.BigInteger

suspend fun main() {
    val client = CoFheClient {
        baseUrl = "https://cofhe.getsova.xyz"
    }

    // Encrypt a single value
    val encrypted = client.encryptUint64(
        value = BigInteger("1000000"),
        userAddress = "0x1234567890123456789012345678901234567890"
    )
    println("Encrypted: ${encrypted.data}")
    println("Proof: ${encrypted.inputProof}")

    // Batch encryption
    val batch = client.encryptBatch(
        userAddress = "0x1234567890123456789012345678901234567890",
        items = listOf(
            BatchItem.uint64(BigInteger("1000000")),
            BatchItem.bool(true),
            BatchItem.address("0xabcdef0123456789abcdef0123456789abcdef01")
        )
    )
    println("Encrypted ${batch.results.size} values in ${batch.totalEncryptionTimeMs}ms")

    client.close()
}
```

## API Reference

### Typed Endpoints

```kotlin
// 8-bit unsigned integer (0..255)
client.encryptUint8(value: Int, userAddress: String): EncryptedValue

// 16-bit unsigned integer (0..65535)
client.encryptUint16(value: Int, userAddress: String): EncryptedValue

// 32-bit unsigned integer (0..4294967295)
client.encryptUint32(value: Long, userAddress: String): EncryptedValue

// 64-bit unsigned integer
client.encryptUint64(value: BigInteger, userAddress: String): EncryptedValue

// 128-bit unsigned integer
client.encryptUint128(value: BigInteger, userAddress: String): EncryptedValue

// 256-bit unsigned integer
client.encryptUint256(value: BigInteger, userAddress: String): EncryptedValue

// Ethereum address (0x + 40 hex chars)
client.encryptAddress(value: String, userAddress: String): EncryptedValue

// Boolean
client.encryptBool(value: Boolean, userAddress: String): EncryptedValue
```

### Generic Endpoint

```kotlin
client.encrypt(
    type: EncryptionType,
    value: Any,
    userAddress: String
): EncryptedValue
```

### Batch Encryption

```kotlin
client.encryptBatch(
    userAddress: String,
    items: List<BatchItem>
): BatchEncryptResult

// Factory methods for BatchItem
BatchItem.uint8(value: Int)
BatchItem.uint16(value: Int)
BatchItem.uint32(value: Long)
BatchItem.uint64(value: BigInteger)
BatchItem.uint128(value: BigInteger)
BatchItem.uint256(value: BigInteger)
BatchItem.address(value: String)
BatchItem.bool(value: Boolean)
```

### Health Checks

```kotlin
// Liveness probe (returns true if service is running)
client.isAlive(): Boolean

// Readiness probe (returns true if FHE SDK is initialized)
client.isReady(): Boolean

// Detailed readiness status
client.healthReady(): HealthStatus
```

## Configuration

```kotlin
val client = CoFheClient {
    baseUrl = "https://cofhe.getsova.xyz"
    requestTimeout = 120.seconds  // default: 60s
    connectTimeout = 10.seconds   // default: 10s
    socketTimeout = 60.seconds    // default: 60s
    enableLogging = true          // default: false
    engine = mockEngine           // for testing (default: OkHttp)
}
```

## Error Handling

All errors throw `CoFheException` sealed class with specific subtypes:

```kotlin
try {
    client.encryptUint64(value, userAddress)
} catch (e: CoFheException.Timeout) {
    // Retry
} catch (e: CoFheException.NotInitialized) {
    // Wait and retry
} catch (e: CoFheException.ValidationFailed) {
    // Check invalidParams
    e.invalidParams.forEach { println("${it.name}: ${it.reason}") }
} catch (e: CoFheException.NetworkError) {
    // Connection failed
} catch (e: CoFheException) {
    // HTTP status code available
    println("Error: ${e.message} (HTTP ${e.statusCode})")
}
```

### Exception Types

**Server errors:**
- `ValidationFailed` (400) — Request validation error with `invalidParams`
- `InvalidAddress` (422) — Invalid Ethereum address format
- `InvalidValue` (422) — Value out of range for type
- `UnsupportedType` (422) — Unknown encryption type
- `EncryptionFailed` (500) — FHE encryption operation failed
- `InternalServerError` (500) — Generic server error
- `NotInitialized` (503) — FHE SDK not initialized yet
- `InitializationFailed` (503) — FHE SDK initialization failed
- `PoolExhausted` (503) — Too many concurrent requests
- `Timeout` (504) — Encryption timeout
- `UnknownServerError` — Unknown URN from server

**Client errors:**
- `NetworkError` — Connection/DNS/TLS failure
- `SerializationError` — JSON parsing failure

## Response Model

```kotlin
data class EncryptedValue(
    val type: EncryptionType,       // euint64, ebool, etc.
    val data: String,               // hex ctHash for smart contract
    val securityZone: Int,          // FHE security zone
    val utype: Int,                 // FHE type identifier
    val inputProof: String,         // hex ZK proof
    val encryptionTimeMs: Long      // encryption duration
)
```

## Testing

### Unit Tests
The SDK uses Ktor `MockEngine` for unit testing:

```kotlin
val mockEngine = MockEngine { request ->
    respond(
        content = """{"type":"euint64","data":"0x123..."}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}

val client = CoFheClient {
    baseUrl = "http://test"
    engine = mockEngine
}
```

### Integration Tests
Integration tests use **WireMock** to test real HTTP communication without requiring a running service:

```kotlin
@Tag("integration")
class CoFheClientIntegrationTest {
    @RegisterExtension
    val wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build()

    @Test
    fun test() {
        wireMock.stubFor(post("/api/v1/encrypt/uint64")
            .willReturn(aResponse().withStatus(200)...))

        val client = CoFheClient { baseUrl = wireMock.baseUrl() }
        // Test real HTTP communication
    }
}
```

### E2E Tests
End-to-end tests run against a real CoFHE service (manual testing only):

Run tests:
```bash
./gradlew :sdk:test                # Unit tests (61 tests)
./gradlew :sdk:integrationTest     # Integration tests with WireMock (9 tests)
./gradlew :sdk:e2eTest             # E2E tests (requires COFHE_BASE_URL env var)
./gradlew :sdk:check               # All checks (unit + integration tests)
```

## Platform Support

- **JVM:** Targets JVM 17 bytecode, built with Java 21 toolchain
- **Android:** Can be converted to AAR if needed (change Gradle plugin)
- **iOS:** Kotlin Multiplatform support possible in future

## Development

```bash
# Build
./gradlew :sdk:build

# Run tests
./gradlew :sdk:test

# Publish to GitHub Packages
SDK_VERSION=0.1.0 ./gradlew :sdk:publish
```

## License

MIT

## Links

- [CoFHE Worker Service](https://github.com/SovaXYZ/platform-cofhe-nodejs-worker)
- [Fhenix CoFHE](https://docs.fhenix.io/)
- [API Documentation](https://cofhe.getsova.xyz/api/docs)
