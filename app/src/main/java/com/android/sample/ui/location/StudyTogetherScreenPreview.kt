package com.android.sample.ui.location

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Version mockée de StudyTogetherScreen pour les tests Compose. Elle évite Google Maps et le GPS
 * pour se concentrer sur la logique d'affichage.
 */
@Composable
fun StudyTogetherScreenMock() {
  var selectedFriend by remember { mutableStateOf<FriendStatus?>(null) }
  var isUserSelected by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp).testTag("study_together_mock")) {
    AnimatedVisibility(
        visible = selectedFriend != null || isUserSelected,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.CenterHorizontally)) {
          when {
            isUserSelected -> UserStatusCard(isStudyMode = true)
            selectedFriend != null ->
                FriendInfoCard(name = selectedFriend!!.name, status = selectedFriend!!.status)
          }
        }

    Spacer(Modifier.height(16.dp))

    Button(onClick = { isUserSelected = true }, modifier = Modifier.testTag("btn_user")) {
      Text("Select User")
    }

    Button(
        onClick = { selectedFriend = FriendStatus("Alae", 0.0, 0.0, "study") },
        modifier = Modifier.testTag("btn_friend")) {
          Text("Select Friend")
        }
  }
}
