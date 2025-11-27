package com.planned

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import com.planned.ui.theme.PlanEdTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanEdTheme {
                Calendars()
            }
        }
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
        // Main content
        content()

        // Clickable overlay
        if (isDrawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 128.dp)
                    .clickable { onDrawerToggle() }
            )
        }

        // Drawer panel
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
            // Drawer content
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // Drawer items
                @Composable
                fun DrawerItem(label: String, onClick: () -> Unit) {
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                }

                DrawerItem("Calendars") {
                /* Action for Calendars */
                }
                DrawerItem("Categories") {
                /* Action for Categories */
                }
                DrawerItem("Events") {
                /* Action for Events */
                }
                DrawerItem("Deadlines") {
                /* Action for Deadlines */
                }
                DrawerItem("Task Buckets") {
                /* Action for Task Buckets */
                }
                DrawerItem("Tasks") {
                /* Action for Tasks */
                }
                DrawerItem("Settings") {
                /* Action for Settings */
                }
                DrawerItem("Developer") {
                /* Action for Developer */
                }
            }
        }
    }
}