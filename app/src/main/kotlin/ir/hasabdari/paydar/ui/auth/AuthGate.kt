package ir.hasabdari.paydar.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.AuthState
import ir.hasabdari.paydar.navigation.AppNavHost

@Composable
fun AuthGate(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    when (state.authState) {
        AuthState.INITIAL,
        AuthState.NEEDS_PIN_SETUP -> PinSetupScreen(
            viewModel = viewModel,
            isInitial = true,
            onDone = null,
            onCancel = null
        )

        AuthState.LOCKED -> LockScreen(viewModel = viewModel)

        AuthState.UNLOCKED -> AppNavHost()
    }
}