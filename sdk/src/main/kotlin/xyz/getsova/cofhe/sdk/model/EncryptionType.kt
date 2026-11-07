package xyz.getsova.cofhe.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EncryptionType(val typeName: String) {
    @SerialName("euint8") UINT8("euint8"),
    @SerialName("euint16") UINT16("euint16"),
    @SerialName("euint32") UINT32("euint32"),
    @SerialName("euint64") UINT64("euint64"),
    @SerialName("euint128") UINT128("euint128"),
    @SerialName("euint256") UINT256("euint256"),
    @SerialName("eaddress") ADDRESS("eaddress"),
    @SerialName("ebool") BOOL("ebool"),
}
