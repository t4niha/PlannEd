package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DeadlineUpdateFormData(
    val categories: List<Category>,
    val events: List<MasterEvent>,
    val courses: List<Course>,
    val selectedCategory: Int?,
    val selectedEvent: Int?,
    val selectedCourse: Int?
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Deadlines(db: AppDatabase) {
    when (deadlinesCurrentView) {
        "main" -> DeadlinesMainView(
            db = db,
            onUpcomingClick = { deadlinesCurrentView = "upcoming" },
            onPassedClick = { deadlinesCurrentView = "passed" }
        )
        "upcoming" -> DeadlinesListView(
            db = db,
            showPassed = false,
            onDeadlineClick = { deadline ->
                deadlinesSelectedDeadline = deadline
                deadlinesListSource = "upcoming"
                deadlinesCurrentView = "info"
            },
            onBack = { deadlinesCurrentView = "main" }
        )
        "passed" -> DeadlinesListView(
            db = db,
            showPassed = true,
            onDeadlineClick = { deadline ->
                deadlinesSelectedDeadline = deadline
                deadlinesListSource = "passed"
                deadlinesCurrentView = "info"
            },
            onBack = { deadlinesCurrentView = "main" }
        )
        "info" -> deadlinesSelectedDeadline?.let { deadline ->
            DeadlineInfoView(
                db = db,
                deadline = deadline,
                onBack = {
                    deadlinesCurrentView = deadlinesListSource
                    deadlinesSelectedDeadline = null
                    deadlinesUpdateFormData = null
                },
                onUpdateDataReady = { data -> deadlinesUpdateFormData = data },
                onUpdate = { deadlinesCurrentView = "update" },
                deadlineReturnScreen = "Deadlines"
            )
        }
        "update" -> deadlinesSelectedDeadline?.let { deadline ->
            deadlinesUpdateFormData?.let { formData ->
                DeadlineUpdateView(
                    db = db,
                    deadline = deadline,
                    preloadedData = formData,
                    onBack = { deadlinesCurrentView = "info" },
                    onSaveSuccess = { updatedDeadline ->
                        deadlinesSelectedDeadline = updatedDeadline
                        deadlinesUpdateFormData = null
                        deadlinesCurrentView = "info"
                    }
                )
            }
        }
    }
}

/* MAIN HUB VIEW */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlinesMainView(
    db: AppDatabase,
    onUpcomingClick: () -> Unit,
    onPassedClick: () -> Unit
) {
    val today = LocalDate.now()
    var upcomingCount by remember { mutableIntStateOf(0) }
    var passedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val all = DeadlineManager.getAll(db)
        upcomingCount = all.count { it.date >= today }
        passedCount = all.count { it.date < today }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeadlineCategoryBox(
                title = "Passed\nDeadlines",
                count = passedCount,
                modifier = Modifier.weight(1f),
                onClick = onPassedClick
            )
            DeadlineCategoryBox(
                title = "Upcoming\nDeadlines",
                count = upcomingCount,
                modifier = Modifier.weight(1f),
                onClick = onUpcomingClick
            )
        }
    }
}

@Composable
fun DeadlineCategoryBox(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(CardColor))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (count > 0) PrimaryColor else Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

/* SHARED LIST VIEW — used for both upcoming and passed */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlinesListView(
    db: AppDatabase,
    showPassed: Boolean,
    onDeadlineClick: (Deadline) -> Unit,
    onBack: () -> Unit
) {
    val today = LocalDate.now()
    var deadlines by remember { mutableStateOf<List<Deadline>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(Unit) {
        val all = DeadlineManager.getAll(db)
        deadlines = if (showPassed) {
            all.filter { it.date < today }.sortedByDescending { it.date }
        } else {
            all.filter { it.date >= today }.sortedBy { it.date }
        }
        categories = CategoryManager.getAll(db)
    }

    val grouped = deadlines.groupBy { it.categoryId }
    val sortedCategoryIds = categories.map { it.id as Int? }.filter { grouped.containsKey(it) } +
            if (grouped.containsKey(null)) listOf(null) else emptyList()

    val title = if (showPassed) "Passed Deadlines" else "Upcoming Deadlines"

    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Back",
            tint = PrimaryColor,
            modifier = Modifier
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onBack() }
                .size(40.dp)
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
            )

            if (deadlines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (showPassed) "No passed deadlines" else "No upcoming deadlines",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortedCategoryIds.forEach { catId ->
                        val categoryName = if (catId == null) "No Category"
                        else categories.find { it.id == catId }?.title ?: "No Category"
                        val categoryDeadlines = grouped[catId] ?: emptyList()
                        item {
                            Text(
                                text = categoryName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(categoryDeadlines) { deadline ->
                            DeadlineItemView(
                                db = db,
                                deadline = deadline,
                                onClick = { onDeadlineClick(deadline) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
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
                    event?.color?.let { Converters.toColor(it) }
                        ?: event?.categoryId?.let { catId -> db.categoryDao().getCategoryById(catId)?.color?.let { Converters.toColor(it) } }
                        ?: deadline.categoryId?.let { catId -> db.categoryDao().getCategoryById(catId)?.color?.let { Converters.toColor(it) } }
                        ?: Color.LightGray
                }
                deadline.categoryId != null -> {
                    val category = db.categoryDao().getCategoryById(deadline.categoryId)
                    category?.color?.let { Converters.toColor(it) } ?: Color.LightGray
                }
                else -> Color.LightGray
            }
        }
    }

    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(INNER_CIRCLE_SIZE).background(deadlineColor, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = deadline.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${deadline.time.format(timeFormatter)}, ${deadline.date.format(dateFormatter)}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlineInfoView(
    db: AppDatabase,
    deadline: Deadline,
    onBack: () -> Unit,
    onUpdateDataReady: (DeadlineUpdateFormData) -> Unit,
    onUpdate: () -> Unit,
    deadlineReturnScreen: String = "Calendars"
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var category by remember { mutableStateOf<Category?>(null) }
    var event by remember { mutableStateOf<MasterEvent?>(null) }
    var course by remember { mutableStateOf<Course?>(null) }
    var currentDeadline by remember { mutableStateOf(deadline) }
    var relatedTasks by remember { mutableStateOf<List<MasterTask>>(emptyList()) }
    var updateDataReady by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deadline.id) {
        currentDeadline = db.deadlineDao().getDeadlineById(deadline.id) ?: deadline
        category = currentDeadline.categoryId?.let { db.categoryDao().getCategoryById(it) }
        event = currentDeadline.eventId?.let { db.eventDao().getMasterEventById(it) }
        course = currentDeadline.courseId?.let { db.courseDao().getById(it) }
        relatedTasks = db.taskDao().getAllMasterTasks().filter { it.deadlineId == deadline.id && it.status != 3 }

        val categories = CategoryManager.getAll(db)
        val events = EventManager.getAll(db)
        val courses = db.courseDao().getAll()
        val selectedCategory = currentDeadline.categoryId?.let { catId ->
            categories.indexOfFirst { it.id == catId }.takeIf { it >= 0 }
        }
        val selectedEvent = currentDeadline.eventId?.let { evId ->
            events.indexOfFirst { it.id == evId }.takeIf { it >= 0 }
        }
        val selectedCourse = currentDeadline.courseId?.let { cId ->
            courses.indexOfFirst { it.id == cId }.takeIf { it >= 0 }
        }
        onUpdateDataReady(DeadlineUpdateFormData(
            categories = categories,
            events = events,
            courses = courses,
            selectedCategory = selectedCategory,
            selectedEvent = selectedEvent,
            selectedCourse = selectedCourse
        ))
        updateDataReady = true
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back", tint = PrimaryColor,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }
                    .size(40.dp)
            )
            Button(
                onClick = {
                    scope.launch {
                        if (currentDeadline.courseId != null) {
                            academicsSelectedCourse = db.courseDao().getById(currentDeadline.courseId!!)
                            academicsEnterGradeDeadlineTitle = currentDeadline.title
                            academicsEnterGradeDeadlineId = currentDeadline.id
                            academicsEnterGradeReturnScreen = "DeadlineInfo"
                            selectedDeadlineForInfo = currentDeadline
                            deadlineInfoReturnScreen = deadlineReturnScreen
                            academicsCurrentView = "enterGrade"
                            currentScreen = "Academics"
                        }
                    }
                },
                enabled = currentDeadline.courseId != null,
                modifier = Modifier.align(Alignment.Bottom),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    disabledContainerColor = Color.LightGray
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Enter Grade", fontSize = 14.sp) }
        }

        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(text = currentDeadline.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            if (!currentDeadline.notes.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(text = currentDeadline.notes!!, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            InfoCard(buildList {
                add("Date" to currentDeadline.date.format(dateFormatter))
                add("Time" to currentDeadline.time.format(timeFormatter))
                add("Event" to (event?.title ?: "None"))
                if (course != null) add("Course" to (course!!.courseCode?.takeIf { it.isNotBlank() }?.let { "$it – ${course!!.title}" } ?: course!!.title))
                add("Category" to (category?.title ?: "None"))
            })

            Spacer(modifier = Modifier.height(18.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text(
                    text = if (relatedTasks.isEmpty()) "No Related Tasks" else "Related Tasks",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                if (relatedTasks.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        relatedTasks.forEach { task ->
                            val taskColor = remember(task) {
                                kotlinx.coroutines.runBlocking {
                                    when {
                                        task.eventId != null -> {
                                            val ev = db.eventDao().getAllMasterEvents().find { it.id == task.eventId }
                                            ev?.color?.let { Converters.toColor(it) }
                                                ?: ev?.categoryId?.let { catId -> db.categoryDao().getAll().find { it.id == catId }?.color?.let { Converters.toColor(it) } }
                                                ?: Color.LightGray
                                        }
                                        task.categoryId != null -> {
                                            val cat = db.categoryDao().getCategoryById(task.categoryId)
                                            cat?.color?.let { Converters.toColor(it) } ?: Color.LightGray
                                        }
                                        else -> Color.LightGray
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(CardColor))
                                    .clickable {
                                        com.planned.deadlinesCurrentView = "main"
                                        com.planned.deadlinesSelectedDeadline = null
                                        com.planned.selectedDeadlineForInfo = currentDeadline
                                        com.planned.deadlineInfoReturnScreen = deadlineReturnScreen
                                        com.planned.selectedTaskForInfo = task
                                        com.planned.taskInfoReturnScreen = "DeadlineInfo"
                                        com.planned.currentScreen = "TaskInfo"
                                    }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(INNER_CIRCLE_SIZE).clip(CircleShape).background(taskColor))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = task.title, fontSize = 16.sp, fontWeight = FontWeight.Normal, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showDeleteDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), contentPadding = PaddingValues(16.dp)) {
                Text("Delete", fontSize = 16.sp, color = Color.White)
            }
            Button(onClick = { if (updateDataReady) onUpdate() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (updateDataReady) PrimaryColor else Color.LightGray), contentPadding = PaddingValues(16.dp)) {
                Text("Update", fontSize = 16.sp, color = Color.White)
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = BackgroundColor, title = null,
                text = { Text("Delete this deadline?", fontSize = 16.sp) },
                confirmButton = {},
                dismissButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showDeleteDialog = false; scope.launch { DeadlineManager.delete(context, db, currentDeadline.id); onBack() } },
                            modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), contentPadding = PaddingValues(12.dp)
                        ) { Text("Delete", fontSize = 12.sp, color = Color.White) }
                        Button(onClick = { showDeleteDialog = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor), contentPadding = PaddingValues(12.dp)) {
                            Text("Cancel", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeadlineUpdateView(
    db: AppDatabase,
    deadline: Deadline,
    preloadedData: DeadlineUpdateFormData,
    onBack: () -> Unit,
    onSaveSuccess: (Deadline) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf(deadline.title) }
    var notes by remember { mutableStateOf(deadline.notes ?: "") }
    var date by remember { mutableStateOf(deadline.date) }
    var time by remember { mutableStateOf(deadline.time) }

    val categories = preloadedData.categories
    val events = preloadedData.events
    val courses = preloadedData.courses
    var selectedCategory by remember { mutableStateOf(preloadedData.selectedCategory) }
    var selectedEvent by remember { mutableStateOf(preloadedData.selectedEvent) }
    var selectedCourse by remember { mutableStateOf(preloadedData.selectedCourse) }
    var previousEvent by remember { mutableStateOf(preloadedData.selectedEvent) }
    var resetTrigger by remember { mutableIntStateOf(0) }
    var showNotification by remember { mutableStateOf(false) }

    LaunchedEffect(selectedEvent, events.size) {
        val currentEventIndex = selectedEvent
        val previousEventIndex = previousEvent
        if (currentEventIndex != previousEventIndex) {
            if (currentEventIndex != null && events.isNotEmpty()) {
                val event = events.getOrNull(currentEventIndex)
                if (event != null) {
                    val eventCategoryId = event.categoryId
                    val categoryIndex = if (eventCategoryId != null) categories.indexOfFirst { it.id == eventCategoryId } else null
                    if (categoryIndex != null) selectedCategory = if (categoryIndex >= 0) categoryIndex else null

                    val eventCourseId = event.courseId
                    val courseIndex = if (eventCourseId != null) courses.indexOfFirst { it.id == eventCourseId } else null
                    selectedCourse = if (courseIndex != null && courseIndex >= 0) courseIndex else null
                }
            } else if (currentEventIndex == null) {
                selectedCourse = null
            }
            previousEvent = currentEventIndex
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(16.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back", tint = PrimaryColor,
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }.size(40.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                val titleValue = textInputField("Title", title, resetTrigger)
                title = titleValue
                Spacer(modifier = Modifier.height(12.dp))

                val notesValue = notesInputField("Notes", notes, resetTrigger)
                notes = notesValue
                Spacer(modifier = Modifier.height(12.dp))

                val dateValue = datePickerField(
                    label = "Date", initialDate = date, isOptional = false, key = resetTrigger,
                    allowPastDates = false, onDateValidated = { validated -> if (validated != date) date = validated }
                )!!
                if (dateValue != date) date = dateValue
                Spacer(modifier = Modifier.height(12.dp))

                val timeValue = timePickerField(label = "Time", initialTime = time, key = resetTrigger, contextDate = date, allowPastTimes = false)
                time = timeValue
                Spacer(modifier = Modifier.height(12.dp))

                val eventValue = dropdownField(label = "Event", items = events.map { it.title }, initialSelection = selectedEvent, key = resetTrigger)
                if (eventValue != selectedEvent) selectedEvent = eventValue
                Spacer(modifier = Modifier.height(12.dp))

                val courseValue = dropdownField(
                    label = "Course",
                    items = courses.map { c -> c.courseCode?.let { "$it – ${c.title}" } ?: c.title },
                    initialSelection = selectedCourse,
                    key = resetTrigger,
                    locked = selectedEvent != null
                )
                if (selectedEvent == null && courseValue != selectedCourse) selectedCourse = courseValue
                Spacer(modifier = Modifier.height(12.dp))

                val categoryValue = dropdownField(
                    label = "Category", items = categories.map { it.title },
                    initialSelection = selectedCategory, key = resetTrigger,
                    locked = selectedEvent != null
                )
                if (selectedEvent == null && categoryValue != selectedCategory) selectedCategory = categoryValue

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = {
                            title = deadline.title; notes = deadline.notes ?: ""
                            date = deadline.date; time = deadline.time
                            selectedCategory = preloadedData.selectedCategory
                            selectedEvent = preloadedData.selectedEvent
                            selectedCourse = preloadedData.selectedCourse
                            resetTrigger++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Reset", fontSize = 16.sp) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                scope.launch { showNotification = true; scrollState.animateScrollTo(0); delay(3000); showNotification = false }
                                return@Button
                            }
                            scope.launch {
                                val updatedDeadline = deadline.copy(
                                    title = title,
                                    notes = notes.ifBlank { null },
                                    date = date, time = time,
                                    categoryId = selectedCategory?.let { categories.getOrNull(it)?.id },
                                    eventId = selectedEvent?.let { events.getOrNull(it)?.id },
                                    courseId = selectedCourse?.let { courses.getOrNull(it)?.id }
                                )
                                DeadlineManager.update(context, db, updatedDeadline)
                                val refreshedDeadline = db.deadlineDao().getDeadlineById(deadline.id) ?: updatedDeadline
                                onSaveSuccess(refreshedDeadline)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Save", fontSize = 16.sp) }
                }
            }
        }

        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val dragOffset = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            Box(modifier = Modifier.offset(y = dragOffset.floatValue.coerceAtMost(0f).dp).draggable(
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                state = androidx.compose.foundation.gestures.rememberDraggableState { delta -> dragOffset.floatValue += delta; if (dragOffset.floatValue < -80f) showNotification = false },
                onDragStopped = { dragOffset.floatValue = 0f }
            )) {
                Surface(color = PrimaryColor, modifier = Modifier.fillMaxWidth().padding(16.dp), shadowElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Title is required", color = BackgroundColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/* Enter Grade Form — launched from deadline info page */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EnterGradeForm(
    db: AppDatabase,
    course: Course,
    initialTitle: String,
    deadlineId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var gradeTitle by remember { mutableStateOf(initialTitle) }
    var selectedType by remember { mutableStateOf(GradeItemType.QUIZ) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var marksReceivedText by remember { mutableStateOf("") }
    var totalMarksText by remember { mutableStateOf("") }

    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }

    fun showBanner(msg: String) {
        notificationMessage = msg; showNotification = true
        scope.launch { kotlinx.coroutines.delay(3000); showNotification = false }
    }

    val availableTypes = listOf(
        GradeItemType.QUIZ, GradeItemType.HOMEWORK, GradeItemType.ASSIGNMENT,
        GradeItemType.MID, GradeItemType.FINAL, GradeItemType.PROJECT,
        GradeItemType.REPORT, GradeItemType.PRESENTATION, GradeItemType.LAB,
        GradeItemType.PRACTICAL, GradeItemType.TUTORIAL,
        GradeItemType.ATTENDANCE, GradeItemType.PARTICIPATION, GradeItemType.OTHER
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(12.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back", tint = PrimaryColor,
                modifier = Modifier.padding(4.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }
                    .size(40.dp)
            )

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Title", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = gradeTitle,
                            onValueChange = { gradeTitle = it },
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundColor, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showTypeDropdown = !showTypeDropdown }
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Type", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text(gradeItemTypeLabel(selectedType), fontSize = 16.sp)
                        }
                        AnimatedVisibility(visible = showTypeDropdown, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Column(modifier = Modifier.padding(top = 8.dp).heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                                availableTypes.forEach { type ->
                                    val isSelected = selectedType == type
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            .background(if (isSelected) PrimaryColor else Color.LightGray, RoundedCornerShape(8.dp))
                                            .clickable { selectedType = type; showTypeDropdown = false }
                                            .padding(12.dp)
                                    ) {
                                        Text(gradeItemTypeLabel(type), fontSize = 16.sp,
                                            color = if (isSelected) BackgroundColor else Color.Black,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Marks Received", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            TextField(
                                value = marksReceivedText, onValueChange = { marksReceivedText = it },
                                modifier = Modifier.width(100.dp).clip(RoundedCornerShape(8.dp)).background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                singleLine = true
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color.LightGray)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Total Marks", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            TextField(
                                value = totalMarksText, onValueChange = { totalMarksText = it },
                                modifier = Modifier.width(100.dp).clip(RoundedCornerShape(8.dp)).background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { gradeTitle = initialTitle; marksReceivedText = ""; totalMarksText = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Clear", fontSize = 16.sp) }
                    Button(
                        onClick = {
                            val received = marksReceivedText.toFloatOrNull()
                            if (received == null || received < 0f) { showBanner("Marks received must be a valid non-negative number"); return@Button }
                            val total = totalMarksText.toFloatOrNull()
                            if (total == null || total <= 0f) { showBanner("Total marks must be a valid positive number"); return@Button }
                            if (received > total) { showBanner("Marks received cannot exceed total marks"); return@Button }
                            scope.launch {
                                db.gradeItemDao().insert(GradeItem(
                                    courseId = course.id,
                                    type = selectedType,
                                    title = gradeTitle.trim(),
                                    marksReceived = received,
                                    totalMarks = total
                                ))
                                db.deadlineDao().deleteById(deadlineId)
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Save", fontSize = 16.sp) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val dragOffset = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            Box(modifier = Modifier.offset(y = dragOffset.floatValue.coerceAtMost(0f).dp).draggable(
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                state = androidx.compose.foundation.gestures.rememberDraggableState { delta -> dragOffset.floatValue += delta; if (dragOffset.floatValue < -80f) showNotification = false },
                onDragStopped = { dragOffset.floatValue = 0f }
            )) {
                Surface(color = PrimaryColor, modifier = Modifier.fillMaxWidth().padding(16.dp), shadowElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(notificationMessage, color = BackgroundColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}