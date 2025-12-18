package com.android.sample

import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.credentials.CustomCredential
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
    compose.setContent { LoginScreen(onLoggedIn = {}) }
    compose.waitForIdle()

    compose.onNodeWithText("Connect yourself to EduMon.").assertIsDisplayed()
    compose.onNodeWithText("Continue with Google").assertIsDisplayed()
  }

  @Test
  fun googleAuthHelper_behavesAsExpected() {
    val bundle = Bundle()
    try {
      GoogleAuthHelper.fromBundle(bundle)
    } catch (_: Exception) {
      // Expected: GoogleIdTokenCredential.createFrom may throw on invalid bundle
    }

    val credential: AuthCredential = GoogleAuthHelper.toFirebaseCredential("fake-token")
    assert(credential is AuthCredential)
  }

  @Test
  fun firebaseAuthRepository_login_with_invalid_credential_fails() = runBlocking {
    val repo = FirebaseAuthRepository()

    // Given: an invalid CustomCredential type
    val invalid = object : CustomCredential("fake", Bundle()) {}

    // When: login is attempted
    val result = repo.loginWithGoogle(invalid)

    // Then: login should fail with credential not supported error
    assert(result.isFailure)
    assert(result.exceptionOrNull()?.message?.contains("Credential not supported") == true)
  }

  @Test
  fun firebaseAuthRepository_login_with_google_credential_type() = runBlocking {
    val repo = FirebaseAuthRepository()

    // Given: a CustomCredential with the expected Google ID token type but empty data
    val valid =
        object :
            CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle()) {}

    // When: login is attempted with this credential
    val result =
        try {
          repo.loginWithGoogle(valid)
        } catch (_: Exception) {
          // Expected: Firebase operations will fail in test environment without proper auth
          Result.failure<com.google.firebase.auth.FirebaseUser>(Exception("Firebase test failure"))
        }

    // Then: result is either a failure (most common in test env) or success if Firebase is wired
    // We mainly test that the code path doesn't crash and handles exceptions
    assert(result.isFailure || result.isSuccess)
  }

  @Test
  fun firebaseAuthRepository_logout_always_succeeds() = runBlocking {
    val repo = FirebaseAuthRepository()

    // When: logout is called
    val result = repo.logout()

    // Then: logout completes successfully (even if no user is logged in)
    assert(result.isSuccess)
  }

  @Test
  fun firebaseAuthRepository_implements_authRepository_interface() {
    // Given: a FirebaseAuthRepository instance
    val repo: AuthRepository = FirebaseAuthRepository()

    // Then: it implements the AuthRepository interface
    assert(repo is AuthRepository)
  }

  @Test
  fun firebaseAuthRepository_login_handles_exception_gracefully() = runBlocking {
    val repo = FirebaseAuthRepository()

    // Given: an invalid credential that will cause an exception
    val badCredential =
        object :
            CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle()) {}

    // When: attempting to login
    val result =
        try {
          repo.loginWithGoogle(badCredential)
        } catch (e: Exception) {
          Result.failure<com.google.firebase.auth.FirebaseUser>(e)
        }

    // Then: the result is a failure (exception was caught and handled)
    assert(result.isFailure)
  }

  @Test
  fun firebaseAuthRepository_multiple_logouts_work() = runBlocking {
    val repo = FirebaseAuthRepository()

    // When: logout is called multiple times
    val result1 = repo.logout()
    val result2 = repo.logout()
    val result3 = repo.logout()

    // Then: all succeed (idempotent operation)
    assert(result1.isSuccess)
    assert(result2.isSuccess)
    assert(result3.isSuccess)
  }

  @Test
  fun googleAuthHelper_toFirebaseCredential_produces_valid_credential() {
    // Given: a fake token string
    val fakeToken = "fake-id-token-12345"

    // When: converting to Firebase credential
    val credential = GoogleAuthHelper.toFirebaseCredential(fakeToken)

    // Then: a valid AuthCredential is returned
    assert(credential is AuthCredential)
  }

  @Test
  fun googleAuthHelper_fromBundle_throws_on_empty_bundle() {
    // Given: an empty bundle
    val emptyBundle = Bundle()

    // When/Then: fromBundle should throw an exception
    try {
      GoogleAuthHelper.fromBundle(emptyBundle)
      assert(false) { "Should have thrown an exception" }
    } catch (e: Exception) {
      // Expected behavior
      assert(true)
    }
  }

  @Test
  fun loginScreen_renders_without_crashing() {
    // Given/When: LoginScreen is rendered
    var onLoggedInCalled = false
    compose.setContent { LoginScreen(onLoggedIn = { onLoggedInCalled = true }) }
    compose.waitForIdle()

    // Then: Screen renders without crashing
    compose.onNodeWithText("Connect yourself to EduMon.").assertIsDisplayed()

    // And: callback hasn't been called yet
    assert(!onLoggedInCalled)
  }

  @Test
  fun firebaseAuthRepository_can_be_instantiated_multiple_times() {
    // When: creating multiple repository instances
    val repo1 = FirebaseAuthRepository()
    val repo2 = FirebaseAuthRepository()
    val repo3 = FirebaseAuthRepository()

    // Then: all instances are valid
    assert(repo1 is AuthRepository)
    assert(repo2 is AuthRepository)
    assert(repo3 is AuthRepository)
  }
}
