package com.malwageni.app.auth

import android.app.Application
import android.content.Context
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
    val errorMessage: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = GoogleAuthManager(application)
    private val prefs = application.getSharedPreferences("malwageni_auth_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _announcements = MutableSharedFlow<String>()
    val announcements: SharedFlow<String> = _announcements.asSharedFlow()

    init {
        restorePersistedSession()
    }

    private fun restorePersistedSession() {
        val savedUserId = prefs.getString("user_id", null)
        val savedName = prefs.getString("user_name", null)
        val savedEmail = prefs.getString("user_email", null)
        val isAnonymous = prefs.getBoolean("is_anonymous", false)

        if (savedUserId != null && savedName != null && savedEmail != null) {
            val restoredUser = UserAccount(
                id = savedUserId,
                displayName = savedName,
                email = savedEmail,
                isAnonymous = isAnonymous
            )
            _uiState.update { it.copy(currentUser = restoredUser) }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            announce("Membuka dialog akun Google. Silakan pilih akun Anda.")

            when (val result = authManager.signInWithGoogle()) {
                is AuthResult.Success -> {
                    persistSession(result.user)
                    _uiState.update { it.copy(currentUser = result.user, isLoading = false) }
                    announce("Berhasil masuk sebagai ${result.user.displayName}. Data toko dan keuangan Anda tersinkronisasi.")
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    announce("Gagal masuk: ${result.message}")
                }
                is AuthResult.Cancelled -> {
                    _uiState.update { it.copy(isLoading = false) }
                    announce("Proses masuk dengan akun Google dibatalkan.")
                }
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            val guest = authManager.createGuestSession()
            persistSession(guest)
            _uiState.update { it.copy(currentUser = guest, isLoading = false) }
            announce("Masuk sebagai mode tamu uji coba offline. Fitur kasir, stok, dan keuangan aktif di memori perangkat.")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            clearSession()
            _uiState.update { it.copy(currentUser = null) }
            announce("Anda telah keluar dari akun. Silakan masuk kembali untuk mengakses data cloud.")
        }
    }

    private fun persistSession(user: UserAccount) {
        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_name", user.displayName)
            .putString("user_email", user.email)
            .putBoolean("is_anonymous", user.isAnonymous)
            .apply()
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun announce(message: String) {
        viewModelScope.launch {
            _announcements.emit(message)
            AccessibilityUtils.announceForAccessibility(getApplication(), message)
        }
    }
}
