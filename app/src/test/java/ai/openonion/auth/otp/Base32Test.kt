package ai.openonion.auth.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class Base32Test {
    @Test
    fun decodesRfc4648Vector() {
        assertArrayEquals("foobar".toByteArray(), Base32.decode("MZXW6YTBOI======"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidAlphabet() {
        Base32.decode("NOT-BASE32-0")
    }
}
