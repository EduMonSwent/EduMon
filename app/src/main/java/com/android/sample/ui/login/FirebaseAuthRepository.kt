package com.android.sample.ui.login

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Implémentation Firebase du dépôt d’authentification.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun loginWithGoogle(credential: Credential): Result<FirebaseUser> {
        return try {
            // Vérifie que le credential est bien un token Google
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleAuthHelper.fromBundle(credential.data).idToken
                val firebaseCred = GoogleAuthHelper.toFirebaseCredential(idToken)
                val user = auth.signInWithCredential(firebaseCred).await().user
                if (user != null) Result.success(user)
                else Result.failure(IllegalStateException("Utilisateur introuvable après connexion."))
            } else {
                Result.failure(IllegalArgumentException("Type de credential non supporté."))
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
