package com.android.sample.ui.onBoarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.sample.R
import com.android.sample.ui.theme.EduMonStarterIds

// This code has been written partially using A.I (LLM).
data class StarterDefinition(
    @StringRes val nameRes: Int,
    @DrawableRes val imageRes: Int,
    val id: String,
)

val onboardingStarters =
    listOf(
        StarterDefinition(
            nameRes = R.string.onboarding_edumon_fire_name,
            imageRes = R.drawable.edumon,
            id = EduMonStarterIds.FIRST, // default / violet-blue theme
        ),
        StarterDefinition(
            nameRes = R.string.onboarding_edumon_water_name,
            imageRes = R.drawable.edumon1,
            id = EduMonStarterIds.SECOND, // warm red-orange-yellow theme
        ),
        StarterDefinition(
            nameRes = R.string.onboarding_edumon_grass_name,
            imageRes = R.drawable.edumon2,
            id = EduMonStarterIds.THIRD, // green theme
        ),
    )

// ✅ Small helper so other screens can go from id -> sprite
fun starterDefinitionFor(id: String?): StarterDefinition =
    onboardingStarters.firstOrNull { it.id == id } ?: onboardingStarters.first()
