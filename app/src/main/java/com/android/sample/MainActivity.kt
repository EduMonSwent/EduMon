package com.android.sample

// This code has been written partially using A.I (LLM).

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.profile.ProfileRepositoryProvider
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.onBoarding.EduMonOnboardingScreen
import com.android.sample.ui.theme.EduMonTheme
import com.android.sample.ui.theme.EdumonAppearance
import com.android.sample.ui.theme.EdumonAppearances
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Capture the intent data (deep link) if present
    val startUri: Uri? = intent?.data

    // Compute start destination based on deep link
    val (startRoute, _) =
        if (startUri?.scheme == "edumon" && startUri.host == "study_session") {
          val id = startUri.pathSegments.firstOrNull()
          if (!id.isNullOrEmpty()) {
            "study/$id" to id
          } else {
            AppDestination.Home.route to null
          }
        } else {
          AppDestination.Home.route to null
        }

    setContent {
      val nav = rememberNavController()
      var user by remember { mutableStateOf(auth.currentUser) }
      val scope = rememberCoroutineScope()

      // Global appearance (theme + sprite + environment)
      var appearance by remember { mutableStateOf<EdumonAppearance>(EdumonAppearances.Default) }
      var hasCompletedOnboarding by remember { mutableStateOf(false) }

      // React to Firebase auth state
      DisposableEffect(Unit) {
        val listener =
            FirebaseAuth.AuthStateListener { fa ->
              val u = fa.currentUser
              user = u

              // If user logs out, reset onboarding + theme
              if (u == null) {
                hasCompletedOnboarding = false
                appearance = EdumonAppearances.Default
              }

              val targetRoute = if (u == null) "login" else "app"
              nav.navigate(targetRoute) {
                popUpTo(nav.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
              }
            }

        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
      }

      EduMonTheme(appearance = appearance) {
        Scaffold { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = if (user == null) "login" else "app") {
              composable("login") {
                LoginScreen(
                    onLoggedIn = {
                      nav.navigate("app") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                      }
                    })
              }

              composable("app") {
                // Keep any side-effects you need on user change
                LaunchedEffect(user?.uid) {
                  user?.let {
                    try {
                      // placeholder for any user-specific loading
                    } catch (_: Exception) {}
                  }
                }

                if (!hasCompletedOnboarding) {
                  // Show onboarding until a starter is chosen
                  EduMonOnboardingScreen(
                      onOnboardingFinished = { _, starterId ->
                        val chosenAppearance = EdumonAppearances.fromStarterId(starterId)
                        appearance = chosenAppearance
                        hasCompletedOnboarding = true
                      })
                } else {
                  // Main app, themed according to the chosen Edumon
                  EduMonNavHost(
                      startDestination = startRoute,
                      creatureResId = appearance.creatureResId,
                      environmentResId = appearance.environmentResId)
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Syncs the profile repository from auth data.
   *
   * This is internal so androidTest can call it directly inside runBlocking.
   */
  internal suspend fun syncProfileFromAuthData(displayName: String?, email: String?) {
    val repo = ProfileRepositoryProvider.repository
    val currentProfile = repo.profile.value

    val resolvedName = displayName?.takeIf { it.isNotBlank() } ?: currentProfile.name
    val resolvedEmail = email?.takeIf { it.isNotBlank() } ?: currentProfile.email

    val updatedProfile = currentProfile.copy(name = resolvedName, email = resolvedEmail)

    runCatching { repo.updateProfile(updatedProfile) }
  }

  // Optional helper if you still want to call it with FirebaseUser from non-coroutine code
  fun syncProfileWithFirebaseUser(user: FirebaseUser?) {
    if (user == null) return
    lifecycleScope.launch { syncProfileFromAuthData(user.displayName, user.email) }
  }

  fun signOutAll() {
    val gso =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    val googleClient = GoogleSignIn.getClient(this, gso)
    googleClient.revokeAccess().addOnCompleteListener { auth.signOut() }
  }
}
