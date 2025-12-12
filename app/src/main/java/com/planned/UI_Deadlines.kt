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
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Deadlines(db: AppDatabase) {
    var currentView by remember { mutableStateOf("list") }
    var selectedDeadline by remember { mutableStateOf<Deadline?>(null) }

    when (currentView) {
        "list" -> DeadlinesListView(
            db = db,
            onDeadlineClick = { deadline ->
                selectedDeadline = deadline
                currentView = "info"
            }
        )
        "info" -> selectedDeadline?.let { deadline ->
            DeadlineInfoView(
                db = db,
                deadline = deadline,
                onBack = {
                    currentView = "list"
                    selectedDeadline = null
                },
                onUpdate = { currentView = "update" }
            )
        }
        "update" -> selectedDeadline?.let { deadline ->
            DeadlineUpdateView(
                db = db,
                deadline = deadline,
                onBack = { currentView = "info" },
                onSaveSuccess = { updatedDeadline ->
                    selectedDeadline = updatedDeadline
                    currentView = "info"
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlinesListView(
    db: AppDatabase,
    onDeadlineClick: (Deadline) -> Unit
) {
    var deadlines by remember { mutableStateOf<List<Deadline>>(emptyList()) }

    LaunchedEffect(Unit) {
        deadlines = DeadlineManager.getAll(db).sortedBy { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Text(
            "Deadlines",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (deadlines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No deadlines",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deadlines) { deadline ->
                    DeadlineItemView(
                        db = db,
                        deadline = deadline,
                        onClick = { onDeadlineClick(deadline) }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlineItemView(
    db: AppDatabase,
    deadline: Deadline,
    onClick: () -> Unit
) {
    val deadlineColor = remember(deadline.id) {
        runBlocking {
            when {
                deadline.eventId != null -> {
                    val event = db.eventDao().getMasterEventById(deadline.eventId)
                    event?.color?.let { Converters.toColor(it) } ?: Color(CardColor)
                }
                deadline.categoryId != null -> {
                    val category = db.categoryDao().getCategoryById(deadline.categoryId)
                    category?.color?.let { Converters.toColor(it) } ?: Color(CardColor)
                }
                else -> Color(CardColor)
            }
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

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
                .background(deadlineColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = deadline.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "${deadline.date.format(dateFormatter)} at ${deadline.time.format(timeFormatter)}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlineInfoView(
    db: AppDatabase,
    deadline: Deadline,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var category by remember { mutableStateOf<Category?>(null) }
    var event by remember { mutableStateOf<MasterEvent?>(null) }
    var currentDeadline by remember { mutableStateOf(deadline) }
    var relatedTasks by remember { mutableStateOf<List<MasterTask>>(emptyList()) }

    LaunchedEffect(deadline.id) {
        currentDeadline = db.deadlineDao().getDeadlineById(deadline.id) ?: deadline
        category = currentDeadline.categoryId?.let { db.categoryDao().getCategoryById(it) }
        event = currentDeadline.eventId?.let { db.eventDao().getMasterEventById(it) }
        relatedTasks = db.taskDao().getAllMasterTasks().filter { it.deadlineId == deadline.id }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

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
                Text(text = currentDeadline.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            if (!currentDeadline.notes.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(text = currentDeadline.notes!!, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoField("Date", currentDeadline.date.format(dateFormatter))
                InfoField("Time", currentDeadline.time.format(timeFormatter))
                InfoField("Event", event?.title ?: "None")
                InfoField("Category", category?.title ?: "None")
                InfoField("Related Tasks", relatedTasks.size.toString())
            }

            if (relatedTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    "Tasks for this deadline:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                relatedTasks.forEach { task ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(CardColor))
                            .padding(12.dp)
                    ) {
                        Text(text = task.title, fontSize = 14.sp)
                    }
                }
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
                        DeadlineManager.delete(db, currentDeadline.id)
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
fun DeadlineUpdateView(
    db: AppDatabase,
    deadline: Deadline,
    onBack: () -> Unit,
    onSaveSuccess: (Deadline) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf(deadline.title) }
    var notes by remember { mutableStateOf(deadline.notes ?: "") }
    var date by remember { mutableStateOf(deadline.date) }
    var time by remember { mutableStateOf(deadline.time) }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var events by remember { mutableStateOf<List<MasterEvent>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    var selectedEvent by remember { mutableStateOf<Int?>(null) }
    var previousEvent by remember { mutableStateOf<Int?>(null) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
        events = EventManager.getAll(db)
        selectedCategory = deadline.categoryId?.let { catId ->
            categories.indexOfFirst { it.id == catId }.takeIf { it >= 0 }
        }
        selectedEvent = deadline.eventId?.let { evId ->
            events.indexOfFirst { it.id == evId }.takeIf { it >= 0 }
        }
        previousEvent = selectedEvent
    }

    // Lock category when event selected
    LaunchedEffect(selectedEvent, events.size) {
        val currentEventIndex = selectedEvent
        val previousEventIndex = previousEvent

        if (currentEventIndex != previousEventIndex) {
            if (currentEventIndex != null && events.isNotEmpty()) {
                val event = events.getOrNull(currentEventIndex)
                if (event != null) {
                    val eventCategoryId = event.categoryId
                    val categoryIndex = if (eventCategoryId != null) {
                        categories.indexOfFirst { it.id == eventCategoryId }
                    } else {
                        null
                    }
                    if (categoryIndex != null) {
                        selectedCategory = if (categoryIndex >= 0) categoryIndex else null
                    }
                }
            }
            previousEvent = currentEventIndex
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
            val titleValue = textInputField("Title", title, resetTrigger)
            title = titleValue
            Spacer(modifier = Modifier.height(12.dp))

            val notesValue = notesInputField("Notes", notes, resetTrigger)
            notes = notesValue
            Spacer(modifier = Modifier.height(12.dp))

            val dateValue = datePickerField(
                label = "Date",
                initialDate = date,
                isOptional = false,
                key = resetTrigger,
                allowPastDates = false,
                onDateValidated = { validated ->
                    if (validated != date) {
                        date = validated
                    }
                }
            )!!
            if (dateValue != date) {
                date = dateValue
            }
            Spacer(modifier = Modifier.height(12.dp))

            val timeValue = timePickerField(
                label = "Time",
                initialTime = time,
                key = resetTrigger,
                contextDate = date,
                allowPastTimes = false
            )
            time = timeValue
            Spacer(modifier = Modifier.height(12.dp))

            val eventValue = dropdownField(
                label = "Event",
                items = events.map { it.title },
                initialSelection = selectedEvent,
                key = resetTrigger
            )
            selectedEvent = eventValue
            Spacer(modifier = Modifier.height(12.dp))

            val categoryValue = dropdownField(
                label = "Category",
                items = categories.map { it.title },
                initialSelection = selectedCategory,
                key = resetTrigger,
                locked = selectedEvent != null
            )
            if (selectedEvent == null) {
                selectedCategory = categoryValue
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        title = deadline.title
                        notes = deadline.notes ?: ""
                        date = deadline.date
                        time = deadline.time
                        selectedCategory = deadline.categoryId?.let { catId ->
                            categories.indexOfFirst { it.id == catId }.takeIf { it >= 0 }
                        }
                        selectedEvent = deadline.eventId?.let { evId ->
                            events.indexOfFirst { it.id == evId }.takeIf { it >= 0 }
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
                            val catId = selectedCategory?.let { categories.getOrNull(it)?.id }
                            val evId = selectedEvent?.let { events.getOrNull(it)?.id }

                            val updatedDeadline = deadline.copy(
                                title = title,
                                notes = notes.ifBlank { null },
                                date = date,
                                time = time,
                                categoryId = catId,
                                eventId = evId
                            )
                            DeadlineManager.update(db, updatedDeadline)
                            val refreshedDeadline = db.deadlineDao().getDeadlineById(deadline.id) ?: updatedDeadline
                            onSaveSuccess(refreshedDeadline)
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