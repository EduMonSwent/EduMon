package com.android.sample.ui.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.theme.*

@Composable
fun DeckListScreen(
    onCreateDeck: () -> Unit,
    onStudyDeck: (String) -> Unit,
    vm: DeckListViewModel = viewModel()
) {
  val decks by vm.decks.collectAsState()

  Column(Modifier.fillMaxSize().background(BackgroundDark).padding(16.dp)) {
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
                Icon(Icons.Default.Add, contentDescription = null, tint = TextLight)
                Spacer(Modifier.width(8.dp))
                Text("New Deck")
              }
        }
    Spacer(Modifier.height(12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      items(decks) { deck -> DeckRow(deck, onStudyDeck) }
    }
  }
}

@Composable
private fun DeckRow(deck: Deck, onStudyDeck: (String) -> Unit) {
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
                  Text(deck.title, style = MaterialTheme.typography.titleLarge, color = TextLight)
                  Text(
                      deck.description,
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextLight.copy(alpha = .75f),
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = { onStudyDeck(deck.id) },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = AccentViolet, contentColor = TextLight)) {
                      Text("Study")
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
}
