package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/* TYPE PICKER FIELD */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun typePickerField(
    initialType: String = "Task",
    key: Int = 0
): String {
    var selectedType by remember(key) { mutableStateOf(initialType) }
    var showTypePicker by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, initialType) {
        selectedType = initialType
    }

    val types = listOf("Task", "Reminder", "Deadline", "Event", "Task Bucket", "Category")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showTypePicker = !showTypePicker }
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp)
    ) {
        Column {
            // Display selected type
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(3.dp, PrimaryColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(selectedType)
            }

            // Type options (all visible when expanded)
            AnimatedVisibility(
                visible = showTypePicker,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    types.forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelected) PrimaryColor else Color.LightGray,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedType = type
                                    showTypePicker = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                type,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    return selectedType
}

/* TEXT INPUT FIELD */
@Composable
fun textInputField(
    label: String,
    initialValue: String = "",
    key: Int = 0
): String {
    var textValue by remember(key) { mutableStateOf(initialValue) }

    LaunchedEffect(key, initialValue) {
        textValue = initialValue
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }

    return textValue
}

/* NOTES INPUT FIELD */
@Composable
fun notesInputField(
    label: String,
    initialValue: String = "",
    key: Int = 0
): String {
    var notesValue by remember(key) { mutableStateOf(initialValue) }

    LaunchedEffect(key, initialValue) {
        notesValue = initialValue
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = notesValue,
                onValueChange = { notesValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 4
            )
        }
    }

    return notesValue
}

/* COLOR PICKER FIELD */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun colorPickerField(
    label: String,
    initialColor: Color = Preset1,
    key: Int = 0
): Color {
    var selectedColor by remember(key) { mutableStateOf(initialColor) }
    var showColorPicker by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, initialColor) {
        selectedColor = initialColor
    }

    val colorPresets = listOf(
        Preset1, Preset2, Preset3, Preset4,
        Preset5, Preset6, Preset7, Preset8,
        Preset9, Preset10, Preset11, Preset12
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showColorPicker = !showColorPicker }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                )
            }

            // Expandable color picker grid
            AnimatedVisibility(
                visible = showColorPicker,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .height(180.dp)
                ) {
                    items(colorPresets.size) { i ->
                        val c = colorPresets[i]
                        val isSelected = c == selectedColor
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(45.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, PrimaryColor, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clip(CircleShape)
                                .background(c)
                                .clickable {
                                    selectedColor = c
                                    showColorPicker = false
                                }
                        )
                    }
                }
            }
        }
    }

    return selectedColor
}

/* DATE PICKER FIELD */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun datePickerField(
    label: String,
    initialDate: LocalDate? = LocalDate.now(),
    isOptional: Boolean = false,
    key: Int = 0
): LocalDate? {
    var selectedDate by remember(key) { mutableStateOf(initialDate ?: LocalDate.now()) }
    var showDatePicker by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, initialDate) {
        selectedDate = initialDate ?: LocalDate.now()
    }

    // Format date as "Nov 19, 2025"
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val displayDate = if (isOptional && initialDate == null) "None" else selectedDate.format(dateFormatter)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { showDatePicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text(displayDate)
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000L
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = LocalDate.ofEpochDay(it / 86400000L)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    return if (isOptional && initialDate == null) null else selectedDate
}

/* TIME PICKER FIELD */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun timePickerField(
    label: String,
    initialTime: LocalTime = LocalTime.now(),
    minTime: LocalTime? = null,
    key: Int = 0
): LocalTime {
    var selectedTime by remember(key) { mutableStateOf(initialTime) }
    var showTimePicker by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, initialTime) {
        selectedTime = initialTime
    }

    // Enforce minimum time if provided
    LaunchedEffect(minTime) {
        if (minTime != null && selectedTime.isBefore(minTime)) {
            selectedTime = minTime.plusMinutes(5).withSecond(0).withNano(0)
        }
    }

    // Format time as "10:00 AM"
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val displayTime = selectedTime.format(timeFormatter)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { showTimePicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text(displayTime)
            }
        }

        // Time picker dialog
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = selectedTime.hour,
                initialMinute = selectedTime.minute,
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        var newTime = LocalTime.of(timePickerState.hour, timePickerState.minute, 0, 0)

                        // Enforce minimum time if provided
                        if (minTime != null && newTime.isBefore(minTime)) {
                            newTime = minTime.plusMinutes(5).withSecond(0).withNano(0)
                        }

                        selectedTime = newTime
                        showTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    TimePicker(state = timePickerState)
                }
            )
        }
    }

    return selectedTime
}

/* RECURRENCE PICKER FIELD */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun recurrencePickerField(
    initialFrequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    initialDaysOfWeek: Set<Int> = setOf(7),
    initialDaysOfMonth: Set<Int> = setOf(1),
    initialEndDate: LocalDate? = null,
    startDate: LocalDate = LocalDate.now(),
    key: Int = 0
): Pair<Triple<RecurrenceFrequency, Set<Int>, Set<Int>>, LocalDate?> {
    var recurrenceFreq by remember(key) { mutableStateOf(initialFrequency) }
    var showRecurrenceDropdown by remember(key) { mutableStateOf(false) }
    var selectedDaysOfWeek by remember(key) { mutableStateOf(initialDaysOfWeek) }
    var selectedDaysOfMonth by remember(key) { mutableStateOf(initialDaysOfMonth) }
    var repeatForever by remember(key) { mutableStateOf(initialEndDate == null) }
    var endDate by remember(key) { mutableStateOf(initialEndDate) }

    LaunchedEffect(key, initialFrequency, initialDaysOfWeek, initialDaysOfMonth, initialEndDate) {
        recurrenceFreq = initialFrequency
        selectedDaysOfWeek = initialDaysOfWeek
        selectedDaysOfMonth = initialDaysOfMonth
        repeatForever = initialEndDate == null
        endDate = initialEndDate
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showRecurrenceDropdown = !showRecurrenceDropdown }
            .padding(16.dp)
    ) {
        Column {
            Text("Recurrence", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            // Display current recurrence frequency
            val recurrenceText = when (recurrenceFreq) {
                RecurrenceFrequency.NONE -> "Don't Repeat"
                RecurrenceFrequency.DAILY -> "Daily"
                RecurrenceFrequency.WEEKLY -> "Weekly"
                RecurrenceFrequency.MONTHLY -> "Monthly"
                RecurrenceFrequency.YEARLY -> "Yearly"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(recurrenceText)
            }

            // Dropdown for frequency selection
            AnimatedVisibility(
                visible = showRecurrenceDropdown,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    listOf(
                        RecurrenceFrequency.NONE to "Don't Repeat",
                        RecurrenceFrequency.DAILY to "Daily",
                        RecurrenceFrequency.WEEKLY to "Weekly",
                        RecurrenceFrequency.MONTHLY to "Monthly",
                        RecurrenceFrequency.YEARLY to "Yearly"
                    ).forEach { (freq, label) ->
                        val isSelected = recurrenceFreq == freq
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelected) PrimaryColor else Color.LightGray,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    recurrenceFreq = freq
                                    showRecurrenceDropdown = false
                                    // Reset end date when switching to "Don't Repeat"
                                    if (freq == RecurrenceFrequency.NONE) {
                                        endDate = null
                                        repeatForever = true
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Weekly recurrence rule - days of week selector
            if (recurrenceFreq == RecurrenceFrequency.WEEKLY) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                    days.forEachIndexed { index, day ->
                        val dayNum = if (index == 0) 7 else index
                        val isSelected = selectedDaysOfWeek.contains(dayNum)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryColor else Color.LightGray)
                                .clickable {
                                    selectedDaysOfWeek = if (isSelected && selectedDaysOfWeek.size > 1) {
                                        selectedDaysOfWeek - dayNum
                                    } else if (!isSelected) {
                                        val newSet = selectedDaysOfWeek + dayNum
                                        if (newSet.size == 7) {
                                            recurrenceFreq = RecurrenceFrequency.DAILY
                                        }
                                        newSet
                                    } else {
                                        selectedDaysOfWeek
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Monthly recurrence rule - days of month selector
            if (recurrenceFreq == RecurrenceFrequency.MONTHLY) {
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(31) { index ->
                        val dayNum = index + 1
                        val isSelected = selectedDaysOfMonth.contains(dayNum)

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(35.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryColor else Color.LightGray)
                                .clickable {
                                    selectedDaysOfMonth = if (isSelected && selectedDaysOfMonth.size > 1) {
                                        selectedDaysOfMonth - dayNum
                                    } else if (!isSelected) {
                                        val newSet = selectedDaysOfMonth + dayNum
                                        if (newSet.size == 31) {
                                            recurrenceFreq = RecurrenceFrequency.DAILY
                                        }
                                        newSet
                                    } else {
                                        selectedDaysOfMonth
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dayNum.toString(),
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // End date options (only if recurrence is NOT "Don't Repeat")
            if (recurrenceFreq != RecurrenceFrequency.NONE) {
                Spacer(modifier = Modifier.height(16.dp))

                // "Repeat Forever" checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = repeatForever,
                        onCheckedChange = {
                            repeatForever = it
                            if (it) {
                                endDate = null
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Repeat Forever", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                // End date picker (only visible when "Repeat Forever" is unchecked)
                AnimatedVisibility(
                    visible = !repeatForever,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("End Date", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(16.dp))

                            var showEndDatePicker by remember { mutableStateOf(false) }
                            val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

                            // Set default to startDate if endDate is null
                            val displayEndDate = endDate ?: startDate
                            val displayDate = displayEndDate.format(dateFormatter)

                            Button(
                                onClick = { showEndDatePicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                            ) {
                                Text(displayDate)
                            }

                            if (showEndDatePicker) {
                                val datePickerState = rememberDatePickerState(
                                    initialSelectedDateMillis = (endDate ?: startDate).toEpochDay() * 86400000L
                                )
                                DatePickerDialog(
                                    onDismissRequest = { showEndDatePicker = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            datePickerState.selectedDateMillis?.let {
                                                val selectedDate = LocalDate.ofEpochDay(it / 86400000L)
                                                // Ensure end date is not before start date
                                                endDate = if (selectedDate.isBefore(startDate)) {
                                                    startDate
                                                } else {
                                                    selectedDate
                                                }
                                            }
                                            showEndDatePicker = false
                                        }) {
                                            Text("OK")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showEndDatePicker = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                ) {
                                    DatePicker(state = datePickerState)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    return Pair(Triple(recurrenceFreq, selectedDaysOfWeek, selectedDaysOfMonth), endDate)
}

/* PRIORITY PICKER FIELD */
@Composable
fun priorityPickerField(
    label: String,
    initialPriority: Int = 3,
    key: Int = 0
): Int {
    var priority by remember(key) { mutableIntStateOf(initialPriority) }

    LaunchedEffect(key, initialPriority) {
        priority = initialPriority
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Priority colors: Red (1) -> Orange (2) -> Yellow (3) -> Lime (4) -> Green (5)
                val priorityColors = listOf(
                    Preset25, // Red
                    Preset26, // Orange
                    Preset27, // Yellow
                    Preset28, // Lime
                    Preset29  // Green
                )

                (1..5).forEach { p ->
                    val isSelected = priority == p
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, PrimaryColor, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clip(CircleShape)
                            .background(priorityColors[p - 1])
                            .clickable { priority = p },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.toString(),
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }

    return priority
}

/* DURATION PICKER FIELD */
@Composable
fun durationPickerField(
    initialHours: Int = 1,
    initialMinutes: Int = 0,
    key: Int = 0
): Pair<Int, Int> {
    var durationHours by remember(key) { mutableIntStateOf(initialHours) }
    var durationMinutes by remember(key) { mutableIntStateOf(initialMinutes) }
    var showDurationPicker by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, initialHours, initialMinutes) {
        durationHours = initialHours
        durationMinutes = initialMinutes
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Duration", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { showDurationPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("${durationHours}h ${durationMinutes}m")
            }
        }

        // Duration picker dialog
        if (showDurationPicker) {
            AlertDialog(
                onDismissRequest = { showDurationPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDurationPicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDurationPicker = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Hours picker
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hours", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Increment button
                            Button(
                                onClick = { durationHours++ },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                            ) {
                                Text("▲")
                            }

                            Text(
                                durationHours.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Decrement button
                            Button(
                                onClick = {
                                    if (durationHours > 0) {
                                        durationHours--
                                        // Prevent 0h 0m
                                        if (durationHours == 0 && durationMinutes == 0) {
                                            durationMinutes = 5
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                            ) {
                                Text("▼")
                            }
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        // Minutes picker (increments of 5)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Minutes", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Increment button
                            Button(
                                onClick = {
                                    durationMinutes = (durationMinutes + 5) % 60
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                            ) {
                                Text("▲")
                            }

                            Text(
                                durationMinutes.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Decrement button
                            Button(
                                onClick = {
                                    durationMinutes = if (durationMinutes - 5 < 0) 55 else durationMinutes - 5
                                    // Prevent 0h 0m
                                    if (durationHours == 0 && durationMinutes == 0) {
                                        durationMinutes = 5
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                            ) {
                                Text("▼")
                            }
                        }
                    }
                }
            )
        }
    }

    return Pair(durationHours, durationMinutes)
}

/* SCHEDULE PICKER FIELD */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun schedulePickerField(
    initialAutoSchedule: Boolean = true,
    initialDate: LocalDate? = null,
    initialTime: LocalTime? = null,
    key: Int = 0
): Triple<Boolean, LocalDate?, LocalTime?> {
    var isAutoSchedule by remember(key) { mutableStateOf(initialAutoSchedule) }
    var startDate by remember(key) { mutableStateOf(initialDate) }
    var startTime by remember(key) { mutableStateOf(initialTime) }

    LaunchedEffect(key, initialAutoSchedule, initialDate, initialTime) {
        isAutoSchedule = initialAutoSchedule
        startDate = initialDate
        startTime = initialTime
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isAutoSchedule,
                    onCheckedChange = {
                        isAutoSchedule = it
                        if (it) {
                            // Reset manual fields when switching to auto
                            startDate = null
                            startTime = null
                        } else {
                            // Set defaults when switching to manual
                            if (startDate == null) startDate = LocalDate.now()
                            if (startTime == null) startTime = LocalTime.now().withSecond(0).withNano(0)
                        }
                    },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Auto Schedule", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            // Manual schedule fields (only visible when checkbox is unchecked)
            AnimatedVisibility(
                visible = !isAutoSchedule,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Start date picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Date", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(16.dp))

                        var showDatePicker by remember { mutableStateOf(false) }
                        val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                        val displayDate = startDate?.format(dateFormatter) ?: "Select Date"

                        Button(
                            onClick = { showDatePicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text(displayDate)
                        }

                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = (startDate ?: LocalDate.now()).toEpochDay() * 86400000L
                            )
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            startDate = LocalDate.ofEpochDay(it / 86400000L)
                                        }
                                        showDatePicker = false
                                    }) {
                                        Text("OK")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("Cancel")
                                    }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Start time picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Time", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(16.dp))

                        var showTimePicker by remember { mutableStateOf(false) }
                        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                        val displayTime = startTime?.format(timeFormatter) ?: "Select Time"

                        Button(
                            onClick = { showTimePicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text(displayTime)
                        }

                        if (showTimePicker) {
                            val timePickerState = rememberTimePickerState(
                                initialHour = startTime?.hour ?: LocalTime.now().hour,
                                initialMinute = startTime?.minute ?: LocalTime.now().minute,
                                is24Hour = false
                            )
                            AlertDialog(
                                onDismissRequest = { showTimePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        startTime = LocalTime.of(timePickerState.hour, timePickerState.minute, 0, 0)
                                        showTimePicker = false
                                    }) {
                                        Text("OK")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTimePicker = false }) {
                                        Text("Cancel")
                                    }
                                },
                                text = {
                                    TimePicker(state = timePickerState)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    return Triple(isAutoSchedule, startDate, startTime)
}

/* CHECKBOX FIELD */
@Composable
fun checkboxField(
    label: String,
    initialChecked: Boolean = false,
    key: Int = 0
): Boolean {
    var isChecked by remember(key) { mutableStateOf(initialChecked) }

    LaunchedEffect(key, initialChecked) {
        isChecked = initialChecked
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                colors = CheckboxDefaults.colors(checkedColor = PrimaryColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }

    return isChecked
}

/* ALL DAY PICKER FIELD */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun allDayPickerField(
    initialAllDay: Boolean = true,
    initialTime: LocalTime = LocalTime.now(),
    key: Int = 0
): Pair<Boolean, LocalTime> {
    var isAllDay by remember(key) { mutableStateOf(initialAllDay) }
    var time by remember(key) { mutableStateOf(initialTime) }

    LaunchedEffect(key, initialAllDay, initialTime) {
        isAllDay = initialAllDay
        time = initialTime
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isAllDay,
                    onCheckedChange = {
                        isAllDay = it
                        if (!it) {
                            time = LocalTime.now().withSecond(0).withNano(0)
                        }
                    },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("All Day", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            // Time picker
            AnimatedVisibility(
                visible = !isAllDay,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Time", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(16.dp))

                        var showTimePicker by remember { mutableStateOf(false) }
                        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                        val displayTime = time.format(timeFormatter)

                        Button(
                            onClick = { showTimePicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text(displayTime)
                        }

                        if (showTimePicker) {
                            val timePickerState = rememberTimePickerState(
                                initialHour = time.hour,
                                initialMinute = time.minute,
                                is24Hour = false
                            )
                            AlertDialog(
                                onDismissRequest = { showTimePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        time = LocalTime.of(timePickerState.hour, timePickerState.minute, 0, 0)
                                        showTimePicker = false
                                    }) {
                                        Text("OK")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTimePicker = false }) {
                                        Text("Cancel")
                                    }
                                },
                                text = {
                                    TimePicker(state = timePickerState)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    return Pair(isAllDay, time)
}

/* DROPDOWN FIELD */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun dropdownField(
    label: String,
    items: List<String>,
    initialSelection: Int? = null,
    key: Int = 0
): Int? {
    var selectedIndex by remember(key) { mutableStateOf(initialSelection) }
    var showDropdown by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, initialSelection) {
        selectedIndex = initialSelection
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showDropdown = !showDropdown }
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            // Display selected item or "None"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    if (selectedIndex != null) items.getOrNull(selectedIndex!!) ?: "None"
                    else "None"
                )
            }

            // Dropdown list with fixed height and scrolling
            AnimatedVisibility(
                visible = showDropdown,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(180.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // "None" option
                    val isNoneSelected = selectedIndex == null
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (isNoneSelected) PrimaryColor else Color.LightGray,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedIndex = null
                                showDropdown = false
                            }
                            .padding(12.dp)
                    ) {
                        Text(
                            "None",
                            color = if (isNoneSelected) Color.White else Color.Black,
                            fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Regular items
                    items.forEachIndexed { index, item ->
                        val isSelected = selectedIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelected) PrimaryColor else Color.LightGray,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedIndex = index
                                    showDropdown = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                item,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    return selectedIndex
}