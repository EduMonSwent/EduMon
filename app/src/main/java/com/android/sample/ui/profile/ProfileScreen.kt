// app/src/main/java/com/android/sample/ui/profile/ProfileScreen.kt
package com.android.sample.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.CreatureStats
import com.android.sample.screens.CreatureStatsCard
import com.android.sample.ui.planner.PetHeaderConnected
import kotlin.math.roundToInt

/**
 * Profile screen wired only to ProfileViewModel public API.
 * No repository calls from UI, emit intents to the ViewModel.
 */
@Composable
fun ProfileScreen(
    vm: ProfileViewModel = viewModel()
) {
    val profile by vm.userProfile.collectAsStateWithLifecycle()
    val pet by vm.petState.collectAsStateWithLifecycle()
    val accent by vm.accentEffective.collectAsStateWithLifecycle()
    val variant by vm.accentVariantFlow.collectAsStateWithLifecycle()
    val palette: List<Color> = vm.accentPalette
    val catalog by vm.accessoryCatalog.collectAsStateWithLifecycle()
    val ownedAuras by vm.ownedAuras.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header pet preview and stats
            ProfilePetSection()

            Text("Profile", style = MaterialTheme.typography.headlineSmall)
            Text("Coins: ${profile.coins}")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.addCoins(500) }) { Text("Add 500 coins") }
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Location")
                Switch(
                    checked = profile.locationEnabled,
                    onCheckedChange = { vm.toggleLocation() }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Focus mode")
                Switch(
                    checked = profile.focusModeEnabled,
                    onCheckedChange = { vm.toggleFocusMode() }
                )
            }

            Divider()

            // Accent color palette
            Text("Avatar accent")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { c ->
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .weight(1f)
                            .background(c)
                            .clickable { vm.setAvatarAccent(c) }
                    )
                }
            }

            // Accent visual variant
            Text("Accent variant")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = variant == AccentVariant.Base, onClick = { vm.setAccentVariant(AccentVariant.Base) }, label = { Text("Base") })
                FilterChip(selected = variant == AccentVariant.Light, onClick = { vm.setAccentVariant(AccentVariant.Light) }, label = { Text("Light") })
                FilterChip(selected = variant == AccentVariant.Dark, onClick = { vm.setAccentVariant(AccentVariant.Dark) }, label = { Text("Dark") })
                FilterChip(selected = variant == AccentVariant.Vibrant, onClick = { vm.setAccentVariant(AccentVariant.Vibrant) }, label = { Text("Vibrant") })
            }

            Divider()

            // Aura section
            AuraSection(vm = vm, ownedAuras = ownedAuras)

            Divider()

            // Equipped summary
            Text("Equipped")
            Row { Text("Head: ${vm.equippedId(AccessorySlot.HEAD) ?: "none"}") }
            Row { Text("Torso: ${vm.equippedId(AccessorySlot.TORSO) ?: "none"}") }
            Row { Text("Legs: ${vm.equippedId(AccessorySlot.LEGS) ?: "none"}") }

            Divider()

            // Accessories list (only owned, plus one None per slot)
            Text("Accessories")
            AccessoriesList(vm = vm, items = catalog.filter { it.id == "none" || vm.isOwned(it.id) })
        }
    }
}

/** Pet header plus compact stats card. */
@Composable
private fun ProfilePetSection() {
    PetHeaderConnected()

    val pet = androidx.lifecycle.compose.collectAsStateWithLifecycle(AppRepositories.petRepository.state).value
    val stats = CreatureStats(
        level = 1,
        energy = (pet.energy * 100).roundToInt().coerceIn(0, 100),
        happiness = (pet.happiness * 100).roundToInt().coerceIn(0, 100),
        health = (pet.growth * 100).roundToInt().coerceIn(0, 100)
    )
    Spacer(Modifier.height(8.dp))
    CreatureStatsCard(stats = stats)
}

/** List of accessories with Equip and Unequip actions. */
@Composable
private fun AccessoriesList(
    vm: ProfileViewModel,
    items: List<AccessoryItem>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        items(items) { item ->
            val equipped = when (item.slot) {
                AccessorySlot.HEAD -> vm.equippedId(AccessorySlot.HEAD) == item.id
                AccessorySlot.TORSO -> vm.equippedId(AccessorySlot.TORSO) == item.id
                AccessorySlot.LEGS -> vm.equippedId(AccessorySlot.LEGS) == item.id
            }
            AccessoryRow(
                item = item,
                equipped = equipped,
                onEquip = { vm.equip(item.slot, item.id) },
                onUnequip = { vm.unequip(item.slot) }
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** Simple row for one accessory entry. */
@Composable
private fun AccessoryRow(
    item: AccessoryItem,
    equipped: Boolean,
    onEquip: () -> Unit,
    onUnequip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.label, style = MaterialTheme.typography.titleMedium)
            Text(item.slot.name.lowercase(), color = MaterialTheme.colorScheme.outline)
        }
        if (equipped) {
            Button(onClick = onUnequip) { Text("Unequip") }
        } else {
            Button(onClick = onEquip) { Text("Equip") }
        }
    }
}

/** Aura chips, only for owned auras, with a None option to clear. */
@Composable
private fun AuraSection(
    vm: ProfileViewModel,
    ownedAuras: List<String>
) {
    val current = vm.currentAuraId()

    Text("Auras", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = current == null,
            onClick = { vm.equipAura(null) },
            label = { Text("None") }
        )
        ownedAuras.forEach { auraId ->
            FilterChip(
                selected = current == auraId,
                onClick = { vm.equipAura(auraId) },
                label = { Text(auraId.replace('_', ' ')) }
            )
        }
    }
}
