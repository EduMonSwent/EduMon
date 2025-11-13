package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.theme.EduMonTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      EduMonTheme {
        val nav = rememberNavController()
        var user by remember { mutableStateOf(auth.currentUser) }

        // Listen to auth and navigate when state changes
        DisposableEffect(Unit) {
          val l = FirebaseAuth.AuthStateListener { fa ->
            val u = fa.currentUser
            val target = if (u == null) "login" else "app"
            user = u
            // Clear back stack and avoid multiple instances
            nav.navigate(target) {
              popUpTo(nav.graph.startDestinationId) { inclusive = true }
              launchSingleTop = true
            }
          }
          auth.addAuthStateListener(l)
          onDispose { auth.removeAuthStateListener(l) }
        }

        Scaffold(
          topBar = {
            CenterAlignedTopAppBar(
              title = { Text(if (user == null) "EduMon — Sign in" else "") },
              actions = {
                if (user != null) {
                  TextButton(onClick = { signOutAll() }) { Text("Sign out") }
                }
              }
            )
          }
        ) { padding ->
          // App navigation host
          Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = if (user == null) "login" else "app") {
              composable("login") { LoginScreen() }
              composable("app") {
                // Initialization guarded, app falls back to fakes if something goes wrong
                EduMonNavHost()
              }
            }
          }
        }
      }
    }
  }

  private fun signOutAll() {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestIdToken(getString(R.string.default_web_client_id))
      .requestEmail()
      .build()
    val googleClient = GoogleSignIn.getClient(this, gso)
    googleClient.revokeAccess().addOnCompleteListener { auth.signOut() }
  }
}
