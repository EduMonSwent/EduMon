package com.android.sample.feature.weeks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.ui.theme.BackgroundDark
import com.android.sample.ui.theme.BackgroundGradientEnd
import com.android.sample.ui.theme.EduMonTheme

// UI text constants
private const val CONTENT_DESC_BACK = "Back"
private const val LABEL_MARK_COMPLETED = "Mark as completed"
private const val LABEL_COURSE = "Course"
private const val LABEL_EXERCISES = "Exercises"
private const val TITLE_COURSE_PDF = "Course PDF"
private const val TITLE_EXERCISES_PDF = "Exercises PDF"
private const val ACTION_OPEN_COURSE = "Open course"
private const val ACTION_OPEN_EXERCISES = "Open exercises"
private const val TIP_MESSAGE =
    "Tip: you can come back and continue later. Once you feel done, tap \"Mark as completed\"."
private const val LABEL_TODAYS_OBJECTIVE = "Today's objective"
private const val LABEL_MIN_FOCUS = " min focus"
private const val NO_PDF_AVAILABLE = "No PDF available"
private const val UNAVAILABLE = "Unavailable"

/**
 * Route-level composable that wraps the screen in EdumonTheme. You can call this from your nav
 * graph / ScheduleScreen later.
 */
@Composable
fun CourseExercisesRoute(
    objective: Objective,
    coursePdfLabel: String,
    exercisesPdfLabel: String,
    coursePdfUrl: String = "",
    exercisePdfUrl: String = "",
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
  EduMonTheme {
    CourseExercisesScreen(
        objective = objective,
        coursePdfLabel = coursePdfLabel,
        exercisesPdfLabel = exercisesPdfLabel,
        coursePdfUrl = coursePdfUrl,
        exercisePdfUrl = exercisePdfUrl,
        onBack = onBack,
        onCompleted = onCompleted,
    )
  }
}

/**
 * Screen that shows two PDFs: one for the course, one for exercises. PDF opening is delegated to
 * callbacks (you can use Intents or your own PDF viewer).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseExercisesScreen(
    objective: Objective,
    coursePdfLabel: String,
    exercisesPdfLabel: String,
    coursePdfUrl: String = "",
    exercisePdfUrl: String = "",
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  val context = androidx.compose.ui.platform.LocalContext.current
  val initialTabIndex =
      when {
        objective.sourceId?.contains(":EXERCISE:") == true -> 1
        objective.sourceId?.contains(":LAB:") == true -> 1
        else -> 0 // LECTURE (default)
      }
  var selectedTab by remember { mutableIntStateOf(initialTabIndex) } // 0 = Course, 1 = Exercises

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.testTag(CourseExercisesTestTags.TOP_BAR)) {
                    Text(
                        text = objective.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(CourseExercisesTestTags.OBJECTIVE_TITLE))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = objective.course,
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(CourseExercisesTestTags.OBJECTIVE_COURSE))
                  }
            },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(CourseExercisesTestTags.BACK_BUTTON)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = CONTENT_DESC_BACK)
                  }
            })
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = onCompleted,
            icon = { Icon(Icons.Default.Check, contentDescription = null) },
            text = { Text(LABEL_MARK_COMPLETED) },
            modifier = Modifier.testTag(CourseExercisesTestTags.COMPLETED_FAB))
      },
      containerColor = Color.Transparent,
      modifier =
          Modifier.background(
                  brush = Brush.verticalGradient(listOf(BackgroundDark, BackgroundGradientEnd)))
              .testTag(CourseExercisesTestTags.SCREEN)) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
              // Hero card with time estimate etc.
              ObjectiveHeaderCard(objective = objective)

              Spacer(Modifier.height(16.dp))

              // Tabs: Course vs Exercises
              TabRow(
                  selectedTabIndex = selectedTab,
                  modifier =
                      Modifier.fillMaxWidth()
                          .clip(RoundedCornerShape(20.dp))
                          .testTag(CourseExercisesTestTags.TAB_ROW),
                  containerColor = cs.surface.copy(alpha = 0.85f)) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(LABEL_COURSE) },
                        icon = {
                          Icon(imageVector = Icons.Default.LibraryBooks, contentDescription = null)
                        },
                        modifier = Modifier.testTag(CourseExercisesTestTags.COURSE_TAB))
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(LABEL_EXERCISES) },
                        icon = {
                          Icon(imageVector = Icons.Default.Description, contentDescription = null)
                        },
                        modifier = Modifier.testTag(CourseExercisesTestTags.EXERCISES_TAB))
                  }

              Spacer(Modifier.height(16.dp))

              when (selectedTab) {
                0 ->
                    PdfCard(
                        title = TITLE_COURSE_PDF,
                        description = coursePdfLabel,
                        primaryActionLabel = ACTION_OPEN_COURSE,
                        pdfUrl = coursePdfUrl,
                        onClick = {
                          // Only called when coursePdfUrl.isNotBlank() (card is disabled otherwise)
                          com.android.sample.core.helpers.PdfHelper.openPdf(context, coursePdfUrl)
                        },
                        cardTag = CourseExercisesTestTags.COURSE_PDF_CARD)
                1 ->
                    PdfCard(
                        title = TITLE_EXERCISES_PDF,
                        description = exercisesPdfLabel,
                        primaryActionLabel = ACTION_OPEN_EXERCISES,
                        pdfUrl = exercisePdfUrl,
                        onClick = {
                          // Only called when exercisePdfUrl.isNotBlank() (card is disabled
                          // otherwise)
                          com.android.sample.core.helpers.PdfHelper.openPdf(context, exercisePdfUrl)
                        },
                        cardTag = CourseExercisesTestTags.EXERCISES_PDF_CARD)
              }

              Spacer(Modifier.height(24.dp))

              Text(
                  text = TIP_MESSAGE,
                  style = MaterialTheme.typography.bodySmall,
                  color = cs.onSurfaceVariant,
                  modifier = Modifier.testTag(CourseExercisesTestTags.TIP_TEXT))
            }
      }
}

@Composable
private fun ObjectiveHeaderCard(objective: Objective) {
  val cs = MaterialTheme.colorScheme
  Surface(
      shape = RoundedCornerShape(24.dp),
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth().testTag(CourseExercisesTestTags.HEADER_CARD)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          // Left: time + course code
          Column(modifier = Modifier.weight(1f)) {
            Text(
                text = LABEL_TODAYS_OBJECTIVE,
                style = MaterialTheme.typography.labelMedium,
                color = cs.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = objective.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(
                text = objective.course,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant)
            if (objective.estimateMinutes > 0) {
              Spacer(Modifier.height(6.dp))
              AssistChip(
                  onClick = {},
                  label = { Text("${objective.estimateMinutes}$LABEL_MIN_FOCUS") },
                  modifier = Modifier.testTag(CourseExercisesTestTags.ESTIMATE_CHIP))
            }
          }
        }
      }
}

/**
 * Nice elevated card that represents a single PDF file. For now it just calls onClick; plug your
 * PDF viewer / Intent logic into that.
 */
@Composable
private fun PdfCard(
    title: String,
    description: String,
    primaryActionLabel: String,
    pdfUrl: String = "",
    onClick: () -> Unit,
    cardTag: String = CourseExercisesTestTags.PDF_CARD,
) {
  val hasPdf = pdfUrl.isNotBlank()

  Surface(
      shape = RoundedCornerShape(24.dp),
      tonalElevation = 6.dp,
      modifier =
          Modifier.fillMaxWidth().clickable(enabled = hasPdf, onClick = onClick).testTag(cardTag)) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
          PdfIconBox(hasPdf = hasPdf)
          Spacer(Modifier.width(16.dp))
          PdfCardContent(title = title, description = description, hasPdf = hasPdf)
          Spacer(Modifier.width(12.dp))
          PdfActionButton(
              hasPdf = hasPdf, primaryActionLabel = primaryActionLabel, onClick = onClick)
        }
      }
}

/** Icon box for PDF card - displays document icon with appropriate styling based on availability */
@Composable
private fun PdfIconBox(hasPdf: Boolean) {
  val cs = MaterialTheme.colorScheme
  val backgroundColor =
      if (hasPdf) cs.primary.copy(alpha = 0.12f) else cs.onSurface.copy(alpha = 0.05f)
  val iconTint = if (hasPdf) cs.primary else cs.onSurface.copy(alpha = 0.3f)

  Box(
      modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(backgroundColor),
      contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = iconTint)
      }
}

/** Content section of PDF card - displays title and description with appropriate styling */
@Composable
private fun RowScope.PdfCardContent(title: String, description: String, hasPdf: Boolean) {
  val cs = MaterialTheme.colorScheme
  val titleColor = if (hasPdf) cs.onSurface else cs.onSurface.copy(alpha = 0.5f)
  val descriptionText = if (hasPdf) description else NO_PDF_AVAILABLE
  val descriptionAlpha = if (hasPdf) 1f else 0.5f

  Column(modifier = Modifier.weight(1f)) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = titleColor,
        modifier = Modifier.testTag(CourseExercisesTestTags.PDF_TITLE))
    Spacer(Modifier.height(4.dp))
    Text(
        text = descriptionText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = descriptionAlpha),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag(CourseExercisesTestTags.PDF_DESCRIPTION))
  }
}

/** Action button for PDF card - opens PDF or shows unavailable state */
@Composable
private fun PdfActionButton(hasPdf: Boolean, primaryActionLabel: String, onClick: () -> Unit) {
  val buttonText = if (hasPdf) primaryActionLabel else UNAVAILABLE

  TextButton(
      onClick = onClick,
      enabled = hasPdf,
      modifier = Modifier.testTag(CourseExercisesTestTags.PDF_OPEN_BUTTON)) {
        Text(buttonText)
      }
}
