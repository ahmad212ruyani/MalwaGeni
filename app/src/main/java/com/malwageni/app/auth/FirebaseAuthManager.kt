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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.malwageni.app.R
import com.malwageni.app.model.UserAccount
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

sealed class AuthResult {
    data class Success(val user: UserAccount) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Cancelled : AuthResult()
}

class FirebaseAuthManager(private val context: Context) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): AuthResult {
        return try {
            val serverClientId = context.getString(R.string.default_web_client_id)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Max 15 seconds timeout for selecting Google Account
            val response: GetCredentialResponse? = withTimeoutOrNull(15000L) {
                credentialManager.getCredential(request = request, context = context)
            }

            if (response == null) {
                return AuthResult.Error("Waktu pemilihan akun Google habis. Silakan coba kembali atau gunakan Mode Tamu.")
            }

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Sign in to Firebase Auth with timeout
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = withTimeoutOrNull(10000L) {
                    firebaseAuth.signInWithCredential(authCredential).await()
                }

                if (authResult?.user != null) {
                    val firebaseUser = authResult.user!!
                    val user = UserAccount(
                        id = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: googleIdTokenCredential.displayName ?: "Pengguna Google",
                        email = firebaseUser.email ?: googleIdTokenCredential.id,
                        profilePictureUrl = firebaseUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                        isAnonymous = false
                    )
                    AuthResult.Success(user)
                } else {
                    // Fallback to Google Id info directly so user is never stuck
                    val fallbackUser = UserAccount(
                        id = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: "Pengguna Google",
                        email = googleIdTokenCredential.id,
                        profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                        isAnonymous = false
                    )
                    AuthResult.Success(fallbackUser)
                }
            } else {
                AuthResult.Error("Kredensial tidak dikenali.")
            }
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: GetCredentialException) {
            AuthResult.Error("Google Play Services: ${e.localizedMessage ?: "Gagal memuat akun Google"}")
        } catch (e: FirebaseAuthException) {
            val msg = if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" || e.message?.contains("disabled", ignoreCase = true) == true) {
                "Google Sign-In belum diaktifkan di Firebase Console (Authentication > Sign-in method). Silakan aktifkan terlebih dahulu."
            } else {
                e.localizedMessage ?: "Terjadi kendala autentikasi Firebase."
            }
            AuthResult.Error(msg)
        } catch (e: Exception) {
            AuthResult.Error("Kendala: ${e.localizedMessage ?: "Gagal terhubung"}")
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        return try {
            val authResult = withTimeoutOrNull(10000L) {
                firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            }

            if (authResult?.user != null) {
                val firebaseUser = authResult.user!!
                val user = UserAccount(
                    id = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: email.substringBefore("@"),
                    email = firebaseUser.email ?: email,
                    profilePictureUrl = firebaseUser.photoUrl?.toString(),
                    isAnonymous = false
                )
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Waktu koneksi habis. Periksa koneksi internet Anda.")
            }
        } catch (e: FirebaseAuthException) {
            val msg = if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" || e.message?.contains("disabled", ignoreCase = true) == true) {
                "Email/Password belum diaktifkan di Firebase Console (Authentication > Sign-in method)."
            } else if (e.errorCode == "ERROR_WRONG_PASSWORD" || e.errorCode == "ERROR_INVALID_CREDENTIAL") {
                "Kata sandi salah. Silakan periksa kembali."
            } else if (e.errorCode == "ERROR_USER_NOT_FOUND") {
                "Akun email belum terdaftar. Silakan daftar terlebih dahulu."
            } else {
                e.localizedMessage ?: "Gagal masuk."
            }
            AuthResult.Error(msg)
        } catch (e: Exception) {
            AuthResult.Error("Gagal masuk: ${e.localizedMessage ?: "Periksa email dan password."}")
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String): AuthResult {
        return try {
            val authResult = withTimeoutOrNull(10000L) {
                firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            }

            if (authResult?.user != null) {
                val firebaseUser = authResult.user!!
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim())
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                } catch (_: Exception) {}

                val user = UserAccount(
                    id = firebaseUser.uid,
                    displayName = name.trim(),
                    email = firebaseUser.email ?: email,
                    profilePictureUrl = null,
                    isAnonymous = false
                )
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Waktu pendaftaran habis. Periksa koneksi internet Anda.")
            }
        } catch (e: FirebaseAuthException) {
            val msg = if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" || e.message?.contains("disabled", ignoreCase = true) == true) {
                "Email/Password belum diaktifkan di Firebase Console (Authentication > Sign-in method)."
            } else if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                "Email sudah terdaftar. Silakan pilih menu Masuk."
            } else {
                e.localizedMessage ?: "Gagal mendaftar."
            }
            AuthResult.Error(msg)
        } catch (e: Exception) {
            AuthResult.Error("Gagal mendaftar: ${e.localizedMessage ?: "Silakan coba lagi."}")
        }
    }

    fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {}
    }

    fun getCurrentFirebaseUser(): UserAccount? {
        val fbUser = firebaseAuth.currentUser ?: return null
        return UserAccount(
            id = fbUser.uid,
            displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Pengguna",
            email = fbUser.email ?: "",
            profilePictureUrl = fbUser.photoUrl?.toString(),
            isAnonymous = fbUser.isAnonymous
        )
    }

    /**
     * Instant local guest session. Never hangs, never requires network.
     */
    fun createInstantGuestSession(): UserAccount {
        return UserAccount(
            id = "guest_local",
            displayName = "Tamu Toko (Mode Offline)",
            email = "offline@malwageni.local",
            profilePictureUrl = null,
            isAnonymous = true
        )
    }
}
