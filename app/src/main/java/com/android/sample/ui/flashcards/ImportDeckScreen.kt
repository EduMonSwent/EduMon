package com.android.sample.ui.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
private const val IMPORT_SUCCESS_DELAY_MS = 100L
private const val BACK_NAVIGATION_ARROW = "Back"

private const val SHARE_CODE = "Share Code"

private const val IMPORT = "Import"

@Composable
fun ImportDeckScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    vm: ImportDeckViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
  val state = vm.state.collectAsState().value
  var token by remember { mutableStateOf("") }

  val colorScheme = MaterialTheme.colorScheme

  // Navigate back when success
  LaunchedEffect(state.success) {
    if (state.success) {
      // Small delay to let the UI finish updating before navigating away (prevents flicker).
      kotlinx.coroutines.delay(IMPORT_SUCCESS_DELAY_MS)
      onSuccess()
    }
  }

  Column(
      Modifier.fillMaxSize()
          .background(colorScheme.background)
          .padding(20.dp)
          .testTag("ImportDeckScreenRoot"),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ---- TOP BAR ----
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = BACK_NAVIGATION_ARROW)
          }
          Spacer(Modifier.width(8.dp))
          Text(
              "Import Shared Deck",
              style = MaterialTheme.typography.headlineLarge,
              color = colorScheme.onBackground)
        }

        // ---- TEXT FIELD ----
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text(SHARE_CODE) },
            modifier = Modifier.fillMaxWidth())

        // ---- IMPORT BUTTON ----
        Button(
            onClick = { vm.importToken(token) },
            enabled = token.isNotBlank() && !state.loading,
            modifier = Modifier.fillMaxWidth()) {
              Text(IMPORT)
            }

        // ---- LOADING ----
        if (state.loading) {
          CircularProgressIndicator()
        }

        // ---- ERROR ----
        state.error?.let { msg ->
          Text(msg, color = colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        }
      }
}
