package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Events(db: AppDatabase) {
    var currentView by remember { mutableStateOf("list") }
    var selectedEvent by remember { mutableStateOf<MasterEvent?>(null) }

    when (currentView) {
        "list" -> EventsList(
            db = db,
            onEventClick = { event ->
                selectedEvent = event
                currentView = "info"
            }
        )
        "info" -> selectedEvent?.let { event ->
            EventInfoPage(
                db = db,
                event = event,
                onBack = {
                    currentView = "list"
                    selectedEvent = null
                },
                onUpdate = { currentView = "update" }
            )
        }
        "update" -> selectedEvent?.let { event ->
            EventUpdateForm(
                db = db,
                event = event,
                onBack = { currentView = "info" },
                onSaveSuccess = { updatedEvent ->
                    selectedEvent = updatedEvent
                    currentView = "info"
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventsList(
    db: AppDatabase,
    onEventClick: (MasterEvent) -> Unit
) {
    var events by remember { mutableStateOf<List<MasterEvent>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(Unit) {
        events = EventManager.getAll(db)
        categories = CategoryManager.getAll(db)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Text(
            "Events",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventListItem(
                        db = db,
                        event = event,
                        categories = categories,
                        onClick = { onEventClick(event) }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventListItem(
    db: AppDatabase,
    event: MasterEvent,
    categories: List<Category>,
    onClick: () -> Unit
) {
    val eventColor = when {
        event.color != null -> Converters.toColor(event.color)
        event.categoryId != null -> {
            categories.find { it.id == event.categoryId }?.color?.let { Converters.toColor(it) } ?: Preset1
        }
        else -> Preset1
    }

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
                .background(eventColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${event.startTime} - ${event.endTime}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventInfoPage(
    db: AppDatabase,
    event: MasterEvent,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var category by remember { mutableStateOf<Category?>(null) }
    var currentEvent by remember { mutableStateOf(event) }

    LaunchedEffect(event.id) {
        currentEvent = db.eventDao().getMasterEventById(event.id) ?: event
        category = currentEvent.categoryId?.let { db.categoryDao().getCategoryById(it) }
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
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(text = currentEvent.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            if (!currentEvent.notes.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(text = currentEvent.notes!!, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoField("Start Date", currentEvent.startDate.toString())
                InfoField("End Date", currentEvent.endDate?.toString() ?: "N/A")
                InfoField("Start Time", currentEvent.startTime.toString())
                InfoField("End Time", currentEvent.endTime.toString())
                InfoField("Recurrence", currentEvent.recurFreq.name)
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
                        EventManager.delete(db, currentEvent.id)
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
fun EventUpdateForm(
    db: AppDatabase,
    event: MasterEvent,
    onBack: () -> Unit,
    onSaveSuccess: (MasterEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf(event.title) }
    var notes by remember { mutableStateOf(event.notes ?: "") }
    var color by remember { mutableStateOf(event.color?.let { Converters.toColor(it) } ?: Preset1) }
    var startDate by remember { mutableStateOf(event.startDate) }
    var endDate by remember { mutableStateOf(event.endDate) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var recurrenceFreq by remember { mutableStateOf(event.recurFreq) }
    var selectedDaysOfWeek by remember { mutableStateOf(event.recurRule.daysOfWeek?.toSet() ?: setOf(7)) }
    var selectedDaysOfMonth by remember { mutableStateOf(event.recurRule.daysOfMonth?.toSet() ?: setOf(1)) }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
        selectedCategory = event.categoryId?.let { catId ->
            categories.indexOfFirst { it.id == catId }.takeIf { it >= 0 }
        }
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
            EventForm(
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
                startTime = startTime,
                onStartTimeChange = { startTime = it },
                endTime = endTime,
                onEndTimeChange = { endTime = it },
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
                        title = event.title
                        notes = event.notes ?: ""
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

                            val updatedEvent = event.copy(
                                title = title,
                                notes = notes.ifBlank { null },
                                color = Converters.fromColor(color),
                                startDate = startDate,
                                endDate = endDate,
                                startTime = startTime,
                                endTime = endTime,
                                recurFreq = recurrenceFreq,
                                recurRule = recurRule,
                                categoryId = selectedCategory?.let { categories.getOrNull(it)?.id }
                            )
                            EventManager.update(db, updatedEvent)
                            val refreshedEvent = db.eventDao().getMasterEventById(event.id) ?: updatedEvent
                            onSaveSuccess(refreshedEvent)
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