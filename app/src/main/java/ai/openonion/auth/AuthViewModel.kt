package ai.openonion.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ai.openonion.auth.data.CredentialVault
import ai.openonion.auth.model.TotpCredential
import ai.openonion.auth.otp.OtpAuthParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = CredentialVault(application)
    private val mutableCredentials = MutableStateFlow(vault.loadAll())

    val credentials: StateFlow<List<TotpCredential>> = mutableCredentials.asStateFlow()

    fun importCredential(rawUri: String): String? {
        return try {
            val credential = OtpAuthParser.parse(rawUri)
            check(
                mutableCredentials.value.none {
                    it.issuer == credential.issuer && it.accountName == credential.accountName
                },
            ) { "That account is already in OpenOnion Auth." }
            vault.save(credential)
            mutableCredentials.value = vault.loadAll()
            null
        } catch (exception: Exception) {
            exception.message ?: "OpenOnion Auth could not import that credential."
        }
    }

    fun deleteCredential(credential: TotpCredential) {
        vault.delete(credential.id)
        mutableCredentials.value = vault.loadAll()
    }
}
