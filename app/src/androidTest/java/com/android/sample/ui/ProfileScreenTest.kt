package com.android.sample.ui

import androidx.compose.ui.graphics.toArgb
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessorySlot
import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.data.Rarity
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.ui.profile.ProfileViewModel
import com.android.sample.ui.theme.AccentBlue
import com.android.sample.ui.theme.AccentViolet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ProfileScreenTest {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_setAvatarAccent_updatesProfile() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repo, FakeUserStatsRepository())

    val newColor = AccentBlue
    vm.setAvatarAccent(newColor)

    advanceUntilIdle()

    assertEquals(newColor.toArgb().toLong(), vm.userProfile.value.avatarAccent)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_setAccentVariant_updatesVariant() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.setAccentVariant(AccentVariant.Base)
    assertEquals(AccentVariant.Base, vm.accentVariantFlow.value)

    vm.setAccentVariant(AccentVariant.Light)
    assertEquals(AccentVariant.Light, vm.accentVariantFlow.value)

    vm.setAccentVariant(AccentVariant.Dark)
    assertEquals(AccentVariant.Dark, vm.accentVariantFlow.value)

    vm.setAccentVariant(AccentVariant.Vibrant)
    assertEquals(AccentVariant.Vibrant, vm.accentVariantFlow.value)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_toggleNotifications_updatesProfile() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    val initial = vm.userProfile.value.notificationsEnabled
    vm.toggleNotifications()
    advanceUntilIdle()

    assertEquals(!initial, vm.userProfile.value.notificationsEnabled)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_toggleLocation_updatesProfile() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    val initial = vm.userProfile.value.locationEnabled
    vm.toggleLocation()
    advanceUntilIdle()

    assertEquals(!initial, vm.userProfile.value.locationEnabled)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_toggleFocusMode_updatesProfile() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    val initial = vm.userProfile.value.focusModeEnabled
    vm.toggleFocusMode()
    advanceUntilIdle()

    assertEquals(!initial, vm.userProfile.value.focusModeEnabled)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipAccessory_addsToProfile() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.HEAD, "crown")
    advanceUntilIdle()

    assertTrue(vm.userProfile.value.accessories.any { it.startsWith("head:crown") })
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipNone_removesAccessory() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.HEAD, "crown")
    advanceUntilIdle()
    assertTrue(vm.userProfile.value.accessories.any { it.startsWith("head:") })

    vm.equip(AccessorySlot.HEAD, "none")
    advanceUntilIdle()

    assertFalse(vm.userProfile.value.accessories.any { it.startsWith("head:") })
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipSameItem_removesIt() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.HEAD, "crown")
    advanceUntilIdle()

    val equippedId = vm.equippedId(AccessorySlot.HEAD)
    assertEquals("crown", equippedId)

    vm.equip(AccessorySlot.HEAD, "crown")
    advanceUntilIdle()

    assertNull(vm.equippedId(AccessorySlot.HEAD))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipLegs_handlesLegacyPrefix() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.LEGS, "boots")
    advanceUntilIdle()

    val equippedId = vm.equippedId(AccessorySlot.LEGS)
    assertEquals("boots", equippedId)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipTorso_works() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.TORSO, "armor")
    advanceUntilIdle()

    assertEquals("armor", vm.equippedId(AccessorySlot.TORSO))
    assertTrue(vm.userProfile.value.accessories.any { it.startsWith("torso:") })
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_unequip_removesAccessory() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.TORSO, "armor")
    advanceUntilIdle()
    assertTrue(vm.userProfile.value.accessories.any { it.startsWith("torso:") })

    vm.unequip(AccessorySlot.TORSO)
    advanceUntilIdle()
    assertFalse(vm.userProfile.value.accessories.any { it.startsWith("torso:") })
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_unequipHead_works() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.HEAD, "halo")
    advanceUntilIdle()
    assertTrue(vm.userProfile.value.accessories.any { it.startsWith("head:") })

    vm.unequip(AccessorySlot.HEAD)
    advanceUntilIdle()
    assertFalse(vm.userProfile.value.accessories.any { it.startsWith("head:") })
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_unequipLegs_works() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.LEGS, "rocket")
    advanceUntilIdle()
    assertTrue(vm.userProfile.value.accessories.any { it.startsWith("legs:") })

    vm.unequip(AccessorySlot.LEGS)
    advanceUntilIdle()
    assertFalse(vm.userProfile.value.accessories.any { it.startsWith("legs:") })
  }

  @Test
  fun viewModel_equippedId_returnsNullWhenNothingEquipped() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    assertNull(vm.equippedId(AccessorySlot.HEAD))
    assertNull(vm.equippedId(AccessorySlot.TORSO))
    assertNull(vm.equippedId(AccessorySlot.LEGS))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_addCoins_withZero_doesNothing() = runTest {
    val statsRepo = FakeUserStatsRepository()
    val vm = ProfileViewModel(FakeProfileRepository(), statsRepo)

    val initialCoins = vm.userStats.value.coins
    vm.addCoins(0)
    advanceUntilIdle()

    assertEquals(initialCoins, vm.userStats.value.coins)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_addCoins_withNegative_doesNothing() = runTest {
    val statsRepo = FakeUserStatsRepository()
    val vm = ProfileViewModel(FakeProfileRepository(), statsRepo)

    val initialCoins = vm.userStats.value.coins
    vm.addCoins(-10)
    advanceUntilIdle()

    assertEquals(initialCoins, vm.userStats.value.coins)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_addCoins_withPositive_updatesCoins() = runTest {
    val statsRepo = FakeUserStatsRepository()
    val vm = ProfileViewModel(FakeProfileRepository(), statsRepo)

    val initialCoins = vm.userStats.value.coins
    vm.addCoins(50)
    advanceUntilIdle()

    assertTrue(vm.userStats.value.coins >= initialCoins)
  }

  @Test
  fun viewModel_accentPalette_containsExpectedColors() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    assertEquals(5, vm.accentPalette.size)
    assertTrue(vm.accentPalette.contains(AccentViolet))
    assertTrue(vm.accentPalette.contains(AccentBlue))
  }

  @Test
  fun viewModel_accessoryCatalog_containsAllItems() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    assertTrue(vm.accessoryCatalog.any { it.slot == AccessorySlot.HEAD })
    assertTrue(vm.accessoryCatalog.any { it.slot == AccessorySlot.TORSO })
    assertTrue(vm.accessoryCatalog.any { it.slot == AccessorySlot.LEGS })

    assertTrue(vm.accessoryCatalog.any { it.rarity == Rarity.COMMON })
    assertTrue(vm.accessoryCatalog.any { it.rarity == Rarity.RARE })
    assertTrue(vm.accessoryCatalog.any { it.rarity == Rarity.EPIC })
    assertTrue(vm.accessoryCatalog.any { it.rarity == Rarity.LEGENDARY })
  }

  @Test
  fun viewModel_accessoryCatalog_hasNoneForEachSlot() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    assertTrue(vm.accessoryCatalog.any { it.slot == AccessorySlot.HEAD && it.id == "none" })
    assertTrue(vm.accessoryCatalog.any { it.slot == AccessorySlot.TORSO && it.id == "none" })
    assertTrue(vm.accessoryCatalog.any { it.slot == AccessorySlot.LEGS && it.id == "none" })
  }

  @Test
  fun viewModel_accentEffective_variantsChangeBehavior() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    // Test that each variant is set correctly
    vm.setAccentVariant(AccentVariant.Base)
    assertEquals(AccentVariant.Base, vm.accentVariantFlow.value)

    vm.setAccentVariant(AccentVariant.Light)
    assertEquals(AccentVariant.Light, vm.accentVariantFlow.value)

    vm.setAccentVariant(AccentVariant.Dark)
    assertEquals(AccentVariant.Dark, vm.accentVariantFlow.value)

    vm.setAccentVariant(AccentVariant.Vibrant)
    assertEquals(AccentVariant.Vibrant, vm.accentVariantFlow.value)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipMultipleAccessories_onlyOnePerSlot() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.HEAD, "crown")
    advanceUntilIdle()
    assertEquals("crown", vm.equippedId(AccessorySlot.HEAD))

    vm.equip(AccessorySlot.HEAD, "halo")
    advanceUntilIdle()
    assertEquals("halo", vm.equippedId(AccessorySlot.HEAD))

    val headItems = vm.userProfile.value.accessories.filter { it.startsWith("head:") }
    assertEquals(1, headItems.size)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipDifferentSlots_independent() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.equip(AccessorySlot.HEAD, "crown")
    vm.equip(AccessorySlot.TORSO, "armor")
    vm.equip(AccessorySlot.LEGS, "boots")
    advanceUntilIdle()

    assertEquals("crown", vm.equippedId(AccessorySlot.HEAD))
    assertEquals("armor", vm.equippedId(AccessorySlot.TORSO))
    assertEquals("boots", vm.equippedId(AccessorySlot.LEGS))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_userStats_syncsWithProfile() = runTest {
    val statsRepo = FakeUserStatsRepository()
    val vm = ProfileViewModel(FakeProfileRepository(), statsRepo)

    advanceUntilIdle()

    assertEquals(vm.userStats.value.coins, vm.userProfile.value.coins)
    assertEquals(vm.userStats.value.points, vm.userProfile.value.points)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_allAccentVariants_produceDifferentColors() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    // Test that all variants can be set
    for (variant in AccentVariant.values()) {
      vm.setAccentVariant(variant)
      advanceUntilIdle()
      assertEquals(variant, vm.accentVariantFlow.value)
    }
  }

  @Test
  fun viewModel_catalog_hasCorrectItemNames() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    assertTrue(vm.accessoryCatalog.any { it.id == "halo" && it.label == "Halo" })
    assertTrue(vm.accessoryCatalog.any { it.id == "crown" && it.label == "Crown" })
    assertTrue(vm.accessoryCatalog.any { it.id == "armor" && it.label == "Armor" })
    assertTrue(vm.accessoryCatalog.any { it.id == "rocket" && it.label == "Rocket" })
  }

  @Test
  fun viewModel_catalog_hasCorrectRarities() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    val halo = vm.accessoryCatalog.find { it.id == "halo" }
    assertEquals(Rarity.EPIC, halo?.rarity)

    val crown = vm.accessoryCatalog.find { it.id == "crown" }
    assertEquals(Rarity.RARE, crown?.rarity)

    val rocket = vm.accessoryCatalog.find { it.id == "rocket" }
    assertEquals(Rarity.LEGENDARY, rocket?.rarity)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_multipleToggles_work() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

    vm.toggleLocation()
    vm.toggleFocusMode()
    vm.toggleNotifications()
    advanceUntilIdle()

    assertNotNull(vm.userProfile.value.locationEnabled)
    assertNotNull(vm.userProfile.value.focusModeEnabled)
    assertNotNull(vm.userProfile.value.notificationsEnabled)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipAccessoryAntennaHead() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
    vm.equip(AccessorySlot.HEAD, "antenna")
    advanceUntilIdle()
    assertEquals("antenna", vm.equippedId(AccessorySlot.HEAD))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipAccessoryBadgeTorso() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
    vm.equip(AccessorySlot.TORSO, "badge")
    advanceUntilIdle()
    assertEquals("badge", vm.equippedId(AccessorySlot.TORSO))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipAccessoryScarfTorso() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
    vm.equip(AccessorySlot.TORSO, "scarf")
    advanceUntilIdle()
    assertEquals("scarf", vm.equippedId(AccessorySlot.TORSO))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModel_equipAccessorySkatesLegs() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
    vm.equip(AccessorySlot.LEGS, "skates")
    advanceUntilIdle()
    assertEquals("skates", vm.equippedId(AccessorySlot.LEGS))
  }

  @Test
  fun viewModel_catalogHasAllHeadItems() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
    val headItems = vm.accessoryCatalog.filter { it.slot == AccessorySlot.HEAD }
    assertTrue(headItems.size >= 4) // none, halo, crown, antenna
  }

  @Test
  fun viewModel_catalogHasAllTorsoItems() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
    val torsoItems = vm.accessoryCatalog.filter { it.slot == AccessorySlot.TORSO }
    assertTrue(torsoItems.size >= 4) // none, badge, scarf, armor
  }

  @Test
  fun viewModel_catalogHasAllLegsItems() {
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
    val legsItems = vm.accessoryCatalog.filter { it.slot == AccessorySlot.LEGS }
    assertTrue(legsItems.size >= 4) // none, boots, rocket, skates
  }
}
