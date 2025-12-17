package com.android.sample.feature.schedule.repository.schedule

import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime

/** This class was implemented with the help of ai (ChatGPT) */
object IcsParser {
  // --- ICS FIELD CONSTANTS (no magic strings) ---
  private const val VEVENT_BEGIN = "BEGIN:VEVENT"
  private const val VEVENT_END = "END:VEVENT"
  private const val FIELD_SUMMARY = "SUMMARY:"
  private const val FIELD_DTSTART = "DTSTART"
  private const val FIELD_DTEND = "DTEND"
  private const val FIELD_LOCATION = "LOCATION:"
  private const val FIELD_DESCRIPTION = "DESCRIPTION:"
  private const val FIELD_RRULE = "RRULE:"
  private const val FIELD_CATEGORIES = "CATEGORIES:"
  private const val FIELD_DURATION = "DURATION:"

  data class IcsClass(
      val title: String,
      val date: LocalDate,
      val start: LocalTime?,
      val end: LocalTime?,
      val location: String?,
      val description: String?,
      val categories: List<String> = emptyList()
  )

  fun parse(input: InputStream): List<IcsClass> {
    val lines = unfoldLines(input.bufferedReader().readLines())
    val events = mutableListOf<IcsClass>()

    var state = EventState()

    for (line in lines) {
      when {
        line.startsWith(VEVENT_BEGIN) -> state = resetEventState()
        line.startsWith(VEVENT_END) -> {
          val title = state.title
          val start = state.dtStart
          if (title != null && start != null) {
            buildIcsClass(
                    title = title,
                    dtStart = start,
                    dtEnd = state.dtEnd,
                    durationMinutes = state.durationMinutes,
                    location = state.location,
                    description = state.description,
                    categories = state.categories)
                ?.let { base -> events += expandWeeklyIfNeeded(base, state.rrule) }
          }
        }
        else -> handleEventLine(line, state)
      }
    }

    return events
  }

  private fun unfoldLines(raw: List<String>): List<String> {
    val result = mutableListOf<String>()
    for (line in raw) {
      if (line.startsWith(" ") || line.startsWith("\t")) {
        if (result.isNotEmpty()) {
          result[result.lastIndex] += line.trimStart()
        }
      } else {
        result += line.trimEnd()
      }
    }
    return result
  }

  private data class EventState(
      var title: String? = null,
      var dtStart: String? = null,
      var dtEnd: String? = null,
      var durationMinutes: Int? = null,
      var location: String? = null,
      var description: String? = null,
      var rrule: String? = null,
      var categories: MutableList<String> = mutableListOf()
  )

  private fun resetEventState() = EventState()

  private fun handleEventLine(line: String, state: EventState) {
    when {
      line.startsWith(FIELD_SUMMARY) -> state.title = line.substringAfter(FIELD_SUMMARY).trim()
      line.startsWith(FIELD_DTSTART) -> state.dtStart = line.substringAfter(":").trim()
      line.startsWith(FIELD_DTEND) -> state.dtEnd = line.substringAfter(":").trim()
      line.startsWith(FIELD_LOCATION) -> state.location = line.substringAfter(FIELD_LOCATION).trim()
      line.startsWith(FIELD_DESCRIPTION) ->
          state.description = line.substringAfter(FIELD_DESCRIPTION).trim()
      line.startsWith(FIELD_RRULE) -> state.rrule = line.substringAfter(FIELD_RRULE).trim()
      line.startsWith(FIELD_CATEGORIES) -> {
        line
            .removePrefix(FIELD_CATEGORIES)
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { state.categories.add(it) }
      }
      line.startsWith(FIELD_DURATION) -> {
        val raw = line.substringAfter(FIELD_DURATION)
        state.durationMinutes = parseDurationMinutes(raw)
      }
    }
  }

  private fun parseDurationMinutes(raw: String): Int? {
    if (!raw.startsWith("PT")) return null

    val minutesPart = raw.removePrefix("PT").removeSuffix("M")
    return minutesPart.toIntOrNull()
  }

  private fun buildIcsClass(
      title: String,
      dtStart: String,
      dtEnd: String?,
      durationMinutes: Int?,
      location: String?,
      description: String?,
      categories: List<String>
  ): IcsClass? {
    if (dtStart.length < 8) return null

    val datePart = dtStart.substring(0, 8)
    val year = datePart.substring(0, 4).toIntOrNull() ?: return null
    val month = datePart.substring(4, 6).toIntOrNull() ?: return null
    val day = datePart.substring(6, 8).toIntOrNull() ?: return null
    val date = LocalDate.of(year, month, day)

    val timePart = dtStart.substringAfter('T', missingDelimiterValue = "")
    val startTime =
        if (timePart.length >= 4) {
          val hh = timePart.substring(0, 2)
          val mm = timePart.substring(2, 4)
          runCatching { LocalTime.parse("$hh:$mm") }.getOrNull()
        } else {
          null
        }

    val endTime =
        when {
          dtEnd != null -> {
            dtEnd
                .substringAfter('T')
                .takeIf { it.length >= 4 }
                ?.let { raw ->
                  val hh = raw.substring(0, 2)
                  val mm = raw.substring(2, 4)
                  runCatching { LocalTime.of(hh.toInt(), mm.toInt()) }.getOrNull()
                }
          }
          durationMinutes != null && startTime != null -> {
            startTime.plusMinutes(durationMinutes.toLong())
          }
          else -> null
        }

    return IcsClass(
        title = title,
        date = date,
        start = startTime,
        end = endTime,
        location = location,
        description = description,
        categories = categories)
  }

  private fun expandWeeklyIfNeeded(base: IcsClass, rrule: String?): List<IcsClass> {
    if (rrule == null || !rrule.contains("FREQ=WEEKLY")) {
      return listOf(base)
    }

    val parts = rrule.split(";")
    val untilPart = parts.firstOrNull { it.startsWith("UNTIL=") }

    val untilDate: LocalDate? =
        untilPart
            ?.substringAfter("UNTIL=")
            ?.takeIf { it.length >= 8 }
            ?.let { u ->
              val y = u.substring(0, 4).toIntOrNull()
              val m = u.substring(4, 6).toIntOrNull()
              val d = u.substring(6, 8).toIntOrNull()
              if (y != null && m != null && d != null) LocalDate.of(y, m, d) else null
            }

    val endDate = untilDate ?: base.date.plusWeeks(12)

    val result = mutableListOf<IcsClass>()
    var current = base.date
    while (!current.isAfter(endDate)) {
      result += base.copy(date = current)
      current = current.plusWeeks(1)
    }
    return result
  }
}
