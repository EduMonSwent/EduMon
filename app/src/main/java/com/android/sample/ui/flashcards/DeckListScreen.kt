package com.android.sample.ui.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.theme.*

/** Flashcards Deck List Screen */
@Composable
fun DeckListScreen(
    onCreateDeck: () -> Unit,
    onStudyDeck: (String) -> Unit,
    onImportDeck: () -> Unit,
    vm: DeckListViewModel = viewModel()
) {
  val decks by vm.decks.collectAsState()

  Box(Modifier.fillMaxSize().background(BackgroundDark)) {
    Column(Modifier.padding(16.dp)) {

      // ---- HEADER ----
      Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically) {
            Text("Flashcards", style = MaterialTheme.typography.headlineLarge, color = TextLight)

            Button(
                onClick = onCreateDeck,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = AccentViolet, contentColor = TextLight)) {
                  Icon(Icons.Default.Add, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text("New Deck")
                }
          }

      Spacer(Modifier.height(12.dp))

      LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(decks) { deck ->
          DeckRow(
              deck = deck, onStudyDeck = onStudyDeck, onDeleteDeck = { vm.deleteDeck(it) }, vm = vm)
        }
      }
    }

    // ---- FLOATING IMPORT BUTTON (BOTTOM RIGHT) ----
    FloatingActionButton(
        onClick = onImportDeck,
        containerColor = AccentMagenta,
        contentColor = TextLight,
        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
          Icon(Icons.Default.Share, contentDescription = "Import deck")
        }
  }
}

/** Deck Row with Study, Share, Delete */
@Composable
fun DeckRow(
    deck: Deck,
    onStudyDeck: (String) -> Unit,
    onDeleteDeck: (String) -> Unit,
    vm: DeckListViewModel
) {
  var confirmDelete by remember { mutableStateOf(false) }
  var showShareDialog by remember { mutableStateOf(false) }
  val isEmpty = deck.cards.isEmpty()

  Surface(
      color = MidDarkCard,
      contentColor = TextLight,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)) {
        Column(Modifier.padding(16.dp)) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                  Text(deck.title, style = MaterialTheme.typography.titleLarge)
                  Text(
                      deck.description,
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextLight.copy(alpha = .75f),
                      maxLines = 2)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {

                  // STUDY BUTTON
                  Button(
                      onClick = { onStudyDeck(deck.id) },
                      enabled = !isEmpty,
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = AccentViolet, contentColor = TextLight)) {
                        Text(if (isEmpty) "No Cards" else "Study")
                      }

                  Spacer(Modifier.width(8.dp))

                  // SHARE BUTTON
                  IconButton(onClick = { showShareDialog = true }) {
                    Icon(
                        Icons.Default.Share, contentDescription = "Share deck", tint = AccentViolet)
                  }

                  Spacer(Modifier.width(8.dp))

                  // DELETE BUTTON
                  IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete deck",
                        tint = AccentMagenta)
                  }
                }
              }

          Spacer(Modifier.height(8.dp))

          AssistChip(
              onClick = {},
              label = { Text("${deck.cards.size} cards") },
              colors =
                  AssistChipDefaults.assistChipColors(
                      containerColor = BackgroundDark, labelColor = TextLight))
        }
      }

  // DELETE DIALOG
  if (confirmDelete) {
    AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete deck?") },
        text = { Text("This will permanently remove “${deck.title}”.") },
        confirmButton = {
          TextButton(
              onClick = {
                confirmDelete = false
                onDeleteDeck(deck.id)
              }) {
                Text("Delete", color = AccentMagenta)
              }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        containerColor = MidDarkCard)
  }

  // SHARE DIALOG
  if (showShareDialog) {
    ShareDeckDialog(deck = deck, vm = vm, onDismiss = { showShareDialog = false })
  }
}
