package com.planned

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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

/* HEADER */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(
    currentView: String,
    onViewSelected: (String) -> Unit,
    onMenuClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    Column {
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
                                selected = currentView == view,
                                onClick = { onViewSelected(view) },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                icon = {},
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = PrimaryColor,
                                    activeContentColor = Color.White,
                                    inactiveContainerColor = Color.White,
                                    inactiveContentColor = Color.Black,
                                ),
                                label = {
                                    Text(
                                        view,
                                        fontSize = 14.sp,
                                        fontWeight = if (currentView == view) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Create button
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Filled.Add, contentDescription = "Create", modifier = Modifier.size(32.dp))
                    }
                }
            },
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
                .background(MaterialTheme.colorScheme.background)
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

                // Icons
                val calendarIcon: @Composable () -> Unit = {
                    Icon(Icons.Filled.CalendarToday,
                        contentDescription = "Calendar",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF1976D2))
                }
                val gearIcon: @Composable () -> Unit = {
                    Icon(Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF1976D2))
                }
                val androidIcon: @Composable () -> Unit = {
                    Icon(Icons.Filled.Android,
                        contentDescription = "Developer",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF1976D2))
                }

                // Checkable boxes
                @Composable
                fun CheckableBox(isChecked: Boolean, onToggle: () -> Unit) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Checked",
                                tint = Color(0xFF1976D2),
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

                // Row items
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
                        // Icon on the left
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            icon()
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // Text button
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
                        // Checkbox on the left
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            CheckableBox(isChecked = isChecked, onToggle = onCheckToggle)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // Text button
                        DrawerItem(label, onClick)
                    }
                }

                // Drawer content rows
                DrawerRow("Categories", calendarIcon) {
                    /* Categories action */
                }
                CheckableDrawerRow("Events", showEvents, {
                    showEvents = !showEvents }) {
                    /* Events action */
                }
                CheckableDrawerRow("Deadlines", showDeadlines, {
                    showDeadlines = !showDeadlines }) {
                    /* Deadlines action */
                }
                CheckableDrawerRow("Task Buckets", showTaskBuckets, {
                    showTaskBuckets = !showTaskBuckets }) {
                    /* Task Buckets action */
                }
                CheckableDrawerRow("Tasks", showTasks, {
                    showTasks = !showTasks }) {
                    /* Tasks action */
                }
                DrawerRow("Settings", gearIcon) {
                    /* Settings action */
                }
                DrawerRow("Developer", androidIcon) {
                    /* Developer action */
                }
            }
        }
    }
}