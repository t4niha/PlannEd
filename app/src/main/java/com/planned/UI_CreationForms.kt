package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import androidx.core.graphics.toColorInt

/* CATEGORY FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryForm(
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    color: Color,
    onColorChange: (Color) -> Unit,
    resetTrigger: Int
) {
    Column {
        val titleValue = textInputField(
            label = "Title",
            initialValue = title,
            key = resetTrigger
        )
        onTitleChange(titleValue)
        Spacer(modifier = Modifier.height(12.dp))

        val notesValue = notesInputField(
            label = "Notes",
            initialValue = notes,
            key = resetTrigger
        )
        onNotesChange(notesValue)
        Spacer(modifier = Modifier.height(12.dp))

        val colorValue = colorPickerField(
            label = "Color",
            initialColor = color,
            key = resetTrigger
        )
        onColorChange(colorValue)
    }
}

/* EVENT FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventForm(
    db: AppDatabase,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    color: Color,
    onColorChange: (Color) -> Unit,
    startDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate?,
    startTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit,
    endTime: LocalTime,
    onEndTimeChange: (LocalTime) -> Unit,
    recurrenceFreq: RecurrenceFrequency,
    selectedDaysOfWeek: Set<Int>,
    selectedDaysOfMonth: Set<Int>,
    onRecurrenceChange: (RecurrenceFrequency, Set<Int>, Set<Int>, LocalDate?) -> Unit,
    selectedCategory: Int?,
    onCategoryChange: (Int?) -> Unit,
    resetTrigger: Int
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var previousCategory by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
    }

    // Auto-update color when category changes
    LaunchedEffect(selectedCategory) {
        if (selectedCategory != previousCategory && selectedCategory != null && categories.isNotEmpty()) {
            val categoryColor = categories.getOrNull(selectedCategory)?.color
            if (categoryColor != null) {
                onColorChange(Color(categoryColor.toColorInt()))
            }
        }
        previousCategory = selectedCategory
    }

    Column {
        val titleValue = textInputField(
            label = "Title",
            initialValue = title,
            key = resetTrigger
        )
        onTitleChange(titleValue)
        Spacer(modifier = Modifier.height(12.dp))

        val notesValue = notesInputField(
            label = "Notes",
            initialValue = notes,
            key = resetTrigger
        )
        onNotesChange(notesValue)
        Spacer(modifier = Modifier.height(12.dp))

        val startDateValue = datePickerField(
            label = "Start Date",
            initialDate = startDate,
            isOptional = false,
            key = resetTrigger
        )!!
        onStartDateChange(startDateValue)
        Spacer(modifier = Modifier.height(12.dp))

        val (recurrence, recurrenceEndDate) = recurrencePickerField(
            initialFrequency = recurrenceFreq,
            initialDaysOfWeek = selectedDaysOfWeek,
            initialDaysOfMonth = selectedDaysOfMonth,
            initialEndDate = endDate,
            startDate = startDate,
            key = resetTrigger
        )
        onRecurrenceChange(recurrence.first, recurrence.second, recurrence.third, recurrenceEndDate)
        Spacer(modifier = Modifier.height(12.dp))

        val startTimeValue = timePickerField(
            label = "Start Time",
            initialTime = startTime,
            key = resetTrigger
        )
        onStartTimeChange(startTimeValue)
        Spacer(modifier = Modifier.height(12.dp))

        val endTimeValue = timePickerField(
            label = "End Time",
            initialTime = endTime,
            minTime = startTime,
            key = resetTrigger
        )
        onEndTimeChange(endTimeValue)
        Spacer(modifier = Modifier.height(12.dp))

        val categoryValue = dropdownField(
            label = "Category",
            items = categories.map { it.title },
            initialSelection = selectedCategory,
            key = resetTrigger
        )
        onCategoryChange(categoryValue)
        Spacer(modifier = Modifier.height(12.dp))

        colorPickerField(
            label = "Color",
            initialColor = color,
            key = resetTrigger,
            onColorChange = onColorChange
        )
    }
}


/* DEADLINE FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlineForm(
    db: AppDatabase,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    selectedCategory: Int?,
    onCategoryChange: (Int?) -> Unit,
    selectedEvent: Int?,
    onEventChange: (Int?) -> Unit,
    autoScheduleTask: Boolean,
    onAutoScheduleTaskChange: (Boolean) -> Unit,
    taskPriority: Int,
    onTaskPriorityChange: (Int) -> Unit,
    taskDurationHours: Int,
    taskDurationMinutes: Int,
    onTaskDurationChange: (Int, Int) -> Unit,
    taskIsBreakable: Boolean,
    onTaskIsBreakableChange: (Boolean) -> Unit,
    taskBreakableLockedByDuration: Boolean,
    onTaskBreakableLockedByDurationChange: (Boolean) -> Unit,
    resetTrigger: Int
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var events by remember { mutableStateOf<List<MasterEvent>>(emptyList()) }
    var previousEvent by remember { mutableStateOf<Int?>(null) }
    var maxBucketDuration by remember { mutableIntStateOf(Int.MAX_VALUE) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
        events = EventManager.getAll(db)

        // Get max bucket duration
        maxBucketDuration = getMaxBucketDurationMinutes(db) ?: Int.MAX_VALUE
    }

    // Check if task duration exceeds max bucket duration
    LaunchedEffect(taskDurationHours, taskDurationMinutes, maxBucketDuration, autoScheduleTask) {
        if (autoScheduleTask) {
            val taskDurationMinutesTotal = (taskDurationHours * 60) + taskDurationMinutes
            val needsToBeBreakable = taskDurationMinutesTotal > maxBucketDuration

            onTaskBreakableLockedByDurationChange(needsToBeBreakable)

            if (needsToBeBreakable) {
                onTaskIsBreakableChange(true)
            }
        } else {
            onTaskBreakableLockedByDurationChange(false)
        }
    }

    // Lock category when event is selected, update when event changes
    LaunchedEffect(selectedEvent, events.size) {
        if (selectedEvent != previousEvent) {
            if (selectedEvent != null && events.isNotEmpty()) {
                val event = events.getOrNull(selectedEvent)
                if (event != null) {
                    val eventCategoryId = event.categoryId
                    val categoryIndex = if (eventCategoryId != null) {
                        categories.indexOfFirst { it.id == eventCategoryId }
                    } else {
                        null
                    }
                    if (categoryIndex != null) {
                        onCategoryChange(if (categoryIndex >= 0) categoryIndex else null)
                    }
                }
            }
            previousEvent = selectedEvent
        }
    }

    Column {
        val titleValue = textInputField(
            label = "Title",
            initialValue = title,
            key = resetTrigger
        )
        onTitleChange(titleValue)
        Spacer(modifier = Modifier.height(12.dp))

        val notesValue = notesInputField(
            label = "Notes",
            initialValue = notes,
            key = resetTrigger
        )
        onNotesChange(notesValue)
        Spacer(modifier = Modifier.height(12.dp))

        val dateValue = datePickerField(
            label = "Date",
            initialDate = date,
            isOptional = false,
            key = resetTrigger
        )!!
        onDateChange(dateValue)
        Spacer(modifier = Modifier.height(12.dp))

        val timeValue = timePickerField(
            label = "Time",
            initialTime = time,
            key = resetTrigger
        )
        onTimeChange(timeValue)
        Spacer(modifier = Modifier.height(12.dp))

        // Auto schedule task checkbox with expandable fields
        val (autoSchedule, priority, durationHours, durationMinutes, breakable) = autoScheduleTaskPickerField(
            initialAutoScheduleTask = autoScheduleTask,
            initialPriority = taskPriority,
            initialDurationHours = taskDurationHours,
            initialDurationMinutes = taskDurationMinutes,
            initialBreakable = taskIsBreakable,
            breakableLockedByDuration = taskBreakableLockedByDuration,
            key = resetTrigger
        )
        onAutoScheduleTaskChange(autoSchedule)
        onTaskPriorityChange(priority)
        onTaskDurationChange(durationHours, durationMinutes)

        // Only update breakable if not locked by duration
        if (!taskBreakableLockedByDuration) {
            onTaskIsBreakableChange(breakable)
        }

        Spacer(modifier = Modifier.height(12.dp))

        val eventValue = dropdownField(
            label = "Event",
            items = events.map { it.title },
            initialSelection = selectedEvent,
            key = resetTrigger
        )
        onEventChange(eventValue)
        Spacer(modifier = Modifier.height(12.dp))

        // Category locked when Event is selected
        val categoryValue = dropdownField(
            label = "Category",
            items = categories.map { it.title },
            initialSelection = selectedCategory,
            key = resetTrigger,
            locked = selectedEvent != null
        )
        // Only allow manual changes when not locked
        if (selectedEvent == null) {
            onCategoryChange(categoryValue)
        }
    }
}

/* TASK BUCKET FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskBucketForm(
    db: AppDatabase,
    startDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate?,
    startTime: LocalTime,
    onStartTimeChange: (LocalTime) -> Unit,
    endTime: LocalTime,
    onEndTimeChange: (LocalTime) -> Unit,
    recurrenceFreq: RecurrenceFrequency,
    selectedDaysOfWeek: Set<Int>,
    selectedDaysOfMonth: Set<Int>,
    onRecurrenceChange: (RecurrenceFrequency, Set<Int>, Set<Int>, LocalDate?) -> Unit,
    resetTrigger: Int
) {
    Column {
        val startDateValue = datePickerField(
            label = "Start Date",
            initialDate = startDate,
            isOptional = false,
            key = resetTrigger
        )!!
        onStartDateChange(startDateValue)
        Spacer(modifier = Modifier.height(12.dp))

        val (recurrence, recurrenceEndDate) = recurrencePickerField(
            initialFrequency = recurrenceFreq,
            initialDaysOfWeek = selectedDaysOfWeek,
            initialDaysOfMonth = selectedDaysOfMonth,
            initialEndDate = endDate,
            startDate = startDate,
            key = resetTrigger
        )
        onRecurrenceChange(recurrence.first, recurrence.second, recurrence.third, recurrenceEndDate)
        Spacer(modifier = Modifier.height(12.dp))

        val startTimeValue = timePickerField(
            label = "Start Time",
            initialTime = startTime,
            key = resetTrigger
        )
        onStartTimeChange(startTimeValue)
        Spacer(modifier = Modifier.height(12.dp))

        val endTimeValue = timePickerField(
            label = "End Time",
            initialTime = endTime,
            minTime = startTime,
            key = resetTrigger
        )
        onEndTimeChange(endTimeValue)
    }
}

/* TASK FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskForm(
    db: AppDatabase,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    isBreakable: Boolean,
    onBreakableChange: (Boolean) -> Unit,
    isAutoSchedule: Boolean,
    startDate: LocalDate?,
    startTime: LocalTime?,
    onScheduleChange: (Boolean, LocalDate?, LocalTime?) -> Unit,
    durationHours: Int,
    durationMinutes: Int,
    onDurationChange: (Int, Int) -> Unit,
    selectedCategory: Int?,
    onCategoryChange: (Int?) -> Unit,
    selectedEvent: Int?,
    onEventChange: (Int?) -> Unit,
    selectedDeadline: Int?,
    onDeadlineChange: (Int?) -> Unit,
    breakableLockedByDuration: Boolean,
    onBreakableLockedByDurationChange: (Boolean) -> Unit,
    resetTrigger: Int
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var events by remember { mutableStateOf<List<MasterEvent>>(emptyList()) }
    var deadlines by remember { mutableStateOf<List<Deadline>>(emptyList()) }
    var previousDeadline by remember { mutableStateOf<Int?>(null) }
    var previousEvent by remember { mutableStateOf<Int?>(null) }
    var maxBucketDuration by remember { mutableIntStateOf(Int.MAX_VALUE) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
        events = EventManager.getAll(db)
        deadlines = DeadlineManager.getAll(db)

        // Get max bucket duration
        maxBucketDuration = getMaxBucketDurationMinutes(db) ?: Int.MAX_VALUE
    }

    // Check if task duration exceeds max bucket duration
    LaunchedEffect(durationHours, durationMinutes, maxBucketDuration, isAutoSchedule) {
        if (isAutoSchedule) {
            val taskDurationMinutes = (durationHours * 60) + durationMinutes
            val needsToBeBreakable = taskDurationMinutes > maxBucketDuration

            onBreakableLockedByDurationChange(needsToBeBreakable)

            if (needsToBeBreakable) {
                onBreakableChange(true)
            }
        } else {
            onBreakableLockedByDurationChange(false)
        }
    }

    // Limit duration when manually scheduled to prevent rollover
    LaunchedEffect(durationHours, durationMinutes, startTime, isAutoSchedule) {
        if (!isAutoSchedule && startTime != null) {
            val taskDurationMinutes = (durationHours * 60) + durationMinutes
            val endTime = startTime.plusMinutes(taskDurationMinutes.toLong())

            if (endTime.isBefore(startTime) || endTime == LocalTime.MIDNIGHT) {
                val minutesUntilMidnight = (23 - startTime.hour) * 60 + (59 - startTime.minute)

                val newHours = minutesUntilMidnight / 60
                val newMinutes = minutesUntilMidnight % 60

                onDurationChange(newHours, newMinutes)
            }
        }
    }

    LaunchedEffect(selectedDeadline, deadlines.size, events.size) {
        if (selectedDeadline != previousDeadline) {
            if (selectedDeadline != null && deadlines.isNotEmpty()) {
                val deadline = deadlines.getOrNull(selectedDeadline)
                if (deadline != null) {
                    // Lock event to deadline's event
                    val deadlineEventId = deadline.eventId
                    val eventIndex = if (deadlineEventId != null) {
                        events.indexOfFirst { it.id == deadlineEventId }
                    } else {
                        null
                    }
                    if (eventIndex != null) {
                        onEventChange(if (eventIndex >= 0) eventIndex else null)
                    }

                    // Lock category to that event's category
                    if (eventIndex != null && eventIndex >= 0) {
                        val event = events[eventIndex]
                        val eventCategoryId = event.categoryId
                        val categoryIndex = if (eventCategoryId != null) {
                            categories.indexOfFirst { it.id == eventCategoryId }
                        } else {
                            null
                        }
                        if (categoryIndex != null) {
                            onCategoryChange(if (categoryIndex >= 0) categoryIndex else null)
                        }
                    } else {
                        // Lock category to deadline's category
                        val deadlineCategoryId = deadline.categoryId
                        val categoryIndex = if (deadlineCategoryId != null) {
                            categories.indexOfFirst { it.id == deadlineCategoryId }
                        } else {
                            null
                        }
                        if (categoryIndex != null) {
                            onCategoryChange(if (categoryIndex >= 0) categoryIndex else null)
                        }
                    }
                }
            }
            previousDeadline = selectedDeadline
        }
    }

    // Lock category when event is selected (deadline not selected)
    LaunchedEffect(selectedEvent, events.size) {
        if (selectedDeadline == null && selectedEvent != previousEvent) {
            if (selectedEvent != null && events.isNotEmpty()) {
                val event = events.getOrNull(selectedEvent)
                if (event != null) {
                    val eventCategoryId = event.categoryId
                    val categoryIndex = if (eventCategoryId != null) {
                        categories.indexOfFirst { it.id == eventCategoryId }
                    } else {
                        null
                    }
                    if (categoryIndex != null) {
                        onCategoryChange(if (categoryIndex >= 0) categoryIndex else null)
                    }
                }
            }
            previousEvent = selectedEvent
        }
    }

    Column {
        val titleValue = textInputField(
            label = "Title",
            initialValue = title,
            key = resetTrigger
        )
        onTitleChange(titleValue)
        Spacer(modifier = Modifier.height(12.dp))

        val notesValue = notesInputField(
            label = "Notes",
            initialValue = notes,
            key = resetTrigger
        )
        onNotesChange(notesValue)
        Spacer(modifier = Modifier.height(12.dp))

        val priorityValue = priorityPickerField(
            label = "Priority",
            initialPriority = priority,
            key = resetTrigger
        )
        onPriorityChange(priorityValue)
        Spacer(modifier = Modifier.height(12.dp))

        val (autoSchedule, date, time) = schedulePickerField(
            initialAutoSchedule = isAutoSchedule,
            initialDate = startDate,
            initialTime = startTime,
            key = resetTrigger
        )
        onScheduleChange(autoSchedule, date, time)
        Spacer(modifier = Modifier.height(12.dp))

        val (hours, minutes) = durationPickerField(
            initialHours = durationHours,
            initialMinutes = durationMinutes,
            key = resetTrigger,
            label = "Duration"
        )
        onDurationChange(hours, minutes)
        Spacer(modifier = Modifier.height(12.dp))

        val breakableValue = checkboxField(
            label = "Breakable",
            initialChecked = isBreakable,
            key = resetTrigger,
            locked = !isAutoSchedule || breakableLockedByDuration,
            forceChecked = breakableLockedByDuration
        )
        // Breakable field with duration-based locking
        if (isAutoSchedule && !breakableLockedByDuration) {
            onBreakableChange(breakableValue)
        } else if (!isAutoSchedule) {
            onBreakableChange(false)
        }
        Spacer(modifier = Modifier.height(12.dp))

        val deadlineValue = dropdownField(
            label = "Deadline",
            items = deadlines.map { it.title },
            initialSelection = selectedDeadline,
            key = resetTrigger
        )
        onDeadlineChange(deadlineValue)
        Spacer(modifier = Modifier.height(12.dp))

        val eventValue = dropdownField(
            label = "Event",
            items = events.map { it.title },
            initialSelection = selectedEvent,
            key = resetTrigger,
            locked = selectedDeadline != null
        )
        // Only allow manual changes when not locked
        if (selectedDeadline == null) {
            onEventChange(eventValue)
        }
        Spacer(modifier = Modifier.height(12.dp))

        val categoryValue = dropdownField(
            label = "Category",
            items = categories.map { it.title },
            initialSelection = selectedCategory,
            key = resetTrigger,
            locked = selectedEvent != null || selectedDeadline != null
        )
        // Only allow manual changes when not locked
        if (selectedEvent == null && selectedDeadline == null) {
            onCategoryChange(categoryValue)
        }
    }
}

/* REMINDER FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReminderForm(
    db: AppDatabase,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    color: Color,
    onColorChange: (Color) -> Unit,
    startDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate?,
    isAllDay: Boolean,
    time: LocalTime,
    onAllDayTimeChange: (Boolean, LocalTime) -> Unit,
    recurrenceFreq: RecurrenceFrequency,
    selectedDaysOfWeek: Set<Int>,
    selectedDaysOfMonth: Set<Int>,
    onRecurrenceChange: (RecurrenceFrequency, Set<Int>, Set<Int>, LocalDate?) -> Unit,
    selectedCategory: Int?,
    onCategoryChange: (Int?) -> Unit,
    resetTrigger: Int
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var previousCategory by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
    }

    // Auto-update color when category changes
    LaunchedEffect(selectedCategory) {
        if (selectedCategory != previousCategory && selectedCategory != null && categories.isNotEmpty()) {
            val categoryColor = categories.getOrNull(selectedCategory)?.color
            if (categoryColor != null) {
                onColorChange(Color(categoryColor.toColorInt()))
            }
        }
        previousCategory = selectedCategory
    }

    Column {
        val titleValue = textInputField(
            label = "Title",
            initialValue = title,
            key = resetTrigger
        )
        onTitleChange(titleValue)
        Spacer(modifier = Modifier.height(12.dp))

        val notesValue = notesInputField(
            label = "Notes",
            initialValue = notes,
            key = resetTrigger
        )
        onNotesChange(notesValue)
        Spacer(modifier = Modifier.height(12.dp))

        val startDateValue = datePickerField(
            label = "Start Date",
            initialDate = startDate,
            isOptional = false,
            key = resetTrigger
        )!!
        onStartDateChange(startDateValue)
        Spacer(modifier = Modifier.height(12.dp))

        val (recurrence, recurrenceEndDate) = recurrencePickerField(
            initialFrequency = recurrenceFreq,
            initialDaysOfWeek = selectedDaysOfWeek,
            initialDaysOfMonth = selectedDaysOfMonth,
            initialEndDate = endDate,
            startDate = startDate,
            key = resetTrigger
        )
        onRecurrenceChange(recurrence.first, recurrence.second, recurrence.third, recurrenceEndDate)
        Spacer(modifier = Modifier.height(12.dp))

        val (allDay, selectedTime) = allDayPickerField(
            initialAllDay = isAllDay,
            initialTime = time,
            key = resetTrigger
        )
        onAllDayTimeChange(allDay, selectedTime)
        Spacer(modifier = Modifier.height(12.dp))

        val categoryValue = dropdownField(
            label = "Category",
            items = categories.map { it.title },
            initialSelection = selectedCategory,
            key = resetTrigger
        )
        onCategoryChange(categoryValue)
        Spacer(modifier = Modifier.height(12.dp))

        colorPickerField(
            label = "Color",
            initialColor = color,
            key = resetTrigger,
            onColorChange = onColorChange
        )
    }
}