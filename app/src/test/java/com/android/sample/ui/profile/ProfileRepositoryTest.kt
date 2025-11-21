package com.android.sample.ui.profile

import com.android.sample.data.UserProfile
import com.android.sample.profile.FakeProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {

  @Test
  fun initial_state_is_default_user() = runTest {
    val repo = FakeProfileRepository()
    val p = repo.profile.value

    assertEquals("Alex", p.name)
    assertEquals("alex@university.edu", p.email)
    assertEquals(true, p.notificationsEnabled)
    assertEquals(true, p.locationEnabled)
    assertEquals(false, p.focusModeEnabled)
  }

  @Test
  fun updateProfile_replaces_entire_object() = runTest {
    val repo = FakeProfileRepository()

    repo.updateProfile(
        UserProfile(
            name = "Khalil",
            email = "k@ex.com",
            notificationsEnabled = false,
            locationEnabled = false,
            focusModeEnabled = true,
        ))

    val p = repo.profile.value
    assertEquals("Khalil", p.name)
    assertEquals("k@ex.com", p.email)
    assertEquals(false, p.notificationsEnabled)
    assertEquals(false, p.locationEnabled)
    assertEquals(true, p.focusModeEnabled)
  }

  @Test
  fun multiple_updates_apply_in_order() = runTest {
    val repo = FakeProfileRepository()

    // first update
    repo.updateProfile(repo.profile.value.copy(points = 200))

    // second update
    repo.updateProfile(repo.profile.value.copy(points = 350, streak = 10))

    val p = repo.profile.value
    assertEquals(350, p.points)
    assertEquals(10, p.streak)
  }
}
