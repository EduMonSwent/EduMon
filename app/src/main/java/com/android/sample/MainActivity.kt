package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.android.sample.ui.theme.EduMonTheme
import com.android.sample.ui.login.LoginScreen
import com.android.sample.screens.EduMonHomeRoute
import com.android.sample.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EduMonTheme {
                var currentUser by remember { mutableStateOf(auth.currentUser) }

                // Keep UI in sync with Firebase session
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser == null) {
                        // 🔹 Not signed in → your existing LoginScreen handles Google Sign-In + Firebase
                        LoginScreen()
                    } else {
                        // 🔹 Signed in → show your Home screen
                        EduMonHomeRoute(
                            creatureResId = R.drawable.edumon,       // use your real drawables
                            environmentResId = R.drawable.home,
                            onNavigate = { _ /* route */ -> /* Hook up NavHost if/when you add it */ }
                        )
                    }
                }
            }
        }
    }

    // Optional: call this from anywhere in your UI if you add a "Sign out" action later
    private fun signOutCompletely() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)

        // Revoke Google access + sign out from Firebase
        googleClient.revokeAccess().addOnCompleteListener { auth.signOut() }
    }
}
