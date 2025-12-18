package com.android.sample

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.theme.EduMonTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // ⭐ SIGN IN BEFORE ANY UI OR REPOSITORY INITIALIZATION
    if (auth.currentUser == null) {
      auth.signInAnonymously()
    }

    // Deep link handling
    val startUri: Uri? = intent?.data
    val (startRoute, _) =
        if (startUri?.scheme == "edumon" && startUri.host == "study_session") {
          val id = startUri.pathSegments.firstOrNull()
          if (!id.isNullOrEmpty()) "study/$id" to id
          else com.android.sample.feature.homeScreen.AppDestination.Home.route to null
        } else com.android.sample.feature.homeScreen.AppDestination.Home.route to null

    setContent {
      EduMonTheme {
        val nav = rememberNavController()

        Scaffold { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = "app") {
              composable("app") {
                // auth is already guaranteed non-null here
                EduMonNavHost(startDestination = startRoute)
              }
            }
          }
        }
      }
    }
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
