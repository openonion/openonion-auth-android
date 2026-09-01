package ai.openonion.auth.model

data class TotpCredential(
    val id: String,
    val issuer: String,
    val accountName: String,
    val secret: ByteArray,
    val algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,
    val digits: Int = 6,
    val periodSeconds: Int = 30,
) {
    val displayIssuer: String
        get() = issuer.ifBlank { accountName }
}

enum class TotpAlgorithm(val macName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
}
