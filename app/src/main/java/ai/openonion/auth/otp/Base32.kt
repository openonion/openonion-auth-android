package ai.openonion.auth.otp

object Base32 {
    private const val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(value: String): ByteArray {
        val normalized = value
            .uppercase()
            .filterNot { it == ' ' || it == '-' || it == '=' }
        require(normalized.isNotEmpty()) { "The TOTP secret is empty." }

        var buffer = 0
        var bitsInBuffer = 0
        val result = ArrayList<Byte>((normalized.length * 5) / 8)

        normalized.forEach { character ->
            val digit = alphabet.indexOf(character)
            require(digit >= 0) { "The TOTP secret is not valid Base32." }
            buffer = (buffer shl 5) or digit
            bitsInBuffer += 5

            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8
                result += ((buffer shr bitsInBuffer) and 0xff).toByte()
                buffer = buffer and ((1 shl bitsInBuffer) - 1)
            }
        }

        require(result.isNotEmpty()) { "The TOTP secret is too short." }
        return result.toByteArray()
    }
}
