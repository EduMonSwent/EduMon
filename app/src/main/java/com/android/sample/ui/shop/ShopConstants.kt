// This code was written with the assistance of an AI (LLM).
package com.android.sample.ui.shop

import com.android.sample.R

object ShopConstants {

  const val OWNED_PREFIX = "owned:"

  object ItemIds {
    const val GLASSES = "glasses"
    const val HAT = "hat"
    const val SCARF = "scarf"
    const val WINGS = "wings"
    const val AURA = "aura"
    const val CAPE = "cape"
  }

  object ItemNames {
    const val GLASSES = "Cool Shades"
    const val HAT = "Wizard Hat"
    const val SCARF = "Red Scarf"
    const val WINGS = "Cyber Wings"
    const val AURA = "Epic Aura"
    const val CAPE = "Hero Cape"
  }

  object Prices {
    const val STANDARD = 200
    const val EPIC = 1500
  }

  object DrawableNames {
    const val GLASSES = "shop_cosmetic_glasses"
    const val HAT = "shop_cosmetic_hat"
    const val SCARF = "shop_cosmetic_scarf"
    const val WINGS = "shop_cosmetic_wings"
    const val AURA = "shop_cosmetic_aura"
    const val CAPE = "shop_cosmetic_cape"
  }

  fun defaultCosmetics(): List<CosmeticItem> =
      listOf(
          CosmeticItem(
              ItemIds.GLASSES,
              ItemNames.GLASSES,
              Prices.STANDARD,
              R.drawable.shop_cosmetic_glasses),
          CosmeticItem(ItemIds.HAT, ItemNames.HAT, Prices.STANDARD, R.drawable.shop_cosmetic_hat),
          CosmeticItem(
              ItemIds.SCARF, ItemNames.SCARF, Prices.STANDARD, R.drawable.shop_cosmetic_scarf),
          CosmeticItem(
              ItemIds.WINGS, ItemNames.WINGS, Prices.STANDARD, R.drawable.shop_cosmetic_wings),
          CosmeticItem(ItemIds.AURA, ItemNames.AURA, Prices.EPIC, R.drawable.shop_cosmetic_aura),
          CosmeticItem(
              ItemIds.CAPE, ItemNames.CAPE, Prices.STANDARD, R.drawable.shop_cosmetic_cape))

  fun imageResNameToDrawable(name: String): Int =
      when (name) {
        DrawableNames.GLASSES -> R.drawable.shop_cosmetic_glasses
        DrawableNames.HAT -> R.drawable.shop_cosmetic_hat
        DrawableNames.SCARF -> R.drawable.shop_cosmetic_scarf
        DrawableNames.WINGS -> R.drawable.shop_cosmetic_wings
        DrawableNames.AURA -> R.drawable.shop_cosmetic_aura
        DrawableNames.CAPE -> R.drawable.shop_cosmetic_cape
        else -> R.drawable.shop_cosmetic_glasses
      }

  fun drawableToImageResName(res: Int): String =
      when (res) {
        R.drawable.shop_cosmetic_glasses -> DrawableNames.GLASSES
        R.drawable.shop_cosmetic_hat -> DrawableNames.HAT
        R.drawable.shop_cosmetic_scarf -> DrawableNames.SCARF
        R.drawable.shop_cosmetic_wings -> DrawableNames.WINGS
        R.drawable.shop_cosmetic_aura -> DrawableNames.AURA
        R.drawable.shop_cosmetic_cape -> DrawableNames.CAPE
        else -> DrawableNames.GLASSES
      }
}
