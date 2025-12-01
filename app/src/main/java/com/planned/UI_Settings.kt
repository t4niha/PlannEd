package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/* GLOBAL SETTINGS */
val startWeekOnMonday: Boolean
    get() = SettingsManager.settings?.startWeekOnMonday ?: false
val PrimaryColor: Color
    get() = SettingsManager.settings?.let {
        Converters.toColor(it.primaryColor)
    } ?: Preset19
val showDeveloper: Boolean
    get() = SettingsManager.settings?.showDeveloper ?: true
val colorPresets = listOf(
    Preset13, Preset14, Preset15, Preset16,
    Preset17, Preset18, Preset19, Preset20,
    Preset21, Preset22, Preset23, Preset24
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Settings(db: AppDatabase) {
    val context = LocalContext.current
    val db = remember { AppDatabaseProvider.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // Derived state from SettingsManager
    val settings = SettingsManager.settings

    var showColorPicker by remember { mutableStateOf(false) }

    // Local state for UI
    settings?.startWeekOnMonday ?: false
    val localDeveloper = settings?.showDeveloper ?: true
    val localPrimary = settings?.let { Converters.toColor(it.primaryColor) } ?: Preset19

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(12.dp)
    ) {
        // Week start switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("First Day of Week", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeekButton(
                        label = "Sun",
                        selected = !startWeekOnMonday,
                        color = PrimaryColor
                    ) {
                        scope.launch {
                            SettingsManager.setStartWeek(db, false)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    WeekButton(
                        label = "Mon",
                        selected = startWeekOnMonday,
                        color = PrimaryColor
                    ) {
                        scope.launch {
                            SettingsManager.setStartWeek(db, true)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Break Duration
        val breakDurationValue = durationPickerField(
            label = "Break Duration",
            initialHours = (settings?.breakDuration ?: 5) / 60,
            initialMinutes = (settings?.breakDuration ?: 5) % 60,
            key = 0
        )

        LaunchedEffect(breakDurationValue) {
            val totalMinutes = (breakDurationValue.first * 60) + breakDurationValue.second
            if (totalMinutes != settings?.breakDuration) {
                SettingsManager.setBreakDuration(db, totalMinutes)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Break Every
        val breakEveryValue = durationPickerField(
            label = "Break Every",
            initialHours = (settings?.breakEvery ?: 25) / 60,
            initialMinutes = (settings?.breakEvery ?: 25) % 60,
            key = 1
        )

        LaunchedEffect(breakEveryValue) {
            val totalMinutes = (breakEveryValue.first * 60) + breakEveryValue.second
            if (totalMinutes != settings?.breakEvery) {
                SettingsManager.setBreakEvery(db, totalMinutes)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showColorPicker = !showColorPicker }
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("App Accent", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(localPrimary)
                    )
                }

                // Color picker
                AnimatedVisibility(
                    visible = showColorPicker,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .wrapContentHeight()
                    ) {
                        items(colorPresets.size) { i ->
                            val c = colorPresets[i]
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable {
                                        scope.launch {
                                            SettingsManager.setPrimaryColor(db, c)
                                        }
                                        showColorPicker = false
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Developer mode switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Developer Mode", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(16.dp))

                Switch(
                    checked = localDeveloper,
                    onCheckedChange = {
                        scope.launch {
                            SettingsManager.setDeveloperMode(db, it)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = localPrimary,
                        uncheckedTrackColor = Color.LightGray,
                        uncheckedThumbColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun WeekButton(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color else Color.LightGray)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.Black,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}