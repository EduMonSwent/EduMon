package com.android.sample.ui.login

// This code has been written partially using A.I (LLM).

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
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
    private val repo: AuthRepository = FirebaseAuthRepository(),
    // This makes the ViewModel testable
    private val credentialProvider: suspend (Context, CredentialManager) -> Credential =
        { context, credentialManager ->
          val option =
              GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id))
                  .build()

          val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

          credentialManager.getCredential(context, request).credential
        }
) : ViewModel() {

  private val _state = MutableStateFlow(LoginUIState())
  val state: StateFlow<LoginUIState> = _state

  fun signIn(context: Context, credentialManager: CredentialManager) {
    if (_state.value.loading) return

    viewModelScope.launch {
      _state.update { it.copy(loading = true, error = null) }

      try {
        val credential = credentialProvider(context, credentialManager)
        val result = repo.loginWithGoogle(credential)

        result
            .onSuccess { user ->
              _state.update { it.copy(loading = false, user = user, error = null) }
            }
            .onFailure { throwable ->
              _state.update {
                it.copy(
                    loading = false,
                    user = null,
                    // No hardcoded string here, we just propagate the error message
                    error = throwable.message)
              }
            }
      } catch (e: Exception) {
        _state.update {
          it.copy(
              loading = false,
              user = null,
              // Same here, use the exception message
              error = e.message)
        }
      }
    }
  }

  fun clearError() {
    _state.update { it.copy(error = null) }
  }
}
