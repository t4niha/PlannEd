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

    // State variables for all fields
    var selectedType by remember { mutableStateOf("Category") }
    var textValue by remember { mutableStateOf("") }
    var notesValue by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Preset1) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var recurrenceFreq by remember { mutableStateOf(RecurrenceFrequency.NONE) }
    var selectedDaysOfWeek by remember { mutableStateOf(setOf(7)) }
    var selectedDaysOfMonth by remember { mutableStateOf(setOf(1)) }
    var priority by remember { mutableIntStateOf(3) }
    var durationHours by remember { mutableIntStateOf(0) }
    var durationMinutes by remember { mutableIntStateOf(5) }
    var isBreakable by remember { mutableStateOf(false) }
    var isAllDay by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    var selectedEvent by remember { mutableStateOf<Int?>(null) }
    var selectedDeadline by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Type field
        selectedType = typePickerField(
            label = "Type",
            initialType = selectedType
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Title field
        textValue = textInputField(
            label = "Title",
            initialValue = textValue
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Notes field
        notesValue = notesInputField(
            label = "Notes",
            initialValue = notesValue
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Start date picker
        startDate = datePickerField(
            label = "Start Date",
            initialDate = startDate
        )
        Spacer(modifier = Modifier.height(12.dp))

        // End date picker
        endDate = datePickerField(
            label = "End Date",
            initialDate = endDate
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Start time picker
        startTime = timePickerField(
            label = "Start Time",
            initialTime = startTime
        )
        Spacer(modifier = Modifier.height(12.dp))

        // End time picker
        endTime = timePickerField(
            label = "End Time",
            initialTime = endTime
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Recurrence frequency and rule
        val (freq, daysWeek, daysMonth) = recurrencePickerField(
            initialFrequency = recurrenceFreq,
            initialDaysOfWeek = selectedDaysOfWeek,
            initialDaysOfMonth = selectedDaysOfMonth
        )
        recurrenceFreq = freq
        selectedDaysOfWeek = daysWeek
        selectedDaysOfMonth = daysMonth
        Spacer(modifier = Modifier.height(12.dp))

        // Priority selector
        priority = priorityPickerField(
            label = "Priority",
            initialPriority = priority
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Duration picker
        val (hours, minutes) = durationPickerField(
            initialHours = durationHours,
            initialMinutes = durationMinutes
        )
        durationHours = hours
        durationMinutes = minutes
        Spacer(modifier = Modifier.height(12.dp))

        // Breakable checkbox
        isBreakable = checkboxField(
            label = "Breakable",
            initialChecked = isBreakable
        )
        Spacer(modifier = Modifier.height(12.dp))

        // All day checkbox
        isAllDay = checkboxField(
            label = "All Day",
            initialChecked = isAllDay
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Category dropdown
        selectedCategory = dropdownField(
            label = "Category",
            items = listOf("Work", "Personal", "Study", "Fitness"),
            initialSelection = selectedCategory
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Color picker
        selectedColor = colorPickerField(
            label = "Color",
            initialColor = selectedColor
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Event dropdown
        selectedEvent = dropdownField(
            label = "Event",
            items = listOf("Meeting", "Conference", "Workshop", "Deadline Review"),
            initialSelection = selectedEvent
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Deadline dropdown
        selectedDeadline = dropdownField(
            label = "Deadline",
            items = listOf("Project Due", "Assignment", "Report Submission"),
            initialSelection = selectedDeadline
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}