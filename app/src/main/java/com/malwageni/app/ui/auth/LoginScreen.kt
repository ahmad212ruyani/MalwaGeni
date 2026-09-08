package com.malwageni.app.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.malwageni.app.R
import com.malwageni.app.auth.AuthViewModel
import com.malwageni.app.ui.components.AccessibleActionButton
import com.malwageni.app.ui.theme.ExpenseRed

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModel.handleGoogleSignInIntentResult(result.data)
    }

    // Pre-resolve strings at Composable level
    val googleSignInA11y = stringResource(R.string.a11y_google_sign_in)
    val emailInputA11y = stringResource(R.string.a11y_email_input)
    val passwordInputA11y = stringResource(R.string.a11y_password_input)
    val signInEmailA11y = stringResource(R.string.a11y_sign_in_email_button)
    val registerEmailA11y = stringResource(R.string.a11y_register_email_button)
    val guestSignInA11y = stringResource(R.string.a11y_guest_sign_in)

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        authViewModel.announcements.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Brand & Title
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "MalwaGeni",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Aplikasi MalwaGeni. Sistem Kasir, Stok, dan Keuangan Ramah Pembaca Layar."
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (uiState.isSignUpMode) {
                    "Daftarkan akun toko baru Anda ke Firebase Cloud."
                } else {
                    "Masuk untuk menyinkronkan data toko dan keuangan Anda secara otomatis ke Firebase Cloud."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary: Google Sign-In Button
            AccessibleActionButton(
                text = "Masuk dengan Akun Google",
                contentDescription = googleSignInA11y,
                onClick = {
                    focusManager.clearFocus()
                    authViewModel.signInWithGoogle(
                        activityContext = context,
                        onFallbackIntent = { intent ->
                            googleSignInLauncher.launch(intent)
                        }
                    )
                },
                icon = Icons.Default.AccountCircle,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.errorMessage != null && uiState.errorMessage!!.contains("Google", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.launchDirectGoogleSignIn(
                            activityContext = context,
                            onFallbackIntent = { intent ->
                                googleSignInLauncher.launch(intent)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Coba Google Sign-In Alternatif. Ketuk jika tombol Google pertama mengalami kendala."
                        },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Coba Google Sign-In Alternatif",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider: Atau dengan Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "  atau gunakan Email  ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Name field (Only in Sign-Up mode)
            if (uiState.isSignUpMode) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama Pemilik Toko / Pengguna") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Kolom input nama pemilik toko atau pengguna"
                        }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Email input field
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Alamat Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = emailInputA11y
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password input field
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Kata Sandi") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (uiState.isSignUpMode) {
                            authViewModel.signUpWithEmail(emailInput, passwordInput, nameInput)
                        } else {
                            authViewModel.signInWithEmail(emailInput, passwordInput)
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = passwordInputA11y
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Assertive
                            contentDescription = "Sedang memproses autentikasi ke server cloud, mohon tunggu..."
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Email Action Button (Masuk or Daftar)
                AccessibleActionButton(
                    text = if (uiState.isSignUpMode) "Daftar Akun Baru" else "Masuk dengan Email",
                    contentDescription = if (uiState.isSignUpMode) registerEmailA11y else signInEmailA11y,
                    onClick = {
                        focusManager.clearFocus()
                        if (uiState.isSignUpMode) {
                            authViewModel.signUpWithEmail(emailInput, passwordInput, nameInput)
                        } else {
                            authViewModel.signInWithEmail(emailInput, passwordInput)
                        }
                    },
                    backgroundColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Sign In / Sign Up Mode
                TextButton(
                    onClick = { authViewModel.toggleAuthMode() },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (uiState.isSignUpMode) {
                                "Sudah punya akun? Ketuk untuk beralih ke formulir Masuk"
                            } else {
                                "Belum punya akun? Ketuk untuk mendaftar akun baru"
                            }
                        }
                ) {
                    Text(
                        text = if (uiState.isSignUpMode) {
                            "Sudah punya akun? Masuk di sini"
                        } else {
                            "Belum punya akun? Daftar akun baru"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Catatan: Agar data tidak hilang saat aplikasi dihapus atau ganti HP, daftarkan akun dengan Email & Password atau Google.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Offline Guest Mode button
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.continueAsGuest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = guestSignInA11y
                        },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Lanjut Mode Tamu (Uji Coba Offline)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Error display card with LiveRegion
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Pemberitahuan error: ${uiState.errorMessage}"
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = ExpenseRed.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
