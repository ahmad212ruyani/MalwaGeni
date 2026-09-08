package com.malwageni.app.auth

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malwageni.app.accessibility.AccessibilityUtils
import com.malwageni.app.model.UserAccount
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: UserAccount? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignUpMode: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = FirebaseAuthManager(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _announcements = MutableSharedFlow<String>()
    val announcements: SharedFlow<String> = _announcements.asSharedFlow()

    init {
        restorePersistedSession()
    }

    private fun restorePersistedSession() {
        val currentFbUser = authManager.getCurrentFirebaseUser()
        if (currentFbUser != null) {
            _uiState.update { it.copy(currentUser = currentFbUser) }
        }
    }

    fun toggleAuthMode() {
        _uiState.update {
            val newMode = !it.isSignUpMode
            announce(if (newMode) "Beralih ke formulir Pendaftaran Akun Baru" else "Beralih ke formulir Masuk dengan Akun")
            it.copy(isSignUpMode = newMode, errorMessage = null)
        }
    }

    fun signInWithGoogle(activityContext: Context, onFallbackIntent: (Intent) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            announce("Menghubungkan ke layanan Akun Google...")

            when (val result = authManager.signInWithGoogle(activityContext, onFallbackIntent)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(currentUser = result.user, isLoading = false) }
                    announce("Berhasil masuk menggunakan akun Google: ${result.user.displayName}. Data toko Anda tersinkronkan ke Firebase Cloud.")
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    announce("Gagal masuk akun Google: ${result.message}")
                }
                is AuthResult.Cancelled -> {
                    _uiState.update { it.copy(isLoading = false) }
                    announce("Masuk dengan Google dibatalkan.")
                }
                is AuthResult.AwaitingIntent -> {
                    announce("Membuka daftar akun Google. Silakan pilih akun Anda.")
                }
            }
        }
    }

    fun launchDirectGoogleSignIn(activityContext: Context, onFallbackIntent: (Intent) -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        announce("Membuka pilihan akun Google...")
        val result = authManager.launchGoogleSignInIntent(activityContext, onFallbackIntent)
        if (result is AuthResult.Error) {
            _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            announce("Gagal membuka akun Google: ${result.message}")
        }
    }

    fun handleGoogleSignInIntentResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authManager.handleGoogleSignInIntentResult(data)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(currentUser = result.user, isLoading = false) }
                    announce("Berhasil masuk menggunakan akun Google: ${result.user.displayName}. Data toko Anda tersinkronkan ke Firebase Cloud.")
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    announce("Gagal masuk akun Google: ${result.message}")
                }
                is AuthResult.Cancelled -> {
                    _uiState.update { it.copy(isLoading = false) }
                    announce("Pemilihan akun Google dibatalkan.")
                }
                is AuthResult.AwaitingIntent -> {}
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            val err = "Email dan kata sandi wajib diisi."
            _uiState.update { it.copy(errorMessage = err) }
            announce(err)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            announce("Memverifikasi akun email...")

            when (val result = authManager.signInWithEmail(email, pass)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(currentUser = result.user, isLoading = false) }
                    announce("Berhasil masuk sebagai ${result.user.displayName}. Selamat datang kembali.")
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    announce(result.message)
                }
                is AuthResult.Cancelled -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is AuthResult.AwaitingIntent -> {}
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        if (email.isBlank() || pass.isBlank() || name.isBlank()) {
            val err = "Nama, email, dan kata sandi wajib diisi."
            _uiState.update { it.copy(errorMessage = err) }
            announce(err)
            return
        }
        if (pass.length < 6) {
            val err = "Kata sandi minimal 6 karakter."
            _uiState.update { it.copy(errorMessage = err) }
            announce(err)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            announce("Mendaftarkan akun baru ke Firebase Cloud...")

            when (val result = authManager.signUpWithEmail(email, pass, name)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(currentUser = result.user, isLoading = false) }
                    announce("Pendaftaran berhasil! Selamat datang, ${result.user.displayName}.")
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    announce(result.message)
                }
                is AuthResult.Cancelled -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is AuthResult.AwaitingIntent -> {}
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            announce("Menyiapkan sesi tamu...")
            val result = authManager.signInAnonymously()
            if (result is AuthResult.Success) {
                _uiState.update { it.copy(currentUser = result.user, isLoading = false) }
                val mode = if (result.user.id != "guest_local") "Tamu Cloud (Tersinkron Online)" else "Tamu Offline (Penyimpanan Lokal)"
                announce("Masuk sebagai $mode. Anda dapat langsung menginput produk dan transaksi.")
            } else {
                val guest = authManager.createInstantGuestSession()
                _uiState.update { it.copy(currentUser = guest, isLoading = false) }
                announce("Masuk sebagai Tamu Offline.")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _uiState.update { it.copy(currentUser = null) }
            announce("Anda telah keluar dari akun. Silakan masuk kembali untuk mengakses data toko.")
        }
    }

    private fun announce(message: String) {
        viewModelScope.launch {
            _announcements.emit(message)
            AccessibilityUtils.announceForAccessibility(getApplication(), message)
        }
    }
}
