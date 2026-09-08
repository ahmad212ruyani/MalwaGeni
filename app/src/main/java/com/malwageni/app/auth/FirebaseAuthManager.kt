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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.malwageni.app.R
import com.malwageni.app.model.UserAccount
import kotlinx.coroutines.tasks.await

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

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Sign in to Firebase Auth with the Google ID Token
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val user = UserAccount(
                        id = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: googleIdTokenCredential.displayName ?: "Pengguna Google",
                        email = firebaseUser.email ?: googleIdTokenCredential.id,
                        profilePictureUrl = firebaseUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                        isAnonymous = false
                    )
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Gagal mengautentikasi profil Firebase.")
                }
            } else {
                AuthResult.Error("Tipe kredensial tidak dikenali.")
            }
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: GetCredentialException) {
            AuthResult.Error("Gagal menghubungkan akun Google: ${e.localizedMessage ?: "Terjadi kesalahan"}")
        } catch (e: Exception) {
            AuthResult.Error("Terjadi kendala autentikasi Google: ${e.localizedMessage ?: "Kesalahan tak terduga"}")
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val user = UserAccount(
                    id = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: email.substringBefore("@"),
                    email = firebaseUser.email ?: email,
                    profilePictureUrl = firebaseUser.photoUrl?.toString(),
                    isAnonymous = false
                )
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Gagal masuk dengan email.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Gagal masuk: ${e.localizedMessage ?: "Periksa email dan kata sandi Anda."}")
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String): AuthResult {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                val user = UserAccount(
                    id = firebaseUser.uid,
                    displayName = name.trim(),
                    email = firebaseUser.email ?: email,
                    profilePictureUrl = null,
                    isAnonymous = false
                )
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Gagal mendaftarkan akun.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Gagal mendaftar: ${e.localizedMessage ?: "Silakan coba lagi."}")
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
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

    suspend fun signInAnonymously(): AuthResult {
        return try {
            val result = firebaseAuth.signInAnonymously().await()
            val fbUser = result.user
            val user = UserAccount(
                id = fbUser?.uid ?: ("guest_" + System.currentTimeMillis()),
                displayName = "Tamu MalwaGeni (Uji Coba)",
                email = "offline.guest@malwageni.local",
                profilePictureUrl = null,
                isAnonymous = true
            )
            AuthResult.Success(user)
        } catch (e: Exception) {
            // Local fallback if offline
            AuthResult.Success(
                UserAccount(
                    id = "guest_" + System.currentTimeMillis(),
                    displayName = "Tamu MalwaGeni (Offline)",
                    email = "offline.guest@malwageni.local",
                    profilePictureUrl = null,
                    isAnonymous = true
                )
            )
        }
    }
}
