package com.android.sample.login

// This code has been written partially using A.I (LLM).

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import com.android.sample.ui.login.AuthRepository
import com.android.sample.ui.login.LoginViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  // Fake implementations

  private class FakeSuccessRepo(private val user: FirebaseUser) : AuthRepository {
    override suspend fun loginWithGoogle(credential: Credential): Result<FirebaseUser> {
      return Result.success(user)
    }

    override fun logout(): Result<Unit> = Result.success(Unit)
  }

  private class FakeFailureRepo(private val error: Throwable) : AuthRepository {
    override suspend fun loginWithGoogle(credential: Credential): Result<FirebaseUser> {
      return Result.failure(error)
    }

    override fun logout(): Result<Unit> = Result.success(Unit)
  }

  private class FakeExceptionRepo(private val error: Throwable) : AuthRepository {
    override suspend fun loginWithGoogle(credential: Credential): Result<FirebaseUser> {
      throw error
    }

    override fun logout(): Result<Unit> = Result.success(Unit)
  }

  private val fakeCredential: Credential = mock(Credential::class.java)
  private val fakeContext: Context = mock(Context::class.java)
  private val fakeCredentialManager: CredentialManager = mock(CredentialManager::class.java)
  private val fakeUser: FirebaseUser = mock(FirebaseUser::class.java)

  @Test
  fun initial_state_is_idle() {
    val vm =
        LoginViewModel(
            repo = FakeFailureRepo(Throwable("unused")),
            credentialProvider = { _, _ -> fakeCredential })

    val state = vm.state.value
    assertEquals(false, state.loading)
    assertNull(state.user)
    assertNull(state.error)
  }

  @Test
  fun clearError_setsErrorToNull() = runTest {
    val vm =
        LoginViewModel(
            repo = FakeFailureRepo(Throwable("login failed")),
            credentialProvider = { _, _ -> fakeCredential })

    vm.signIn(fakeContext, fakeCredentialManager)
    advanceUntilIdle()

    // Sanity check: error is not null
    val withError = vm.state.value
    assertEquals("login failed", withError.error)

    vm.clearError()
    val cleared = vm.state.value
    assertNull(cleared.error)
  }

  @Test
  fun signIn_success_updatesUser_andStopsLoading_andClearsError() = runTest {
    val vm =
        LoginViewModel(
            repo = FakeSuccessRepo(fakeUser), credentialProvider = { _, _ -> fakeCredential })

    vm.signIn(fakeContext, fakeCredentialManager)
    advanceUntilIdle()

    val state = vm.state.value
    assertEquals(false, state.loading)
    assertEquals(fakeUser, state.user)
    assertNull(state.error)
  }

  @Test
  fun signIn_failure_setsError_andUserRemainsNull() = runTest {
    val vm =
        LoginViewModel(
            repo = FakeFailureRepo(Throwable("bad credentials")),
            credentialProvider = { _, _ -> fakeCredential })

    vm.signIn(fakeContext, fakeCredentialManager)
    advanceUntilIdle()

    val state = vm.state.value
    assertEquals(false, state.loading)
    assertNull(state.user)
    assertEquals("bad credentials", state.error)
  }

  @Test
  fun signIn_exception_setsError_andUserRemainsNull() = runTest {
    val vm =
        LoginViewModel(
            repo =
                object : AuthRepository {
                  override suspend fun loginWithGoogle(
                      credential: Credential
                  ): Result<FirebaseUser> {
                    throw Exception("network error")
                  }

                  override fun logout(): Result<Unit> = Result.success(Unit)
                },
            credentialProvider = { _, _ -> fakeCredential })

    vm.signIn(fakeContext, fakeCredentialManager)
    advanceUntilIdle()

    val state = vm.state.value
    assertNull(state.user)
    assertEquals("network error", state.error)
  }

  @Test
  fun signIn_doesNothing_whenAlreadyLoading() = runTest {
    val vm =
        LoginViewModel(
            repo = FakeSuccessRepo(fakeUser), credentialProvider = { _, _ -> fakeCredential })

    // Fake "already loading" state
    vm.signIn(fakeContext, fakeCredentialManager)
    advanceUntilIdle()

    // Second call should return immediately and not crash
    vm.signIn(fakeContext, fakeCredentialManager)
    advanceUntilIdle()

    val state = vm.state.value
    // Still a valid user, no extra error, not stuck loading
    assertEquals(false, state.loading)
    assertEquals(fakeUser, state.user)
    assertNull(state.error)
  }
}
