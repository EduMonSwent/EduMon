package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    setContent { EduMonTheme { ScheduleScreen() } }

    /*setContent {
      EduMonTheme {
        val nav = rememberNavController()
        var user by remember { mutableStateOf(auth.currentUser) }
        val scope = rememberCoroutineScope()

        DisposableEffect(Unit) {
          val l =
              FirebaseAuth.AuthStateListener { fa ->
                val u = fa.currentUser
                val goTo = if (u == null) "login" else "app"
                user = u
                nav.navigate(goTo) {
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
                  title = { Text(if (user == null) "EduMon — Connection " else "") },
                  actions = {
                    if (user != null) {
                      TextButton(onClick = { signOutAll() }) { Text("Logout") }
                    }
                  })
            }) { padding ->
              Box(Modifier.fillMaxSize().padding(padding)) {
                NavHost(
                    navController = nav, startDestination = if (user == null) "login" else "app") {
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
                        LaunchedEffect(user?.uid) { user?.let { try {} catch (_: Exception) {} } }
                        EduMonNavHost()
                      }
                    }
              }
            }
      }
    }*/
  }

  private fun signOutAll() {
    val gso =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    val googleClient = GoogleSignIn.getClient(this, gso)
    googleClient.revokeAccess().addOnCompleteListener { auth.signOut() }
  }
}
