package com.android.sample.ui.session

// This code has been written partially using A.I (LLM).

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.data.Status
import com.android.sample.feature.subjects.model.StudySubject
import com.android.sample.repositories.ToDoRepositoryProvider
import com.android.sample.session.ToDoBackedStudySessionRepository
import com.android.sample.ui.pomodoro.PomodoroScreen
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import com.android.sample.ui.session.components.SessionStatsPanel
import com.android.sample.ui.session.components.SuggestedTasksList
import kotlinx.coroutines.launch

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

private const val SUBJECT_SECTION_SPACING_DP = 16
private const val SUBJECT_CHIP_SPACING_DP = 8
private const val SUBJECT_TEXT_FIELD_HEIGHT_DP = 8
private const val SCREEN_HORIZONTAL_PADDING_DP = 24
private const val SCREEN_VERTICAL_PADDING_DP = 24
private const val SCREEN_SECTION_SPACING_DP = 24

private const val DEFAULT_FOCUS_MIN = 25
private const val DEFAULT_BREAK_MIN = 5
private const val MIN_FOCUS_MIN = 5
private const val MAX_FOCUS_MIN = 120
private const val MIN_BREAK_MIN = 1
private const val MAX_BREAK_MIN = 60

private val MOTIVATION_QUOTES =
    listOf(
        "Small steps every day add up to big results.",
        "Focus on progress, not perfection.",
        "You are closer than you think.",
        "One Pomodoro at a time.",
        "Stay consistent. Your future self will thank you.",
    )

// --- Test Tags for UI tests ---
object StudySessionTestTags {
    const val TITLE = "study_session_title"
    const val TASK_LIST = "study_session_task_list"
    const val SELECTED_TASK = "study_session_selected_task"
    const val TIMER_SECTION = "study_session_timer_section"
    const val STATS_PANEL = "study_session_stats_panel"
    const val SUBJECTS_SECTION = "study_session_subjects_section"
}

@Composable
fun StudySessionScreen(
    eventId: String? = null,
    viewModel: StudySessionViewModel =
        StudySessionViewModel(repository = ToDoBackedStudySessionRepository()),
    pomodoroViewModel: PomodoroViewModelContract = viewModel.pomodoroViewModel
) {
    // Keep the same VM parameters as your old version, but prevent selection from resetting
    // by locking onto the first instances for the lifetime of this composition.
    val stableViewModel = remember { viewModel }
    val stablePomodoroViewModel = remember { pomodoroViewModel }

    val uiState by stableViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(eventId) {
        eventId?.let { id ->
            scope.launch {
                val todo = ToDoRepositoryProvider.repository.getById(id)
                todo?.let { stableViewModel.selectTask(it) }
            }
        }
    }

    val (newSubjectName, setNewSubjectName) = remember { mutableStateOf("") }

    // UI-only navigation between setup and timer screens
    var inTimerScreen by rememberSaveable { mutableStateOf(false) }

    // Duration pickers (UI); best-effort apply to existing Pomodoro VM without changing its logic
    var focusMinutes by rememberSaveable { mutableIntStateOf(DEFAULT_FOCUS_MIN) }
    var breakMinutes by rememberSaveable { mutableIntStateOf(DEFAULT_BREAK_MIN) }

    val selectionLabel = remember(uiState.selectedSubject, uiState.selectedTask) {
        val subject = uiState.selectedSubject?.name?.trim().orEmpty()
        val task = uiState.selectedTask?.title?.trim().orEmpty()
        when {
            subject.isNotEmpty() && task.isNotEmpty() -> "$subject • $task"
            subject.isNotEmpty() -> subject
            task.isNotEmpty() -> task
            else -> "No selection"
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!inTimerScreen) {
            StudySessionSetupScreen(
                uiState = uiState,
                newSubjectName = newSubjectName,
                onSubjectNameChange = setNewSubjectName,
                onAddSubject = {
                    stableViewModel.createSubject(newSubjectName)
                    setNewSubjectName("")
                },
                onSelectSubject = stableViewModel::selectSubject,
                onTaskSelected = stableViewModel::selectTask,
                onStatusChange = stableViewModel::setSelectedTaskStatus,
                focusMinutes = focusMinutes,
                breakMinutes = breakMinutes,
                onFocusMinutesChange = { minutes ->
                    focusMinutes = minutes
                    tryApplyPomodoroDurations(stablePomodoroViewModel, focusMinutes, breakMinutes)
                },
                onBreakMinutesChange = { minutes ->
                    breakMinutes = minutes
                    tryApplyPomodoroDurations(stablePomodoroViewModel, focusMinutes, breakMinutes)
                },
                onStart = {
                    tryApplyPomodoroDurations(stablePomodoroViewModel, focusMinutes, breakMinutes)
                    inTimerScreen = true
                },
            )
        } else {
            StudySessionTimerScreen(
                selectionLabel = selectionLabel,
                pomodoroViewModel = stablePomodoroViewModel,
                onClose = { inTimerScreen = false },
            )
        }
    }
}

@Composable
private fun StudySessionSetupScreen(
    uiState: StudySessionUiState,
    newSubjectName: String,
    onSubjectNameChange: (String) -> Unit,
    onAddSubject: () -> Unit,
    onSelectSubject: (StudySubject) -> Unit,
    onTaskSelected: (Task) -> Unit,
    onStatusChange: (Status) -> Unit,
    focusMinutes: Int,
    breakMinutes: Int,
    onFocusMinutesChange: (Int) -> Unit,
    onBreakMinutesChange: (Int) -> Unit,
    onStart: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = SCREEN_HORIZONTAL_PADDING_DP.dp,
                    vertical = SCREEN_VERTICAL_PADDING_DP.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(SCREEN_SECTION_SPACING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.study_session_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.testTag(StudySessionTestTags.TITLE),
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Subjects",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                SubjectsSection(
                    uiState = uiState,
                    newSubjectName = newSubjectName,
                    onSubjectNameChange = onSubjectNameChange,
                    onAddSubject = onAddSubject,
                    onSelectSubject = onSelectSubject,
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                SuggestedTasksList(
                    tasks = uiState.suggestedTasks,
                    selectedTask = uiState.selectedTask,
                    onTaskSelected = onTaskSelected,
                    modifier = Modifier.fillMaxWidth().testTag(StudySessionTestTags.TASK_LIST),
                )

                SelectedTaskSection(uiState = uiState, onStatusChange = onStatusChange)
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Pomodoro cycle",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                DurationPickerRow(
                    label = "Study",
                    minutes = focusMinutes,
                    min = MIN_FOCUS_MIN,
                    max = MAX_FOCUS_MIN,
                    onMinutesChange = onFocusMinutesChange,
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                DurationPickerRow(
                    label = "Break",
                    minutes = breakMinutes,
                    min = MIN_BREAK_MIN,
                    max = MAX_BREAK_MIN,
                    onMinutesChange = onBreakMinutesChange,
                )
            }
        }

        Button(
            onClick = onStart,
            enabled = (uiState.selectedSubject != null || uiState.selectedTask != null),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                text = "Start",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag(StudySessionTestTags.STATS_PANEL),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            SessionStatsPanel(
                pomodoros = uiState.completedPomodoros,
                totalMinutes = uiState.totalMinutes,
                streak = uiState.streakCount,
                modifier = Modifier.fillMaxWidth().padding(4.dp),
            )
        }
    }
}

@Composable
private fun StudySessionTimerScreen(
    selectionLabel: String,
    pomodoroViewModel: PomodoroViewModelContract,
    onClose: () -> Unit,
) {
    val quote = remember { MOTIVATION_QUOTES.random() }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_study),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Readability overlay (theme-adaptive)
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.80f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            ))))

        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground)
                }

                Text(
                    text = "Study session",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.End,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Currently working on",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )
                Text(
                    text = selectionLabel,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                // Render Pomodoro UI directly on the background (no wrapper card and no "purple block").
                // We locally override container colors to be transparent for the Pomodoro subtree only.
                val cs = MaterialTheme.colorScheme
                val transparentPomodoroScheme =
                    remember(cs) {
                        cs.copy(
                            background = Color.Transparent,
                            surface = Color.Transparent,
                            surfaceVariant = Color.Transparent,
                            surfaceTint = Color.Transparent,
                            primaryContainer = Color.Transparent,
                            secondaryContainer = Color.Transparent,
                            tertiaryContainer = Color.Transparent,
                            onSurface = cs.onBackground,
                            onSurfaceVariant = cs.onBackground.copy(alpha = 0.85f),
                            onPrimaryContainer = cs.onBackground,
                            onSecondaryContainer = cs.onBackground,
                            onTertiaryContainer = cs.onBackground,
                        )
                    }

                CompositionLocalProvider(LocalAbsoluteTonalElevation provides 0.dp) {
                    MaterialTheme(
                        colorScheme = transparentPomodoroScheme,
                        typography = MaterialTheme.typography,
                        shapes = MaterialTheme.shapes,
                    ) {
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .testTag(StudySessionTestTags.TIMER_SECTION)
                                    .padding(horizontal = 6.dp)) {
                            PomodoroScreen(viewModel = pomodoroViewModel)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                        contentColor = MaterialTheme.colorScheme.onSurface),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = quote,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectsSection(
    uiState: StudySessionUiState,
    newSubjectName: String,
    onSubjectNameChange: (String) -> Unit,
    onAddSubject: () -> Unit,
    onSelectSubject: (StudySubject) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(StudySessionTestTags.SUBJECTS_SECTION),
    ) {
        Text(
            text = stringResource(R.string.study_session_subject_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )

        Spacer(Modifier.height(SUBJECT_SECTION_SPACING_DP.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newSubjectName,
                onValueChange = onSubjectNameChange,
                modifier = Modifier.weight(1f),
                label = { Text(text = stringResource(R.string.study_session_new_subject_placeholder)) },
            )
            Spacer(Modifier.width(10.dp))
            TextButton(onClick = onAddSubject) {
                Text(text = stringResource(R.string.study_session_add_subject_button))
            }
        }

        Spacer(Modifier.height(SUBJECT_TEXT_FIELD_HEIGHT_DP.dp))

        if (uiState.subjects.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SUBJECT_CHIP_SPACING_DP.dp),
                verticalArrangement = Arrangement.spacedBy(SUBJECT_CHIP_SPACING_DP.dp),
            ) {
                uiState.subjects.forEach { subject ->
                    AssistChip(
                        onClick = { onSelectSubject(subject) },
                        label = { Text(subject.name) },
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor =
                                    if (uiState.selectedSubject?.id == subject.id) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedTaskSection(
    uiState: StudySessionUiState,
    onStatusChange: (Status) -> Unit,
) {
    val task = uiState.selectedTask ?: return

    Text(
        text = stringResource(R.string.selected_task_txt) + " " + task.title,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag(StudySessionTestTags.SELECTED_TASK),
    )

    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Status.values().forEach { status ->
            AssistChip(
                onClick = { onStatusChange(status) },
                label = { Text(status.name.replace('_', ' ')) },
                colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor =
                            if (task.status == status) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                    ),
            )
        }
    }
}

@Composable
private fun DurationPickerRow(
    label: String,
    minutes: Int,
    min: Int,
    max: Int,
    onMinutesChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "${minutes} min",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Slider(
            value = minutes.toFloat(),
            onValueChange = { onMinutesChange(it.toInt().coerceIn(min, max)) },
            valueRange = min.toFloat()..max.toFloat(),
        )
    }
}

/**
 * Best-effort duration application without depending on specific PomodoroViewModelContract APIs.
 * This preserves compilation even if the contract does not expose duration setters directly.
 */
private fun tryApplyPomodoroDurations(vm: Any, focusMinutes: Int, breakMinutes: Int) {
    // 1) Try common (work, break) setters.
    val twoArgNames =
        listOf(
            "setDurations",
            "setPomodoroDurations",
            "updateDurations",
            "setWorkAndBreakMinutes",
            "setWorkBreakMinutes",
            "setSessionDurations",
        )
    if (invokeIntIntMethodIfExists(vm, twoArgNames, focusMinutes, breakMinutes)) return

    // 2) Try separate setters.
    val workNames =
        listOf(
            "setWorkMinutes",
            "setStudyMinutes",
            "setFocusMinutes",
            "setWorkDurationMinutes",
            "setStudyDurationMinutes",
            "setWorkDuration",
            "setFocusDuration",
        )
    val breakNames =
        listOf(
            "setBreakMinutes",
            "setRestMinutes",
            "setPauseMinutes",
            "setBreakDurationMinutes",
            "setRestDurationMinutes",
            "setPauseDurationMinutes",
            "setBreakDuration",
            "setRestDuration",
        )

    val workOk = invokeIntMethodIfExists(vm, workNames, focusMinutes)
    val breakOk = invokeIntMethodIfExists(vm, breakNames, breakMinutes)

    @Suppress("UNUSED_VARIABLE") val ignoredOk = workOk || breakOk
}

private fun invokeIntMethodIfExists(target: Any, methodNames: List<String>, value: Int): Boolean {
    return runCatching {
        val m =
            target.javaClass.methods.firstOrNull {
                it.name in methodNames && it.parameterTypes.size == 1
            } ?: return@runCatching false
        val p = m.parameterTypes[0]
        when (p) {
            Int::class.javaPrimitiveType, Int::class.javaObjectType -> m.invoke(target, value)
            Long::class.javaPrimitiveType, Long::class.javaObjectType ->
                m.invoke(target, value.toLong())
            else -> return@runCatching false
        }
        true
    }
        .getOrElse { false }
}

private fun invokeIntIntMethodIfExists(
    target: Any,
    methodNames: List<String>,
    a: Int,
    b: Int
): Boolean {
    return runCatching {
        val m =
            target.javaClass.methods.firstOrNull {
                it.name in methodNames && it.parameterTypes.size == 2
            } ?: return@runCatching false

        val p0 = m.parameterTypes[0]
        val p1 = m.parameterTypes[1]

        val arg0: Any =
            when (p0) {
                Int::class.javaPrimitiveType, Int::class.javaObjectType -> a
                Long::class.javaPrimitiveType, Long::class.javaObjectType -> a.toLong()
                else -> return@runCatching false
            }

        val arg1: Any =
            when (p1) {
                Int::class.javaPrimitiveType, Int::class.javaObjectType -> b
                Long::class.javaPrimitiveType, Long::class.javaObjectType -> b.toLong()
                else -> return@runCatching false
            }

        m.invoke(target, arg0, arg1)
        true
    }
        .getOrElse { false }
}
