package com.android.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.sample.todo.ToDoRoutes
import com.android.sample.todo.ui.AddToDoScreen
import com.android.sample.todo.ui.EditToDoScreen
import com.android.sample.todo.ui.OverviewScreen
import com.android.sample.todo.ui.TestTags
import com.android.sample.ui.theme.SampleAppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      SampleAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val navController = rememberNavController()

          NavHost(
              navController = navController,
              startDestination = ToDoRoutes.Todos,
              modifier = Modifier.testTag(TestTags.NavHost)) {
                // 🗂 Overview screen – shows all todos
                composable(ToDoRoutes.Todos) {
                  OverviewScreen(
                      onAddClicked = { navController.navigate(ToDoRoutes.Add) },
                      onEditClicked = { id -> navController.navigate(ToDoRoutes.edit(id)) })
                }

                // ➕ Add new ToDo
                composable(ToDoRoutes.Add) {
                  AddToDoScreen(onBack = { navController.popBackStack() })
                }

                // ✏️ Edit existing ToDo (with ID argument)
                composable(
                    route = ToDoRoutes.Edit,
                    arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        backStackEntry ->
                      val context = LocalContext.current
                      val id = backStackEntry.arguments?.getString("id")
                      if (id != null) {
                        EditToDoScreen(id = id, onBack = { navController.popBackStack() })
                      } else {
                        Log.e("MainActivity", "ToDo id is null")
                        Toast.makeText(context, "ToDo id is missing or invalid", Toast.LENGTH_SHORT)
                            .show()
                        navController.popBackStack()
                      }
                    }
              }
        }
      }
    }
  }
}
