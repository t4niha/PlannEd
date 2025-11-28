package com.planned

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/* CALENDAR FILTERS */
var showEvents by mutableStateOf(true)
var showDeadlines by mutableStateOf(true)
var showTaskBuckets by mutableStateOf(true)
var showTasks by mutableStateOf(true)
var showReminders by mutableStateOf(true)