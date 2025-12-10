package com.android.sample.ui.shop.repository

import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

class FirestoreShopRepositoryEmulatorTest {

  private lateinit var repo: FirestoreShopRepository

  @Before
  fun setUp() {
    // Initialize Firebase and connect to emulator
    FirebaseEmulator.initIfNeeded(ApplicationProvider.getApplicationContext())
    FirebaseEmulator.connectIfRunning()

    // Skip tests if emulator is not running
    assumeTrue(
        "Firebase Emulator is not running. Start it with: firebase emulators:start",
        FirebaseEmulator.isRunning)

    // Clear all data and sign in anonymously
    runTest {
      FirebaseEmulator.clearAll()
      Tasks.await(FirebaseEmulator.auth.signInAnonymously())
    }

    // Create repository instance
    repo = FirestoreShopRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      runTest { FirebaseEmulator.clearAll() }
    }
  }

  // ========== Basic Repository Operations ==========

  @Test
  fun getItems_seeds_default_when_empty() = runTest {
    // When
    val items = repo.getItems()

    // Then
    assertEquals(6, items.size)
    assertTrue(items.any { it.id == "glasses" && it.name == "Cool Shades" })
    assertTrue(items.any { it.id == "hat" && it.name == "Wizard Hat" })
    assertTrue(items.any { it.id == "scarf" && it.name == "Red Scarf" })
    assertTrue(items.any { it.id == "wings" && it.name == "Cyber Wings" })
    assertTrue(items.any { it.id == "aura" && it.name == "Epic Aura" })
    assertTrue(items.any { it.id == "cape" && it.name == "Hero Cape" })
  }

  @Test
  fun getItems_returns_correct_prices() = runTest {
    // When
    val items = repo.getItems()

    // Then
    assertEquals(200, items.find { it.id == "glasses" }?.price)
    assertEquals(200, items.find { it.id == "hat" }?.price)
    assertEquals(200, items.find { it.id == "scarf" }?.price)
    assertEquals(200, items.find { it.id == "wings" }?.price)
    assertEquals(1500, items.find { it.id == "aura" }?.price)
    assertEquals(200, items.find { it.id == "cape" }?.price)
  }

  @Test
  fun getItems_returns_correct_drawable_resources() = runTest {
    // When
    val items = repo.getItems()

    // Then
    assertEquals(R.drawable.shop_cosmetic_glasses, items.find { it.id == "glasses" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_hat, items.find { it.id == "hat" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_scarf, items.find { it.id == "scarf" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_wings, items.find { it.id == "wings" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_aura, items.find { it.id == "aura" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_cape, items.find { it.id == "cape" }?.imageRes)
  }

  @Test
  fun getItems_all_items_initially_not_owned() = runTest {
    // When
    val items = repo.getItems()

    // Then
    assertTrue(items.all { !it.owned })
  }

  @Test
  fun getItems_persists_to_firestore() = runTest {
    // Given - first call seeds the data
    repo.getItems()

    // When - second call retrieves from Firestore
    val items = repo.getItems()

    // Then
    assertEquals(6, items.size)
    assertTrue(items.all { it.id.isNotEmpty() && it.name.isNotEmpty() })
  }

  // ========== Purchase Operations ==========

  @Test
  fun purchaseItem_returns_true_for_unowned_item() = runTest {
    // When
    val result = repo.purchaseItem("glasses")

    // Then
    assertTrue(result)
  }

  @Test
  fun purchaseItem_marks_item_as_owned_in_firestore() = runTest {
    // When
    repo.purchaseItem("glasses")
    val items = repo.getItems()

    // Then
    val glassesItem = items.find { it.id == "glasses" }!!
    assertTrue(glassesItem.owned)
  }

  @Test
  fun purchaseItem_updates_items_flow() = runTest {
    // When
    repo.purchaseItem("hat")

    // Then
    val flowValue = repo.items.value
    val hatItem = flowValue.find { it.id == "hat" }!!
    assertTrue(hatItem.owned)
  }

  @Test
  fun purchaseItem_returns_false_for_already_owned_item() = runTest {
    // Given
    repo.purchaseItem("scarf")

    // When
    val result = repo.purchaseItem("scarf")

    // Then
    assertFalse(result)
  }

  @Test
  fun purchaseItem_only_marks_specified_item() = runTest {
    // When
    repo.purchaseItem("wings")
    val items = repo.getItems()

    // Then
    assertTrue(items.find { it.id == "wings" }!!.owned)
    assertFalse(items.find { it.id == "glasses" }!!.owned)
    assertFalse(items.find { it.id == "hat" }!!.owned)
    assertFalse(items.find { it.id == "scarf" }!!.owned)
    assertFalse(items.find { it.id == "aura" }!!.owned)
    assertFalse(items.find { it.id == "cape" }!!.owned)
  }

  @Test
  fun purchaseItem_multiple_purchases_work() = runTest {
    // When
    repo.purchaseItem("glasses")
    repo.purchaseItem("hat")
    repo.purchaseItem("scarf")
    val items = repo.getItems()

    // Then
    assertTrue(items.find { it.id == "glasses" }!!.owned)
    assertTrue(items.find { it.id == "hat" }!!.owned)
    assertTrue(items.find { it.id == "scarf" }!!.owned)
    assertFalse(items.find { it.id == "wings" }!!.owned)
    assertFalse(items.find { it.id == "aura" }!!.owned)
    assertFalse(items.find { it.id == "cape" }!!.owned)
  }

  @Test
  fun purchaseItem_persists_across_repository_instances() = runTest {
    // Given
    repo.purchaseItem("aura")

    // When - create new repo instance
    val newRepo = FirestoreShopRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
    val items = newRepo.getItems()

    // Then
    val auraItem = items.find { it.id == "aura" }!!
    assertTrue(auraItem.owned)
  }

  // ========== Owned Items Operations ==========

  @Test
  fun getOwnedItemIds_returns_empty_initially() = runTest {
    // When
    val ownedIds = repo.getOwnedItemIds()

    // Then
    assertTrue(ownedIds.isEmpty())
  }

  @Test
  fun getOwnedItemIds_returns_purchased_items() = runTest {
    // Given
    repo.purchaseItem("glasses")
    repo.purchaseItem("hat")

    // When
    val ownedIds = repo.getOwnedItemIds()

    // Then
    assertEquals(2, ownedIds.size)
    assertTrue(ownedIds.contains("glasses"))
    assertTrue(ownedIds.contains("hat"))
  }

  @Test
  fun getOwnedItemIds_does_not_include_unpurchased() = runTest {
    // Given
    repo.purchaseItem("glasses")

    // When
    val ownedIds = repo.getOwnedItemIds()

    // Then
    assertFalse(ownedIds.contains("hat"))
    assertFalse(ownedIds.contains("scarf"))
    assertFalse(ownedIds.contains("wings"))
  }

  @Test
  fun getOwnedItemIds_persists_across_instances() = runTest {
    // Given
    repo.purchaseItem("glasses")
    repo.purchaseItem("hat")

    // When - new repo instance
    val newRepo = FirestoreShopRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
    val ownedIds = newRepo.getOwnedItemIds()

    // Then
    assertEquals(2, ownedIds.size)
    assertTrue(ownedIds.contains("glasses"))
    assertTrue(ownedIds.contains("hat"))
  }

  // ========== Refresh Operations ==========

  @Test
  fun refreshOwnedStatus_updates_items_flow() = runTest {
    // Given
    repo.purchaseItem("wings")

    // When
    repo.refreshOwnedStatus()

    // Then
    val flowValue = repo.items.value
    assertTrue(flowValue.find { it.id == "wings" }!!.owned)
    assertFalse(flowValue.find { it.id == "glasses" }!!.owned)
  }

  @Test
  fun refreshOwnedStatus_syncs_with_firestore() = runTest {
    // Given
    repo.purchaseItem("glasses")
    repo.purchaseItem("hat")

    // When - create new repo and refresh
    val newRepo = FirestoreShopRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
    newRepo.refreshOwnedStatus()

    // Then
    val flowValue = newRepo.items.value
    assertTrue(flowValue.find { it.id == "glasses" }!!.owned)
    assertTrue(flowValue.find { it.id == "hat" }!!.owned)
    assertFalse(flowValue.find { it.id == "scarf" }!!.owned)
  }

  // ========== Edge Cases ==========

  @Test
  fun purchaseItem_all_items_can_be_owned() = runTest {
    // When
    repo.purchaseItem("glasses")
    repo.purchaseItem("hat")
    repo.purchaseItem("scarf")
    repo.purchaseItem("wings")
    repo.purchaseItem("aura")
    repo.purchaseItem("cape")
    val items = repo.getItems()

    // Then
    assertTrue(items.all { it.owned })
  }

  @Test
  fun items_flow_reflects_current_state() = runTest {
    // Given
    val initialFlow = repo.items.value
    assertFalse(initialFlow.find { it.id == "glasses" }!!.owned)

    // When
    repo.purchaseItem("glasses")

    // Then
    val updatedFlow = repo.items.value
    assertTrue(updatedFlow.find { it.id == "glasses" }!!.owned)
  }

  @Test
  fun repository_handles_multiple_sequential_operations() = runTest {
    // When
    repo.purchaseItem("glasses")
    val firstCheck = repo.getOwnedItemIds()

    repo.purchaseItem("hat")
    val secondCheck = repo.getOwnedItemIds()

    repo.purchaseItem("scarf")
    val thirdCheck = repo.getOwnedItemIds()

    // Then
    assertEquals(1, firstCheck.size)
    assertEquals(2, secondCheck.size)
    assertEquals(3, thirdCheck.size)
  }

  @Test
  fun defaultCosmetics_returns_correct_structure() {
    // When
    val items = FirestoreShopRepository.defaultCosmetics()

    // Then
    assertEquals(6, items.size)
    items.forEach { item ->
      assertTrue(item.id.isNotEmpty())
      assertTrue(item.name.isNotEmpty())
      assertTrue(item.price > 0)
      assertTrue(item.imageRes != 0)
      assertFalse(item.owned)
    }
  }

  // ========== User Isolation ==========

  @Test
  fun purchases_are_user_specific() = runTest {
    // Given - User 1 purchases an item
    repo.purchaseItem("glasses")
    val user1OwnedIds = repo.getOwnedItemIds()

    // When - Sign out and sign in as new user
    FirebaseEmulator.auth.signOut()
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())
    val newRepo = FirestoreShopRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
    val user2OwnedIds = newRepo.getOwnedItemIds()

    // Then - User 2 should not have User 1's purchases
    assertEquals(1, user1OwnedIds.size)
    assertTrue(user2OwnedIds.isEmpty())
  }
}

