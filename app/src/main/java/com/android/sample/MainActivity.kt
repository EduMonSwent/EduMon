package com.android.sample

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.theme.EduMonTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startUri: Uri? = intent?.data

        val (startRoute, _) =
            if (startUri?.scheme == "edumon" && startUri.host == "study_session") {
                val id = startUri.pathSegments.firstOrNull()
                if (!id.isNullOrEmpty()) "study/$id" to id
                else com.android.sample.feature.homeScreen.AppDestination.Home.route to null
            } else {
                com.android.sample.feature.homeScreen.AppDestination.Home.route to null
            }

        setContent {
            EduMonTheme {
                val nav = rememberNavController()

                var user by remember { mutableStateOf(auth.currentUser) }

                // ---- Firebase Anonymous Login (required for repos) ----
                LaunchedEffect(Unit) {
                    if (auth.currentUser == null) {
                        auth.signInAnonymously().await()
                    }
                    user = auth.currentUser
                }

                if (user == null) {
                    // Loading screen until Firebase login completes
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier)
                    }
                } else {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("EduMon") }
                            )
                        }
                    ) { padding ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            NavHost(
                                navController = nav,
                                startDestination = "app"
                            ) {
                                composable("app") {
                                    EduMonNavHost(startDestination = startRoute)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}