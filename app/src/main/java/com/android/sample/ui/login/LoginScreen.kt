package com.android.sample.ui.login

// This code has been written partially using A.I (LLM).

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    @DrawableRes logoResId: Int = R.drawable.edumon, // <- overridable logo / Edumon
) {
  val context = LocalContext.current
  val activity = context as Activity
  val vm: LoginViewModel = viewModel()

  val state by vm.state.collectAsState()
  val credentialManager = remember { CredentialManager.create(activity) }
  val colorScheme = MaterialTheme.colorScheme

  LaunchedEffect(state.user) {
    if (state.user != null) {
      onLoggedIn()
    }
  }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(
                  Brush.verticalGradient(
                      listOf(
                          colorScheme.background,
                          colorScheme.surface,
                          colorScheme.surfaceVariant))),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Image(
              painter = painterResource(logoResId),
              contentDescription = "logo",
              modifier = Modifier.size(120.dp))

          Spacer(Modifier.height(40.dp))

          Text(
              stringResource(R.string.login_title),
              color = colorScheme.onBackground,
              fontSize = 22.sp)

          Spacer(Modifier.height(32.dp))

          Button(
              onClick = { vm.signIn(activity, credentialManager) },
              enabled = !state.loading,
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = colorScheme.surface, contentColor = colorScheme.onSurface)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google icon",
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.loading) stringResource(R.string.login_button_loading)
                    else stringResource(R.string.login_button_google))
              }

          state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = colorScheme.error)
          }
        }
      }
}
