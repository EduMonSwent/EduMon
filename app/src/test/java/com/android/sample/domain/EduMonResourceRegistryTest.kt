// This code was written with the assistance of an AI (LLM).
package com.android.sample.domain

import com.android.sample.R
import com.android.sample.data.AccessorySlot
import com.android.sample.domain.model.AccessoryType
import com.android.sample.domain.model.EduMonType
import com.android.sample.domain.ressources.EduMonResourceRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EduMonResourceRegistryTest {

    private companion object {
        const val NO_RESOURCE = 0
        const val INVALID_ID = "nonexistent"
        const val EMPTY_STRING = ""
    }

    @Test
    fun getBaseDrawable_pyromon_returnsCorrectDrawable() {
        val result = EduMonResourceRegistry.getBaseDrawable(EduMonType.PYROMON)
        assertEquals(R.drawable.edumon, result)
    }

    @Test
    fun getBaseDrawable_aquamon_returnsCorrectDrawable() {
        val result = EduMonResourceRegistry.getBaseDrawable(EduMonType.AQUAMON)
        assertEquals(R.drawable.edumon2, result)
    }

    @Test
    fun getBaseDrawable_floramon_returnsCorrectDrawable() {
        val result = EduMonResourceRegistry.getBaseDrawable(EduMonType.FLORAMON)
        assertEquals(R.drawable.edumon1, result)
    }

    @Test
    fun getBaseDrawable_allTypes_returnDifferentDrawables() {
        val pyromon = EduMonResourceRegistry.getBaseDrawable(EduMonType.PYROMON)
        val aquamon = EduMonResourceRegistry.getBaseDrawable(EduMonType.AQUAMON)
        val floramon = EduMonResourceRegistry.getBaseDrawable(EduMonType.FLORAMON)

        assertNotEquals(pyromon, aquamon)
        assertNotEquals(aquamon, floramon)
        assertNotEquals(pyromon, floramon)
    }

    @Test
    fun getAccessoryDrawable_pyromonHat_returnsCorrectDrawable() {
        val result = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.PYROMON,
            AccessorySlot.HEAD,
            AccessoryType.HAT
        )
        assertEquals(R.drawable.pyromon_acc_hat, result)
    }

    @Test
    fun getAccessoryDrawable_aquamonWings_returnsCorrectDrawable() {
        val result = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.AQUAMON,
            AccessorySlot.BACK,
            AccessoryType.WINGS
        )
        assertEquals(R.drawable.aquamon_acc_wings, result)
    }

    @Test
    fun getAccessoryDrawable_floramonScarf_returnsCorrectDrawable() {
        val result = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.FLORAMON,
            AccessorySlot.TORSO,
            AccessoryType.SCARF
        )
        assertEquals(R.drawable.floramon_acc_scarf, result)
    }

    @Test
    fun getAccessoryDrawable_withStringId_returnsCorrectDrawable() {
        val result = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.PYROMON,
            AccessorySlot.HEAD,
            AccessoryType.GLASSES.id
        )
        assertEquals(R.drawable.pyromon_acc_glasses, result)
    }

    @Test
    fun getAccessoryDrawable_invalidStringId_returnsNoResource() {
        val result = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.PYROMON,
            AccessorySlot.HEAD,
            INVALID_ID
        )
        assertEquals(NO_RESOURCE, result)
    }

    @Test
    fun getAccessoryDrawable_emptyStringId_returnsNoResource() {
        val result = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.PYROMON,
            AccessorySlot.HEAD,
            EMPTY_STRING
        )
        assertEquals(NO_RESOURCE, result)
    }

    @Test
    fun getAccessoryDrawable_sameAccessory_returnsDifferentDrawablePerEduMon() {
        val pyromonHat = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.PYROMON,
            AccessorySlot.HEAD,
            AccessoryType.HAT
        )
        val aquamonHat = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.AQUAMON,
            AccessorySlot.HEAD,
            AccessoryType.HAT
        )
        val floramonHat = EduMonResourceRegistry.getAccessoryDrawable(
            EduMonType.FLORAMON,
            AccessorySlot.HEAD,
            AccessoryType.HAT
        )

        assertNotEquals(pyromonHat, aquamonHat)
        assertNotEquals(aquamonHat, floramonHat)
        assertNotEquals(pyromonHat, floramonHat)
    }

    @Test
    fun eduMonType_fromId_validIds_returnsCorrectType() {
        assertEquals(EduMonType.PYROMON, EduMonType.fromId(EduMonType.PYROMON.id))
        assertEquals(EduMonType.AQUAMON, EduMonType.fromId(EduMonType.AQUAMON.id))
        assertEquals(EduMonType.FLORAMON, EduMonType.fromId(EduMonType.FLORAMON.id))
    }

    @Test
    fun eduMonType_fromId_null_returnsDefault() {
        assertEquals(EduMonType.PYROMON, EduMonType.fromId(null))
    }

    @Test
    fun eduMonType_fromId_unknownId_returnsDefault() {
        assertEquals(EduMonType.PYROMON, EduMonType.fromId(INVALID_ID))
        assertEquals(EduMonType.PYROMON, EduMonType.fromId(EMPTY_STRING))
    }

    @Test
    fun accessoryType_fromId_validIds_returnsCorrectType() {
        assertEquals(AccessoryType.HAT, AccessoryType.fromId(AccessoryType.HAT.id))
        assertEquals(AccessoryType.GLASSES, AccessoryType.fromId(AccessoryType.GLASSES.id))
        assertEquals(AccessoryType.SCARF, AccessoryType.fromId(AccessoryType.SCARF.id))
        assertEquals(AccessoryType.WINGS, AccessoryType.fromId(AccessoryType.WINGS.id))
        assertEquals(AccessoryType.AURA, AccessoryType.fromId(AccessoryType.AURA.id))
        assertEquals(AccessoryType.CAPE, AccessoryType.fromId(AccessoryType.CAPE.id))
    }

    @Test
    fun accessoryType_fromId_invalidId_returnsNull() {
        assertNull(AccessoryType.fromId(null))
        assertNull(AccessoryType.fromId(EMPTY_STRING))
        assertNull(AccessoryType.fromId(INVALID_ID))
    }

    @Test
    fun allHeadAccessories_availableForAllEduMonTypes() {
        val headAccessories = listOf(AccessoryType.HAT, AccessoryType.GLASSES)

        for (eduMon in EduMonType.values()) {
            for (accessory in headAccessories) {
                val result = EduMonResourceRegistry.getAccessoryDrawable(
                    eduMon,
                    AccessorySlot.HEAD,
                    accessory
                )
                assertNotEquals(
                    "Missing: $eduMon HEAD $accessory",
                    NO_RESOURCE,
                    result
                )
            }
        }
    }

    @Test
    fun allTorsoAccessories_availableForAllEduMonTypes() {
        val torsoAccessories = listOf(AccessoryType.SCARF, AccessoryType.CAPE)

        for (eduMon in EduMonType.values()) {
            for (accessory in torsoAccessories) {
                val result = EduMonResourceRegistry.getAccessoryDrawable(
                    eduMon,
                    AccessorySlot.TORSO,
                    accessory
                )
                assertNotEquals(
                    "Missing: $eduMon TORSO $accessory",
                    NO_RESOURCE,
                    result
                )
            }
        }
    }

    @Test
    fun allBackAccessories_availableForAllEduMonTypes() {
        val backAccessories = listOf(AccessoryType.WINGS, AccessoryType.AURA)

        for (eduMon in EduMonType.values()) {
            for (accessory in backAccessories) {
                val result = EduMonResourceRegistry.getAccessoryDrawable(
                    eduMon,
                    AccessorySlot.BACK,
                    accessory
                )
                assertNotEquals(
                    "Missing: $eduMon BACK $accessory",
                    NO_RESOURCE,
                    result
                )
            }
        }
    }
}