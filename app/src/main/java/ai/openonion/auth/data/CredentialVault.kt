package ai.openonion.auth.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import ai.openonion.auth.model.TotpAlgorithm
import ai.openonion.auth.model.TotpCredential
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CredentialVault(context: Context) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(keyStoreProvider).apply { load(null) }

    fun loadAll(): List<TotpCredential> {
        return preferences.getStringSet(indexKey, emptySet()).orEmpty()
            .sorted()
            .map { id ->
                val envelope = requireNotNull(preferences.getString(entryPrefix + id, null)) {
                    "The encrypted credential index is inconsistent."
                }
                decrypt(id, envelope)
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayIssuer })
    }

    fun save(credential: TotpCredential) {
        val ids = preferences.getStringSet(indexKey, emptySet()).orEmpty().toMutableSet()
        ids += credential.id
        preferences.edit {
            putString(entryPrefix + credential.id, encrypt(credential))
            putStringSet(indexKey, ids)
        }
    }

    fun delete(id: String) {
        val ids = preferences.getStringSet(indexKey, emptySet()).orEmpty().toMutableSet()
        ids -= id
        preferences.edit {
            remove(entryPrefix + id)
            putStringSet(indexKey, ids)
        }
    }

    private fun encrypt(credential: TotpCredential): String {
        val cipher = Cipher.getInstance(cipherTransformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(credential.id.toByteArray(StandardCharsets.UTF_8))
        val plaintext = credential.toJson().toString().toByteArray(StandardCharsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintext)
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf(
            envelopeVersion,
            encoder.encodeToString(cipher.iv),
            encoder.encodeToString(ciphertext),
        ).joinToString(".")
    }

    private fun decrypt(id: String, envelope: String): TotpCredential {
        val parts = envelope.split('.')
        require(parts.size == 3 && parts[0] == envelopeVersion) {
            "The encrypted credential envelope is not supported."
        }
        val decoder = Base64.getUrlDecoder()
        val iv = decoder.decode(parts[1])
        val ciphertext = decoder.decode(parts[2])
        val cipher = Cipher.getInstance(cipherTransformation)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        cipher.updateAAD(id.toByteArray(StandardCharsets.UTF_8))
        val plaintext = cipher.doFinal(ciphertext)
        return JSONObject(String(plaintext, StandardCharsets.UTF_8)).toCredential(id)
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUnlockedDeviceRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun TotpCredential.toJson(): JSONObject {
        return JSONObject()
            .put("issuer", issuer)
            .put("accountName", accountName)
            .put("secret", Base64.getEncoder().encodeToString(secret))
            .put("algorithm", algorithm.name)
            .put("digits", digits)
            .put("periodSeconds", periodSeconds)
    }

    private fun JSONObject.toCredential(id: String): TotpCredential {
        return TotpCredential(
            id = id,
            issuer = getString("issuer"),
            accountName = getString("accountName"),
            secret = Base64.getDecoder().decode(getString("secret")),
            algorithm = TotpAlgorithm.valueOf(getString("algorithm")),
            digits = getInt("digits"),
            periodSeconds = getInt("periodSeconds"),
        )
    }

    private companion object {
        const val preferencesName = "encrypted_totp_vault"
        const val indexKey = "credential_ids"
        const val entryPrefix = "credential."
        const val keyStoreProvider = "AndroidKeyStore"
        const val keyAlias = "openonion.auth.vault.v1"
        const val cipherTransformation = "AES/GCM/NoPadding"
        const val envelopeVersion = "v1"
    }
}
