package com.android.sample.ui.flashcards.data

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.sample.ui.flashcards.StudyViewModel
import com.android.sample.ui.flashcards.model.Confidence
import com.android.sample.ui.theme.*

@Composable
fun StudyScreen(deckId: String, onBack: () -> Unit) {
  // simple factory to pass deckId
  val vm: StudyViewModel =
      viewModel(factory = viewModelFactory { initializer { StudyViewModel(deckId) } })
  val s by vm.state.collectAsState()

  Column(Modifier.fillMaxSize().background(BackgroundDark).padding(16.dp)) {
    TextButton(
        onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = TextLight)) {
          Text("← Back")
        }
    Text(s.deck.title, style = MaterialTheme.typography.headlineMedium, color = AccentMagenta)
    Text("Card ${s.index + 1} of ${s.total}", color = TextLight.copy(alpha = .7f))
    Spacer(Modifier.height(16.dp))

    FlipCard(
        front = { QuestionSide(text = s.current.question) },
        back = { AnswerSide(text = s.current.answer) },
        flipped = s.showingAnswer,
        onToggle = vm::flip,
        modifier = Modifier.weight(1f).fillMaxWidth())

    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      OutlinedButton(
          onClick = vm::prev,
          enabled = !s.isFirst,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)) {
            Text("Previous")
          }

      OutlinedButton(
          onClick = vm::flip,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)) {
            Text(if (s.showingAnswer) "Hide" else "Reveal")
          }

      Button(
          onClick = vm::next,
          enabled = !s.isLast,
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = AccentViolet, contentColor = TextLight)) {
            Text("Next")
          }
    }

    Spacer(Modifier.height(12.dp))
    Text("How confident are you?", color = TextLight.copy(alpha = .8f))
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
          onClick = { vm.record(Confidence.LOW) },
          modifier = Modifier.weight(1f),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = AccentMagenta, contentColor = TextLight)) {
            Text("Low")
          }
      Button(
          onClick = { vm.record(Confidence.MEDIUM) },
          modifier = Modifier.weight(1f),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = AccentViolet, contentColor = TextLight)) {
            Text("Medium")
          }
      Button(
          onClick = { vm.record(Confidence.HIGH) },
          modifier = Modifier.weight(1f),
          colors =
              ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = TextLight)) {
            Text("High")
          }
    }
  }
}

@Composable private fun QuestionSide(text: String) = CenterCard(title = "Question", body = text)

@Composable private fun AnswerSide(text: String) = CenterCard(title = "Answer", body = text)

@Composable
private fun CenterCard(title: String, body: String) {
  Surface(
      color = MidDarkCard,
      contentColor = TextLight,
      tonalElevation = 3.dp,
      shape = MaterialTheme.shapes.extraLarge,
      modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(title, style = MaterialTheme.typography.titleMedium, color = AccentViolet)
              Spacer(Modifier.height(8.dp))
              Text(
                  body,
                  style = MaterialTheme.typography.headlineSmall,
                  color = TextLight,
                  textAlign = TextAlign.Center)
              Spacer(Modifier.height(8.dp))
              Text("Tap to reveal answer", color = TextLight.copy(alpha = .6f))
            }
      }
}

/** Simple 3D flip using rotationY. */
@Composable
private fun FlipCard(
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
    flipped: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
  val rotation by animateFloatAsState(if (flipped) 180f else 0f, label = "flip")
  val cameraDistance = 12_000f

  Box(
      modifier
          .graphicsLayer {
            this.cameraDistance = cameraDistance
            rotationY = rotation
          }
          .clickable { onToggle() }) {
        if (rotation <= 90f) {
          Box(Modifier.graphicsLayer { rotationY = 0f }) { front() }
        } else {
          Box(Modifier.graphicsLayer { rotationY = 180f }) { back() }
        }
      }
}
