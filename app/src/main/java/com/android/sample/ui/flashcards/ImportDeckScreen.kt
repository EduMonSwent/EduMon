package com.android.sample.ui.flashcards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.sample.ui.flashcards.model.ImportDeckViewModel

/**
 * Screen for importing a shared deck using a share code. The UI allows the user to enter a token,
 * trigger the import, and shows loading/error states from the ImportDeckViewModel. Some parts of
 * this code have been written by an LLM(ChatGPT)
 */
@Composable
fun ImportDeckScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    vm: ImportDeckViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
  val state = vm.state.collectAsState().value
  var token by remember { mutableStateOf("") }

  // Navigate back when success
  LaunchedEffect(state.success) {
    if (state.success) {
      kotlinx.coroutines.delay(100)
      onSuccess()
    }
  }

  Column(
      Modifier.fillMaxSize().padding(20.dp).testTag("ImportDeckScreenRoot"),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ---- TOP BAR ----
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
          Spacer(Modifier.width(8.dp))
          Text("Import Shared Deck", style = MaterialTheme.typography.headlineLarge)
        }

        // ---- TEXT FIELD ----
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Share Code") },
            modifier = Modifier.fillMaxWidth())

        // ---- IMPORT BUTTON ----
        Button(
            onClick = { vm.importToken(token) },
            enabled = token.isNotBlank() && !state.loading,
            modifier = Modifier.fillMaxWidth()) {
              Text("Import")
            }

        // ---- LOADING ----
        if (state.loading) {
          CircularProgressIndicator()
        }

        // ---- ERROR ----
        state.error?.let { msg ->
          Text(
              msg,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyLarge)
        }
      }
}
