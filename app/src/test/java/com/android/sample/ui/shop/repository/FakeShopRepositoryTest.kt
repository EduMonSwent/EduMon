package com.android.sample.ui.shop.repository

import com.android.sample.R
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

class FakeShopRepositoryTest {

  private lateinit var repository: FakeShopRepository

  @Before
  fun setup() {
    repository = FakeShopRepository()
  }

  @Test
  fun `getItems returns default cosmetics`() = runTest {
    // When
    val items = repository.getItems()

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
  fun `initial items have correct prices`() = runTest {
    // When
    val items = repository.getItems()

    // Then
    assertEquals(200, items.find { it.id == "glasses" }?.price)
    assertEquals(200, items.find { it.id == "hat" }?.price)
    assertEquals(200, items.find { it.id == "scarf" }?.price)
    assertEquals(200, items.find { it.id == "wings" }?.price)
    assertEquals(1500, items.find { it.id == "aura" }?.price)
    assertEquals(200, items.find { it.id == "cape" }?.price)
  }

  @Test
  fun `initial items have correct drawable resources`() = runTest {
    // When
    val items = repository.getItems()

    // Then
    assertEquals(R.drawable.shop_cosmetic_glasses, items.find { it.id == "glasses" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_hat, items.find { it.id == "hat" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_scarf, items.find { it.id == "scarf" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_wings, items.find { it.id == "wings" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_aura, items.find { it.id == "aura" }?.imageRes)
    assertEquals(R.drawable.shop_cosmetic_cape, items.find { it.id == "cape" }?.imageRes)
  }

  @Test
  fun `all items are initially not owned`() = runTest {
    // When
    val items = repository.getItems()

    // Then
    assertTrue(items.all { !it.owned })
  }

  @Test
  fun `items flow emits default cosmetics initially`() = runTest {
    // When
    val flowValue = repository.items.value

    // Then
    assertEquals(6, flowValue.size)
    assertTrue(flowValue.all { !it.owned })
  }

  @Test
  fun `purchaseItem returns true for unowned item`() = runTest {
    // When
    val result = repository.purchaseItem("glasses")

    // Then
    assertTrue(result)
  }

  @Test
  fun `purchaseItem marks item as owned in items list`() = runTest {
    // When
    repository.purchaseItem("glasses")
    val items = repository.getItems()

    // Then
    val glassesItem = items.find { it.id == "glasses" }!!
    assertTrue(glassesItem.owned)
  }

  @Test
  fun `purchaseItem updates items flow`() = runTest {
    // When
    repository.purchaseItem("hat")

    // Then
    val flowValue = repository.items.value
    val hatItem = flowValue.find { it.id == "hat" }!!
    assertTrue(hatItem.owned)
  }

  @Test
  fun `purchaseItem returns false for already owned item`() = runTest {
    // Given
    repository.purchaseItem("scarf")

    // When
    val result = repository.purchaseItem("scarf")

    // Then
    assertFalse(result)
  }

  @Test
  fun `purchaseItem only marks specified item as owned`() = runTest {
    // When
    repository.purchaseItem("wings")
    val items = repository.getItems()

    // Then
    assertTrue(items.find { it.id == "wings" }!!.owned)
    assertFalse(items.find { it.id == "glasses" }!!.owned)
    assertFalse(items.find { it.id == "hat" }!!.owned)
    assertFalse(items.find { it.id == "scarf" }!!.owned)
    assertFalse(items.find { it.id == "aura" }!!.owned)
    assertFalse(items.find { it.id == "cape" }!!.owned)
  }

  @Test
  fun `multiple purchases work correctly`() = runTest {
    // When
    repository.purchaseItem("glasses")
    repository.purchaseItem("hat")
    repository.purchaseItem("scarf")
    val items = repository.getItems()

    // Then
    assertTrue(items.find { it.id == "glasses" }!!.owned)
    assertTrue(items.find { it.id == "hat" }!!.owned)
    assertTrue(items.find { it.id == "scarf" }!!.owned)
    assertFalse(items.find { it.id == "wings" }!!.owned)
    assertFalse(items.find { it.id == "aura" }!!.owned)
    assertFalse(items.find { it.id == "cape" }!!.owned)
  }

  @Test
  fun `getOwnedItemIds returns empty set initially`() = runTest {
    // When
    val ownedIds = repository.getOwnedItemIds()

    // Then
    assertTrue(ownedIds.isEmpty())
  }

  @Test
  fun `getOwnedItemIds returns purchased items`() = runTest {
    // Given
    repository.purchaseItem("glasses")
    repository.purchaseItem("hat")

    // When
    val ownedIds = repository.getOwnedItemIds()

    // Then
    assertEquals(2, ownedIds.size)
    assertTrue(ownedIds.contains("glasses"))
    assertTrue(ownedIds.contains("hat"))
  }

  @Test
  fun `getOwnedItemIds does not include unpurchased items`() = runTest {
    // Given
    repository.purchaseItem("glasses")

    // When
    val ownedIds = repository.getOwnedItemIds()

    // Then
    assertFalse(ownedIds.contains("hat"))
    assertFalse(ownedIds.contains("scarf"))
  }

  @Test
  fun `refreshOwnedStatus updates items flow with owned status`() = runTest {
    // Given
    repository.purchaseItem("aura")

    // When
    repository.refreshOwnedStatus()

    // Then
    val flowValue = repository.items.value
    assertTrue(flowValue.find { it.id == "aura" }!!.owned)
    assertFalse(flowValue.find { it.id == "glasses" }!!.owned)
  }

  @Test
  fun `purchasing all items marks all as owned`() = runTest {
    // When
    repository.purchaseItem("glasses")
    repository.purchaseItem("hat")
    repository.purchaseItem("scarf")
    repository.purchaseItem("wings")
    repository.purchaseItem("aura")
    repository.purchaseItem("cape")
    val items = repository.getItems()

    // Then
    assertTrue(items.all { it.owned })
  }

  @Test
  fun `defaultCosmetics returns correct count`() {
    // When
    val items = FakeShopRepository.defaultCosmetics()

    // Then
    assertEquals(6, items.size)
  }

  @Test
  fun `defaultCosmetics returns items with expected structure`() {
    // When
    val items = FakeShopRepository.defaultCosmetics()

    // Then
    items.forEach { item ->
      assertTrue(item.id.isNotEmpty())
      assertTrue(item.name.isNotEmpty())
      assertTrue(item.price > 0)
      assertTrue(item.imageRes != 0)
      assertFalse(item.owned)
    }
  }

  @Test
  fun `repository maintains state across multiple operations`() = runTest {
    // When
    repository.purchaseItem("glasses")
    val firstCheck = repository.getOwnedItemIds()

    repository.purchaseItem("hat")
    val secondCheck = repository.getOwnedItemIds()

    repository.purchaseItem("scarf")
    val thirdCheck = repository.getOwnedItemIds()

    // Then
    assertEquals(1, firstCheck.size)
    assertEquals(2, secondCheck.size)
    assertEquals(3, thirdCheck.size)
  }
}

