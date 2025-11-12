package com.android.sample.ui.shop.model

/**
 * This code has been written partially using A.I (LLM).
 *
 * UI model for items sold in the shop.
 */
data class CosmeticItem(
    val id: String,
    val name: String,
    val price: Int,
    val imageRes: Int,
    val owned: Boolean = false
)
