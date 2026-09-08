package com.malwageni.app.model

data class UserAccount(
    val id: String,
    val displayName: String,
    val email: String,
    val profilePictureUrl: String? = null,
    val isAnonymous: Boolean = false
) {
    fun getAccessibilityDescription(): String {
        return if (isAnonymous) {
            "Masuk sebagai Akun Tamu Uji Coba (Offline Mode)"
        } else {
            "Akun Google aktif: $displayName, email: $email"
        }
    }
}
