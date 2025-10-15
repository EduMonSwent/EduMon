package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.theme.EduMonTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.android.sample.resources.C
import com.android.sample.screens.EduMonHomeRoute
import com.android.sample.ui.theme.SampleAppTheme

class MainActivity : ComponentActivity() {

  private val auth = FirebaseAuth.getInstance()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      EduMonTheme {
        var currentUser by remember { mutableStateOf(auth.currentUser) }

        // 🔹 Écoute les changements de session Firebase
        DisposableEffect(Unit) {
          val listener =
              FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUser = firebaseAuth.currentUser
              }
          auth.addAuthStateListener(listener)
          onDispose { auth.removeAuthStateListener(listener) }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          if (currentUser == null) {
            // Utilisateur non connecté → écran de login
            LoginScreen()
          } else {
            // Utilisateur connecté → écran de bienvenue
            WelcomeScreen(
                name = currentUser?.displayName ?: "Utilisateur",
                email = currentUser?.email ?: "",
                onLogout = {
                  // 🔹 Déconnexion complète Google + Firebase
                  val context = this@MainActivity
                  val gso =
                      GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                          .requestIdToken(getString(R.string.default_web_client_id))
                          .requestEmail()
                          .build()

                  val googleClient = GoogleSignIn.getClient(context, gso)

                  googleClient.revokeAccess().addOnCompleteListener { auth.signOut() }
                })
          }
        }
      }
    }
  }
}

@Composable
fun WelcomeScreen(name: String, email: String, onLogout: () -> Unit) {
  val context = LocalContext.current

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = "Bienvenue 👋",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground)
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = name, style = MaterialTheme.typography.titleLarge)
      Text(text = email, style = MaterialTheme.typography.bodyMedium)
      Spacer(modifier = Modifier.height(24.dp))
      Button(
          onClick = onLogout,
          colors =
              ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text("Se déconnecter", color = MaterialTheme.colorScheme.onPrimary)
          }
    }
  }
}
