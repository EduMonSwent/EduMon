package com.android.sample.feature.schedule.repository.schedule

import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime

/** This class was implemented with the help of ai (ChatGPT) */
object IcsParser {
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
    val rawLines = input.bufferedReader().readLines()

    // Unfold ICS "folded" lines
    val lines = mutableListOf<String>()
    for (line in rawLines) {
      if (line.startsWith(" ") || line.startsWith("\t")) {
        if (lines.isNotEmpty()) {
          lines[lines.lastIndex] += line.trimStart()
        }
      } else {
        lines += line.trimEnd()
      }
    }

    val events = mutableListOf<IcsClass>()

    var title: String? = null
    var dtStart: String? = null
    var dtEnd: String? = null
    var location: String? = null
    var description: String? = null
    var rrule: String? = null
    var categories = mutableListOf<String>()

    for (line in lines) {
      when {
        line.startsWith("BEGIN:VEVENT") -> {
          title = null
          dtStart = null
          dtEnd = null
          location = null
          description = null
          rrule = null
          categories = mutableListOf()
        }
        line.startsWith("SUMMARY:") -> title = line.substringAfter("SUMMARY:").trim()
        line.startsWith("DTSTART") -> dtStart = line.substringAfter(":").trim()
        line.startsWith("DTEND") -> dtEnd = line.substringAfter(":").trim()
        line.startsWith("LOCATION:") -> location = line.substringAfter("LOCATION:").trim()
        line.startsWith("DESCRIPTION:") -> description = line.substringAfter("DESCRIPTION:").trim()
        line.startsWith("RRULE:") -> rrule = line.substringAfter("RRULE:").trim()
        line.startsWith("CATEGORIES:") -> {
          val raw = line.removePrefix("CATEGORIES:").trim()
          if (raw.isNotBlank()) {
            categories.add(raw)
          }
        }
        line.startsWith("END:VEVENT") -> {
          if (title != null && dtStart != null) {
            val base =
                buildIcsClass(
                    title = title!!,
                    dtStart = dtStart!!,
                    dtEnd = dtEnd,
                    location = location,
                    description = description,
                    categories = categories)

            if (base != null) {
              val expanded = expandWeeklyIfNeeded(base, rrule)
              events += expanded
            }
          }
        }
      }
    }
    return events
  }

  private fun buildIcsClass(
      title: String,
      dtStart: String,
      dtEnd: String?,
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
        dtEnd?.let { end ->
          val endTimePart = end.substringAfter('T', missingDelimiterValue = "")
          if (endTimePart.length >= 4) {
            val hh = endTimePart.substring(0, 2)
            val mm = endTimePart.substring(2, 4)
            runCatching { LocalTime.parse("$hh:$mm") }.getOrNull()
          } else {
            null
          }
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
