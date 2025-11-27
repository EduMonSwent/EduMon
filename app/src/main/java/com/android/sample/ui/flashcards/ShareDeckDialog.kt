package com.android.sample.ui.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.theme.*
import kotlinx.coroutines.launch

private const val ALLOW_SHARING = "Allow sharing"

private const val GENERATE_SHARE_LINK = "Generate share link"

private const val SHARE_THIS_CODE_WITH_A_FRIEND_ = "Share this code with a friend:"

private const val COPY = "Copy"

private const val CLOSE = "Close"

/**
 * Dialog for sharing a deck. Lets the user:
 * - Enable/disable sharing for the deck
 * - Generate a share token (if sharing is enabled)
 * - Copy the token to clipboard Some parts of this code have been written by an LLM(ChatGPT)
 */
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
            Text(ALLOW_SHARING)
            Spacer(Modifier.weight(1f))
            Switch(checked = deck.shareable, onCheckedChange = { vm.toggleShareable(deck.id, it) })
          }

          if (deck.shareable) {
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

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
                      Text(GENERATE_SHARE_LINK)
                    }
                  }
            }

            // ---- Token ready: display it & copy button ----
            shareToken?.let { token ->
              Text(SHARE_THIS_CODE_WITH_A_FRIEND_)

              Box(
                  Modifier.fillMaxWidth()
                      .background(BackgroundDark, shape = MaterialTheme.shapes.medium)
                      .padding(12.dp)) {
                    Text(token, color = AccentViolet)
                  }

              Button(
                  onClick = { clipboard.setText(AnnotatedString(token)) },
                  colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)) {
                    Text(COPY)
                  }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text(CLOSE) } },
      containerColor = MidDarkCard,
      textContentColor = TextLight,
      titleContentColor = TextLight)
}
