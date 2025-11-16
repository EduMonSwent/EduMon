package com.android.sample.ui.shop

import com.android.sample.R
import com.android.sample.data.UserProfile
import com.android.sample.profile.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

  private lateinit var profileRepository: ProfileRepository
  private lateinit var viewModel: ShopViewModel
  private lateinit var profileFlow: MutableStateFlow<UserProfile>
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Mock profile with initial state
    val initialProfile =
        UserProfile(
            name = "Test User", email = "test@example.com", coins = 1000, accessories = emptyList())

    profileFlow = MutableStateFlow(initialProfile)

    // Mock ProfileRepository
    profileRepository = mockk(relaxed = true)
    every { profileRepository.profile } returns profileFlow

    viewModel = ShopViewModel(profileRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `userCoins should reflect profile coins`() = runTest {
    // Given
    val expectedCoins = 1000

    // Collect the flow to activate it
    backgroundScope.launch { viewModel.userCoins.collect {} }

    // When
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertEquals(expectedCoins, viewModel.userCoins.value)
  }

  @Test
  fun `userCoins should update when profile changes`() = runTest {
    // Collect the flow to activate it
    backgroundScope.launch { viewModel.userCoins.collect {} }

    // Given
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(1000, viewModel.userCoins.value)

    // When
    val updatedProfile = profileFlow.value.copy(coins = 2500)
    profileFlow.value = updatedProfile
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertEquals(2500, viewModel.userCoins.value)
  }

  @Test
  fun `items should contain all initial cosmetics`() {
    // When
    val items = viewModel.items.value

    // Then
    assertEquals(6, items.size)
    assertTrue(items.any { it.id == "glasses" && it.name == "Cool Shades" && it.price == 200 })
    assertTrue(items.any { it.id == "hat" && it.name == "Wizard Hat" && it.price == 200 })
    assertTrue(items.any { it.id == "scarf" && it.name == "Red Scarf" && it.price == 200 })
    assertTrue(items.any { it.id == "wings" && it.name == "Cyber Wings" && it.price == 200 })
    assertTrue(items.any { it.id == "aura" && it.name == "Epic Aura" && it.price == 1500 })
    assertTrue(items.any { it.id == "cape" && it.name == "Hero Cape" && it.price == 200 })
  }

  @Test
  fun `items should have correct drawable resources`() {
    // When
    val items = viewModel.items.value

    // Then
    assertEquals(R.drawable.shop_cosmetic_glasses, items.find { it.id == "glasses" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_hat, items.find { it.id == "hat" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_scarf, items.find { it.id == "scarf" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_wings, items.find { it.id == "wings" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_aura, items.find { it.id == "aura" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_cape, items.find { it.id == "cape" }?.imageRes)
  }

  @Test
  fun `buyItem should return true and update profile when user has enough coins`() = runTest {
    // Given
    coEvery { profileRepository.updateProfile(any()) } returns Unit
    val item = viewModel.items.value.find { it.id == "glasses" }!!

    // When
    val result = viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertTrue(result)
    coVerify {
      profileRepository.updateProfile(
          match { it.coins == 800 && it.accessories.contains("owned:glasses") })
    }
  }

  @Test
  fun `buyItem should deduct correct amount from coins`() = runTest {
    // Given
    coEvery { profileRepository.updateProfile(any()) } returns Unit
    val item = viewModel.items.value.find { it.id == "scarf" }!!
    assertEquals(200, item.price)

    // When
    viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify {
      profileRepository.updateProfile(
          match { it.coins == 800 } // 1000 - 200
          )
    }
  }

  @Test
  fun `buyItem should add item to accessories list`() = runTest {
    // Given
    coEvery { profileRepository.updateProfile(any()) } returns Unit
    val item = viewModel.items.value.find { it.id == "hat" }!!

    // When
    viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify { profileRepository.updateProfile(match { it.accessories == listOf("owned:hat") }) }
  }

  @Test
  fun `buyItem should append to existing accessories`() = runTest {
    // Given
    profileFlow.value = profileFlow.value.copy(accessories = listOf("owned:glasses"))
    coEvery { profileRepository.updateProfile(any()) } returns Unit
    val item = viewModel.items.value.find { it.id == "hat" }!!

    // When
    viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify {
      profileRepository.updateProfile(
          match { it.accessories == listOf("owned:glasses", "owned:hat") })
    }
  }

  @Test
  fun `buyItem should mark item as owned in items list`() = runTest {
    // Given
    coEvery { profileRepository.updateProfile(any()) } returns Unit
    val item = viewModel.items.value.find { it.id == "wings" }!!
    assertFalse(item.owned)

    // When
    viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    val updatedItem = viewModel.items.value.find { it.id == "wings" }!!
    assertTrue(updatedItem.owned)
  }

  @Test
  fun `buyItem should return false when user has insufficient coins`() = runTest {
    // Given
    profileFlow.value = profileFlow.value.copy(coins = 100)
    val item = viewModel.items.value.find { it.id == "glasses" }!! // costs 200

    // When
    val result = viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertFalse(result)
    coVerify(exactly = 0) { profileRepository.updateProfile(any()) }
  }

  @Test
  fun `buyItem should return false when item is already owned`() = runTest {
    // Given
    val item = viewModel.items.value.find { it.id == "cape" }!!.copy(owned = true)

    // When
    val result = viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertFalse(result)
    coVerify(exactly = 0) { profileRepository.updateProfile(any()) }
  }

  @Test
  fun `buyItem should not update items when purchase fails due to insufficient coins`() = runTest {
    // Given
    profileFlow.value = profileFlow.value.copy(coins = 50)
    val item = viewModel.items.value.find { it.id == "aura" }!! // costs 1500

    // When
    val result = viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertFalse(result)
    val itemAfter = viewModel.items.value.find { it.id == "aura" }!!
    assertFalse(itemAfter.owned)
  }

  @Test
  fun `buyItem should not update items when item already owned`() = runTest {
    // Given
    val ownedItem = viewModel.items.value.find { it.id == "glasses" }!!.copy(owned = true)

    // When
    viewModel.buyItem(ownedItem)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    val itemsAfter = viewModel.items.value
    assertEquals(6, itemsAfter.size)
    assertFalse(
        itemsAfter.find { it.id == "glasses" }!!.owned) // Still not owned in the actual list
  }

  @Test
  fun `buyItem expensive item should work when user has exact coins`() = runTest {
    // Given
    profileFlow.value = profileFlow.value.copy(coins = 1500)
    coEvery { profileRepository.updateProfile(any()) } returns Unit
    val item = viewModel.items.value.find { it.id == "aura" }!! // costs 1500

    // When
    val result = viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    assertTrue(result)
    coVerify {
      profileRepository.updateProfile(
          match { it.coins == 0 && it.accessories.contains("owned:aura") })
    }
  }

  @Test
  fun `buyItem should only mark the purchased item as owned`() = runTest {
    // Given
    coEvery { profileRepository.updateProfile(any()) } returns Unit
    val item = viewModel.items.value.find { it.id == "scarf" }!!

    // When
    viewModel.buyItem(item)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    val items = viewModel.items.value
    assertTrue(items.find { it.id == "scarf" }!!.owned)
    assertFalse(items.find { it.id == "glasses" }!!.owned)
    assertFalse(items.find { it.id == "hat" }!!.owned)
    assertFalse(items.find { it.id == "wings" }!!.owned)
    assertFalse(items.find { it.id == "aura" }!!.owned)
    assertFalse(items.find { it.id == "cape" }!!.owned)
  }

  @Test
  fun `multiple purchases should accumulate accessories correctly`() = runTest {
    // Given
    coEvery { profileRepository.updateProfile(any()) } answers
        {
          val updated = firstArg<UserProfile>()
          profileFlow.value = updated
        }

    val glasses = viewModel.items.value.find { it.id == "glasses" }!!
    val hat = viewModel.items.value.find { it.id == "hat" }!!

    // When
    viewModel.buyItem(glasses)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.buyItem(hat)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify {
      profileRepository.updateProfile(match { it.accessories.contains("owned:glasses") })
      profileRepository.updateProfile(match { it.accessories.contains("owned:hat") })
    }
  }
}
