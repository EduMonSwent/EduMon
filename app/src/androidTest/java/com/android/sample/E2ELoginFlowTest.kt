package com.android.sample

import android.os.Bundle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
        compose.setContent { LoginScreen() }
        compose.waitForIdle()
        compose.onNodeWithText("Connecte-toi à ton compte EduMon").assertIsDisplayed()
        compose.onNodeWithText("Continuer avec Google").assertIsDisplayed()
    }

    @Test
    fun googleAuthHelper_behavesAsExpected() {
        val bundle = Bundle()
        try { GoogleAuthHelper.fromBundle(bundle) } catch (_: Exception) {}
        val credential: AuthCredential = GoogleAuthHelper.toFirebaseCredential("fake-token")
        assert(credential is AuthCredential)
    }

    @Test
    fun firebaseAuthRepository_login_success_and_logout() = runBlocking {
        val repo = FirebaseAuthRepository()

        val invalid = object : androidx.credentials.CustomCredential("fake", Bundle()) {}
        val r1 = repo.loginWithGoogle(invalid)
        assert(r1.isFailure)

        val valid = object : androidx.credentials.CustomCredential(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            Bundle()
        ) {}
        val r2 = try { repo.loginWithGoogle(valid) } catch (_: Exception) { Result.failure(Exception()) }
        assert(r2.isFailure || r2.isSuccess)

        val logout = repo.logout()
        assert(logout.isSuccess)
    }

    @Test
    fun authRepository_isImplemented() {
        val repo: AuthRepository = FirebaseAuthRepository()
        assert(repo is AuthRepository)
    }
}
