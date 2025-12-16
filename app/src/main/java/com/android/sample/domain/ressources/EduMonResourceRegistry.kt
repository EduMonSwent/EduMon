// This code was written with the assistance of an AI (LLM).
package com.android.sample.domain.ressources

import androidx.annotation.DrawableRes
import com.android.sample.R
import com.android.sample.data.AccessorySlot
import com.android.sample.domain.model.AccessoryType
import com.android.sample.domain.model.EduMonType

object EduMonResourceRegistry {

    private const val NO_RESOURCE = 0

    private val pyromonAccessories: Map<AccessorySlot, Map<AccessoryType, Int>> = mapOf(
        AccessorySlot.HEAD to mapOf(
            AccessoryType.HAT to R.drawable.pyromon_acc_hat,
            AccessoryType.GLASSES to R.drawable.pyromon_acc_glasses
        ),
        AccessorySlot.TORSO to mapOf(
            AccessoryType.SCARF to R.drawable.pyromon_acc_scarf,
            AccessoryType.CAPE to R.drawable.pyromon_acc_cape
        ),
        AccessorySlot.BACK to mapOf(
            AccessoryType.WINGS to R.drawable.pyromon_acc_wings,
            AccessoryType.AURA to R.drawable.pyromon_acc_aura
        )
    )

    private val aquamonAccessories: Map<AccessorySlot, Map<AccessoryType, Int>> = mapOf(
        AccessorySlot.HEAD to mapOf(
            AccessoryType.HAT to R.drawable.aquamon_acc_hat,
            AccessoryType.GLASSES to R.drawable.aquamon_acc_glasses
        ),
        AccessorySlot.TORSO to mapOf(
            AccessoryType.SCARF to R.drawable.aquamon_acc_scarf,
            AccessoryType.CAPE to R.drawable.aquamon_acc_cape
        ),
        AccessorySlot.BACK to mapOf(
            AccessoryType.WINGS to R.drawable.aquamon_acc_wings,
            AccessoryType.AURA to R.drawable.pyromon_acc_aura
        )
    )

    private val floramonAccessories: Map<AccessorySlot, Map<AccessoryType, Int>> = mapOf(
        AccessorySlot.HEAD to mapOf(
            AccessoryType.HAT to R.drawable.floramon_acc_hat,
            AccessoryType.GLASSES to R.drawable.floramon_acc_glasses
        ),
        AccessorySlot.TORSO to mapOf(
            AccessoryType.SCARF to R.drawable.floramon_acc_scarf,
            AccessoryType.CAPE to R.drawable.floramon_acc_cape
        ),
        AccessorySlot.BACK to mapOf(
            AccessoryType.WINGS to R.drawable.floramon_acc_wings,
            AccessoryType.AURA to R.drawable.pyromon_acc_aura
        )
    )

    private val accessoryDrawables: Map<EduMonType, Map<AccessorySlot, Map<AccessoryType, Int>>> =
        mapOf(
            EduMonType.PYROMON to pyromonAccessories,
            EduMonType.AQUAMON to aquamonAccessories,
            EduMonType.FLORAMON to floramonAccessories
        )

    @DrawableRes
    fun getBaseDrawable(type: EduMonType): Int = when (type) {
        EduMonType.PYROMON -> R.drawable.edumon
        EduMonType.AQUAMON -> R.drawable.edumon2
        EduMonType.FLORAMON -> R.drawable.edumon1
    }

    @DrawableRes
    fun getAccessoryDrawable(
        eduMonType: EduMonType,
        slot: AccessorySlot,
        accessoryType: AccessoryType
    ): Int {
        return accessoryDrawables[eduMonType]?.get(slot)?.get(accessoryType) ?: NO_RESOURCE
    }

    @DrawableRes
    fun getAccessoryDrawable(
        eduMonType: EduMonType,
        slot: AccessorySlot,
        accessoryId: String
    ): Int {
        val accessoryType = AccessoryType.fromId(accessoryId) ?: return NO_RESOURCE
        return getAccessoryDrawable(eduMonType, slot, accessoryType)
    }
}