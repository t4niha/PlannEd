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
import androidx.compose.foundation.verticalScroll
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
val atiPaddingEnabled: Boolean
    get() = SettingsManager.settings?.atiPaddingEnabled ?: true
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
    val localAtiPadding = settings?.atiPaddingEnabled ?: true
    val localPrimary = settings?.let { Converters.toColor(it.primaryColor) } ?: Preset19
    val scrollState = androidx.compose.foundation.rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        Text(
            text = "Calendar",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
        )

        // Week start switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("First Day of Week", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
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

        Spacer(modifier = Modifier.height(12.dp))

        // Occurrence Window
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Generate Months", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { if (generationMonths > 1) generationMonths-- },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryColor,
                        contentColor = BackgroundColor
                    ),
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("-", fontSize = 20.sp) }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .background(BackgroundColor, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(generationMonths.toString(), fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { if (generationMonths < 6) generationMonths++ },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryColor,
                        contentColor = BackgroundColor
                    ),
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("+", fontSize = 20.sp) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Task Timer",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
        )

        // Break Every + Break Duration in one card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                // Break Every
                var breakEveryHours by remember { mutableIntStateOf((settings?.breakEvery ?: 25) / 60) }
                var breakEveryMinutes by remember { mutableIntStateOf((settings?.breakEvery ?: 25) % 60) }
                var showBreakEveryPicker by remember { mutableStateOf(false) }
                var tempBreakEveryH by remember { mutableIntStateOf(breakEveryHours) }
                var tempBreakEveryM by remember { mutableIntStateOf(breakEveryMinutes) }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Break Every", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { tempBreakEveryH = breakEveryHours; tempBreakEveryM = breakEveryMinutes; showBreakEveryPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) { Text("${breakEveryHours}h ${breakEveryMinutes}m") }
                }
                if (showBreakEveryPicker) {
                    AlertDialog(
                        onDismissRequest = { showBreakEveryPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                breakEveryHours = tempBreakEveryH
                                breakEveryMinutes = tempBreakEveryM
                                showBreakEveryPicker = false
                                scope.launch { SettingsManager.setBreakEvery(db, tempBreakEveryH * 60 + tempBreakEveryM) }
                            }) { Text("OK", color = Color.Black, fontSize = 16.sp) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBreakEveryPicker = false }) { Text("Cancel", color = Color.Black, fontSize = 16.sp) }
                        },
                        containerColor = BackgroundColor,
                        text = {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Hours", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { tempBreakEveryH++ }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("▲") }
                                    Text(tempBreakEveryH.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                                    Button(onClick = { if (tempBreakEveryH > 0 && !(tempBreakEveryH == 1 && tempBreakEveryM == 0)) tempBreakEveryH-- }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("▼") }
                                }
                                Spacer(modifier = Modifier.width(32.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Minutes", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { val n = (tempBreakEveryM + 5) % 60; if (!(tempBreakEveryH == 0 && n == 0)) tempBreakEveryM = n }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("▲") }
                                    Text(tempBreakEveryM.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                                    Button(onClick = { val n = if (tempBreakEveryM - 5 < 0) 55 else tempBreakEveryM - 5; if (!(tempBreakEveryH == 0 && n == 0)) tempBreakEveryM = n }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("▼") }
                                }
                            }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color.LightGray)

                // Break Duration
                var breakDurationHours by remember { mutableIntStateOf((settings?.breakDuration ?: 5) / 60) }
                var breakDurationMinutes by remember { mutableIntStateOf((settings?.breakDuration ?: 5) % 60) }
                var showBreakDurationPicker by remember { mutableStateOf(false) }
                var tempBreakDurH by remember { mutableIntStateOf(breakDurationHours) }
                var tempBreakDurM by remember { mutableIntStateOf(breakDurationMinutes) }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Break Duration", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { tempBreakDurH = breakDurationHours; tempBreakDurM = breakDurationMinutes; showBreakDurationPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) { Text("${breakDurationHours}h ${breakDurationMinutes}m") }
                }
                if (showBreakDurationPicker) {
                    AlertDialog(
                        onDismissRequest = { showBreakDurationPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                breakDurationHours = tempBreakDurH
                                breakDurationMinutes = tempBreakDurM
                                showBreakDurationPicker = false
                                scope.launch { SettingsManager.setBreakDuration(db, tempBreakDurH * 60 + tempBreakDurM) }
                            }) { Text("OK", color = Color.Black, fontSize = 16.sp) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBreakDurationPicker = false }) { Text("Cancel", color = Color.Black, fontSize = 16.sp) }
                        },
                        containerColor = BackgroundColor,
                        text = {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Hours", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { tempBreakDurH++ }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("▲") }
                                    Text(tempBreakDurH.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                                    Button(onClick = { if (tempBreakDurH > 0 && !(tempBreakDurH == 1 && tempBreakDurM == 0)) tempBreakDurH-- }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("▼") }
                                }
                                Spacer(modifier = Modifier.width(32.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Minutes", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { val n = (tempBreakDurM + 5) % 60; if (!(tempBreakDurH == 0 && n == 0)) tempBreakDurM = n }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("▲") }
                                    Text(tempBreakDurM.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                                    Button(onClick = { val n = if (tempBreakDurM - 5 < 0) 55 else tempBreakDurM - 5; if (!(tempBreakDurH == 0 && n == 0)) tempBreakDurM = n }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) { Text("▼") }
                                }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "App",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
        )

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
                    Spacer(modifier = Modifier.weight(1f))
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

        // ATI padding switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Predict Overtime", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))

                Switch(
                    checked = localAtiPadding,
                    onCheckedChange = {
                        scope.launch {
                            SettingsManager.setAtiPaddingEnabled(db, it)
                            generateTaskIntervals(db)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundColor,
                        checkedTrackColor = localPrimary,
                        uncheckedTrackColor = Color.LightGray,
                        uncheckedThumbColor = BackgroundColor
                    )
                )
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
                Spacer(modifier = Modifier.weight(1f))

                Switch(
                    checked = localDeveloper,
                    onCheckedChange = {
                        scope.launch {
                            SettingsManager.setDeveloperMode(db, it)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundColor,
                        checkedTrackColor = localPrimary,
                        uncheckedTrackColor = Color.LightGray,
                        uncheckedThumbColor = BackgroundColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Analytics",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
        )

        // Refresh Schedule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(CardColor), RoundedCornerShape(12.dp))
                .clickable {
                    scope.launch {
                        generateTaskIntervals(db)
                    }
                }
                .padding(16.dp)
        ) {
            Text("Refresh Schedule", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))
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
            color = if (selected) BackgroundColor else Color.Black,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}