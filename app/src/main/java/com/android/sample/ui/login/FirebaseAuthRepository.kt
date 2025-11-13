package com.android.sample.ui.login

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

private const val ERROR_NO_USER_AFTER_LOGIN = "No user after connexion"
private const val ERROR_CREDENTIAL_NOT_SUPPORTED = "Credential not supported"

class FirebaseAuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) :
    AuthRepository {

  override suspend fun loginWithGoogle(credential: Credential): Result<FirebaseUser> {
    return try {
      if (credential is CustomCredential &&
          credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val idToken = GoogleAuthHelper.fromBundle(credential.data).idToken
        val firebaseCred = GoogleAuthHelper.toFirebaseCredential(idToken)
        val user = auth.signInWithCredential(firebaseCred).await().user
        if (user != null) Result.success(user)
        else Result.failure(IllegalStateException(ERROR_NO_USER_AFTER_LOGIN))
      } else {
        Result.failure(IllegalArgumentException(ERROR_CREDENTIAL_NOT_SUPPORTED))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override fun logout(): Result<Unit> {
    return try {
      auth.signOut()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
