package com.android.sample.feature.weeks.ui

// Centralized test tags for WeekProgDailyObj and its subcomponents.
object WeekProgDailyObjTags {
  const val ROOT_CARD = "WEEK_PROG_DAILY_OBJ_CARD"

  // Week progress section
  const val WEEK_PROGRESS_SECTION = "WEEK_PROGRESS_SECTION"
  const val WEEK_PROGRESS_TOGGLE = "WEEK_PROGRESS_TOGGLE"
  const val WEEK_PROGRESS_BAR = "WEEK_PROGRESS_BAR"
  const val WEEK_PROGRESS_PERCENT = "WEEK_PROGRESS_PERCENT"
  const val WEEKS_LIST = "WEEKS_LIST"

  // Week rows prefixes (index appended)
  const val WEEK_ROW_PREFIX = "WEEK_ROW_"
  const val WEEK_RING_PREFIX = "WEEK_RING_"
  const val WEEK_PERCENT_PREFIX = "WEEK_PERCENT_"
  const val WEEK_STATUS_PREFIX = "WEEK_STATUS_"

  // Expanded week content (index appended)
  const val WEEK_CONTENT_PREFIX = "WEEK_CONTENT_"
  const val WEEK_COURSES_HEADER_PREFIX = "WEEK_COURSES_HEADER_"
  const val WEEK_EXERCISES_HEADER_PREFIX = "WEEK_EXERCISES_HEADER_"
  const val WEEK_COURSE_ITEM_PREFIX = "WEEK_COURSE_ITEM_" // + index + _ + id
  const val WEEK_EXERCISE_ITEM_PREFIX = "WEEK_EXERCISE_ITEM_" // + index + _ + id

  // Objectives section
  const val OBJECTIVES_SECTION = "OBJECTIVES_SECTION"
  const val OBJECTIVES_TOGGLE = "OBJECTIVES_TOGGLE"
  const val OBJECTIVES_EMPTY = "OBJECTIVES_EMPTY"
  const val OBJECTIVES_SHOW_ALL_BUTTON = "OBJECTIVES_SHOW_ALL_BUTTON"
  const val OBJECTIVES_SHOW_ALL_LABEL = "OBJECTIVES_SHOW_ALL_LABEL"

  // Objective rows prefixes (index appended)
  const val OBJECTIVE_ROW_PREFIX = "OBJECTIVE_ROW_"
  const val OBJECTIVE_START_BUTTON_PREFIX = "OBJECTIVE_START_BUTTON_"
  const val OBJECTIVE_REASON_PREFIX = "OBJECTIVE_REASON_"

  // Footer dots
  const val WEEK_DOTS_ROW = "WEEK_DOTS_ROW"
  const val WEEK_DOT_PREFIX = "WEEK_DOT_" // DayOfWeek.name appended
}
