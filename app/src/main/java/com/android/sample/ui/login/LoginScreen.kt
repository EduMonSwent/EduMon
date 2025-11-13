package com.android.sample.ui.login

// This code has been written partially using A.I (LLM).

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {

  val context = LocalContext.current
  val activity = context as Activity
  val vm: LoginViewModel = viewModel()

  val state by vm.state.collectAsState()
  val credentialManager = remember { CredentialManager.create(activity) }

  LaunchedEffect(state.user) {
    if (state.user != null) {
      onLoggedIn()
    }
  }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Brush.verticalGradient(listOf(Color(0xFF12122A), Color(0xFF181830)))),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Image(
              painter = painterResource(R.drawable.edumon),
              contentDescription = "logo",
              modifier = Modifier.size(120.dp))

          Spacer(Modifier.height(40.dp))

          Text("Connect yourself to EduMon.", color = Color.White, fontSize = 22.sp)

          Spacer(Modifier.height(32.dp))

          Button(
              onClick = { vm.signIn(activity, credentialManager) },
              enabled = !state.loading,
              colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google icon",
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.loading) "Connexion..." else "Continue with google",
                    color = Color.Black)
              }

          state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFF6B6B))
          }
        }
      }
}
