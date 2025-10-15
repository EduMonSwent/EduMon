package com.android.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.Screen
import com.android.sample.ui.overview.AddTodoScreen
import com.android.sample.ui.overview.EditToDoScreen
import com.android.sample.ui.overview.OverviewScreen
import com.android.sample.ui.theme.SampleAppTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // This allows the app to use Firebase emulators for local development
    // Firebase.firestore.useEmulator("10.0.2.2", 8080)
    // Firebase.auth.useEmulator("10.0.2.2", 9099)

    setContent {
      SampleAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          SampleApp() // ✅ call the correct composable
        }
      }
    }
  }
}

/** `SampleApp` is the main composable function that sets up the app UI. */
@Composable
fun SampleApp(context: Context = LocalContext.current) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)

  // ✅ Start directly on the Overview screen and remove the Map graph entirely
  NavHost(navController = navController, startDestination = Screen.Overview.route) {
    composable(Screen.Overview.route) {
      OverviewScreen(
          onSelectTodo = { navigationActions.navigateTo(Screen.EditToDo(it.uid)) },
          onAddTodo = { navigationActions.navigateTo(Screen.AddToDo) },
          navigationActions = navigationActions)
    }

    composable(Screen.AddToDo.route) {
      AddTodoScreen(
          onSaved = { navigationActions.navigateTo(Screen.Overview) },
          goBack = { navigationActions.goBack() })
    }

    composable(Screen.EditToDo.route) { navBackStackEntry ->
      val uid = navBackStackEntry.arguments?.getString("uid")
      uid?.let {
        EditToDoScreen(
            onEdit = { navigationActions.navigateTo(Screen.Overview) },
            goBack = { navigationActions.goBack() },
            todoUid = it)
      }
          ?: run {
            Log.e("EditToDoScreen", "ToDo UID is null")
            Toast.makeText(context, "ToDo UID is null", Toast.LENGTH_SHORT).show()
          }
    }
  }
}
