package com.android.sample.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen() {
  val context = LocalContext.current
  val auth = FirebaseAuth.getInstance()
  val webClientId = context.getString(R.string.default_web_client_id)

  val gso =
      GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
          .requestIdToken(webClientId)
          .requestEmail()
          .build()

  val googleClient = GoogleSignIn.getClient(context, gso)

  val launcher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
              val account = task.result
              val credential = GoogleAuthProvider.getCredential(account.idToken, null)
              // Quand la connexion réussit, FirebaseAuth se mettra à jour
              auth.signInWithCredential(credential)
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }

  // --- UI ---
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Brush.verticalGradient(listOf(Color(0xFF12122A), Color(0xFF181830)))),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Image(
              painter = painterResource(id = R.drawable.edumon),
              contentDescription = "logo",
              modifier = Modifier.size(120.dp))
          Spacer(modifier = Modifier.height(40.dp))
          Text(
              "Connecte-toi à ton compte EduMon",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp)
          Spacer(modifier = Modifier.height(32.dp))
          Button(
              onClick = { launcher.launch(googleClient.signInIntent) },
              colors = ButtonDefaults.buttonColors(containerColor = Color.White),
              shape = RoundedCornerShape(10.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google icon",
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continuer avec Google", color = Color.Black)
              }
        }
      }
}
