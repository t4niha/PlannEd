package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.graphicsLayer

var currentScreen by mutableStateOf("Calendars")
var currentCalendarView by mutableStateOf("Day")
var calendarResetTrigger by mutableIntStateOf(0)

// Task
var selectedTaskForInfo by mutableStateOf<MasterTask?>(null)
var navUpdateFormData by mutableStateOf<UpdateFormData?>(null)
var taskInfoReturnScreen by mutableStateOf("Calendars")

// Event
var selectedEventForInfo by mutableStateOf<MasterEvent?>(null)
var navEventUpdateFormData by mutableStateOf<EventUpdateFormData?>(null)

// Reminder
var selectedReminderForInfo by mutableStateOf<MasterReminder?>(null)
var navReminderUpdateFormData by mutableStateOf<ReminderUpdateFormData?>(null)

// Deadline
var selectedDeadlineForInfo by mutableStateOf<Deadline?>(null)
var navDeadlineUpdateFormData by mutableStateOf<DeadlineUpdateFormData?>(null)
var deadlineInfoReturnScreen by mutableStateOf("Calendars")

// Task Bucket
var selectedBucketForInfo by mutableStateOf<MasterTaskBucket?>(null)

// All-Day Task (reuses selectedTaskForInfo flow but separate screen keys)
var selectedAllDayTaskForInfo by mutableStateOf<MasterTask?>(null)

/* APP NAVIGATION */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(db: AppDatabase) {
    var isDrawerOpen by remember { mutableStateOf(false) }

    NavigationDrawer(
        isDrawerOpen = isDrawerOpen,
        onDrawerToggle = { isDrawerOpen = !isDrawerOpen }
    ) {
        Scaffold(
            containerColor = BackgroundColor,
            topBar = {
                Header(
                    currentView = currentCalendarView,
                    onViewSelected = { view ->
                        currentCalendarView = view
                        currentScreen = "Calendars"
                        if (isDrawerOpen) isDrawerOpen = false
                    },
                    onMenuClick = { isDrawerOpen = !isDrawerOpen },
                    onCreateClick = {
                        currentScreen = "Creation"
                        if (isDrawerOpen) isDrawerOpen = false
                    }
                )
            },
            content = { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (currentScreen) {
                        "Calendars" -> Calendars(db)
                        "Categories" -> Categories(db)
                        "Events" -> Events(db)
                        "Deadlines" -> Deadlines(db)
                        "TaskBuckets" -> TaskBuckets(db)
                        "Tasks" -> Tasks(db)
                        "Reminders" -> Reminders(db)
                        "Settings" -> Settings(db)
                        "Developer" -> Developer(db)
                        "Creation" -> Creation(db)

                        // ── Task ──────────────────────────────────────────────────────────
                        "TaskInfo" -> selectedTaskForInfo?.let { task ->
                            TaskInfoPage(
                                db = db,
                                task = task,
                                onBack = {
                                    val returnTo = taskInfoReturnScreen
                                    taskInfoReturnScreen = "Calendars"
                                    navUpdateFormData = null
                                    selectedTaskForInfo = null
                                    currentScreen = returnTo
                                },
                                onUpdateDataReady = { data -> navUpdateFormData = data },
                                onUpdate = { currentScreen = "TaskUpdate" },
                                onPlay = { currentScreen = "TaskPomodoro" }
                            )
                        }
                        "TaskUpdate" -> selectedTaskForInfo?.let { task ->
                            navUpdateFormData?.let { formData ->
                                TaskUpdateForm(
                                    db = db,
                                    task = task,
                                    preloadedData = formData,
                                    onBack = { currentScreen = "TaskInfo" },
                                    onSaveSuccess = { updatedTask ->
                                        selectedTaskForInfo = updatedTask
                                        navUpdateFormData = null
                                        currentScreen = "TaskInfo"
                                    }
                                )
                            }
                        }
                        "TaskPomodoro" -> selectedTaskForInfo?.let { task ->
                            PomodoroPage(
                                db = db,
                                task = task,
                                onBack = { currentScreen = "TaskInfo" }
                            )
                        }

                        // ── All-Day Task ──────────────────────────────────────────────────
                        "AllDayTaskInfo" -> selectedAllDayTaskForInfo?.let { task ->
                            AllDayTaskInfoPage(
                                db = db,
                                task = task,
                                onBack = {
                                    currentScreen = "Calendars"
                                    selectedAllDayTaskForInfo = null
                                },
                                onUpdate = { currentScreen = "AllDayTaskUpdate" },
                                onPlay = { currentScreen = "AllDayTaskPomodoro" }
                            )
                        }
                        "AllDayTaskUpdate" -> selectedAllDayTaskForInfo?.let { task ->
                            AllDayTaskUpdateForm(
                                db = db,
                                task = task,
                                onBack = { currentScreen = "AllDayTaskInfo" },
                                onSaveSuccess = { updatedTask ->
                                    selectedAllDayTaskForInfo = updatedTask
                                    currentScreen = "AllDayTaskInfo"
                                }
                            )
                        }
                        "AllDayTaskPomodoro" -> selectedAllDayTaskForInfo?.let { task ->
                            AllDayPomodoroPage(
                                db = db,
                                task = task,
                                onBack = { currentScreen = "AllDayTaskInfo" }
                            )
                        }

                        // ── Event ─────────────────────────────────────────────────────────
                        "EventInfo" -> selectedEventForInfo?.let { event ->
                            EventInfoPage(
                                db = db,
                                event = event,
                                onBack = {
                                    currentScreen = "Calendars"
                                    selectedEventForInfo = null
                                    navEventUpdateFormData = null
                                },
                                onUpdateDataReady = { data -> navEventUpdateFormData = data },
                                onUpdate = { currentScreen = "EventUpdate" }
                            )
                        }
                        "EventUpdate" -> selectedEventForInfo?.let { event ->
                            navEventUpdateFormData?.let { formData ->
                                EventUpdateForm(
                                    db = db,
                                    event = event,
                                    preloadedData = formData,
                                    onBack = { currentScreen = "EventInfo" },
                                    onSaveSuccess = { updatedEvent ->
                                        selectedEventForInfo = updatedEvent
                                        navEventUpdateFormData = null
                                        currentScreen = "EventInfo"
                                    }
                                )
                            }
                        }

                        // ── Reminder ──────────────────────────────────────────────────────
                        "ReminderInfo" -> selectedReminderForInfo?.let { reminder ->
                            ReminderInfoView(
                                db = db,
                                reminder = reminder,
                                onBack = {
                                    currentScreen = "Calendars"
                                    selectedReminderForInfo = null
                                    navReminderUpdateFormData = null
                                },
                                onUpdateDataReady = { data -> navReminderUpdateFormData = data },
                                onUpdate = { currentScreen = "ReminderUpdate" }
                            )
                        }
                        "ReminderUpdate" -> selectedReminderForInfo?.let { reminder ->
                            navReminderUpdateFormData?.let { formData ->
                                ReminderUpdateView(
                                    db = db,
                                    reminder = reminder,
                                    preloadedData = formData,
                                    onBack = { currentScreen = "ReminderInfo" },
                                    onSaveSuccess = { updatedReminder ->
                                        selectedReminderForInfo = updatedReminder
                                        navReminderUpdateFormData = null
                                        currentScreen = "ReminderInfo"
                                    }
                                )
                            }
                        }

                        // ── Deadline ──────────────────────────────────────────────────────
                        "DeadlineInfo" -> selectedDeadlineForInfo?.let { deadline ->
                            DeadlineInfoView(
                                db = db,
                                deadline = deadline,
                                onBack = {
                                    val returnTo = deadlineInfoReturnScreen
                                    deadlineInfoReturnScreen = "Calendars"
                                    navDeadlineUpdateFormData = null
                                    selectedDeadlineForInfo = null
                                    currentScreen = returnTo
                                },
                                onUpdateDataReady = { data -> navDeadlineUpdateFormData = data },
                                onUpdate = { currentScreen = "DeadlineUpdate" }
                            )
                        }
                        "DeadlineUpdate" -> selectedDeadlineForInfo?.let { deadline ->
                            navDeadlineUpdateFormData?.let { formData ->
                                DeadlineUpdateView(
                                    db = db,
                                    deadline = deadline,
                                    preloadedData = formData,
                                    onBack = { currentScreen = "DeadlineInfo" },
                                    onSaveSuccess = { updatedDeadline ->
                                        selectedDeadlineForInfo = updatedDeadline
                                        navDeadlineUpdateFormData = null
                                        currentScreen = "DeadlineInfo"
                                    }
                                )
                            }
                        }

                        // ── Task Bucket ───────────────────────────────────────────────────
                        "BucketInfo" -> selectedBucketForInfo?.let { bucket ->
                            TaskBucketInfoPage(
                                db = db,
                                bucket = bucket,
                                onBack = {
                                    currentScreen = "Calendars"
                                    selectedBucketForInfo = null
                                },
                                onUpdate = { currentScreen = "BucketUpdate" }
                            )
                        }
                        "BucketUpdate" -> selectedBucketForInfo?.let { bucket ->
                            TaskBucketUpdateForm(
                                db = db,
                                bucket = bucket,
                                onBack = { currentScreen = "BucketInfo" },
                                onSaveSuccess = { updatedBucket ->
                                    selectedBucketForInfo = updatedBucket
                                    currentScreen = "BucketInfo"
                                }
                            )
                        }
                    }
                }
            }
        )
    }
}

/* HEADER */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(
    currentView: String,
    onViewSelected: (String) -> Unit,
    onMenuClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    Column(modifier = Modifier.background(BackgroundColor)) {
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.size(32.dp))
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1.8f)) {
                        val options = listOf("Day", "Week", "Month")
                        options.forEachIndexed { index, view ->
                            SegmentedButton(
                                selected = currentView == view && currentScreen == "Calendars",
                                onClick = {
                                    currentScreen = "Calendars"
                                    currentCalendarView = view
                                    calendarResetTrigger++
                                    onViewSelected(view)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                icon = {},
                                border = BorderStroke(0.dp, Color.Transparent),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = PrimaryColor,
                                    activeContentColor = BackgroundColor,
                                    inactiveContainerColor = Color(CardColor),
                                    inactiveContentColor = Color.Black,
                                ),
                                label = {
                                    Text(
                                        view,
                                        fontSize = 14.sp,
                                        fontWeight = if (currentView == view && currentScreen == "Calendars") FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Filled.Add, contentDescription = "Create", modifier = Modifier.size(32.dp))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
    }
}

/* NAVIGATION DRAWER */
@Composable
fun NavigationDrawer(
    isDrawerOpen: Boolean,
    onDrawerToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val offsetX by animateDpAsState(
        targetValue = if (isDrawerOpen) 0.dp else -DrawerWidth,
        animationSpec = tween(durationMillis = AnimationDuration)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (isDrawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 128.dp)
                    .clickable { onDrawerToggle() }
            )
        }

        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .fillMaxHeight()
                .padding(top = 128.dp)
                .width(DrawerWidth)
                .background(BackgroundColor)
                .border(
                    width = 1.dp,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                @Composable
                fun DrawerItem(label: String, onClick: () -> Unit) {
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            label,
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                }

                @Composable
                fun CheckableBox(isChecked: Boolean, onToggle: () -> Unit) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                            .background(BackgroundColor)
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Checked",
                                tint = PrimaryColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        scaleX = 1.6f
                                        scaleY = 1.6f
                                    }
                            )
                        }
                    }
                }

                @Composable
                fun DrawerRow(
                    label: String,
                    icon: @Composable () -> Unit,
                    onClick: () -> Unit
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            icon()
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        DrawerItem(label, onClick)
                    }
                }

                @Composable
                fun CheckableDrawerRow(
                    label: String,
                    isChecked: Boolean,
                    onCheckToggle: () -> Unit,
                    onClick: () -> Unit
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            CheckableBox(isChecked = isChecked, onToggle = onCheckToggle)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        DrawerItem(label, onClick)
                    }
                }

                @Composable
                fun CalendarIcon() {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = "Calendar",
                        modifier = Modifier.size(24.dp),
                        tint = PrimaryColor
                    )
                }
                @Composable
                fun GearIcon() {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(24.dp),
                        tint = PrimaryColor
                    )
                }
                @Composable
                fun AndroidIcon() {
                    Icon(
                        Icons.Filled.Android,
                        contentDescription = "Developer",
                        modifier = Modifier.size(24.dp),
                        tint = PrimaryColor
                    )
                }

                DrawerRow("Categories", { CalendarIcon() }) {
                    currentScreen = "Categories"
                    onDrawerToggle()
                }
                CheckableDrawerRow("Events", showEvents, {
                    showEvents = !showEvents
                }) {
                    currentScreen = "Events"
                    onDrawerToggle()
                }
                CheckableDrawerRow("Deadlines", showDeadlines, {
                    showDeadlines = !showDeadlines
                }) {
                    currentScreen = "Deadlines"
                    onDrawerToggle()
                }
                CheckableDrawerRow("Reminders", showReminders, {
                    showReminders = !showReminders
                }) {
                    currentScreen = "Reminders"
                    onDrawerToggle()
                }
                CheckableDrawerRow("Tasks", showTasks, {
                    showTasks = !showTasks
                }) {
                    currentScreen = "Tasks"
                    onDrawerToggle()
                }
                CheckableDrawerRow("Task Buckets", showTaskBuckets, {
                    showTaskBuckets = !showTaskBuckets
                }) {
                    currentScreen = "TaskBuckets"
                    onDrawerToggle()
                }
                DrawerRow("Settings", { GearIcon() }) {
                    currentScreen = "Settings"
                    onDrawerToggle()
                }
                if (showDeveloper) {
                    DrawerRow("Developer", { AndroidIcon() }) {
                        currentScreen = "Developer"
                        onDrawerToggle()
                    }
                }
            }
        }
    }
}