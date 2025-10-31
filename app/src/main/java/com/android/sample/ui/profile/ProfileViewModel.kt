package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.ui.login.UserProfile
import com.android.sample.ui.theme.AccentBlue
import com.android.sample.ui.theme.AccentMagenta
import com.android.sample.ui.theme.AccentMint
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.EventColorSports
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------- Accessories & Accent ----------

enum class AccessorySlot {
  HEAD,
  TORSO,
  LEGS
}

enum class Rarity {
  COMMON,
  RARE,
  EPIC,
  LEGENDARY
}

data class AccessoryItem(
    val id: String,
    val slot: AccessorySlot,
    val label: String,
    val iconRes: Int? = null, // remplace par tes drawables quand tu les auras
    val rarity: Rarity = Rarity.COMMON
)

enum class AccentVariant {
  Base,
  Light,
  Dark,
  Vibrant
}

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

  // ✅ Permet d'utiliser viewModel() sans factory
  constructor() : this(FakeProfileRepository())

  // Palette issue de ton thème
  val accentPalette: List<Color> =
      listOf(AccentViolet, AccentBlue, AccentMint, EventColorSports, AccentMagenta)

  // Catalogue avec rareté + "None" pour chaque slot (déséquiper)
  val accessoryCatalog: List<AccessoryItem> =
      listOf(
          // HEAD
          AccessoryItem("none", AccessorySlot.HEAD, "None"),
          AccessoryItem("halo", AccessorySlot.HEAD, "Halo", rarity = Rarity.EPIC),
          AccessoryItem("crown", AccessorySlot.HEAD, "Crown", rarity = Rarity.RARE),
          AccessoryItem("antenna", AccessorySlot.HEAD, "Antenna", rarity = Rarity.COMMON),
          // TORSO
          AccessoryItem("none", AccessorySlot.TORSO, "None"),
          AccessoryItem("badge", AccessorySlot.TORSO, "Badge", rarity = Rarity.COMMON),
          AccessoryItem("scarf", AccessorySlot.TORSO, "Scarf", rarity = Rarity.RARE),
          AccessoryItem("armor", AccessorySlot.TORSO, "Armor", rarity = Rarity.EPIC),
          // LEGS
          AccessoryItem("none", AccessorySlot.LEGS, "None"),
          AccessoryItem("boots", AccessorySlot.LEGS, "Boots", rarity = Rarity.COMMON),
          AccessoryItem("rocket", AccessorySlot.LEGS, "Rocket", rarity = Rarity.LEGENDARY),
          AccessoryItem("skates", AccessorySlot.LEGS, "Skates", rarity = Rarity.RARE),
      )

  // Profil exposé par le dépôt existant
  val userProfile: StateFlow<UserProfile> = repository.profile

  // Variation non persistée (Firestore plus tard)
  private val accentVariant = MutableStateFlow(AccentVariant.Base)
  val accentVariantFlow: StateFlow<AccentVariant> = accentVariant

  // Couleur d’accent effective = base (profil) + variation
  val accentEffective: StateFlow<Color> =
      combine(userProfile, accentVariantFlow) { user, variant ->
            applyAccentVariant(Color(user.avatarAccent.toInt()), variant)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentViolet)

  // ---------- Intents ----------

  fun setAvatarAccent(color: Color) =
      viewModelScope.launch {
        val cur = userProfile.value
        repository.updateProfile(cur.copy(avatarAccent = color.toArgb().toLong()))
      }

  fun setAccentVariant(variant: AccentVariant) {
    accentVariant.value = variant
  }

  fun toggleNotifications() = update { it.copy(notificationsEnabled = !it.notificationsEnabled) }

  fun toggleLocation() = update { it.copy(locationEnabled = !it.locationEnabled) }

  fun toggleFocusMode() = update { it.copy(focusModeEnabled = !it.focusModeEnabled) }

  fun equip(slot: AccessorySlot, id: String) =
      viewModelScope.launch {
        val cur = userProfile.value
        val prefixesToClean: List<String> =
            when (slot) {
              AccessorySlot.LEGS -> listOf("legs:", "leg:")
              else -> listOf(slot.name.lowercase() + ":")
            }
        val cleaned =
            cur.accessories.filterNot { s -> prefixesToClean.any { p -> s.startsWith(p) } }
        val currentId = equippedId(slot)
        val next =
            when {
              id == "none" -> cleaned
              currentId == id -> cleaned
              else -> cleaned + (slot.name.lowercase() + ":" + id)
            }
        repository.updateProfile(cur.copy(accessories = next))
      }

  fun unequip(slot: AccessorySlot) =
      viewModelScope.launch {
        val cur = userProfile.value
        val prefix = slot.name.lowercase() + ":"
        repository.updateProfile(
            cur.copy(accessories = cur.accessories.filterNot { it.startsWith(prefix) }))
      }

  fun equippedId(slot: AccessorySlot): String? {
    val prefixes: List<String> =
        when (slot) {
          AccessorySlot.LEGS -> listOf("legs:", "leg:")
          else -> listOf(slot.name.lowercase() + ":")
        }
    val entry =
        userProfile.value.accessories.firstOrNull { s -> prefixes.any { p -> s.startsWith(p) } }
            ?: return null
    return entry.substringAfter(':')
  }

  private fun update(edit: (UserProfile) -> UserProfile) =
      viewModelScope.launch { repository.updateProfile(edit(userProfile.value)) }

  // ---------- Color utils ----------
  private fun applyAccentVariant(base: Color, v: AccentVariant): Color =
      when (v) {
        AccentVariant.Base -> base
        AccentVariant.Light -> base.blend(Color.White, 0.25f)
        AccentVariant.Dark -> base.blend(Color.Black, 0.25f)
        AccentVariant.Vibrant -> base.boostSaturation(1.2f).boostValue(1.1f)
      }

  private fun Color.blend(other: Color, amt: Float): Color {
    val t = amt.coerceIn(0f, 1f)
    fun ch(a: Float, b: Float) = a + (b - a) * t
    return Color(
        ch(red, other.red), ch(green, other.green), ch(blue, other.blue), ch(alpha, other.alpha))
  }

  private fun Color.boostSaturation(f: Float): Color {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val v = max
    val s = if (max == 0f) 0f else (max - min) / max
    val newS = (s * f).coerceIn(0f, 1f)
    val scale = if (max == 0f) 1f else (newS / (s.takeIf { it > 0f } ?: 1f))
    val nr = ((red - v) * scale) + v
    val ng = ((green - v) * scale) + v
    val nb = ((blue - v) * scale) + v
    return Color(nr, ng, nb, alpha)
  }

  private fun Color.boostValue(f: Float): Color {
    fun ch(x: Float) = (x * f).coerceIn(0f, 1f)
    return Color(ch(red), ch(green), ch(blue), alpha)
  }
}
