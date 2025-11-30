package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import androidx.room.TypeConverter
import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalTime

/* RECURRENCE LOGIC */
//<editor-fold desc="Recurrence">

enum class RecurrenceFrequency {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}
data class RecurrenceRule(
    val daysOfWeek: List<Int>? = null,      // 1 = Mon ... 7 = Sun
    val daysOfMonth: List<Int>? = null,     // 1 - 31
    val monthAndDay: Pair<Int, Int>? = null // DD-MM
)
//</editor-fold>

/* TYPE CONVERTERS */
object Converters {
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    // LocalDate -> Long
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    // Long -> LocalDate
    fun toLocalDate(days: Long?): LocalDate? = days?.let { LocalDate.ofEpochDay(it) }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    // LocalTime -> Int
    fun fromLocalTime(time: LocalTime?): Int? = time?.toSecondOfDay()

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    // Int -> LocalTime
    fun toLocalTime(seconds: Int?): LocalTime? = seconds?.let { LocalTime.ofSecondOfDay(it.toLong()) }

    @TypeConverter
    // RecurrenceFrequency -> String
    fun fromRecurrenceFrequency(freq: RecurrenceFrequency): String = freq.name

    @TypeConverter
    // String -> RecurrenceFrequency
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)

    @TypeConverter
    // RecurrenceRule -> JSON String
    fun fromRecurrenceRule(rule: RecurrenceRule): String = Gson().toJson(rule)

    @TypeConverter
    // JSON String -> RecurrenceRule
    fun toRecurrenceRule(value: String): RecurrenceRule = Gson().fromJson(value, RecurrenceRule::class.java)

    @TypeConverter
    // Color -> String
    fun fromColor(color: Color): String {
        return "#${color.toArgb().toUInt().toString(16).padStart(8, '0')}"
    }

    @TypeConverter
    // String -> Color
    fun toColor(value: String): Color {
        return try {
            Color(value.toColorInt())
        } catch (_: Exception) {
            Color.LightGray
        }
    }
}

/* Generate EventOccurrences from MasterEvent */
@RequiresApi(Build.VERSION_CODES.O)
fun generateEventOccurrences(master: MasterEvent): List<EventOccurrence> {
    val today = LocalDate.now()
    val occurrences = mutableListOf<EventOccurrence>()

    val endLimit = master.endDate?.let {
        if (it.isBefore(today.plusMonths(OCCURRENCE_GENERATION_MONTHS.toLong()))) it
        else today.plusMonths(OCCURRENCE_GENERATION_MONTHS.toLong())
    } ?: today.plusMonths(OCCURRENCE_GENERATION_MONTHS.toLong())

    var current = if (master.startDate.isBefore(today)) today else master.startDate

    while (!current.isAfter(endLimit)) {
        // Determine if this date matches the recurrence rule
        val matchesRule = when (master.recurFreq) {
            RecurrenceFrequency.NONE -> current == master.startDate
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> master.recurRule.daysOfWeek?.contains(current.dayOfWeek.value) ?: true
            RecurrenceFrequency.MONTHLY -> master.recurRule.daysOfMonth?.contains(current.dayOfMonth) ?: true
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
        // FIXED: Always increment by 1 day for WEEKLY and MONTHLY to catch all selected days
        current = when (master.recurFreq) {
            RecurrenceFrequency.NONE,
            RecurrenceFrequency.DAILY,
            RecurrenceFrequency.WEEKLY,
            RecurrenceFrequency.MONTHLY -> current.plusDays(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }

    return occurrences
}

// TODO: Merge overlapping TaskBuckets
/* Generate TaskBucketOccurrences from MasterTaskBucket, merge overlapping TaskBuckets */
@RequiresApi(Build.VERSION_CODES.O)
fun generateTaskBucketOccurrences(master: MasterTaskBucket): List<TaskBucketOccurrence> {
    val today = LocalDate.now()
    val occurrences = mutableListOf<TaskBucketOccurrence>()

    val endLimit = master.endDate?.let {
        if (it.isBefore(today.plusMonths(OCCURRENCE_GENERATION_MONTHS.toLong()))) it
        else today.plusMonths(OCCURRENCE_GENERATION_MONTHS.toLong())
    } ?: today.plusMonths(OCCURRENCE_GENERATION_MONTHS.toLong())

    var current = if (master.startDate.isBefore(today)) today else master.startDate

    while (!current.isAfter(endLimit)) {
        // Determine if this date matches the recurrence rule
        val matchesRule = when (master.recurFreq) {
            RecurrenceFrequency.NONE -> current == master.startDate
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> master.recurRule.daysOfWeek?.contains(current.dayOfWeek.value) ?: true
            RecurrenceFrequency.MONTHLY -> master.recurRule.daysOfMonth?.contains(current.dayOfMonth) ?: true
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
        // FIXED: Always increment by 1 day for WEEKLY and MONTHLY to catch all selected days
        current = when (master.recurFreq) {
            RecurrenceFrequency.NONE,
            RecurrenceFrequency.DAILY,
            RecurrenceFrequency.WEEKLY,
            RecurrenceFrequency.MONTHLY -> current.plusDays(1)
            RecurrenceFrequency.YEARLY -> current.plusYears(1)
        }
    }

    return occurrences
}

// TODO: Set up after heuristic algorithm
/* Generate TaskIntervals from MasterTask */

