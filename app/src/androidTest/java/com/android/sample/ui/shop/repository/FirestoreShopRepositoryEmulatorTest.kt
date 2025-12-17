// This code was written with the assistance of an AI (LLM).
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

class FirestoreShopRepositoryEmulatorTest {

  private lateinit var repo: FirestoreShopRepository

  @Before
  fun setUp() {
    FirebaseEmulator.initIfNeeded(ApplicationProvider.getApplicationContext())
    FirebaseEmulator.connectIfRunning()

    assumeTrue(
        "Firebase Emulator is not running. Start it with: firebase emulators:start",
        FirebaseEmulator.isRunning)

    runTest {
      FirebaseEmulator.clearAll()
      Tasks.await(FirebaseEmulator.auth.signInAnonymously())
    }

    repo = FirestoreShopRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      runTest { FirebaseEmulator.clearAll() }
    }
  }

  @Test
  fun getItems_seeds_default_when_empty() = runTest {
    val items = repo.getItems()

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
    val items = repo.getItems()

    assertEquals(200, items.find { it.id == "glasses" }?.price)
    assertEquals(200, items.find { it.id == "hat" }?.price)
    assertEquals(200, items.find { it.id == "scarf" }?.price)
    assertEquals(200, items.find { it.id == "wings" }?.price)
    assertEquals(1500, items.find { it.id == "aura" }?.price)
    assertEquals(200, items.find { it.id == "cape" }?.price)
  }

  @Test
  fun getItems_returns_correct_drawable_resources() = runTest {
    val items = repo.getItems()

    assertEquals(R.drawable.shop_cosmetic_glasses, items.find { it.id == "glasses" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_hat, items.find { it.id == "hat" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_scarf, items.find { it.id == "scarf" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_wings, items.find { it.id == "wings" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_aura, items.find { it.id == "aura" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_cape, items.find { it.id == "cape" }?.imageRes)
  }

  @Test
  fun getItems_all_items_initially_not_owned() = runTest {
    val items = repo.getItems()

    assertTrue(items.all { !it.owned })
  }

  @Test
  fun getItems_persists_to_firestore() = runTest {
    repo.getItems()

    val items = repo.getItems()

    assertEquals(6, items.size)
    assertTrue(items.all { it.id.isNotEmpty() && it.name.isNotEmpty() })
  }

  @Test
  fun purchaseItem_returns_true_for_unowned_item() = runTest {
    val result = repo.purchaseItem("glasses")

    assertTrue(result)
  }

  @Test
  fun purchaseItem_marks_item_as_owned_in_flow() = runTest {
    repo.purchaseItem("glasses")

    val flowValue = repo.items.value
    val glassesItem = flowValue.find { it.id == "glasses" }!!
    assertTrue(glassesItem.owned)
  }

  @Test
  fun purchaseItem_updates_items_flow() = runTest {
    repo.purchaseItem("hat")

    val flowValue = repo.items.value
    val hatItem = flowValue.find { it.id == "hat" }!!
    assertTrue(hatItem.owned)
  }

  @Test
  fun purchaseItem_only_marks_specified_item_in_flow() = runTest {
    repo.purchaseItem("wings")

    val flowValue = repo.items.value
    assertTrue(flowValue.find { it.id == "wings" }!!.owned)
    assertFalse(flowValue.find { it.id == "glasses" }!!.owned)
    assertFalse(flowValue.find { it.id == "hat" }!!.owned)
    assertFalse(flowValue.find { it.id == "scarf" }!!.owned)
    assertFalse(flowValue.find { it.id == "aura" }!!.owned)
    assertFalse(flowValue.find { it.id == "cape" }!!.owned)
  }

  @Test
  fun purchaseItem_multiple_purchases_update_flow() = runTest {
    repo.purchaseItem("glasses")
    repo.purchaseItem("hat")
    repo.purchaseItem("scarf")

    val flowValue = repo.items.value
    assertTrue(flowValue.find { it.id == "glasses" }!!.owned)
    assertTrue(flowValue.find { it.id == "hat" }!!.owned)
    assertTrue(flowValue.find { it.id == "scarf" }!!.owned)
    assertFalse(flowValue.find { it.id == "wings" }!!.owned)
    assertFalse(flowValue.find { it.id == "aura" }!!.owned)
    assertFalse(flowValue.find { it.id == "cape" }!!.owned)
  }

  @Test
  fun getOwnedItemIds_returns_empty_initially() = runTest {
    val ownedIds = repo.getOwnedItemIds()

    assertTrue(ownedIds.isEmpty())
  }

  @Test
  fun refreshOwnedStatus_updates_items_flow_from_current_state() = runTest {
    repo.purchaseItem("wings")

    val newRepo = FirestoreShopRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
    newRepo.refreshOwnedStatus()

    val flowValue = newRepo.items.value
    assertFalse(flowValue.find { it.id == "glasses" }!!.owned)
  }

  @Test
  fun purchaseItem_all_items_can_be_owned() = runTest {
    repo.purchaseItem("glasses")
    repo.purchaseItem("hat")
    repo.purchaseItem("scarf")
    repo.purchaseItem("wings")
    repo.purchaseItem("aura")
    repo.purchaseItem("cape")

    val flowValue = repo.items.value
    assertTrue(flowValue.all { it.owned })
  }

  @Test
  fun items_flow_reflects_current_state() = runTest {
    val initialFlow = repo.items.value
    assertFalse(initialFlow.find { it.id == "glasses" }!!.owned)

    repo.purchaseItem("glasses")

    val updatedFlow = repo.items.value
    assertTrue(updatedFlow.find { it.id == "glasses" }!!.owned)
  }

  @Test
  fun defaultCosmetics_returns_correct_structure() {
    val items = FirestoreShopRepository.defaultCosmetics()

    assertEquals(6, items.size)
    items.forEach { item ->
      assertTrue(item.id.isNotEmpty())
      assertTrue(item.name.isNotEmpty())
      assertTrue(item.price > 0)
      assertTrue(item.imageRes != 0)
      assertFalse(item.owned)
    }
  }

  @Test
  fun purchases_are_user_specific() = runTest {
    repo.purchaseItem("glasses")
    val user1Flow = repo.items.value
    assertTrue(user1Flow.find { it.id == "glasses" }!!.owned)

    FirebaseEmulator.auth.signOut()
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())
    val newRepo = FirestoreShopRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)

    val user2Flow = newRepo.items.value
    assertFalse(user2Flow.find { it.id == "glasses" }!!.owned)
  }
}
