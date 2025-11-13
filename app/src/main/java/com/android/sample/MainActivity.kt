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
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.theme.EduMonTheme

class MainActivity : ComponentActivity() {

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Capture the intent data (deep link) if present
    val startUri: Uri? = intent?.data

    setContent {
      EduMonTheme {
        val navController = rememberNavController()

        // If there is a deep link, navigate to the right screen after composition
        LaunchedEffect(startUri) {
          startUri?.let { uri ->
            // scheme edumon://study_session/{id} => host = "study_session", pathSegments[0] = id
            if (uri.scheme == "edumon" && uri.host == "study_session") {
              val id = uri.pathSegments.firstOrNull()
              if (!id.isNullOrEmpty()) {
                navController.navigate("study/$id")
              }
            }
          }
        }

        Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("EduMon") }) }) { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            EduMonNavHost(navController = navController)
          }
        }
      }
    }
  }
}
