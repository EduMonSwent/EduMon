package com.android.sample.ui.login

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {

  suspend fun loginWithGoogle(credential: Credential): Result<FirebaseUser>

  fun logout(): Result<Unit>
}
