package com.android.sample.ui.login

// This code has been written partially using A.I (LLM).

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUIState(
    val loading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

class LoginViewModel(
    private val repo: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUIState())
    val state: StateFlow<LoginUIState> = _state

    fun clearError() { _state.update { it.copy(error = null) } }

    fun signIn(context: Context, credentialManager: CredentialManager) {
        if (_state.value.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            try {
                val option = GetSignInWithGoogleOption.Builder(
                    context.getString(com.android.sample.R.string.default_web_client_id)
                ).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val credential = credentialManager.getCredential(context, request).credential

                val result = repo.loginWithGoogle(credential)

                result.onSuccess { user ->
                    _state.update { it.copy(loading = false, user = user) }
                }.onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                }

            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}
