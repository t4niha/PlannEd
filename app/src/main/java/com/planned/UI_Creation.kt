package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime

/* CREATION DETAILS */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Creation() {
    val scrollState = rememberScrollState()
    var selectedType by remember { mutableStateOf("Task") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Type field
        selectedType = typePickerField(
            initialType = selectedType
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Display appropriate form based on selected type
        when (selectedType) {
            "Task" -> TaskForm()
            "Reminder" -> ReminderForm()
            "Deadline" -> DeadlineForm()
            "Event" -> EventForm()
            "Task Bucket" -> TaskBucketForm()
            "Category" -> CategoryForm()
        }
    }
}

/* CATEGORY FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryForm() {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(Preset1) }

    Column {
        // Title field (required)
        title = textInputField(
            label = "Title",
            initialValue = title
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Notes field (optional)
        notes = notesInputField(
            label = "Notes",
            initialValue = notes
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Color picker (required)
        color = colorPickerField(
            label = "Color",
            initialColor = color
        )
    }
}

/* EVENT FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventForm() {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(Preset1) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var endTime by remember { mutableStateOf(LocalTime.now().plusHours(1)) }
    var recurrenceFreq by remember { mutableStateOf(RecurrenceFrequency.NONE) }
    var selectedDaysOfWeek by remember { mutableStateOf(setOf(7)) }
    var selectedDaysOfMonth by remember { mutableStateOf(setOf(1)) }
    var selectedCategory by remember { mutableStateOf<Int?>(null) }

    Column {
        // Title field (required)
        title = textInputField(
            label = "Title",
            initialValue = title
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Notes field (optional)
        notes = notesInputField(
            label = "Notes",
            initialValue = notes
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Start date picker (required)
        startDate = datePickerField(
            label = "Start Date",
            initialDate = startDate,
            isOptional = false
        )!!
        Spacer(modifier = Modifier.height(12.dp))

        // Recurrence frequency and rule with end date
        val (recurrence, recurrenceEndDate) = recurrencePickerField(
            initialFrequency = recurrenceFreq,
            initialDaysOfWeek = selectedDaysOfWeek,
            initialDaysOfMonth = selectedDaysOfMonth,
            initialEndDate = endDate,
            startDate = startDate
        )
        recurrenceFreq = recurrence.first
        selectedDaysOfWeek = recurrence.second
        selectedDaysOfMonth = recurrence.third
        endDate = recurrenceEndDate
        Spacer(modifier = Modifier.height(12.dp))

        // Start time picker (required)
        startTime = timePickerField(
            label = "Start Time",
            initialTime = startTime
        )
        Spacer(modifier = Modifier.height(12.dp))

        // End time picker (required)
        endTime = timePickerField(
            label = "End Time",
            initialTime = endTime
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Category dropdown (optional)
        selectedCategory = dropdownField(
            label = "Category",
            items = listOf("Work", "Personal", "Study", "Fitness"), // TODO: Load from DB
            initialSelection = selectedCategory
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Color picker (optional)
        color = colorPickerField(
            label = "Color",
            initialColor = color
        )
    }
}

/* DEADLINE FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlineForm() {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.of(23, 59)) }
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    var selectedEvent by remember { mutableStateOf<Int?>(null) }

    Column {
        // Title field (required)
        title = textInputField(
            label = "Title",
            initialValue = title
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Notes field (optional)
        notes = notesInputField(
            label = "Notes",
            initialValue = notes
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Date picker (required)
        date = datePickerField(
            label = "Date",
            initialDate = date,
            isOptional = false
        )!!
        Spacer(modifier = Modifier.height(12.dp))

        // Time picker (required)
        time = timePickerField(
            label = "Time",
            initialTime = time
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Category dropdown (optional)
        selectedCategory = dropdownField(
            label = "Category",
            items = listOf("Work", "Personal", "Study", "Fitness"), // TODO: Load from DB
            initialSelection = selectedCategory
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Event dropdown (optional)
        selectedEvent = dropdownField(
            label = "Event",
            items = listOf("Meeting", "Conference", "Workshop"), // TODO: Load from DB
            initialSelection = selectedEvent
        )
    }
}

/* TASK BUCKET FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskBucketForm() {
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var startTime by remember { mutableStateOf(LocalTime.now()) }
    var endTime by remember { mutableStateOf(LocalTime.now().plusHours(8)) }
    var recurrenceFreq by remember { mutableStateOf(RecurrenceFrequency.NONE) }
    var selectedDaysOfWeek by remember { mutableStateOf(setOf(7)) }
    var selectedDaysOfMonth by remember { mutableStateOf(setOf(1)) }

    Column {
        // Start date picker (required)
        startDate = datePickerField(
            label = "Start Date",
            initialDate = startDate,
            isOptional = false
        )!!
        Spacer(modifier = Modifier.height(12.dp))

        // Recurrence frequency and rule (required)
        val (recurrence, recurrenceEndDate) = recurrencePickerField(
            initialFrequency = recurrenceFreq,
            initialDaysOfWeek = selectedDaysOfWeek,
            initialDaysOfMonth = selectedDaysOfMonth,
            initialEndDate = endDate,
            startDate = startDate
        )
        recurrenceFreq = recurrence.first
        selectedDaysOfWeek = recurrence.second
        selectedDaysOfMonth = recurrence.third
        endDate = recurrenceEndDate
        Spacer(modifier = Modifier.height(12.dp))

        // Start time picker (required)
        startTime = timePickerField(
            label = "Start Time",
            initialTime = startTime
        )
        Spacer(modifier = Modifier.height(12.dp))

        // End time picker (required)
        endTime = timePickerField(
            label = "End Time",
            initialTime = endTime
        )
    }
}

/* TASK FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskForm() {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(3) }
    var isBreakable by remember { mutableStateOf(false) }
    var isAutoSchedule by remember { mutableStateOf(true) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var startTime by remember { mutableStateOf<LocalTime?>(null) }
    var durationHours by remember { mutableIntStateOf(1) }
    var durationMinutes by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    var selectedEvent by remember { mutableStateOf<Int?>(null) }
    var selectedDeadline by remember { mutableStateOf<Int?>(null) }

    Column {
        // Title field (required)
        title = textInputField(
            label = "Title",
            initialValue = title
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Notes field (optional)
        notes = notesInputField(
            label = "Notes",
            initialValue = notes
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Priority selector (required, default 3)
        priority = priorityPickerField(
            label = "Priority",
            initialPriority = priority
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Duration picker (required - predictedDuration)
        val (hours, minutes) = durationPickerField(
            initialHours = durationHours,
            initialMinutes = durationMinutes
        )
        durationHours = hours
        durationMinutes = minutes
        Spacer(modifier = Modifier.height(12.dp))

        // Breakable checkbox (optional, default false)
        isBreakable = checkboxField(
            label = "Breakable",
            initialChecked = isBreakable
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Schedule picker (Auto Schedule with expandable manual fields)
        val (autoSchedule, date, time) = schedulePickerField(
            initialAutoSchedule = isAutoSchedule,
            initialDate = startDate,
            initialTime = startTime
        )
        isAutoSchedule = autoSchedule
        startDate = date
        startTime = time
        Spacer(modifier = Modifier.height(12.dp))

        // Category dropdown (optional)
        selectedCategory = dropdownField(
            label = "Category",
            items = listOf("Work", "Personal", "Study", "Fitness"), // TODO: Load from DB
            initialSelection = selectedCategory
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Event dropdown (optional)
        selectedEvent = dropdownField(
            label = "Event",
            items = listOf("Meeting", "Conference", "Workshop"), // TODO: Load from DB
            initialSelection = selectedEvent
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Deadline dropdown (optional)
        selectedDeadline = dropdownField(
            label = "Deadline",
            items = listOf("Project Due", "Assignment", "Report Submission"), // TODO: Load from DB
            initialSelection = selectedDeadline
        )
    }
}

/* REMINDER FORM */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReminderForm() {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(Preset1) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var isAllDay by remember { mutableStateOf(true) }
    var time by remember { mutableStateOf(LocalTime.now()) }

    Column {
        // Title field (required)
        title = textInputField(
            label = "Title",
            initialValue = title
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Notes field (optional)
        notes = notesInputField(
            label = "Notes",
            initialValue = notes
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Date picker (required)
        date = datePickerField(
            label = "Date",
            initialDate = date,
            isOptional = false
        )!!
        Spacer(modifier = Modifier.height(12.dp))

        // All day checkbox with expandable time picker
        val (allDay, selectedTime) = allDayPickerField(
            initialAllDay = isAllDay,
            initialTime = time
        )
        isAllDay = allDay
        time = selectedTime
        Spacer(modifier = Modifier.height(12.dp))

        // Color picker (required)
        color = colorPickerField(
            label = "Color",
            initialColor = color
        )
    }
}