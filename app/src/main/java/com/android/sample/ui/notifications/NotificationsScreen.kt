package com.android.sample.ui.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.ui.theme.MidDarkCard
import com.android.sample.ui.theme.TextLight
import java.util.Calendar

// Parts of this code were written with ChatGPT assistance

/* ---------- Helpers testables ---------- */

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

/* ------------ NEW SMALL DEDUPLICATION HELPER (minimal change) ------------ */

@Composable
private fun localizedDayName(day: Int): String =
    when (day) {
      Calendar.MONDAY -> stringResource(R.string.day_short_mon)
      Calendar.TUESDAY -> stringResource(R.string.day_short_tue)
      Calendar.WEDNESDAY -> stringResource(R.string.day_short_wed)
      Calendar.THURSDAY -> stringResource(R.string.day_short_thu)
      Calendar.FRIDAY -> stringResource(R.string.day_short_fri)
      Calendar.SATURDAY -> stringResource(R.string.day_short_sat)
      Calendar.SUNDAY -> stringResource(R.string.day_short_sun)
      else -> stringResource(R.string.unknown)
    }

/* ------------------------------------------------------------------------- */

@Composable
internal fun formatDayTimeLabelLocalized(day: Int, times: Map<Int, Pair<Int, Int>>): String {
  val (h, m) = times[day] ?: (9 to 0)
  return "%s %02d:%02d".format(localizedDayName(day), h.coerceIn(0, 23), m.coerceIn(0, 59))
}

/* -------------------------- Sub-composables (extraits) -------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderBar(onBack: () -> Unit, onGoHome: () -> Unit) {
  TopAppBar(
      title = {
        Text(
            stringResource(id = R.string.notifications_title),
            modifier = Modifier.testTag("notifications_title"))
      },
      navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
      },
      actions = { IconButton(onClick = onGoHome) { Icon(Icons.Outlined.Home, null) } })
}

@Composable
private fun StartupErrorBanner(startupError: String?) {
  startupError?.let { msg ->
    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
      Text(stringResource(R.string.notification_setup_error_fmt, msg), color = Color.Red)
    }
  }
}

@Composable
private fun KickoffSection(
    kickoffEnabled: Boolean,
    kickoffDays: Set<Int>,
    kickoffTimes: Map<Int, Pair<Int, Int>>,
    onToggleKickoff: (Boolean) -> Unit,
    onToggleDay: (Int) -> Unit,
    onPickRequest: (Int) -> Unit,
    onApply: () -> Unit
) {
  SectionCard(
      title = stringResource(R.string.study_kickoff_title),
      subtitle = stringResource(R.string.study_kickoff_subtitle),
      enabled = kickoffEnabled,
      onToggle = onToggleKickoff) {
        DayChipsRow(selected = kickoffDays, onToggle = onToggleDay, enabled = kickoffEnabled)
        if (kickoffDays.isEmpty()) {
          Text(
              if (kickoffEnabled) stringResource(R.string.select_days_set_times)
              else stringResource(R.string.enable_to_configure_schedule),
              color = TextLight.copy(0.7f))
        }
        if (kickoffDays.isNotEmpty()) {
          TimeChipsRow(
              days = kickoffDays,
              times = kickoffTimes,
              enabled = kickoffEnabled,
              onPickRequest = onPickRequest)
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(enabled = kickoffEnabled, onClick = onApply) {
              Text(stringResource(R.string.apply_kickoff_schedule))
            }
          }
        }
      }
}

@Composable
private fun StreakSection(streakEnabled: Boolean, onToggle: (Boolean) -> Unit) {
  SectionCard(
      title = stringResource(R.string.keep_streak_title),
      subtitle = stringResource(R.string.keep_streak_subtitle),
      enabled = streakEnabled,
      onToggle = onToggle) {
        Text(
            stringResource(R.string.keep_streak_desc),
            color = TextLight.copy(0.75f),
            style = MaterialTheme.typography.bodySmall)
      }
}

@Composable
private fun TaskNotificationsSection(taskEnabled: Boolean, onToggle: (Boolean) -> Unit) {
  SectionCard(
      title = stringResource(R.string.task_notifications_title),
      subtitle = stringResource(R.string.task_notifications_subtitle),
      enabled = taskEnabled,
      onToggle = onToggle) {
        Text(
            stringResource(R.string.task_notifications_desc),
            color = TextLight.copy(0.75f),
            style = MaterialTheme.typography.bodySmall)
      }
}

@Composable
private fun CampusEntrySection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    vm: NotificationsUiModel,
    ctx: android.content.Context,
    backgroundLocationLauncher:
        androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>
) {
  var showBackgroundLocationDialog by remember { mutableStateOf(false) }
  var pendingEnableRequest by remember { mutableStateOf(false) }

  // Check if we need background location permission
  val needsBackgroundLocation = remember(enabled) { vm.needsBackgroundLocationPermission(ctx) }

  SectionCard(
      title = stringResource(R.string.campus_entry_toggle_title),
      subtitle = stringResource(R.string.campus_entry_toggle_subtitle),
      enabled = enabled,
      onToggle = { on ->
        if (on && vm.needsBackgroundLocationPermission(ctx)) {
          // Show educational dialog before requesting permission
          pendingEnableRequest = true
          showBackgroundLocationDialog = true
        } else {
          onToggle(on)
        }
      },
      switchTag = "campus_entry_switch") {
        if (enabled && needsBackgroundLocation) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = stringResource(R.string.campus_background_location_needed),
              color = MaterialTheme.colorScheme.error.copy(0.9f),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold)
          TextButton(
              onClick = {
                showBackgroundLocationDialog = true
                pendingEnableRequest = false
              }) {
                Text(stringResource(R.string.grant_permission))
              }
        }
      }

  // Educational dialog for background location permission
  if (showBackgroundLocationDialog) {
    AlertDialog(
        onDismissRequest = {
          showBackgroundLocationDialog = false
          pendingEnableRequest = false
        },
        title = { Text(stringResource(R.string.background_location_dialog_title)) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.background_location_dialog_text))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  stringResource(R.string.background_location_dialog_instruction),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold)
            }
          }
        },
        confirmButton = {
          TextButton(
              onClick = {
                showBackgroundLocationDialog = false
                vm.requestBackgroundLocationIfNeeded(ctx) { permission ->
                  backgroundLocationLauncher.launch(permission)
                }
              }) {
                Text(stringResource(R.string.grant_permission))
              }
        },
        dismissButton = {
          TextButton(
              onClick = {
                showBackgroundLocationDialog = false
                if (pendingEnableRequest) {
                  // User declined permission, don't enable the feature
                  pendingEnableRequest = false
                }
              }) {
                Text(stringResource(R.string.cancel))
              }
        })
  }

  // Handle permission result
  LaunchedEffect(backgroundLocationLauncher) {
    // This will be triggered when permission result comes back
    // The actual handling is done through the launcher callback
  }

  // If we had a pending enable request and permission is now granted, enable the feature
  LaunchedEffect(pendingEnableRequest) {
    if (pendingEnableRequest && vm.hasBackgroundLocationPermission(ctx)) {
      pendingEnableRequest = false
      onToggle(true)
    }
  }
}

@Composable
private fun TestNotificationButton(
    vm: NotificationsUiModel,
    ctx: android.content.Context,
    launcherTest: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>
) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    Button(
        modifier = Modifier.testTag("btn_test_1_min"),
        onClick = { vm.requestOrSchedule(ctx) { permission -> launcherTest.launch(permission) } }) {
          Text(stringResource(R.string.send_notification_1_min))
        }
  }
}

@Composable
private fun DeepLinkDemoButton(
    vm: NotificationsUiModel,
    ctx: android.content.Context,
    launcherDemo: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>
) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    val needs = vm.needsNotificationPermission(ctx)
    Button(
        modifier = Modifier.testTag("btn_demo_deep_link"),
        onClick = {
          if (needs && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcherDemo.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            vm.sendDeepLinkDemoNotification(ctx)
          }
        }) {
          Text(stringResource(R.string.send_deep_link_demo))
        }
  }
}

@Composable
private fun StartScheduleObserver(
    taskNotificationsEnabled: Boolean,
    vm: NotificationsUiModel,
    ctx: android.content.Context,
    onError: (String?) -> Unit
) {
  LaunchedEffect(taskNotificationsEnabled) {
    if (!taskNotificationsEnabled) {
      onError(null)
      return@LaunchedEffect
    }
    runCatching { vm.startObservingSchedule(ctx) }
        .onSuccess { onError(null) }
        .onFailure {
          android.util.Log.e("NotificationsScreen", "Failed", it)
          onError(it.message ?: "Failed to start schedule observer")
        }
  }
}

/* ------------------------------------ UI ------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    vm: NotificationsUiModel = viewModel<NotificationsViewModel>(),
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = {},
    forceDialogForDay: Int? = null
) {
  val ctx = LocalContext.current

  val launcherTest =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.scheduleTestNotification(ctx)
      }
  val launcherDemo =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.sendDeepLinkDemoNotification(ctx)
      }

  // Background location permission launcher for campus entry feature
  val launcherBackgroundLocation =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && vm is NotificationsViewModel) {
          // Permission granted, enable campus notifications
          vm.setCampusEntryEnabled(ctx, true)
        }
      }

  val kickoffEnabled by vm.kickoffEnabled.collectAsState()
  val kickoffDays by vm.kickoffDays.collectAsState()
  val kickoffTimes by vm.kickoffTimes.collectAsState()
  val taskNotificationsEnabled by vm.taskNotificationsEnabled.collectAsState()
  val streakEnabled by vm.streakEnabled.collectAsState()
  val campusEntryEnabled by vm.campusEntryEnabled.collectAsState() // removed cast

  var kickoffPickDay by remember { mutableStateOf<Int?>(null) }
  var startupError by remember { mutableStateOf<String?>(null) }

  StartScheduleObserver(taskNotificationsEnabled, vm, ctx) { startupError = it }

  Scaffold(topBar = { HeaderBar(onBack, onGoHome) }) { padding ->
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E102A), Color(0xFF171A36))))
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
          StartupErrorBanner(startupError)

          KickoffSection(
              kickoffEnabled = kickoffEnabled,
              kickoffDays = kickoffDays,
              kickoffTimes = kickoffTimes,
              onToggleKickoff = { on -> vm.setKickoffEnabled(ctx, on) },
              onToggleDay = vm::toggleKickoffDay,
              onPickRequest = { day -> kickoffPickDay = day },
              onApply = { vm.applyKickoffSchedule(ctx) })

          StreakSection(
              streakEnabled = streakEnabled, onToggle = { on -> vm.setStreakEnabled(ctx, on) })

          TaskNotificationsSection(
              taskEnabled = taskNotificationsEnabled,
              onToggle = { on -> vm.setTaskNotificationsEnabled(ctx, on) })

          CampusEntrySection(
              enabled = campusEntryEnabled,
              onToggle = { on -> vm.setCampusEntryEnabled(ctx, on) },
              vm = vm,
              ctx = ctx,
              backgroundLocationLauncher = launcherBackgroundLocation)

          TestNotificationButton(vm, ctx, launcherTest)
          DeepLinkDemoButton(vm, ctx, launcherDemo)
        }
  }

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
    switchTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
  Card(
      modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp)),
      colors = CardDefaults.cardColors(containerColor = MidDarkCard),
      shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text(title, color = TextLight, fontWeight = FontWeight.SemiBold)
                  Text(
                      subtitle,
                      color = TextLight.copy(0.75f),
                      style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    modifier = if (switchTag != null) Modifier.testTag(switchTag) else Modifier,
                    checked = enabled,
                    onCheckedChange = onToggle)
              }
              content()
            }
      }
}

@Composable
private fun DayChipsRow(selected: Set<Int>, onToggle: (Int) -> Unit, enabled: Boolean) {
  val days =
      listOf(
          Calendar.MONDAY,
          Calendar.TUESDAY,
          Calendar.WEDNESDAY,
          Calendar.THURSDAY,
          Calendar.FRIDAY,
          Calendar.SATURDAY,
          Calendar.SUNDAY)

  Row(
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
          FilterChip(
              enabled = enabled,
              selected = selected.contains(day),
              onClick = { onToggle(day) },
              label = { Text(localizedDayName(day)) })
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
          AssistChip(
              enabled = enabled,
              label = { Text(formatDayTimeLabelLocalized(day, times)) },
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
              Text(stringResource(R.string.ok))
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
      title = { Text(stringResource(R.string.pick_time)) },
      text = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                  value = hh,
                  onValueChange = { hh = it.filter(Char::isDigit).take(2) },
                  label = { Text(stringResource(R.string.hh_label)) },
                  singleLine = true,
                  modifier = Modifier.width(84.dp))
              Text(stringResource(R.string.time_separator), color = TextLight)
              OutlinedTextField(
                  value = mm,
                  onValueChange = { mm = it.filter(Char::isDigit).take(2) },
                  label = { Text(stringResource(R.string.mm_label)) },
                  singleLine = true,
                  modifier = Modifier.width(84.dp))
            }
      },
      containerColor = MidDarkCard)
}
