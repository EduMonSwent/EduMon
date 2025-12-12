package com.android.sample.ui.onBoarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.sample.R

// This code has been written partially using A.I (LLM).
data class StarterDefinition(
    @StringRes val nameRes: Int,
    @DrawableRes val imageRes: Int,
)

val onboardingStarters =
    listOf(
        StarterDefinition(
            nameRes = R.string.onboarding_edumon_fire_name,
            imageRes = R.drawable.edumon,
        ),
        StarterDefinition(
            nameRes = R.string.onboarding_edumon_water_name,
            imageRes = R.drawable.edumon1,
        ),
        StarterDefinition(
            nameRes = R.string.onboarding_edumon_grass_name,
            imageRes = R.drawable.edumon2,
        ),
    )
