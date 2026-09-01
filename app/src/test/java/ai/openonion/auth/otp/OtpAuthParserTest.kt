package ai.openonion.auth.otp

import ai.openonion.auth.model.TotpAlgorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpAuthParserTest {
    @Test
    fun parsesStandardTotpUri() {
        val credential = OtpAuthParser.parse(
            "otpauth://totp/Example:alice%40example.com" +
                "?secret=JBSWY3DPEHPK3PXP&issuer=Example&algorithm=SHA256&digits=8&period=45",
            id = "fixture-id",
        )

        assertEquals("fixture-id", credential.id)
        assertEquals("Example", credential.issuer)
        assertEquals("alice@example.com", credential.accountName)
        assertEquals(TotpAlgorithm.SHA256, credential.algorithm)
        assertEquals(8, credential.digits)
        assertEquals(45, credential.periodSeconds)
        assertTrue(credential.secret.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHotpInFirstRelease() {
        OtpAuthParser.parse("otpauth://hotp/Example:alice?secret=JBSWY3DPEHPK3PXP&counter=1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMismatchedIssuer() {
        OtpAuthParser.parse(
            "otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP&issuer=Different",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateSecret() {
        OtpAuthParser.parse(
            "otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP&secret=JBSWY3DPEHPK3PXP",
        )
    }
}
