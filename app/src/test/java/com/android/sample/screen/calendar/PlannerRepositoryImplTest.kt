package com.android.sample.screen.calendar

import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.calendar.PlannerRepository
import com.android.sample.model.calendar.PlannerRepositoryImpl
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class PlannerRepositoryImplTest {

  @Test
  fun save_get_update_delete_flow() = runBlocking {
    val repo: PlannerRepository = PlannerRepositoryImpl() // pre-seeded
    val d = LocalDate.now()

    val item =
        StudyItem(
            title = "Unit test task",
            date = d,
            time = LocalTime.of(9, 30),
            durationMinutes = 45,
            type = TaskType.WORK)

    // create
    repo.saveTask(item)
    val created = repo.getTaskById(item.id)
    Assert.assertNotNull(created)
    Assert.assertEquals("Unit test task", created!!.title)

    // update
    val updated = created.copy(title = "Updated")
    repo.saveTask(updated)
    val again = repo.getTaskById(item.id)
    Assert.assertEquals("Updated", again!!.title)

    // delete
    repo.deleteTask(item.id)
    Assert.assertNull(repo.getTaskById(item.id))
  }

  @Test
  fun getAllTasksReflectsMutations() = runBlocking {
    val repo: PlannerRepository = PlannerRepositoryImpl()
    val initial = repo.getAllTasks().size

    val newItem =
        StudyItem(
            title = "New", date = LocalDate.now(), durationMinutes = null, type = TaskType.PERSONAL)
    repo.saveTask(newItem)
    Assert.assertEquals(initial + 1, repo.getAllTasks().size)

    repo.deleteTask(newItem.id)
    Assert.assertEquals(initial, repo.getAllTasks().size)
  }
}
