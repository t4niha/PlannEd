package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Reminders(db: AppDatabase) {
    var currentView by remember { mutableStateOf("list") }
    var selectedReminder by remember { mutableStateOf<MasterReminder?>(null) }

    when (currentView) {
        "list" -> RemindersListView(
            db = db,
            onReminderClick = { reminder ->
                selectedReminder = reminder
                currentView = "info"
            }
        )
        "info" -> selectedReminder?.let { reminder ->
            ReminderInfoView(
                db = db,
                reminder = reminder,
                onBack = {
                    currentView = "list"
                    selectedReminder = null
                },
                onUpdate = { currentView = "update" }
            )
        }
        "update" -> selectedReminder?.let { reminder ->
            ReminderUpdateView(
                db = db,
                reminder = reminder,
                onBack = { currentView = "info" },
                onSaveSuccess = { updatedReminder ->
                    selectedReminder = updatedReminder
                    currentView = "info"
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RemindersListView(
    db: AppDatabase,
    onReminderClick: (MasterReminder) -> Unit
) {
    var reminders by remember { mutableStateOf<List<MasterReminder>>(emptyList()) }

    LaunchedEffect(Unit) {
        reminders = ReminderManager.getAll(db).sortedBy { it.startDate }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Text(
            "Reminders",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reminders",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders) { reminder ->
                    ReminderItemView(
                        reminder = reminder,
                        onClick = { onReminderClick(reminder) }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReminderItemView(
    reminder: MasterReminder,
    onClick: () -> Unit
) {
    val reminderColor = Converters.toColor(reminder.color)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(INNER_CIRCLE_SIZE)
                .background(reminderColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = if (reminder.allDay)
                    "${reminder.startDate.format(dateFormatter)} - All Day"
                else
                    "${reminder.startDate.format(dateFormatter)} at ${reminder.time?.format(timeFormatter) ?: ""}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReminderInfoView(
    db: AppDatabase,
    reminder: MasterReminder,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var category by remember { mutableStateOf<Category?>(null) }
    var currentReminder by remember { mutableStateOf(reminder) }

    LaunchedEffect(reminder.id) {
        currentReminder = db.reminderDao().getMasterReminderById(reminder.id) ?: reminder
        category = currentReminder.categoryId?.let { db.categoryDao().getCategoryById(it) }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(text = currentReminder.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            if (!currentReminder.notes.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(text = currentReminder.notes!!, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Color: ", fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Converters.toColor(currentReminder.color))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoField("Start Date", currentReminder.startDate.format(dateFormatter))
                InfoField("End Date", currentReminder.endDate?.format(dateFormatter) ?: "N/A")
                InfoField(
                    "Time",
                    if (currentReminder.allDay) "All Day"
                    else currentReminder.time?.format(timeFormatter) ?: ""
                )
                InfoField("Recurrence", currentReminder.recurFreq.name)

                when (currentReminder.recurFreq) {
                    RecurrenceFrequency.WEEKLY -> {
                        val daysOfWeek = currentReminder.recurRule.daysOfWeek
                        if (daysOfWeek != null && daysOfWeek.isNotEmpty()) {
                            val dayNames = daysOfWeek.sorted().joinToString(", ") { dayNum ->
                                when (dayNum) {
                                    1 -> "Mon"
                                    2 -> "Tue"
                                    3 -> "Wed"
                                    4 -> "Thu"
                                    5 -> "Fri"
                                    6 -> "Sat"
                                    7 -> "Sun"
                                    else -> ""
                                }
                            }
                            InfoField("Days", dayNames)
                        }
                    }
                    RecurrenceFrequency.MONTHLY -> {
                        val daysOfMonth = currentReminder.recurRule.daysOfMonth
                        if (daysOfMonth != null && daysOfMonth.isNotEmpty()) {
                            InfoField("Days of Month", daysOfMonth.sorted().joinToString(", "))
                        }
                    }
                    else -> {}
                }

                InfoField("Category", category?.title ?: "None")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        ReminderManager.delete(db, currentReminder.id)
                        onBack()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Delete", fontSize = 16.sp, color = Color.White)
            }
            Button(
                onClick = onUpdate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Update", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReminderUpdateView(
    db: AppDatabase,
    reminder: MasterReminder,
    onBack: () -> Unit,
    onSaveSuccess: (MasterReminder) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf(reminder.title) }
    var notes by remember { mutableStateOf(reminder.notes ?: "") }
    var color by remember { mutableStateOf(Converters.toColor(reminder.color)) }
    var startDate by remember { mutableStateOf(reminder.startDate) }
    var endDate by remember { mutableStateOf(reminder.endDate) }
    var isAllDay by remember { mutableStateOf(reminder.allDay) }
    var time by remember { mutableStateOf(reminder.time ?: java.time.LocalTime.of(10, 0)) }
    var recurrenceFreq by remember { mutableStateOf(reminder.recurFreq) }
    var selectedDaysOfWeek by remember { mutableStateOf(reminder.recurRule.daysOfWeek?.toSet() ?: setOf(7)) }
    var selectedDaysOfMonth by remember { mutableStateOf(reminder.recurRule.daysOfMonth?.toSet() ?: setOf(1)) }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    var previousCategory by remember { mutableStateOf<Int?>(null) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
        selectedCategory = reminder.categoryId?.let { catId ->
            categories.indexOfFirst { it.id == catId }.takeIf { it >= 0 }
        }
        previousCategory = selectedCategory
    }

    LaunchedEffect(selectedCategory) {
        val currentCatIndex = selectedCategory
        val previousCatIndex = previousCategory

        if (currentCatIndex != previousCatIndex && currentCatIndex != null && categories.isNotEmpty()) {
            val categoryColor = categories.getOrNull(currentCatIndex)?.color
            if (categoryColor != null) {
                color = Converters.toColor(categoryColor)
            }
        }
        previousCategory = currentCatIndex
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            ReminderForm(
                db = db,
                title = title,
                onTitleChange = { title = it },
                notes = notes,
                onNotesChange = { notes = it },
                color = color,
                onColorChange = { color = it },
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                isAllDay = isAllDay,
                time = time,
                onAllDayTimeChange = { allDay, timeVal ->
                    isAllDay = allDay
                    time = timeVal
                },
                recurrenceFreq = recurrenceFreq,
                selectedDaysOfWeek = selectedDaysOfWeek,
                selectedDaysOfMonth = selectedDaysOfMonth,
                onRecurrenceChange = { freq, daysWeek, daysMonth, endDateVal ->
                    recurrenceFreq = freq
                    selectedDaysOfWeek = daysWeek
                    selectedDaysOfMonth = daysMonth
                    endDate = endDateVal
                },
                selectedCategory = selectedCategory,
                onCategoryChange = { selectedCategory = it },
                resetTrigger = resetTrigger
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        title = reminder.title
                        notes = reminder.notes ?: ""
                        color = Converters.toColor(reminder.color)
                        startDate = reminder.startDate
                        endDate = reminder.endDate
                        isAllDay = reminder.allDay
                        time = reminder.time ?: java.time.LocalTime.of(10, 0)
                        recurrenceFreq = reminder.recurFreq
                        selectedDaysOfWeek = reminder.recurRule.daysOfWeek?.toSet() ?: setOf(7)
                        selectedDaysOfMonth = reminder.recurRule.daysOfMonth?.toSet() ?: setOf(1)
                        selectedCategory = reminder.categoryId?.let { catId ->
                            categories.indexOfFirst { it.id == catId }.takeIf { it >= 0 }
                        }
                        resetTrigger++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Reset", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (title.isBlank()) return@Button
                        scope.launch {
                            val recurRule = when (recurrenceFreq) {
                                RecurrenceFrequency.NONE -> RecurrenceRule()
                                RecurrenceFrequency.DAILY -> RecurrenceRule()
                                RecurrenceFrequency.WEEKLY -> RecurrenceRule(daysOfWeek = selectedDaysOfWeek.toList())
                                RecurrenceFrequency.MONTHLY -> RecurrenceRule(daysOfMonth = selectedDaysOfMonth.toList())
                                RecurrenceFrequency.YEARLY -> RecurrenceRule(monthAndDay = Pair(startDate.dayOfMonth, startDate.monthValue))
                            }

                            val catId = selectedCategory?.let { categories.getOrNull(it)?.id }

                            val updatedReminder = reminder.copy(
                                title = title,
                                notes = notes.ifBlank { null },
                                color = Converters.fromColor(color),
                                startDate = startDate,
                                endDate = endDate,
                                time = if (isAllDay) null else time,
                                allDay = isAllDay,
                                recurFreq = recurrenceFreq,
                                recurRule = recurRule,
                                categoryId = catId
                            )
                            ReminderManager.update(db, updatedReminder)
                            val refreshedReminder = db.reminderDao().getMasterReminderById(reminder.id) ?: updatedReminder
                            onSaveSuccess(refreshedReminder)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Save", fontSize = 16.sp)
                }
            }
        }
    }
}