package com.android.sample.ui.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ShareDeckDialog(deck: Deck, vm: DeckListViewModel, onDismiss: () -> Unit) {
  var generating by remember { mutableStateOf(false) }
  var shareToken by remember { mutableStateOf<String?>(null) }

  val scope = rememberCoroutineScope()
  val clipboard = LocalClipboardManager.current

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Share “${deck.title}”") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

          // ---- Sharing toggle ----
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Allow sharing")
            Spacer(Modifier.weight(1f))
            Switch(checked = deck.shareable, onCheckedChange = { vm.toggleShareable(deck.id, it) })
          }

          if (deck.shareable) {
            Divider()

            // ---- Generate token button ----
            if (shareToken == null) {
              Button(
                  onClick = {
                    generating = true
                    scope.launch {
                      shareToken = vm.createShareToken(deck.id)
                      generating = false
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)) {
                    if (generating) {
                      CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextLight)
                    } else {
                      Text("Generate share link")
                    }
                  }
            }

            // ---- Token ready: display it & copy button ----
            shareToken?.let { token ->
              Text("Share this code with a friend:")

              Box(
                  Modifier.fillMaxWidth()
                      .background(BackgroundDark, shape = MaterialTheme.shapes.medium)
                      .padding(12.dp)) {
                    Text(token, color = AccentViolet)
                  }

              Button(
                  onClick = { clipboard.setText(AnnotatedString(token)) },
                  colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)) {
                    Text("Copy")
                  }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
      containerColor = MidDarkCard,
      textContentColor = TextLight,
      titleContentColor = TextLight)
}
