package com.android.sample

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.theme.EduMonTheme

class MainActivity : ComponentActivity() {

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Capture the intent data (deep link) if present
    val startUri: Uri? = intent?.data

    // Compute start destination based on deep link
    val (startRoute, _) =
        if (startUri?.scheme == "edumon" && startUri.host == "study_session") {
          val id = startUri.pathSegments.firstOrNull()
          if (!id.isNullOrEmpty()) "study/$id" to id
          else com.android.sample.feature.homeScreen.AppDestination.Home.route to null
        } else com.android.sample.feature.homeScreen.AppDestination.Home.route to null

    setContent {
      EduMonTheme {
        val navController = rememberNavController()
        Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("EduMon") }) }) { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            EduMonNavHost(navController = navController, startDestination = startRoute)
          }
        }
      }
    }
  }
}
