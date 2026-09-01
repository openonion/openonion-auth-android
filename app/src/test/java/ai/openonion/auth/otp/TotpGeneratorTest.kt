package ai.openonion.auth.otp

import ai.openonion.auth.model.TotpAlgorithm
import org.junit.Assert.assertEquals
import org.junit.Test

class TotpGeneratorTest {
    @Test
    fun matchesRfc6238PublishedVectors() {
        val sha1Secret = "12345678901234567890".toByteArray()
        val sha256Secret = "12345678901234567890123456789012".toByteArray()
        val sha512Secret = (
            "12345678901234567890123456789012" +
                "34567890123456789012345678901234"
            ).toByteArray()
        val vectors = listOf(
            Vector(59L, "94287082", "46119246", "90693936"),
            Vector(1_111_111_109L, "07081804", "68084774", "25091201"),
            Vector(1_111_111_111L, "14050471", "67062674", "99943326"),
            Vector(1_234_567_890L, "89005924", "91819424", "93441116"),
            Vector(2_000_000_000L, "69279037", "90698825", "38618901"),
            Vector(20_000_000_000L, "65353130", "77737706", "47863826"),
        )

        vectors.forEach { vector ->
            val timestamp = vector.timestampSeconds * 1_000L
            assertEquals(
                vector.sha1,
                TotpGenerator.generate(sha1Secret, timestamp, digits = 8),
            )
            assertEquals(
                vector.sha256,
                TotpGenerator.generate(
                    sha256Secret,
                    timestamp,
                    digits = 8,
                    algorithm = TotpAlgorithm.SHA256,
                ),
            )
            assertEquals(
                vector.sha512,
                TotpGenerator.generate(
                    sha512Secret,
                    timestamp,
                    digits = 8,
                    algorithm = TotpAlgorithm.SHA512,
                ),
            )
        }
    }

    @Test
    fun reportsSecondsUntilNextPeriod() {
        assertEquals(30, TotpGenerator.secondsRemaining(0L, 30))
        assertEquals(1, TotpGenerator.secondsRemaining(29_999L, 30))
        assertEquals(30, TotpGenerator.secondsRemaining(30_000L, 30))
    }

    private data class Vector(
        val timestampSeconds: Long,
        val sha1: String,
        val sha256: String,
        val sha512: String,
    )
}
