package com.android.sample.ui.notifications

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.theme.MidDarkCard
import com.android.sample.ui.theme.TextLight
import java.util.Calendar

/* ---------- Helpers testables (augmentent la couverture de ce fichier) ---------- */

@VisibleForTesting
internal fun clampTimeInputs(hh: String, mm: String): Pair<Int, Int> {
  val h = hh.filter(Char::isDigit).take(2).toIntOrNull() ?: 0
  val m = mm.filter(Char::isDigit).take(2).toIntOrNull() ?: 0
  return h.coerceIn(0, 23) to m.coerceIn(0, 59)
}

@VisibleForTesting
internal val DAY_SHORT: Map<Int, String> =
    mapOf(
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat",
        Calendar.SUNDAY to "Sun",
    )

@VisibleForTesting
internal fun formatDayTimeLabel(day: Int, times: Map<Int, Pair<Int, Int>>): String {
  val (h, m) = times[day] ?: (9 to 0)
  val hh = h.coerceIn(0, 23)
  val mm = m.coerceIn(0, 59)
  val d = DAY_SHORT[day] ?: "?"
  return "%s %02d:%02d".format(d, hh, mm)
}

/* ------------------------------------ UI ------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    vm: NotificationsViewModel = viewModel(),
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = {},
    /** Test-only: forcer l’ouverture du dialog pour couvrir ce chemin */
    forceDialogForDay: Int? = null
) {
  val ctx = LocalContext.current

  val kickoffEnabled by vm.kickoffEnabled.collectAsState()
  val kickoffDays by vm.kickoffDays.collectAsState()
  val kickoffTimes by vm.kickoffTimes.collectAsState()
  val streakEnabled by vm.streakEnabled.collectAsState()

  var kickoffPickDay by remember { mutableStateOf<Int?>(null) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Notifications", modifier = Modifier.testTag("notifications_title")) },
            navigationIcon = {
              IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
            },
            actions = { IconButton(onClick = onGoHome) { Icon(Icons.Outlined.Home, null) } })
      }) { padding ->
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0E102A), Color(0xFF171A36))))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

              // === Study kickoff ===
              SectionCard(
                  title = "Study kickoff",
                  subtitle = "Plan your first session at set times",
                  enabled = kickoffEnabled,
                  onToggle = { on -> vm.setKickoffEnabled(ctx, on) }) {
                    DayChipsRow(
                        selected = kickoffDays,
                        onToggle = vm::toggleKickoffDay,
                        enabled = kickoffEnabled)

                    if (kickoffDays.isEmpty()) {
                      Text(
                          if (kickoffEnabled) "Select days to set times"
                          else "Enable to configure schedule",
                          color = TextLight.copy(0.7f))
                    } else {
                      TimeChipsRow(
                          days = kickoffDays,
                          times = kickoffTimes,
                          enabled = kickoffEnabled,
                          onPickRequest = { day -> kickoffPickDay = day })
                      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            enabled = kickoffEnabled, onClick = { vm.applyKickoffSchedule(ctx) }) {
                              Text("Apply kickoff schedule")
                            }
                      }
                    }
                  }

              // === Keep streak (toggle only) ===
              SectionCard(
                  title = "Keep your streak",
                  subtitle = "Daily reminder uses your current streak",
                  enabled = streakEnabled,
                  onToggle = { on -> vm.setStreakEnabled(ctx, on) }) {
                    Text(
                        "We’ll remind you once a day. The message shows your current streak.",
                        color = TextLight.copy(0.75f),
                        style = MaterialTheme.typography.bodySmall)
                  }

              // Test button centered
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(
                    modifier = Modifier.testTag("btn_test_1_min"),
                    onClick = { vm.scheduleTestNotification(ctx) }) {
                      Text("Send notification in 1 min")
                    }
              }
            }
      }

  // Time picker dialog for kickoff (forceDialogForDay pour la couverture de test)
  val openFor = kickoffPickDay ?: forceDialogForDay
  openFor?.let { day ->
    val init = kickoffTimes[day] ?: (9 to 0)
    TimePickerDialog(
        initial = init,
        onConfirm = { h, m ->
          vm.updateKickoffTime(day, h, m)
          kickoffPickDay = null
        },
        onDismiss = { kickoffPickDay = null })
  }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
  Card(
      modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp)),
      colors = CardDefaults.cardColors(containerColor = MidDarkCard),
      shape = RoundedCornerShape(20.dp),
  ) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text(title, color = TextLight, fontWeight = FontWeight.SemiBold)
              Text(
                  subtitle,
                  color = TextLight.copy(0.75f),
                  style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
          }
          content()
        }
  }
}

@Composable
private fun DayChipsRow(selected: Set<Int>, onToggle: (Int) -> Unit, enabled: Boolean) {
  val days =
      listOf(
          Calendar.MONDAY to "Mon",
          Calendar.TUESDAY to "Tue",
          Calendar.WEDNESDAY to "Wed",
          Calendar.THURSDAY to "Thu",
          Calendar.FRIDAY to "Fri",
          Calendar.SATURDAY to "Sat",
          Calendar.SUNDAY to "Sun",
      )
  Row(
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { (day, label) ->
          FilterChip(
              enabled = enabled,
              selected = selected.contains(day),
              onClick = { onToggle(day) },
              label = { Text(label) })
        }
      }
}

@Composable
private fun TimeChipsRow(
    days: Set<Int>,
    times: Map<Int, Pair<Int, Int>>,
    enabled: Boolean,
    onPickRequest: (Int) -> Unit
) {
  Row(
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        days.forEach { day ->
          val label = formatDayTimeLabel(day, times)
          AssistChip(
              enabled = enabled,
              label = { Text(label) },
              onClick = { onPickRequest(day) },
              leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null) })
        }
      }
}

@Composable
private fun TimePickerDialog(
    initial: Pair<Int, Int>,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
  var hh by remember { mutableStateOf(initial.first.coerceIn(0, 23).toString().padStart(2, '0')) }
  var mm by remember { mutableStateOf(initial.second.coerceIn(0, 59).toString().padStart(2, '0')) }

  AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(
            onClick = {
              val (H, M) = clampTimeInputs(hh, mm)
              onConfirm(H, M)
            }) {
              Text("OK")
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
      title = { Text("Pick time") },
      text = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                  value = hh,
                  onValueChange = { hh = it.filter(Char::isDigit).take(2) },
                  label = { Text("HH") },
                  singleLine = true,
                  modifier = Modifier.width(84.dp))
              Text(":", color = TextLight)
              OutlinedTextField(
                  value = mm,
                  onValueChange = { mm = it.filter(Char::isDigit).take(2) },
                  label = { Text("MM") },
                  singleLine = true,
                  modifier = Modifier.width(84.dp))
            }
      },
      containerColor = MidDarkCard)
}
