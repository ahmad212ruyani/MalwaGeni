package com.malwageni.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.malwageni.app.auth.AuthViewModel
import com.malwageni.app.ui.auth.LoginScreen
import com.malwageni.app.ui.home.MainScreen
import com.malwageni.app.ui.home.MainViewModel
import com.malwageni.app.ui.theme.MalwaGeniTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MalwaGeniTheme {
                val authUiState by authViewModel.uiState.collectAsState()

                val currentUser = authUiState.currentUser

                LaunchedEffect(currentUser?.id) {
                    if (currentUser != null) {
                        mainViewModel.setUserSession(currentUser.id)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser == null) {
                        LoginScreen(authViewModel = authViewModel)
                    } else {
                        MainScreen(
                            viewModel = mainViewModel,
                            currentUser = currentUser,
                            onSignOut = { authViewModel.signOut() }
                        )
                    }
                }
            }
        }
    }
}
