package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.android.sample.ui.schedule.ScheduleScreen
import com.android.sample.ui.theme.EduMonTheme

class MainActivity : ComponentActivity() {

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      /*EduMonTheme {
        val nav = rememberNavController()

        Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("EduMon") }) }) { padding ->
          Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = "app") {
              composable("app") { EduMonNavHost() }
            }
          }
        }
      }*/
      EduMonTheme { ScheduleScreen() }
    }
  }
}
