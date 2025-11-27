package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Generate individual occurrence dates from a master startDate and recurrence rule.
 * Only generates future occurrences (from today) up to 1 year or master endDate.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun generateOccurrenceDates(
    masterStartDate: LocalDate,
    masterEndDate: LocalDate?,
    recurrence: RecurrenceRule?
): List<LocalDate> {
    if (recurrence == null || recurrence.frequency == RecurrenceFrequency.NONE) {
        return listOf(masterStartDate)
    }

    val occurrences = mutableListOf<LocalDate>()
    val today = LocalDate.now()
    var date = if (masterStartDate.isBefore(today)) today else masterStartDate

    val maxEnd = listOfNotNull(date.plusYears(1), masterEndDate).minOrNull()!!

    while (!date.isAfter(maxEnd)) {
        when (recurrence.frequency) {
            RecurrenceFrequency.DAILY -> {
                occurrences.add(date)
                date = date.plusDays(1)
            }

            RecurrenceFrequency.WEEKLY -> {
                val daysOfWeek = recurrence.daysOfWeek ?: listOf(date.dayOfWeek.value)
                for (dow in daysOfWeek.sorted()) {
                    val nextDate = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dow)))
                    if (!nextDate.isAfter(maxEnd) && !occurrences.contains(nextDate) && !nextDate.isBefore(masterStartDate)) {
                        occurrences.add(nextDate)
                    }
                }
                date = date.plusWeeks(1)
            }

            RecurrenceFrequency.MONTHLY -> {
                val dayOfMonth = recurrence.dayOfMonth
                val lastDay = date.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
                val actualDay = if (dayOfMonth != null && dayOfMonth > lastDay) lastDay else dayOfMonth ?: date.dayOfMonth
                val nextDate = date.withDayOfMonth(actualDay)
                if (!nextDate.isAfter(maxEnd) && !occurrences.contains(nextDate) && !nextDate.isBefore(masterStartDate)) {
                    occurrences.add(nextDate)
                }
                date = date.plusMonths(1)
            }

            RecurrenceFrequency.YEARLY -> {
                val nextDate = LocalDate.of(date.year, masterStartDate.monthValue, masterStartDate.dayOfMonth)
                if (!nextDate.isAfter(maxEnd) && !occurrences.contains(nextDate) && !nextDate.isBefore(masterStartDate)) {
                    occurrences.add(nextDate)
                }
                date = date.plusYears(1)
            }

            else -> break
        }
    }

    return occurrences.sorted()
}

/**
 * Generate Event occurrences from a master Event.
 * Each occurrence gets a new auto-generated ID (id = 0),
 * parentRecurrenceId = master.id, and recurrenceInstanceDate set.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun generateEventOccurrences(master: Event): List<Event> {
    val dates = generateOccurrenceDates(master.startDate, master.endDate, master.recurrence)
    return dates.map { date ->
        master.copy(
            id = 0, // new auto-generated ID
            parentRecurrenceId = master.id,
            recurrenceInstanceDate = date,
            isException = false
        )
    }
}

/**
 * Generate TaskBucket occurrences from a master TaskBucket.
 * Each occurrence gets a new auto-generated ID (id = 0),
 * parentRecurrenceId = master.id, and recurrenceInstanceDate set.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun generateTaskBucketOccurrences(master: TaskBucket): List<TaskBucket> {
    val start = master.startDate ?: LocalDate.now()
    val dates = generateOccurrenceDates(start, master.endDate, master.recurrence)
    return dates.map { date ->
        master.copy(
            id = 0,
            parentRecurrenceId = master.id,
            recurrenceInstanceDate = date,
            isException = false
        )
    }
}