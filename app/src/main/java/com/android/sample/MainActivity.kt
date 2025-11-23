package com.android.sample

import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.theme.EduMonTheme

class MainActivity : ComponentActivity() {

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val startUri: Uri? = intent?.data

    val startRoute =
        if (startUri?.scheme == "edumon" && startUri.host == "study_session") {
          val id = startUri.pathSegments.firstOrNull()
          if (!id.isNullOrEmpty()) "study/$id"
          else com.android.sample.feature.homeScreen.AppDestination.Home.route
        } else {
          com.android.sample.feature.homeScreen.AppDestination.Home.route
        }

    setContent {
      EduMonTheme {
        val nav = rememberNavController()

        Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("EduMon") }) }) { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = "app") {
              composable("app") { EduMonNavHost(startDestination = startRoute) }
            }
          }
        }
      }
    }
  }
}
