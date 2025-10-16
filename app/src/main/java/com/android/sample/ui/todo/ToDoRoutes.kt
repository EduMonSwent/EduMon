package com.android.sample.ui.todo

/** Central place to define navigation routes for the To-Do feature. */
object ToDoRoutes {

  // Route name for the main To-Do list (Overview screen)
  const val Todos = "todos"

  // Route name for the Add To-Do screen
  const val Add = "todos/add"

  // Route pattern for editing an existing To-Do
  const val Edit = "todos/edit/{id}"

  // Helper function to generate a real route string for a specific To-Do item.
  fun edit(id: String) = "todos/edit/$id"
}
