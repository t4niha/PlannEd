package com.planned

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Calendar Filters
var showEvents by mutableStateOf(true)
var showDeadlines by mutableStateOf(true)
var showTaskBuckets by mutableStateOf(true)
var showTasks by mutableStateOf(true)

/* COLORS */
val BackgroundColor = Color.White
val PrimaryColor = Color(0xFF1976D2)

/* SHAPES */
val CircleShapePrimary = CircleShape

/* CALENDARS */
const val INITIAL_PAGE = 10000
const val TOTAL_PAGES = 20000
val HourHeight = 80.dp

/* NAVIGATION */
val DrawerWidth: Dp = 200.dp
const val AnimationDuration = 300