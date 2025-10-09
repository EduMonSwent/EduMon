package com.android.sample.todo.ui

object TestTags {
  // Nav / screens
  const val NavHost = "NavHost"
  const val OverviewScreen = "OverviewScreen"
  const val AddScreen = "AddToDoScreen"
  const val EditScreen = "EditToDoScreen"

  // Overview
  const val FabAdd = "AddToDoFab"
  const val List = "ToDoList"
  const val cardPrefix = "card/" // cards are tagged as "card/<id>"

  fun card(id: String) = "ToDoCard_$id"

  fun delete(id: String) = "Delete_$id"

  fun status(id: String) = "Status_$id"

  fun priority(id: String) = "Priority_$id"

  // Form (shared by Add/Edit)
  const val TitleField = "TitleField"
  const val DueDateField = "DueDateField"
  const val ChangeDateBtn = "ChangeDateButton"
  const val PriorityDropdown = "PriorityDropdown"
  const val StatusDropdown = "StatusDropdown"
  const val OptionalToggle = "OptionalToggle"
  const val LocationField = "LocationField"
  const val LinksField = "LinksField"
  const val NoteField = "NoteField"
  const val NotificationsSwitch = "NotificationsSwitch"
  const val SaveButton = "SaveButton"
}
