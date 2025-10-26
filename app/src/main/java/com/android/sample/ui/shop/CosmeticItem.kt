package com.android.sample.ui.shop

/** Data class representing a cosmetic item in the EduMon shop. */
data class CosmeticItem(
    val id: String,
    val name: String,
    val price: Int,
    val imageRes: Int,
    val owned: Boolean = false
)
