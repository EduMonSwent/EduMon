package com.android.sample.ui.shop.repository

import junit.framework.TestCase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

/**
 * Unit tests for FirestoreShopRepository.
 *
 * Note: Tests involving Firebase async operations (Tasks.await()) are complex to mock
 * and are better tested in FirestoreShopRepositoryEmulatorTest with real Firebase.
 *
 * These tests focus on static methods and simple validations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreShopRepositoryTest {

  @Test
  fun `defaultCosmetics returns six items`() {
    // When
    val items = FirestoreShopRepository.defaultCosmetics()

    // Then
    assertEquals(6, items.size)
  }

  @Test
  fun `defaultCosmetics items have correct structure`() {
    // When
    val items = FirestoreShopRepository.defaultCosmetics()

    // Then
    items.forEach { item ->
      assertTrue("Item ID should not be empty", item.id.isNotEmpty())
      assertTrue("Item name should not be empty", item.name.isNotEmpty())
      assertTrue("Item price should be positive", item.price > 0)
      assertTrue("Item should have a drawable resource", item.imageRes != 0)
      assertFalse("Item should not be owned initially", item.owned)
    }
  }

  @Test
  fun `defaultCosmetics contains expected items`() {
    // When
    val items = FirestoreShopRepository.defaultCosmetics()
    val itemIds = items.map { it.id }.toSet()

    // Then
    assertTrue("Should contain glasses", itemIds.contains("glasses"))
    assertTrue("Should contain hat", itemIds.contains("hat"))
    assertTrue("Should contain scarf", itemIds.contains("scarf"))
    assertTrue("Should contain wings", itemIds.contains("wings"))
    assertTrue("Should contain aura", itemIds.contains("aura"))
    assertTrue("Should contain cape", itemIds.contains("cape"))
  }

  @Test
  fun `defaultCosmetics has correct prices`() {
    // When
    val items = FirestoreShopRepository.defaultCosmetics()

    // Then
    assertEquals(200, items.find { it.id == "glasses" }?.price)
    assertEquals(200, items.find { it.id == "hat" }?.price)
    assertEquals(200, items.find { it.id == "scarf" }?.price)
    assertEquals(200, items.find { it.id == "wings" }?.price)
    assertEquals(1500, items.find { it.id == "aura" }?.price)
    assertEquals(200, items.find { it.id == "cape" }?.price)
  }

  @Test
  fun `defaultCosmetics aura is most expensive`() {
    // When
    val items = FirestoreShopRepository.defaultCosmetics()
    val maxPrice = items.maxOf { it.price }

    // Then
    assertEquals(1500, maxPrice)
    assertEquals("aura", items.find { it.price == maxPrice }?.id)
  }

  @Test
  fun `defaultCosmetics all items have unique ids`() {
    // When
    val items = FirestoreShopRepository.defaultCosmetics()
    val ids = items.map { it.id }

    // Then
    assertEquals("All item IDs should be unique", ids.size, ids.toSet().size)
  }

  @Test
  fun `defaultCosmetics all items have unique names`() {
    // When
    val items = FirestoreShopRepository.defaultCosmetics()
    val names = items.map { it.name }

    // Then
    assertEquals("All item names should be unique", names.size, names.toSet().size)
  }
}

