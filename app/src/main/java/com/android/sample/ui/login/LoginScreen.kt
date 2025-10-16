package com.android.sample.ui.login

import android.app.Activity
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.android.sample.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.security.SecureRandom
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
  // --- Config ---
  val webClientId = "154991771075-mluie21sbgjbjuv66l4jrl1oqofk824n.apps.googleusercontent.com"

  val context = LocalContext.current
  val activity = context as Activity

  val credentialManager = remember(activity) { CredentialManager.create(activity) }
  val auth = remember { FirebaseAuth.getInstance() }
  val scope = rememberCoroutineScope()

  // --- UI state ---
  var loading by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  fun randomNonce(): String {
    val b = ByteArray(32)
    SecureRandom().nextBytes(b)
    return Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  fun signIn() {
    if (loading) return
    loading = true
    error = null

    scope.launch {
      try {
        val googleIdOption =
            GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(randomNonce())
                .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        // ⬇️ Utiliser l’Activity (évite l’erreur de contexte non composable)
        val result = credentialManager.getCredential(activity, request)

        val google = GoogleIdTokenCredential.createFrom(result.credential.data)
        val idToken = google.idToken

        val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
        auth
            .signInWithCredential(firebaseCred)
            .addOnSuccessListener {
              loading = false
              Log.d("Login", "Connecté: ${it.user?.email}")
            }
            .addOnFailureListener { e ->
              loading = false
              error = e.message ?: "Erreur Firebase"
              Log.e("Login", "Firebase error", e)
            }
      } catch (_: GetCredentialCancellationException) {
        loading = false
      } catch (e: GetCredentialException) {
        loading = false
        error = e.message ?: "Google indisponible sur cet appareil (MAJ Play services ?)"
      } catch (e: Exception) {
        loading = false
        error = e.message ?: "Erreur inconnue"
      }
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
          Spacer(Modifier.height(40.dp))
          Text(
              "Connecte-toi à ton compte EduMon",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp)
          Spacer(Modifier.height(32.dp))
          Button(
              onClick = { signIn() },
              enabled = !loading,
              colors = ButtonDefaults.buttonColors(containerColor = Color.White),
              shape = RoundedCornerShape(10.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google icon",
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Connexion..." else "Continuer avec Google", color = Color.Black)
              }
          error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFF6B6B))
          }
        }
      }
}
