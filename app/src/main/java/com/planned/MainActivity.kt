package com.planned

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planned.ui.theme.PlanEdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanEdTheme {
                HomeScreen()
            }
        }
    }
}

// ---------------- HomeScreen Composable ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var currentView by remember { mutableStateOf("Day") } // day/week/month
    var calendarText by remember { mutableStateOf("Today's hourly schedule") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PlannEd", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open menu drawer */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Navigate to account page */ }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Slider buttons for Day / Week / Month
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Day", "Week", "Month").forEach { view ->
                        Button(
                            onClick = {
                                currentView = view
                                calendarText = when (view) {
                                    "Day" -> "Today's hourly schedule"
                                    "Week" -> "Week schedule (hourly for each day)"
                                    "Month" -> "Month view calendar"
                                    else -> ""
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(view)
                        }
                    }
                }

                // Placeholder calendar / schedule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(calendarText, fontSize = 18.sp)
                }

                // Bottom navigation arrows
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { /* TODO: previous day/week/month */ }) {
                        Text("<")
                    }
                    Button(onClick = { /* TODO: next day/week/month */ }) {
                        Text(">")
                    }
                }
            }
        }
    )
}