package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

/* Generate individual EventOccurrence objects from a MasterEvent */

@RequiresApi(Build.VERSION_CODES.O)
fun generateEventOccurrences(master: MasterEvent): List<EventOccurrence> {
    val today = LocalDate.now()
    val occurrences = mutableListOf<EventOccurrence>()

    val endLimit = master.endDate?.let { if (it.isBefore(today.plusYears(1))) it else today.plusYears(1) }
        ?: today.plusYears(1)

    var current = if (master.startDate.isBefore(today)) today else master.startDate

    while (!current.isAfter(endLimit)) {
        // Determine if this date matches the recurrence rule
        val matchesRule = when (master.recurFreq) {
            RecurrenceFrequency.NONE -> current == master.startDate
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> master.recurRule.daysOfWeek?.contains(current.dayOfWeek.value) ?: true
            RecurrenceFrequency.MONTHLY -> master.recurRule.dayOfMonth?.let { it == current.dayOfMonth } ?: true
            RecurrenceFrequency.YEARLY -> master.recurRule.monthAndDay?.let { it.first == current.dayOfMonth && it.second == current.monthValue } ?: true
        }

        if (matchesRule) {
            occurrences.add(
                EventOccurrence(
                    masterEventId = master.id,
                    notes = master.notes,
                    occurDate = current,
                    startTime = master.startTime,
                    endTime = master.endTime
                )
            )
        }

        // Increment current date based on frequency
        current = when (master.recurFreq) {
            RecurrenceFrequency.NONE, RecurrenceFrequency.DAILY -> current.plusDays(1)
            RecurrenceFrequency.WEEKLY -> current.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> current.plusMonths(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }

    return occurrences
}

/* Generate individual TaskBucketOccurrence objects from a MasterTaskBucket
* */
@RequiresApi(Build.VERSION_CODES.O)
fun generateTaskBucketOccurrences(master: MasterTaskBucket): List<TaskBucketOccurrence> {
    val today = LocalDate.now()
    val occurrences = mutableListOf<TaskBucketOccurrence>()

    val endLimit = master.endDate?.let { if (it.isBefore(today.plusYears(1))) it else today.plusYears(1) }
        ?: today.plusYears(1)

    var current = if (master.startDate.isBefore(today)) today else master.startDate

    while (!current.isAfter(endLimit)) {
        // Determine if this date matches the recurrence rule
        val matchesRule = when (master.recurFreq) {
            RecurrenceFrequency.NONE -> current == master.startDate
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> master.recurRule.daysOfWeek?.contains(current.dayOfWeek.value) ?: true
            RecurrenceFrequency.MONTHLY -> master.recurRule.dayOfMonth?.let { it == current.dayOfMonth } ?: true
            RecurrenceFrequency.YEARLY -> master.recurRule.monthAndDay?.let { it.first == current.dayOfMonth && it.second == current.monthValue } ?: true
        }

        if (matchesRule) {
            occurrences.add(
                TaskBucketOccurrence(
                    masterBucketId = master.id,
                    occurDate = current,
                    startTime = master.startTime,
                    endTime = master.endTime
                )
            )
        }

        // Increment current date based on frequency
        current = when (master.recurFreq) {
            RecurrenceFrequency.NONE, RecurrenceFrequency.DAILY -> current.plusDays(1)
            RecurrenceFrequency.WEEKLY -> current.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> current.plusMonths(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }

    return occurrences
}

/* Generate task intervals from a master task */

