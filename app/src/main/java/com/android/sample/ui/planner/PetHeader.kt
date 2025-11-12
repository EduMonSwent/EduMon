// Parts of this file were generated with the help of an AI assistant.

package com.android.sample.ui.planner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.sample.R
import com.android.sample.repos_providors.AppRepositories

/**
 * Connected header. Reads PetState StateFlow from the repository and renders bars and sprite.
 * UI stays the same as the original version.
 */
@Composable
fun PetHeaderConnected(modifier: Modifier = Modifier) {
    val pet by AppRepositories.petRepository.state.collectAsStateWithLifecycle()
    PetHeader(
        energy = pet.energy,
        happiness = pet.happiness,
        growth = pet.growth,
        modifier = modifier
    )
}

/** Pure UI, used by the connected version and by previews. */
@Composable
fun PetHeader(
    energy: Float,
    happiness: Float,
    growth: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.home),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(id = R.drawable.edumon),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                contentScale = ContentScale.Fit
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBar("Energy", energy)
            StatBar("Happiness", happiness)
            StatBar("Growth", growth)
        }
    }
}

@Composable
private fun StatBar(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(10.dp)
                .fillMaxWidth(0.25f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val pct = value.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .fillMaxWidth(pct)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
