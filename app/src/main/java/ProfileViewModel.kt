import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.Rarity
import com.android.sample.data.UserProfile
import com.android.sample.pet.model.PetState
import com.android.sample.profile.ProfileRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.data.shop.ShopRepository
import com.android.sample.data.shop.ShopRepositoryProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single source of truth for Profile.
 * Reads Pet state from PetRepository, profile from ProfileRepository,
 * owned cosmetics from ShopRepository. All mutations go through ProfileRepository.
 */
class ProfileViewModel(
    private val repository: ProfileRepository = AppRepositories.profileRepository,
    private val shopRepo: ShopRepository = ShopRepositoryProvider.repository
) : ViewModel() {

    // Pet state shared across screens
    val petState: StateFlow<PetState> = AppRepositories.petRepository.state

    // Editable mirror of profile with optimistic UI
    private val _userProfile = MutableStateFlow(repository.profile.value.copy())
    val userProfile: StateFlow<UserProfile> = _userProfile

    // Owned cosmetic ids coming from Firestore (shop)
    val ownedIds: StateFlow<Set<String>> =
        shopRepo.observeOwnedItemIds(Firebase.auth.currentUser?.uid.orEmpty())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Accent palette
    val accentPalette: List<Color> =
        listOf(
            Color(0xFF7C4DFF),
            Color(0xFF2962FF),
            Color(0xFF00C853),
            Color(0xFFFF6D00),
            Color(0xFFD500F9)
        )

    // Local visual variant for preview in UI
    private val accentVariant = MutableStateFlow(AccentVariant.Base)
    val accentVariantFlow: StateFlow<AccentVariant> = accentVariant

    // Effective accent color
    val accentEffective: StateFlow<Color> =
        combine(_userProfile, accentVariant) { user, variant ->
            // avatarAccent is stored as ARGB long in the profile
            applyAccentVariant(Color(user.avatarAccent.toInt()), variant)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Color(0xFF7C4DFF))

    // Base catalog visible in profile
    // Base catalog with named args so we do not collide with the Int? param
    private val baseCatalog: List<AccessoryItem> =
        listOf(
            // head
            AccessoryItem(id = "none",   slot = AccessorySlot.HEAD, label = "None",   iconRes = null, rarity = Rarity.COMMON),
            AccessoryItem(id = "halo",   slot = AccessorySlot.HEAD, label = "Halo",   iconRes = null, rarity = Rarity.EPIC),
            AccessoryItem(id = "crown",  slot = AccessorySlot.HEAD, label = "Crown",  iconRes = null, rarity = Rarity.RARE),
            AccessoryItem(id = "antenna",slot = AccessorySlot.HEAD, label = "Antenna",iconRes = null, rarity = Rarity.COMMON),

            // torso
            AccessoryItem(id = "none",   slot = AccessorySlot.TORSO, label = "None",  iconRes = null, rarity = Rarity.COMMON),
            AccessoryItem(id = "badge",  slot = AccessorySlot.TORSO, label = "Badge", iconRes = null, rarity = Rarity.COMMON),
            AccessoryItem(id = "scarf",  slot = AccessorySlot.TORSO, label = "Scarf", iconRes = null, rarity = Rarity.RARE),
            AccessoryItem(id = "armor",  slot = AccessorySlot.TORSO, label = "Armor", iconRes = null, rarity = Rarity.EPIC),

            // legs
            AccessoryItem(id = "none",   slot = AccessorySlot.LEGS, label = "None",   iconRes = null, rarity = Rarity.COMMON),
            AccessoryItem(id = "boots",  slot = AccessorySlot.LEGS, label = "Boots",  iconRes = null, rarity = Rarity.COMMON),
            AccessoryItem(id = "rocket", slot = AccessorySlot.LEGS, label = "Rocket", iconRes = null, rarity = Rarity.LEGENDARY),
            AccessoryItem(id = "skates", slot = AccessorySlot.LEGS, label = "Skates", iconRes = null, rarity = Rarity.RARE),
        )


    /**
     * Final catalog presented to the screen.
     * Merges the base catalog with any extra items the user owns from the shop.
     */
    val accessoryCatalog: StateFlow<List<AccessoryItem>> =
        ownedIds.map { owned ->
            val knownIds = baseCatalog.map { it.id }.toSet()
            val extra = owned.filter { it !in knownIds }.map { id ->
                // Cheap slot inference by id
                val slot =
                    when {
                        id.contains("scarf", true) || id.contains("badge", true) || id.contains("armor", true) -> AccessorySlot.TORSO
                        id.contains("boots", true) || id.contains("skate", true) || id.contains("leg", true) -> AccessorySlot.LEGS
                        else -> AccessorySlot.HEAD
                    }
                AccessoryItem(id, slot, id.replaceFirstChar { it.uppercase() }, iconRes = null, Rarity.COMMON)
            }
            baseCatalog + extra
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), baseCatalog)

    // -------- intents

    fun setAvatarAccent(color: Color) {
        val argb: Long = color.value.toLong()
        _userProfile.update { it.copy(avatarAccent = argb) }
        pushProfile()
    }

    fun setAccentVariant(variant: AccentVariant) {
        accentVariant.value = variant
    }

    fun toggleLocation() = updateLocal { it.copy(locationEnabled = !it.locationEnabled) }

    fun toggleFocusMode() = updateLocal { it.copy(focusModeEnabled = !it.focusModeEnabled) }

    fun addCoins(amount: Int) = updateLocal { it.copy(coins = it.coins + amount) }

    /** Equip an item for a given slot. Accessories are stored as "slot:id". */
    fun equip(slot: AccessorySlot, id: String) {
        val cur = _userProfile.value
        val prefixesToClean =
            if (slot == AccessorySlot.LEGS) listOf("legs:", "leg:")
            else listOf(slot.name.lowercase() + ":")
        val cleaned = cur.accessories.filterNot { s -> prefixesToClean.any { p -> s.startsWith(p) } }
        val next = if (id == "none") cleaned else cleaned + (slot.name.lowercase() + ":" + id)
        val updated = cur.copy(accessories = next)
        _userProfile.value = updated
        pushProfile(updated)
    }

    fun unequip(slot: AccessorySlot) {
        val cur = _userProfile.value
        val prefix = slot.name.lowercase() + ":"
        val updated = cur.copy(accessories = cur.accessories.filterNot { it.startsWith(prefix) })
        _userProfile.value = updated
        pushProfile(updated)
    }

    fun equippedId(slot: AccessorySlot): String? {
        val prefixes =
            if (slot == AccessorySlot.LEGS) listOf("legs:", "leg:")
            else listOf(slot.name.lowercase() + ":")
        val s = _userProfile.value.accessories.firstOrNull { a -> prefixes.any { p -> a.startsWith(p) } }
        return s?.substringAfter(':')
    }

    // -------- internals

    private fun updateLocal(block: (UserProfile) -> UserProfile) {
        val updated = block(_userProfile.value)
        _userProfile.value = updated
        pushProfile(updated)
    }

    private fun pushProfile(updated: UserProfile = _userProfile.value) {
        viewModelScope.launch { runCatching { repository.updateProfile(updated) } }
    }

    // Color helpers

    /** Apply a local visual variant to the base accent. */
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
            ch(this.red, other.red),
            ch(this.green, other.green),
            ch(this.blue, other.blue),
            ch(this.alpha, other.alpha)
        )
    }

    private fun Color.boostSaturation(f: Float): Color {
        val r = this.red
        val g = this.green
        val b = this.blue
        val p = kotlin.math.sqrt((r * r * 0.299f) + (g * g * 0.587f) + (b * b * 0.114f))
        fun mix(c: Float) = p + (c - p) * f
        return Color(mix(r), mix(g), mix(b), this.alpha)
    }

    private fun Color.boostValue(f: Float): Color =
        Color(
            (this.red * f).coerceIn(0f, 1f),
            (this.green * f).coerceIn(0f, 1f),
            (this.blue * f).coerceIn(0f, 1f),
            this.alpha
        )
}
