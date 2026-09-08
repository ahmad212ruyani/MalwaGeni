package com.malwageni.app.auth

import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
    object AwaitingIntent : AuthResult()
}

class FirebaseAuthManager(private val context: Context) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getGoogleSignInClient(activityContext: Context): GoogleSignInClient {
        val serverClientId = activityContext.getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activityContext, gso)
    }

    suspend fun signInWithGoogle(
        activityContext: Context,
        onFallbackIntent: (Intent) -> Unit
    ): AuthResult {
        val serverClientId = activityContext.getString(R.string.default_web_client_id)

        try {
            val credManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Max 15 seconds timeout for Credential Manager
            val response: GetCredentialResponse? = withTimeoutOrNull(15000L) {
                credManager.getCredential(request = request, context = activityContext)
            }

            if (response == null) {
                return launchGoogleSignInIntent(activityContext, onFallbackIntent)
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
                    return AuthResult.Success(user)
                } else {
                    val fallbackUser = UserAccount(
                        id = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: "Pengguna Google",
                        email = googleIdTokenCredential.id,
                        profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                        isAnonymous = false
                    )
                    return AuthResult.Success(fallbackUser)
                }
            } else {
                return launchGoogleSignInIntent(activityContext, onFallbackIntent)
            }
        } catch (e: GetCredentialCancellationException) {
            return AuthResult.Cancelled
        } catch (e: Exception) {
            // When Credential Manager fails with NoCredentialException or any other error,
            // immediately trigger legacy GoogleSignInClient picker fallback!
            return launchGoogleSignInIntent(activityContext, onFallbackIntent)
        }
    }

    fun launchGoogleSignInIntent(
        activityContext: Context,
        onFallbackIntent: (Intent) -> Unit
    ): AuthResult {
        return try {
            val client = getGoogleSignInClient(activityContext)
            try {
                client.signOut()
            } catch (_: Exception) {}
            onFallbackIntent(client.signInIntent)
            AuthResult.AwaitingIntent
        } catch (e: Exception) {
            AuthResult.Error("Gagal membuka akun Google: ${e.localizedMessage ?: "Terjadi kendala"}")
        }
    }

    suspend fun handleGoogleSignInIntentResult(data: Intent?): AuthResult {
        if (data == null) {
            return AuthResult.Cancelled
        }
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken.isNullOrBlank()) {
                return AuthResult.Error("Token Google tidak ditemukan. Pastikan akun Google terhubung di perangkat Anda.")
            }

            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = withTimeoutOrNull(10000L) {
                firebaseAuth.signInWithCredential(authCredential).await()
            }

            if (authResult?.user != null) {
                val fbUser = authResult.user!!
                val user = UserAccount(
                    id = fbUser.uid,
                    displayName = fbUser.displayName ?: account.displayName ?: "Pengguna Google",
                    email = fbUser.email ?: account.email ?: "",
                    profilePictureUrl = fbUser.photoUrl?.toString() ?: account.photoUrl?.toString(),
                    isAnonymous = false
                )
                AuthResult.Success(user)
            } else {
                val fallbackUser = UserAccount(
                    id = account.id ?: "google_user",
                    displayName = account.displayName ?: "Pengguna Google",
                    email = account.email ?: "",
                    profilePictureUrl = account.photoUrl?.toString(),
                    isAnonymous = false
                )
                AuthResult.Success(fallbackUser)
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                12501 -> AuthResult.Cancelled
                12500 -> AuthResult.Error("Google Sign-In belum aktif di Firebase (Status 12500). Solusi instan: Gunakan menu Masuk/Daftar dengan Email di bawah ini.")
                10 -> AuthResult.Error("Konfigurasi Google SHA-1 belum cocok di Firebase (Kode 10). Solusi instan: Silakan Masuk atau Daftar langsung menggunakan Email & Kata Sandi di bawah.")
                7 -> AuthResult.Error("Koneksi jaringan bermasalah saat menghubungkan ke Google Play Services.")
                else -> AuthResult.Error("Google Play Services (Kode ${e.statusCode}): ${e.localizedMessage ?: "Gagal memproses login"}. Anda bisa langsung Masuk/Daftar dengan Email di bawah.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Gagal autentikasi Google: ${e.localizedMessage ?: "Terjadi kesalahan"}")
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
     * Authenticates as Anonymous user in Firebase Auth.
     * If anonymous auth is enabled in Firebase Console, gives real Cloud Firestore persistence.
     * Falls back to local guest session if offline or not enabled.
     */
    suspend fun signInAnonymously(): AuthResult {
        return try {
            val authResult = withTimeoutOrNull(8000L) {
                firebaseAuth.signInAnonymously().await()
            }
            if (authResult?.user != null) {
                val fbUser = authResult.user!!
                val user = UserAccount(
                    id = fbUser.uid,
                    displayName = "Tamu Toko (Cloud)",
                    email = "guest@malwageni.app",
                    profilePictureUrl = null,
                    isAnonymous = true
                )
                AuthResult.Success(user)
            } else {
                AuthResult.Success(createInstantGuestSession())
            }
        } catch (_: Exception) {
            AuthResult.Success(createInstantGuestSession())
        }
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
