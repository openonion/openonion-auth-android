package ai.openonion.auth.otp

import ai.openonion.auth.model.TotpAlgorithm
import ai.openonion.auth.model.TotpCredential
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object OtpAuthParser {
    fun parse(rawValue: String, id: String = UUID.randomUUID().toString()): TotpCredential {
        val value = rawValue.trim()
        require(value.startsWith("otpauth://", ignoreCase = true)) {
            "Paste or scan an otpauth://totp QR code."
        }

        val uri = runCatching { URI(value) }
            .getOrElse { throw IllegalArgumentException("The otpauth URI is malformed.") }
        require(uri.scheme.equals("otpauth", ignoreCase = true)) {
            "Only otpauth URIs are supported."
        }
        require(uri.host.equals("totp", ignoreCase = true)) {
            "Version 0.1 supports TOTP only, not HOTP."
        }
        require(uri.fragment == null && uri.userInfo == null && uri.port == -1) {
            "The otpauth URI contains unsupported fields."
        }

        val label = decode(uri.rawPath.orEmpty().removePrefix("/"))
        require(label.isNotBlank()) { "The account label is missing." }
        val query = parseQuery(uri.rawQuery.orEmpty())
        val secret = Base32.decode(query.requireValue("secret"))

        val labelParts = label.split(':', limit = 2)
        val labelIssuer = if (labelParts.size == 2) labelParts[0].trim() else ""
        val accountName = if (labelParts.size == 2) labelParts[1].trim() else label.trim()
        require(accountName.isNotBlank()) { "The account name is missing." }

        val queryIssuer = query["issuer"]?.trim().orEmpty()
        require(labelIssuer.isBlank() || queryIssuer.isBlank() || labelIssuer == queryIssuer) {
            "The issuer in the label does not match the issuer parameter."
        }

        val algorithm = when (query["algorithm"]?.uppercase() ?: "SHA1") {
            "SHA1" -> TotpAlgorithm.SHA1
            "SHA256" -> TotpAlgorithm.SHA256
            "SHA512" -> TotpAlgorithm.SHA512
            else -> throw IllegalArgumentException("Unsupported TOTP algorithm.")
        }
        val digits = query["digits"]?.toIntOrNull() ?: 6
        require(digits == 6 || digits == 8) { "TOTP digits must be 6 or 8." }
        val period = query["period"]?.toIntOrNull() ?: 30
        require(period in 15..120) { "TOTP period must be between 15 and 120 seconds." }

        return TotpCredential(
            id = id,
            issuer = queryIssuer.ifBlank { labelIssuer },
            accountName = accountName,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            periodSeconds = period,
        )
    }

    private fun parseQuery(rawQuery: String): Map<String, String> {
        if (rawQuery.isBlank()) return emptyMap()
        val pairs = linkedMapOf<String, String>()
        rawQuery.split('&').forEach { component ->
            val parts = component.split('=', limit = 2)
            val key = decode(parts[0]).lowercase()
            require(key.isNotBlank() && key !in pairs) { "The otpauth URI has duplicate fields." }
            pairs[key] = decode(parts.getOrElse(1) { "" })
        }
        return pairs
    }

    private fun Map<String, String>.requireValue(key: String): String {
        return this[key]?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("The $key parameter is missing.")
    }

    private fun decode(value: String): String {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8.name())
    }
}
