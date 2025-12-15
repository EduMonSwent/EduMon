package com.android.sample.ui.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.flashcards.util.rememberIsOnline
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.BackgroundDark
import com.android.sample.ui.theme.MidDarkCard
import com.android.sample.ui.theme.TextLight
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
 * - Copy the token to clipboard
 *
 * This code has been written partially using A.I (LLM).
 */
@Composable
fun ShareDeckDialog(deck: Deck, vm: DeckListViewModel, onDismiss: () -> Unit) {
  var generating by remember { mutableStateOf(false) }
  var shareToken by remember { mutableStateOf<String?>(null) }

  val isOnline by rememberIsOnline()

  val scope = rememberCoroutineScope()
  val clipboard = LocalClipboardManager.current

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Share \"${deck.title}\"") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          ShareableToggle(deck = deck, vm = vm)

          if (deck.shareable) {
            ShareableContent(
                isOnline = isOnline,
                shareToken = shareToken,
                generating = generating,
                onGenerateToken = {
                  generating = true
                  scope.launch {
                    try {
                      shareToken = vm.createShareToken(deck.id)
                    } finally {
                      generating = false
                    }
                  }
                },
                onCopyToken = { token -> clipboard.setText(AnnotatedString(token)) })
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text(CLOSE) } },
      containerColor = MidDarkCard,
      textContentColor = TextLight,
      titleContentColor = TextLight)
}

@Composable
private fun ShareableToggle(deck: Deck, vm: DeckListViewModel) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(ALLOW_SHARING)
    Spacer(Modifier.weight(1f))
    Switch(checked = deck.shareable, onCheckedChange = { vm.toggleShareable(deck.id, it) })
  }
}

@Composable
private fun ShareableContent(
    isOnline: Boolean,
    shareToken: String?,
    generating: Boolean,
    onGenerateToken: () -> Unit,
    onCopyToken: (String) -> Unit
) {
  HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

  if (!isOnline) {
    OfflineWarning()
  }

  if (shareToken != null) {
    TokenDisplay(token = shareToken, onCopy = onCopyToken)
  } else {
    GenerateTokenButton(isOnline = isOnline, generating = generating, onClick = onGenerateToken)
  }
}

@Composable
private fun OfflineWarning() {
  Text(
      text = stringResource(R.string.flashcards_offline_generate_link),
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun GenerateTokenButton(isOnline: Boolean, generating: Boolean, onClick: () -> Unit) {
  Button(
      onClick = { if (isOnline) onClick() },
      enabled = isOnline && !generating,
      colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)) {
        if (generating) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextLight)
        } else {
          Text(GENERATE_SHARE_LINK)
        }
      }
}

@Composable
private fun TokenDisplay(token: String, onCopy: (String) -> Unit) {
  Text(SHARE_THIS_CODE_WITH_A_FRIEND_)

  Box(
      Modifier.fillMaxWidth()
          .background(BackgroundDark, shape = MaterialTheme.shapes.medium)
          .padding(12.dp)) {
        Text(token, color = AccentViolet)
      }

  Button(
      onClick = { onCopy(token) },
      colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)) {
        Text(COPY)
      }
}
