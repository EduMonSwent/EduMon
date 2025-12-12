package com.android.sample.ui.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.flashcards.model.Deck

/** Flashcards Deck List Screen */
@Composable
fun DeckListScreen(
    onCreateDeck: () -> Unit,
    onStudyDeck: (String) -> Unit,
    onImportDeck: () -> Unit,
    vm: DeckListViewModel = viewModel()
) {
  val decks by vm.decks.collectAsState()
  val colorScheme = MaterialTheme.colorScheme

  Box(Modifier.fillMaxSize().background(colorScheme.background)) {
    Column(Modifier.padding(16.dp)) {

      // ---- HEADER ----
      Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Flashcards",
                style = MaterialTheme.typography.headlineLarge,
                color = colorScheme.onBackground)

            Button(
                onClick = onCreateDeck,
                modifier = Modifier.testTag("CreateDeckButton"),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary)) {
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
        containerColor = colorScheme.secondary,
        contentColor = colorScheme.onSecondary,
        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).testTag("ImportDeckFab")) {
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
  val colorScheme = MaterialTheme.colorScheme

  var confirmDelete by remember { mutableStateOf(false) }
  var showShareDialog by remember { mutableStateOf(false) }
  val isEmpty = deck.cards.isEmpty()

  Surface(
      color = colorScheme.surfaceVariant,
      contentColor = colorScheme.onSurfaceVariant,
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
                      color = colorScheme.onSurfaceVariant.copy(alpha = .75f),
                      maxLines = 2)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {

                  // STUDY BUTTON
                  Button(
                      onClick = { onStudyDeck(deck.id) },
                      enabled = !isEmpty,
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = colorScheme.primary,
                              contentColor = colorScheme.onPrimary)) {
                        Text(if (isEmpty) "No Cards" else "Study")
                      }

                  Spacer(Modifier.width(8.dp))

                  // SHARE BUTTON
                  IconButton(onClick = { showShareDialog = true }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share deck",
                        tint = colorScheme.primary)
                  }

                  Spacer(Modifier.width(8.dp))

                  // DELETE BUTTON
                  IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete deck",
                        tint = colorScheme.error)
                  }
                }
              }

          Spacer(Modifier.height(8.dp))

          AssistChip(
              onClick = {},
              label = { Text("${deck.cards.size} cards") },
              colors =
                  AssistChipDefaults.assistChipColors(
                      containerColor = colorScheme.surface, labelColor = colorScheme.onSurface))
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
                Text("Delete", color = colorScheme.error)
              }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        containerColor = colorScheme.surface)
  }

  // SHARE DIALOG
  if (showShareDialog) {
    ShareDeckDialog(deck = deck, vm = vm, onDismiss = { showShareDialog = false })
  }
}
