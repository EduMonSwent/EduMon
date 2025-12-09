package com.android.sample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.media3.common.util.UnstableApi
import com.android.sample.ui.login.LoginTapToStartScreen
import com.android.sample.ui.onBoarding.LoopingVideoBackgroundFromAssets
import com.android.sample.ui.theme.EduMonTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AppScreen {
  TAP_TO_START,
  LOGGING_IN,
  APP
}

class MainActivity : ComponentActivity() {

  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

  @UnstableApi
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      EduMonTheme {
        // Si déjà connecté, aller directement à APP
        val initialScreen =
            if (auth.currentUser != null) {
              Log.d("MainActivity", "User already logged in: ${auth.currentUser?.uid}")
              AppScreen.APP
            } else {
              Log.d("MainActivity", "No user, showing TapToStart")
              AppScreen.TAP_TO_START
            }

        var currentScreen by remember { mutableStateOf(initialScreen) }
        val scope = rememberCoroutineScope()
        val activity = this@MainActivity

        Scaffold { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            when (currentScreen) {
              AppScreen.TAP_TO_START -> {
                LoginTapToStartScreen(
                    onTap = {
                      currentScreen = AppScreen.LOGGING_IN
                      scope.launch {
                        val success = performGoogleSignIn(activity)
                        Log.d("MainActivity", "SignIn result: $success")
                        currentScreen = if (success) AppScreen.APP else AppScreen.TAP_TO_START
                      }
                    })
              }
              AppScreen.LOGGING_IN -> {
                // Vidéo en background pendant le choix du compte Google
                Box(modifier = Modifier.fillMaxSize()) {
                  LoopingVideoBackgroundFromAssets(
                      assetFileName = "onboarding_background_epfl.mp4",
                      modifier = Modifier.fillMaxSize())

                  // Indicateur de chargement par-dessus
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                  }
                }
              }
              AppScreen.APP -> {
                EduMonNavHost(
                    onSignOut = {
                      signOutAll()
                      currentScreen = AppScreen.TAP_TO_START
                    })
              }
            }
          }
        }
      }
    }
  }

  private suspend fun performGoogleSignIn(activity: Activity): Boolean {
    return try {
      val credentialManager = CredentialManager.create(activity)

      val googleIdOption =
          GetSignInWithGoogleOption.Builder(getString(R.string.default_web_client_id)).build()

      val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential

      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      val idToken = googleIdTokenCredential.idToken

      val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
      auth.signInWithCredential(firebaseCredential).await()

      Log.d("MainActivity", "SignIn OK, user=${auth.currentUser?.uid}")
      auth.currentUser != null
    } catch (e: Exception) {
      Log.e("MainActivity", "SignIn failed", e)
      false
    }
  }

  fun signOutAll() {
    Log.d("MainActivity", "SignOut")
    val gso =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    val googleClient = GoogleSignIn.getClient(this, gso)
    googleClient.revokeAccess().addOnCompleteListener { auth.signOut() }
  }
}
