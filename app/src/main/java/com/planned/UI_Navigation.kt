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
var selectedTaskForInfo by mutableStateOf<MasterTask?>(null)

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
                        "TaskInfo" -> selectedTaskForInfo?.let { task ->
                            TaskInfoPage(
                                db = db,
                                task = task,
                                onBack = {
                                    currentScreen = "Calendars"
                                    selectedTaskForInfo = null
                                },
                                onUpdate = { currentScreen = "TaskUpdate" },
                                onPlay = { currentScreen = "TaskPomodoro" }
                            )
                        }
                        "TaskUpdate" -> selectedTaskForInfo?.let { task ->
                            TaskUpdateForm(
                                db = db,
                                task = task,
                                onBack = {
                                    currentScreen = "TaskInfo"
                                },
                                onSaveSuccess = { updatedTask ->
                                    selectedTaskForInfo = updatedTask
                                    currentScreen = "TaskInfo"
                                }
                            )
                        }
                        "TaskPomodoro" -> selectedTaskForInfo?.let { task ->
                            PomodoroPage(
                                db = db,
                                task = task,
                                onBack = {
                                    currentScreen = "TaskInfo"
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
            // Menu button
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.size(32.dp))
                }
            },
            title = {
                // View selector buttons
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

                    // Create button
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Filled.Add,
                            contentDescription = "Create",
                            modifier = Modifier.size(32.dp))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackgroundColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
    }
}

/* NAVIGATION MENU */
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

                // Drawer items
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

                // Checkable boxes
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

                // Drawer rows
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

                // Icons
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

                // Drawer Contents
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