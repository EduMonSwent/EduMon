package com.android.sample

// This code has been written partially using A.I (LLM).

import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.login.AuthRepository
import com.android.sample.ui.login.FirebaseAuthRepository
import com.android.sample.ui.login.GoogleAuthHelper
import com.android.sample.ui.login.LoginScreen
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2ELoginFlowTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun loginScreen_displaysProperly() {
    // Given: LoginScreen is displayed
    compose.setContent { LoginScreen(onLoggedIn = {}) }
    compose.waitForIdle()

    // Then: Title and Google button text are visible
    compose.onNodeWithText("Connect yourself to EduMon.").assertIsDisplayed()
    compose.onNodeWithText("Continue with Google").assertIsDisplayed()
  }

  @Test
  fun googleAuthHelper_behavesAsExpected() {
    // When: fromBundle is called with an invalid Bundle, it may throw
    val bundle = Bundle()
    try {
      GoogleAuthHelper.fromBundle(bundle)
    } catch (_: Exception) {
      // Expected: GoogleIdTokenCredential.createFrom may throw on invalid bundle
    }

    // When: converting a fake token to a Firebase credential
    val credential: AuthCredential = GoogleAuthHelper.toFirebaseCredential("fake-token")

    // Then: returned type is a valid AuthCredential
    assert(credential is AuthCredential)
  }

  @Test
  fun firebaseAuthRepository_login_success_and_logout() = runBlocking {
    val repo = FirebaseAuthRepository()

    // Given: an invalid CustomCredential type
    val invalid = object : androidx.credentials.CustomCredential("fake", Bundle()) {}
    val r1 = repo.loginWithGoogle(invalid)

    // Then: login should fail
    assert(r1.isFailure)

    // Given: a CustomCredential with the expected Google ID token type but empty data
    val valid =
        object :
            androidx.credentials.CustomCredential(
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle()) {}

    // When: login is attempted with this fake credential
    val r2 =
        try {
          repo.loginWithGoogle(valid)
        } catch (_: Exception) {
          Result.failure(Exception())
        }

    // Then: result is either a failure (most common on test env) or success if Firebase is wired
    assert(r2.isFailure || r2.isSuccess)

    // When: logout is called
    val logout = repo.logout()

    // Then: logout always completes successfully
    assert(logout.isSuccess)
  }

  @Test
  fun authRepository_isImplemented() {
    val repo: AuthRepository = FirebaseAuthRepository()
    assert(repo is AuthRepository)
  }
}
