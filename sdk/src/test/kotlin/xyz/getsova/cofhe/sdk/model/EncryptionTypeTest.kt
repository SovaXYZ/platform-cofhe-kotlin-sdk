package xyz.getsova.cofhe.sdk.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EncryptionTypeTest {

    @Test
    fun `should serialize to JSON string`() {
        assertEquals("\"euint64\"", Json.encodeToString(EncryptionType.UINT64))
        assertEquals("\"ebool\"", Json.encodeToString(EncryptionType.BOOL))
        assertEquals("\"eaddress\"", Json.encodeToString(EncryptionType.ADDRESS))
    }

    @Test
    fun `should deserialize from JSON string`() {
        assertEquals(EncryptionType.UINT8, Json.decodeFromString("\"euint8\""))
        assertEquals(EncryptionType.UINT256, Json.decodeFromString("\"euint256\""))
    }

    @Test
    fun `should have correct typeName`() {
        assertEquals("euint8", EncryptionType.UINT8.typeName)
        assertEquals("euint16", EncryptionType.UINT16.typeName)
        assertEquals("euint32", EncryptionType.UINT32.typeName)
        assertEquals("euint64", EncryptionType.UINT64.typeName)
        assertEquals("euint128", EncryptionType.UINT128.typeName)
        assertEquals("euint256", EncryptionType.UINT256.typeName)
        assertEquals("eaddress", EncryptionType.ADDRESS.typeName)
        assertEquals("ebool", EncryptionType.BOOL.typeName)
    }

    @Test
    fun `should have 8 enum entries`() {
        assertEquals(8, EncryptionType.entries.size)
    }
}
