package ai.openonion.auth.otp

import ai.openonion.auth.model.TotpAlgorithm
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TotpGenerator {
    fun generate(
        secret: ByteArray,
        timestampMillis: Long,
        periodSeconds: Int = 30,
        digits: Int = 6,
        algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,
    ): String {
        require(secret.isNotEmpty()) { "TOTP secret must not be empty." }
        require(periodSeconds > 0) { "TOTP period must be positive." }
        require(digits == 6 || digits == 8) { "TOTP digits must be 6 or 8." }

        val counter = timestampMillis / 1_000L / periodSeconds
        val message = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(counter).array()
        val mac = Mac.getInstance(algorithm.macName)
        mac.init(SecretKeySpec(secret, algorithm.macName))
        val digest = mac.doFinal(message)

        val offset = digest.last().toInt() and 0x0f
        val binary = ((digest[offset].toInt() and 0x7f) shl 24) or
            ((digest[offset + 1].toInt() and 0xff) shl 16) or
            ((digest[offset + 2].toInt() and 0xff) shl 8) or
            (digest[offset + 3].toInt() and 0xff)
        val modulus = if (digits == 6) 1_000_000 else 100_000_000
        return (binary % modulus).toString().padStart(digits, '0')
    }

    fun secondsRemaining(timestampMillis: Long, periodSeconds: Int): Int {
        val elapsed = (timestampMillis / 1_000L) % periodSeconds
        return periodSeconds - elapsed.toInt()
    }
}
