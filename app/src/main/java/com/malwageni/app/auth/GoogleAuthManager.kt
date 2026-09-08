package com.malwageni.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.malwageni.app.R
import com.malwageni.app.model.UserAccount

sealed class AuthResult {
    data class Success(val user: UserAccount) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Cancelled : AuthResult()
}

class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): AuthResult {
        return try {
            val serverClientId = context.getString(R.string.default_web_client_id)

            // If user hasn't replaced placeholder yet, inform politely
            if (serverClientId == "YOUR_WEB_CLIENT_ID_HERE" || serverClientId.isBlank()) {
                return AuthResult.Error(
                    "Konfigurasi Google Client ID belum diisi di strings.xml. Anda dapat menggunakan tombol 'Lanjut Mode Tamu' untuk mencoba aplikasi."
                )
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResponse(response)
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: GetCredentialException) {
            AuthResult.Error("Gagal menghubungkan akun Google: ${e.localizedMessage ?: "Terjadi kesalahan"}")
        } catch (e: Exception) {
            AuthResult.Error("Terjadi kendala autentikasi: ${e.localizedMessage ?: "Kesalahan tak terduga"}")
        }
    }

    private fun handleSignInResponse(response: GetCredentialResponse): AuthResult {
        val credential = response.credential
        return if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val user = UserAccount(
                    id = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id,
                    email = googleIdTokenCredential.id, // Primary identifier / email
                    profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                    isAnonymous = false
                )
                AuthResult.Success(user)
            } catch (e: Exception) {
                AuthResult.Error("Gagal membaca kredensial Google: ${e.localizedMessage}")
            }
        } else {
            AuthResult.Error("Tipe kredensial tidak dikenali.")
        }
    }

    /**
     * Creates a local guest session for immediate offline testing.
     */
    fun createGuestSession(): UserAccount {
        return UserAccount(
            id = "guest_" + System.currentTimeMillis(),
            displayName = "Tamu MalwaGeni (Uji Coba)",
            email = "offline.guest@malwageni.local",
            profilePictureUrl = null,
            isAnonymous = true
        )
    }
}
