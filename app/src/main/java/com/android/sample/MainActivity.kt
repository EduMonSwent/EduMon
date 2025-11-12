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
import com.android.sample.pet.PetController
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

        DisposableEffect(Unit) {
          val l = FirebaseAuth.AuthStateListener { fa ->
            val u = fa.currentUser
            user = u
            val goTo = if (u == null) "login" else "app"
            nav.navigate(goTo) {
              popUpTo(nav.graph.startDestinationId) { inclusive = true }
              launchSingleTop = true
            }
            if (u != null) PetController.start(u.uid) else PetController.stop()
          }
          auth.addAuthStateListener(l)
          onDispose { auth.removeAuthStateListener(l) }
        }

        Scaffold(
          topBar = {
            CenterAlignedTopAppBar(
              title = { Text(if (user == null) "EduMon (login)" else "EduMon") },
              actions = {
                if (user != null) {
                  TextButton(onClick = { signOutAll() }) { Text("Logout") }
                }
              }
            )
          }
        ) { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = if (user == null) "login" else "app") {
              composable("login") { LoginScreen() }
              composable("app") { EduMonNavHost() }
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
