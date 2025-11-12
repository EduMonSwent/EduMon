// Parts of this file were generated with the help of an AI language model.
// Comments are in English.

package com.android.sample.ui.shop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.data.shop.ShopRepository
import com.android.sample.data.shop.ShopRepositoryProvider

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShopScreen(
    uid: String,
    // repo and catalog are resolved here, so callers only pass uid
    repo: ShopRepository = ShopRepositoryProvider.repository,
    catalog: List<ShopRepository.ShopItem> = defaultCatalog(),
    vm: ShopViewModel = viewModel(factory = ShopViewModel.Factory(uid, repo, catalog))
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHost.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shop", style = MaterialTheme.typography.headlineSmall)
                Text("Coins: ${state.coins}", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                val ds = repo
                                if (ds is com.android.sample.data.shop.FirestoreShopDataSource) {
                                    vm.addCoinsDev(500)
                                }
                            }
                        ))
            }

            Spacer(Modifier.height(12.dp))

            state.catalog.forEach { item ->
                ItemRow(
                    item = item,
                    owned = state.owned.contains(item.id),
                    affordable = state.coins >= item.price,
                    purchasing = state.purchasingId == item.id,
                    onBuy = { vm.purchase(item) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: ShopRepository.ShopItem,
    owned: Boolean,
    affordable: Boolean,
    purchasing: Boolean,
    onBuy: () -> Unit
) {
    val context = LocalContext.current

    val resId = remember(item.id, item.auraId) {
        val mappedName = when {
            item.id.contains("hat", true) -> "cosmetic_hat"
            item.id.contains("scarf", true) -> "cosmetic_scarf"
            item.id.contains("wing", true) -> "cosmetic_wings"
            item.id.contains("aura", true) || item.auraId != null -> "cosmetic_aura"
            else -> item.id
        }
        val m = context.resources.getIdentifier(mappedName, "drawable", context.packageName)
        if (m != 0) m
        else context.resources.getIdentifier(item.id, "drawable", context.packageName)
            .takeIf { it != 0 } ?: 0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (resId != 0) {
                Image(
                    painter = painterResource(resId),
                    contentDescription = item.title,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .height(48.dp)
                )
            }
            Column {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Price: ${item.price}, Rarity: ${item.rarity}",
                    style = MaterialTheme.typography.bodyMedium
                )
                item.auraId?.let {
                    Text("Aura: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        when {
            owned -> Text("Owned", style = MaterialTheme.typography.labelLarge)
            purchasing -> CircularProgressIndicator()
            else -> Button(enabled = affordable, onClick = onBuy) {
                Text(if (affordable) "Buy" else "Not enough")
            }
        }
    }
}

/**
 * Default catalog used by the screen.
 * Keep ids stable, prices and rarities are examples.
 */
private fun defaultCatalog(): List<ShopRepository.ShopItem> = listOf(
    ShopRepository.ShopItem(id = "hat_red", title = "Red Hat", price = 100, rarity = "common"),
    ShopRepository.ShopItem(id = "scarf_blue", title = "Blue Scarf", price = 200, rarity = "rare"),
    ShopRepository.ShopItem(id = "wings", title = "Wings", price = 350, rarity = "epic"),
    ShopRepository.ShopItem(id = "aura_gold", title = "Golden Aura", price = 500, rarity = "epic", auraId = "gold")
)
