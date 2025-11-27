package com.planned

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

/* CONSTANTS */
private val DrawerWidth: Dp = 200.dp
private const val AnimationDuration = 300

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
                Text("Calendars", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Categories", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Events", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Deadlines", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Task Buckets", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tasks", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Settings", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Developer", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}