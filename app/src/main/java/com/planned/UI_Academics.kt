package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.room.TypeConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Delete

// Type converters
@TypeConverter
fun fromGradeItemType(type: GradeItemType): String = type.name

@TypeConverter
fun toGradeItemType(value: String): GradeItemType = GradeItemType.valueOf(value)

// Semester helpers
fun semesterName(semester: Int): String = when (semester) {
    1 -> "Spring"; 2 -> "Summer"; 3 -> "Fall"; 4 -> "Winter"; else -> "?"
}
fun semesterLabel(year: Int, semester: Int) = "${semesterName(semester)} $year"

// Grade calculation
fun calculateCurrentGrade(course: Course, items: List<GradeItem>): Float? {
    val weightMap = mapOf(
        GradeItemType.QUIZ         to course.weightQuiz,
        GradeItemType.MID          to course.weightMid,
        GradeItemType.ASSIGNMENT   to course.weightAssignment,
        GradeItemType.PROJECT      to course.weightProject,
        GradeItemType.FINAL        to course.weightFinal,
        GradeItemType.LAB          to course.weightLab,
        GradeItemType.ATTENDANCE   to course.weightAttendance,
        GradeItemType.PARTICIPATION to course.weightParticipation,
        GradeItemType.REPORT       to course.weightReport,
        GradeItemType.OTHER        to course.weightOther
    )
    val grouped = items.groupBy { it.type }
    if (grouped.isEmpty()) return null

    var weightedSum = 0f
    var totalWeightUsed = 0f

    for ((type, typeItems) in grouped) {
        val weight = weightMap[type] ?: 0f
        if (weight <= 0f) continue
        val avg = typeItems.map { it.marksReceived / it.totalMarks }.average().toFloat()
        weightedSum += avg * weight
        totalWeightUsed += weight
    }
    if (totalWeightUsed <= 0f) return null
    return (weightedSum / totalWeightUsed) * 100f
}

fun gradeItemTypeLabel(type: GradeItemType): String = when (type) {
    GradeItemType.QUIZ          -> "Quiz"
    GradeItemType.MID           -> "Midterm"
    GradeItemType.ASSIGNMENT    -> "Assignment"
    GradeItemType.PROJECT       -> "Project"
    GradeItemType.FINAL         -> "Final"
    GradeItemType.LAB           -> "Lab"
    GradeItemType.ATTENDANCE    -> "Attendance"
    GradeItemType.PARTICIPATION -> "Participation"
    GradeItemType.REPORT        -> "Report"
    GradeItemType.OTHER         -> "Other"
}

// Main entry composable
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Academics(db: AppDatabase) {
    when (academicsCurrentView) {
        "main" -> AcademicsMainView(
            db = db,
            onActiveClick = { academicsCurrentView = "active" },
            onHistoryClick = { academicsCurrentView = "history" }
        )
        "active" -> AcademicsActiveList(
            db = db,
            onBack = { academicsCurrentView = "main" },
            onCourseClick = { course ->
                academicsSelectedCourse = course
                academicsCurrentView = "courseInfo"
            },
            onAddCourse = { academicsCurrentView = "addCourse" }
        )
        "addCourse" -> AcademicsAddCourseForm(
            db = db,
            onBack = { academicsCurrentView = "active" },
            onSaved = { academicsCurrentView = "active" }
        )
        "courseInfo" -> academicsSelectedCourse?.let { course ->
            AcademicsCourseInfoPage(
                db = db,
                course = course,
                onBack = {
                    academicsSelectedCourse = null
                    academicsCurrentView = "active"
                },
                onAddGrade = { academicsCurrentView = "addGrade" },
                onDeleted = {
                    academicsSelectedCourse = null
                    academicsCurrentView = "active"
                },
                onSubmitted = {
                    academicsSelectedCourse = null
                    academicsCurrentView = "active"
                },
                onUpdateDataReady = { data -> academicsCourseUpdateFormData = data },
                onUpdate = { academicsCurrentView = "updateCourse" }
            )
        }
        "updateCourse" -> academicsSelectedCourse?.let { course ->
            academicsCourseUpdateFormData?.let { formData ->
                AcademicsCourseUpdateForm(
                    db = db,
                    course = course,
                    preloadedData = formData,
                    onBack = { academicsCurrentView = "courseInfo" },
                    onSaved = { academicsCurrentView = "courseInfo" }
                )
            }
        }
        "addGrade" -> academicsSelectedCourse?.let { course ->
            AcademicsAddGradeForm(
                db = db,
                course = course,
                onBack = { academicsCurrentView = "courseInfo" },
                onSaved = { academicsCurrentView = "courseInfo" }
            )
        }
        "history" -> AcademicsHistoryList(
            db = db,
            onBack = { academicsCurrentView = "main" },
            onCourseClick = { completed ->
                academicsSelectedCompletedCourse = completed
                academicsCurrentView = "completedInfo"
            }
        )
        "completedInfo" -> academicsSelectedCompletedCourse?.let { completed ->
            AcademicsCompletedInfoPage(
                db = db,
                course = completed,
                onBack = {
                    academicsSelectedCompletedCourse = null
                    academicsCurrentView = "history"
                },
                onDeleted = {
                    academicsSelectedCompletedCourse = null
                    academicsCurrentView = "history"
                }
            )
        }
    }
}

// Main view
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AcademicsMainView(
    db: AppDatabase,
    onActiveClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    var activeCount by remember { mutableIntStateOf(0) }
    var historyCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        activeCount = db.courseDao().getAll().size
        historyCount = db.completedCourseDao().getAll().size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AcademicsBox(
                title = "Active\nCourses",
                count = activeCount,
                modifier = Modifier.weight(1f),
                onClick = onActiveClick
            )
            AcademicsBox(
                title = "Grade\nHistory",
                count = historyCount,
                modifier = Modifier.weight(1f),
                onClick = onHistoryClick
            )
        }
    }
}

@Composable
fun AcademicsBox(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(CardColor))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (count > 0) PrimaryColor else Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(count.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
}

// Active courses list
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AcademicsActiveList(
    db: AppDatabase,
    onBack: () -> Unit,
    onCourseClick: (Course) -> Unit,
    onAddCourse: () -> Unit
) {
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var gradeItems by remember { mutableStateOf<List<GradeItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        courses = db.courseDao().getAll()
        gradeItems = courses.flatMap { db.gradeItemDao().getByCourseId(it.id) }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = PrimaryColor,
                modifier = Modifier
                    .padding(12.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }
                    .size(40.dp)
            )
            Button(
                onClick = onAddCourse,
                modifier = Modifier.align(Alignment.Bottom),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Add Course", fontSize = 14.sp)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Active Courses", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp, top = 4.dp))

            if (courses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active courses", fontSize = 18.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(courses) { course ->
                        val items = gradeItems.filter { it.courseId == course.id }
                        val grade = calculateCurrentGrade(course, items)
                        AcademicsCourseItem(course = course, grade = grade, onClick = { onCourseClick(course) })
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicsCourseItem(course: Course, grade: Float?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(course.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            if (!course.courseCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    course.courseCode,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                if (grade != null) "${"%.1f".format(grade)}%" else "No grades yet",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

// Add Course form
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AcademicsAddCourseForm(
    db: AppDatabase,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creditsText by remember { mutableStateOf("1") }
    var year by remember { mutableIntStateOf(java.time.Year.now().value) }
    var semester by remember { mutableIntStateOf(4) }
    var showSemesterDropdown by remember { mutableStateOf(false) }

    var wQuiz by remember { mutableStateOf("") }
    var wMid by remember { mutableStateOf("") }
    var wAssignment by remember { mutableStateOf("") }
    var wProject by remember { mutableStateOf("") }
    var wFinal by remember { mutableStateOf("") }
    var wLab by remember { mutableStateOf("") }
    var wAttendance by remember { mutableStateOf("") }
    var wParticipation by remember { mutableStateOf("") }
    var wReport by remember { mutableStateOf("") }
    var wOther by remember { mutableStateOf("") }

    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }

    fun showBanner(msg: String) {
        notificationMessage = msg
        showNotification = true
        scope.launch { delay(3000); showNotification = false }
    }

    fun validateWeightField(value: String, label: String): Float? {
        if (value.isBlank()) return 0f
        val f = value.toIntOrNull()
        if (f == null) { showBanner("$label weight must be a whole number"); return null }
        if (f < 0 || f > 100) { showBanner("$label weight must be between 0 and 100"); return null }
        return f.toFloat()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(12.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = PrimaryColor,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }
                    .size(40.dp)
            )

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Title", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Course Code
                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Course Code", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = courseCode,
                            onValueChange = { courseCode = it },
                            modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Description", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            minLines = 3
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Credits
                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Credits", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { val v = creditsText.toIntOrNull() ?: 1; if (v > 1) creditsText = (v - 1).toString() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = BackgroundColor),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("-", fontSize = 20.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.width(50.dp).background(BackgroundColor, RoundedCornerShape(8.dp)).padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(creditsText, fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { val v = creditsText.toIntOrNull() ?: 1; if (v < 9) creditsText = (v + 1).toString() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = BackgroundColor),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+", fontSize = 20.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Year
                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Year", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { year-- },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = BackgroundColor),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("-", fontSize = 20.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.width(70.dp).background(BackgroundColor, RoundedCornerShape(8.dp)).padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(year.toString(), fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { year++ },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = BackgroundColor),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+", fontSize = 20.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Semester dropdown
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSemesterDropdown = !showSemesterDropdown }
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Semester", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(12.dp)
                        ) { Text(semesterName(semester), fontSize = 16.sp) }
                        AnimatedVisibility(visible = showSemesterDropdown, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                listOf(4 to "Winter", 1 to "Spring", 2 to "Summer", 3 to "Fall").forEach { (num, name) ->
                                    val isSelected = semester == num
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            .background(if (isSelected) PrimaryColor else Color.LightGray, RoundedCornerShape(8.dp))
                                            .clickable { semester = num; showSemesterDropdown = false }
                                            .padding(12.dp)
                                    ) {
                                        Text(name, fontSize = 16.sp, color = if (isSelected) BackgroundColor else Color.Black, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Weights card
                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Grade Weights (%)", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))

                        val weightFields = listOf(
                            "Quiz" to Pair(wQuiz) { v: String -> wQuiz = v },
                            "Midterm" to Pair(wMid) { v: String -> wMid = v },
                            "Assignment" to Pair(wAssignment) { v: String -> wAssignment = v },
                            "Project" to Pair(wProject) { v: String -> wProject = v },
                            "Final" to Pair(wFinal) { v: String -> wFinal = v },
                            "Lab" to Pair(wLab) { v: String -> wLab = v },
                            "Attendance" to Pair(wAttendance) { v: String -> wAttendance = v },
                            "Participation" to Pair(wParticipation) { v: String -> wParticipation = v },
                            "Report" to Pair(wReport) { v: String -> wReport = v },
                            "Other" to Pair(wOther) { v: String -> wOther = v }
                        )

                        weightFields.forEachIndexed { index, (label, pairVal) ->
                            val (value, setter) = pairVal
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                TextField(
                                    value = value,
                                    onValueChange = { setter(it) },
                                    modifier = Modifier.width(80.dp).background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                    singleLine = true
                                )
                            }
                            if (index < weightFields.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            title = ""; courseCode = ""; description = ""
                            creditsText = "1"; year = java.time.Year.now().value; semester = 1
                            wQuiz = ""; wMid = ""; wAssignment = ""; wProject = ""; wFinal = ""
                            wLab = ""; wAttendance = ""; wParticipation = ""; wReport = ""; wOther = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Clear", fontSize = 16.sp) }

                    Button(
                        onClick = {
                            if (title.isBlank()) { showBanner("Title is required"); return@Button }
                            val credits = creditsText.toIntOrNull()
                            if (credits == null || credits < 1) { showBanner("Credits must be a positive number"); return@Button }
                            val q  = validateWeightField(wQuiz, "Quiz") ?: return@Button
                            val m  = validateWeightField(wMid, "Midterm") ?: return@Button
                            val a  = validateWeightField(wAssignment, "Assignment") ?: return@Button
                            val p  = validateWeightField(wProject, "Project") ?: return@Button
                            val f  = validateWeightField(wFinal, "Final") ?: return@Button
                            val l  = validateWeightField(wLab, "Lab") ?: return@Button
                            val at = validateWeightField(wAttendance, "Attendance") ?: return@Button
                            val pa = validateWeightField(wParticipation, "Participation") ?: return@Button
                            val r  = validateWeightField(wReport, "Report") ?: return@Button
                            val o  = validateWeightField(wOther, "Other") ?: return@Button
                            scope.launch {
                                db.courseDao().insert(Course(
                                    title = title.trim(),
                                    courseCode = courseCode.trim(),
                                    description = description.trim().ifBlank { null },
                                    credits = credits, year = year, semester = semester,
                                    weightQuiz = q, weightMid = m, weightAssignment = a, weightProject = p,
                                    weightFinal = f, weightLab = l, weightAttendance = at,
                                    weightParticipation = pa, weightReport = r, weightOther = o
                                ))
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Save", fontSize = 16.sp) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Banner
        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val dragOffset = remember { mutableFloatStateOf(0f) }
            Box(modifier = Modifier.offset(y = dragOffset.floatValue.coerceAtMost(0f).dp).draggable(
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                state = androidx.compose.foundation.gestures.rememberDraggableState { delta -> dragOffset.floatValue += delta; if (dragOffset.floatValue < -80f) showNotification = false },
                onDragStopped = { dragOffset.floatValue = 0f }
            )) {
                Surface(color = PrimaryColor, modifier = Modifier.fillMaxWidth().padding(16.dp), shadowElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(notificationMessage, color = BackgroundColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Course info page
data class CourseUpdateFormData(
    val title: String,
    val courseCode: String,
    val description: String,
    val credits: Int,
    val year: Int,
    val semester: Int,
    val wQuiz: String,
    val wMid: String,
    val wAssignment: String,
    val wProject: String,
    val wFinal: String,
    val wLab: String,
    val wAttendance: String,
    val wParticipation: String,
    val wReport: String,
    val wOther: String
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AcademicsCourseInfoPage(
    db: AppDatabase,
    course: Course,
    onBack: () -> Unit,
    onAddGrade: () -> Unit,
    onDeleted: () -> Unit,
    onSubmitted: () -> Unit,
    onUpdateDataReady: (CourseUpdateFormData) -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var gradeItems by remember { mutableStateOf<List<GradeItem>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var finalGradeText by remember { mutableStateOf("") }
    var showBanner by remember { mutableStateOf(false) }
    var bannerMessage by remember { mutableStateOf("") }
    var gradeItemToDelete by remember { mutableStateOf<GradeItem?>(null) }
    var updateDataReady by remember { mutableStateOf(false) }

    fun showMsg(msg: String) { bannerMessage = msg; showBanner = true; scope.launch { delay(3000); showBanner = false } }

    LaunchedEffect(course.id) {
        gradeItems = db.gradeItemDao().getByCourseId(course.id)
        fun Float.toWeightString() = if (this <= 0f) "" else this.toInt().toString()
        onUpdateDataReady(CourseUpdateFormData(
            title = course.title,
            courseCode = course.courseCode ?: "",
            description = course.description ?: "",
            credits = course.credits,
            year = course.year,
            semester = course.semester,
            wQuiz = course.weightQuiz.toWeightString(),
            wMid = course.weightMid.toWeightString(),
            wAssignment = course.weightAssignment.toWeightString(),
            wProject = course.weightProject.toWeightString(),
            wFinal = course.weightFinal.toWeightString(),
            wLab = course.weightLab.toWeightString(),
            wAttendance = course.weightAttendance.toWeightString(),
            wParticipation = course.weightParticipation.toWeightString(),
            wReport = course.weightReport.toWeightString(),
            wOther = course.weightOther.toWeightString()
        ))
        updateDataReady = true
    }

    val currentGrade = calculateCurrentGrade(course, gradeItems)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = PrimaryColor,
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }
                        .size(40.dp)
                )
                Button(
                    onClick = onAddGrade,
                    modifier = Modifier.align(Alignment.Bottom),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("Add Grade", fontSize = 14.sp) }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(course.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }

                InfoCard(buildList {
                    if (!course.courseCode.isNullOrBlank()) add("Course Code" to course.courseCode)
                    if (!course.description.isNullOrBlank()) add("Description" to course.description)
                    add("Semester" to semesterLabel(course.year, course.semester))
                    add("Credits" to course.credits.toString())
                    if (currentGrade != null && currentGrade > 0f)
                        add("Current Grade" to "${"%.1f".format(currentGrade)}%")
                })
                Spacer(modifier = Modifier.height(18.dp))

                val weights = listOf(
                    "Quiz" to course.weightQuiz, "Midterm" to course.weightMid,
                    "Assignment" to course.weightAssignment, "Project" to course.weightProject,
                    "Final" to course.weightFinal, "Lab" to course.weightLab,
                    "Attendance" to course.weightAttendance, "Participation" to course.weightParticipation,
                    "Report" to course.weightReport, "Other" to course.weightOther
                ).filter { it.second > 0f }

                if (weights.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp))) {
                        Column {
                            weights.forEachIndexed { index, (label, weight) ->
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("${weight.toInt()}%", fontSize = 16.sp)
                                }
                                if (index < weights.lastIndex) HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Text(
                    if (gradeItems.isEmpty()) "No Grades Entered" else "Grades Entered",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                if (gradeItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gradeItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(CardColor)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${gradeItemTypeLabel(item.type)}${if (item.title.isNotBlank()) ": ${item.title}" else ""}",
                                        fontSize = 16.sp, fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${"%.1f".format(item.marksReceived)} / ${"%.1f".format(item.totalMarks)}",
                                        fontSize = 14.sp, color = Color.Gray
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp).clickable(
                                        interactionSource = remember { MutableInteractionSource() }, indication = null
                                    ) { gradeItemToDelete = item }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (gradeItems.isEmpty()) 12.dp else 24.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Submit Final Grade", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = finalGradeText,
                                onValueChange = { finalGradeText = it },
                                placeholder = { Text("e.g. A-", color = Color.Gray) },
                                modifier = Modifier.weight(1f).background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (finalGradeText.isBlank()) { showMsg("Enter a grade first"); return@Button }
                                    val gradeRegex = Regex("^([A-Fa-f][+-]?|[Uu]|[Ww]|[Ii]|[Ss]|[Nn][Cc]?|[Pp])$")
                                    if (!gradeRegex.matches(finalGradeText.trim())) { showMsg("Invalid letter grade"); return@Button }
                                    scope.launch {
                                        db.completedCourseDao().insert(CompletedCourse(
                                            courseTitle = course.title,
                                            courseCode = course.courseCode,
                                            description = course.description,
                                            credits = course.credits,
                                            year = course.year,
                                            semester = course.semester,
                                            calculatedGrade = currentGrade ?: 0f,
                                            submitGrade = finalGradeText.trim().uppercase()
                                        ))
                                        db.gradeItemDao().deleteByCourseId(course.id)
                                        db.courseDao().deleteById(course.id)
                                        onSubmitted()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) { Text("Submit", fontSize = 14.sp) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    contentPadding = PaddingValues(16.dp)
                ) { Text("Delete", fontSize = 16.sp, color = Color.White) }
                Button(
                    onClick = { if (updateDataReady) onUpdate() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (updateDataReady) PrimaryColor else Color.LightGray),
                    contentPadding = PaddingValues(16.dp)
                ) { Text("Update", fontSize = 16.sp, color = Color.White) }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = BackgroundColor,
                title = null,
                text = { Text("Delete this course and all its grades?", fontSize = 16.sp) },
                confirmButton = {},
                dismissButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                scope.launch {
                                    db.gradeItemDao().deleteByCourseId(course.id)
                                    db.courseDao().deleteById(course.id)
                                    onDeleted()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            contentPadding = PaddingValues(12.dp)
                        ) { Text("Delete", fontSize = 12.sp, color = Color.White) }
                        Button(
                            onClick = { showDeleteDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            contentPadding = PaddingValues(12.dp)
                        ) { Text("Cancel", fontSize = 12.sp, color = Color.White) }
                    }
                }
            )
        }

        gradeItemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { gradeItemToDelete = null },
                containerColor = BackgroundColor,
                title = null,
                text = { Text("Delete this ${gradeItemTypeLabel(item.type).lowercase()}?", fontSize = 16.sp) },
                confirmButton = {},
                dismissButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val toDelete = item
                                gradeItemToDelete = null
                                scope.launch {
                                    db.gradeItemDao().deleteById(toDelete.id)
                                    gradeItems = db.gradeItemDao().getByCourseId(course.id)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            contentPadding = PaddingValues(12.dp)
                        ) { Text("Delete", fontSize = 12.sp, color = Color.White) }
                        Button(
                            onClick = { gradeItemToDelete = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            contentPadding = PaddingValues(12.dp)
                        ) { Text("Cancel", fontSize = 12.sp, color = Color.White) }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val dragOffset = remember { mutableFloatStateOf(0f) }
            Box(modifier = Modifier.offset(y = dragOffset.floatValue.coerceAtMost(0f).dp).draggable(
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                state = androidx.compose.foundation.gestures.rememberDraggableState { delta -> dragOffset.floatValue += delta; if (dragOffset.floatValue < -80f) showBanner = false },
                onDragStopped = { dragOffset.floatValue = 0f }
            )) {
                Surface(color = PrimaryColor, modifier = Modifier.fillMaxWidth().padding(16.dp), shadowElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(bannerMessage, color = BackgroundColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Course update form
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AcademicsCourseUpdateForm(
    db: AppDatabase,
    course: Course,
    preloadedData: CourseUpdateFormData,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf(preloadedData.title) }
    var courseCode by remember { mutableStateOf(preloadedData.courseCode) }
    var description by remember { mutableStateOf(preloadedData.description) }
    var creditsText by remember { mutableStateOf(preloadedData.credits.toString()) }
    var year by remember { mutableIntStateOf(preloadedData.year) }
    var semester by remember { mutableIntStateOf(preloadedData.semester) }
    var showSemesterDropdown by remember { mutableStateOf(false) }

    var wQuiz by remember { mutableStateOf(preloadedData.wQuiz) }
    var wMid by remember { mutableStateOf(preloadedData.wMid) }
    var wAssignment by remember { mutableStateOf(preloadedData.wAssignment) }
    var wProject by remember { mutableStateOf(preloadedData.wProject) }
    var wFinal by remember { mutableStateOf(preloadedData.wFinal) }
    var wLab by remember { mutableStateOf(preloadedData.wLab) }
    var wAttendance by remember { mutableStateOf(preloadedData.wAttendance) }
    var wParticipation by remember { mutableStateOf(preloadedData.wParticipation) }
    var wReport by remember { mutableStateOf(preloadedData.wReport) }
    var wOther by remember { mutableStateOf(preloadedData.wOther) }

    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }

    fun showBanner(msg: String) {
        notificationMessage = msg; showNotification = true
        scope.launch { delay(3000); showNotification = false }
    }

    fun validateWeightField(value: String, label: String): Float? {
        if (value.isBlank()) return 0f
        val f = value.toIntOrNull()
        if (f == null) { showBanner("$label weight must be a whole number"); return null }
        if (f < 0 || f > 100) { showBanner("$label weight must be between 0 and 100"); return null }
        return f.toFloat()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(12.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = PrimaryColor,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }
                    .size(40.dp)
            )

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Title", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = title, onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Course Code", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = courseCode, onValueChange = { courseCode = it },
                            modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Description", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = description, onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            minLines = 3
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Credits", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { val v = creditsText.toIntOrNull() ?: 1; if (v > 1) creditsText = (v - 1).toString() }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = BackgroundColor), modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("-", fontSize = 20.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.width(50.dp).background(BackgroundColor, RoundedCornerShape(8.dp)).padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(creditsText, fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = { val v = creditsText.toIntOrNull() ?: 1; if (v < 9) creditsText = (v + 1).toString() }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = BackgroundColor), modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 20.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Year", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { year-- }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = BackgroundColor), modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("-", fontSize = 20.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.width(70.dp).background(BackgroundColor, RoundedCornerShape(8.dp)).padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(year.toString(), fontSize = 18.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = { year++ }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor, contentColor = BackgroundColor), modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("+", fontSize = 20.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showSemesterDropdown = !showSemesterDropdown }
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Semester", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text(semesterName(semester), fontSize = 16.sp)
                        }
                        AnimatedVisibility(visible = showSemesterDropdown, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                listOf(4 to "Winter", 1 to "Spring", 2 to "Summer", 3 to "Fall").forEach { (num, name) ->
                                    val isSelected = semester == num
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            .background(if (isSelected) PrimaryColor else Color.LightGray, RoundedCornerShape(8.dp))
                                            .clickable { semester = num; showSemesterDropdown = false }
                                            .padding(12.dp)
                                    ) { Text(name, fontSize = 16.sp, color = if (isSelected) BackgroundColor else Color.Black, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Grade Weights (%)", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        val weightFields = listOf(
                            "Quiz" to Pair(wQuiz) { v: String -> wQuiz = v },
                            "Midterm" to Pair(wMid) { v: String -> wMid = v },
                            "Assignment" to Pair(wAssignment) { v: String -> wAssignment = v },
                            "Project" to Pair(wProject) { v: String -> wProject = v },
                            "Final" to Pair(wFinal) { v: String -> wFinal = v },
                            "Lab" to Pair(wLab) { v: String -> wLab = v },
                            "Attendance" to Pair(wAttendance) { v: String -> wAttendance = v },
                            "Participation" to Pair(wParticipation) { v: String -> wParticipation = v },
                            "Report" to Pair(wReport) { v: String -> wReport = v },
                            "Other" to Pair(wOther) { v: String -> wOther = v }
                        )
                        weightFields.forEachIndexed { index, (label, pairVal) ->
                            val (value, setter) = pairVal
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                TextField(
                                    value = value, onValueChange = { setter(it) },
                                    modifier = Modifier.width(80.dp).background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                    singleLine = true
                                )
                            }
                            if (index < weightFields.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            title = preloadedData.title; courseCode = preloadedData.courseCode
                            description = preloadedData.description
                            creditsText = preloadedData.credits.toString()
                            year = preloadedData.year; semester = preloadedData.semester
                            wQuiz = preloadedData.wQuiz; wMid = preloadedData.wMid
                            wAssignment = preloadedData.wAssignment; wProject = preloadedData.wProject
                            wFinal = preloadedData.wFinal; wLab = preloadedData.wLab
                            wAttendance = preloadedData.wAttendance; wParticipation = preloadedData.wParticipation
                            wReport = preloadedData.wReport; wOther = preloadedData.wOther
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Reset", fontSize = 16.sp) }

                    Button(
                        onClick = {
                            if (title.isBlank()) { showBanner("Title is required"); return@Button }
                            val credits = creditsText.toIntOrNull()
                            if (credits == null || credits < 1) { showBanner("Credits must be a positive number"); return@Button }
                            val q  = validateWeightField(wQuiz, "Quiz") ?: return@Button
                            val m  = validateWeightField(wMid, "Midterm") ?: return@Button
                            val a  = validateWeightField(wAssignment, "Assignment") ?: return@Button
                            val p  = validateWeightField(wProject, "Project") ?: return@Button
                            val f  = validateWeightField(wFinal, "Final") ?: return@Button
                            val l  = validateWeightField(wLab, "Lab") ?: return@Button
                            val at = validateWeightField(wAttendance, "Attendance") ?: return@Button
                            val pa = validateWeightField(wParticipation, "Participation") ?: return@Button
                            val r  = validateWeightField(wReport, "Report") ?: return@Button
                            val o  = validateWeightField(wOther, "Other") ?: return@Button
                            scope.launch {
                                db.courseDao().update(Course(
                                    id = course.id,
                                    title = title.trim(),
                                    courseCode = courseCode.trim(),
                                    description = description.trim().ifBlank { null },
                                    credits = credits, year = year, semester = semester,
                                    weightQuiz = q, weightMid = m, weightAssignment = a, weightProject = p,
                                    weightFinal = f, weightLab = l, weightAttendance = at,
                                    weightParticipation = pa, weightReport = r, weightOther = o
                                ))
                                academicsSelectedCourse = db.courseDao().getById(course.id)
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Save", fontSize = 16.sp) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val dragOffset = remember { mutableFloatStateOf(0f) }
            Box(modifier = Modifier.offset(y = dragOffset.floatValue.coerceAtMost(0f).dp).draggable(
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                state = androidx.compose.foundation.gestures.rememberDraggableState { delta -> dragOffset.floatValue += delta; if (dragOffset.floatValue < -80f) showNotification = false },
                onDragStopped = { dragOffset.floatValue = 0f }
            )) {
                Surface(color = PrimaryColor, modifier = Modifier.fillMaxWidth().padding(16.dp), shadowElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(notificationMessage, color = BackgroundColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Add Grade form
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AcademicsAddGradeForm(
    db: AppDatabase,
    course: Course,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var gradeTitle by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(GradeItemType.QUIZ) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var marksReceivedText by remember { mutableStateOf("") }
    var totalMarksText by remember { mutableStateOf("") }

    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }

    fun showBanner(msg: String) {
        notificationMessage = msg
        showNotification = true
        scope.launch { delay(3000); showNotification = false }
    }

    // Only show types that have a weight > 0
    val weightMap = mapOf(
        GradeItemType.QUIZ to course.weightQuiz, GradeItemType.MID to course.weightMid,
        GradeItemType.ASSIGNMENT to course.weightAssignment, GradeItemType.PROJECT to course.weightProject,
        GradeItemType.FINAL to course.weightFinal, GradeItemType.LAB to course.weightLab,
        GradeItemType.ATTENDANCE to course.weightAttendance, GradeItemType.PARTICIPATION to course.weightParticipation,
        GradeItemType.REPORT to course.weightReport, GradeItemType.OTHER to course.weightOther
    )
    val availableTypes = GradeItemType.entries

    // Make sure selectedType is valid
    LaunchedEffect(Unit) { if (selectedType !in availableTypes) selectedType = availableTypes.first() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(12.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = PrimaryColor,
                modifier = Modifier.padding(4.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }.size(40.dp)
            )

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("Title", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = gradeTitle,
                            onValueChange = { gradeTitle = it },
                            modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Type dropdown
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showTypeDropdown = !showTypeDropdown }
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Type", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text(gradeItemTypeLabel(selectedType), fontSize = 16.sp)
                        }
                        AnimatedVisibility(visible = showTypeDropdown, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Column(modifier = Modifier.padding(top = 8.dp).heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                                availableTypes.forEach { type ->
                                    val isSelected = selectedType == type
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            .background(if (isSelected) PrimaryColor else Color.LightGray, RoundedCornerShape(8.dp))
                                            .clickable { selectedType = type; showTypeDropdown = false }
                                            .padding(12.dp)
                                    ) {
                                        Text(gradeItemTypeLabel(type), fontSize = 16.sp, color = if (isSelected) BackgroundColor else Color.Black, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Marks received and total
                Box(modifier = Modifier.fillMaxWidth().background(Color(CardColor), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Marks Received", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            TextField(
                                value = marksReceivedText,
                                onValueChange = { marksReceivedText = it },
                                modifier = Modifier.width(100.dp).background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                singleLine = true
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color.LightGray)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Total Marks", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            TextField(
                                value = totalMarksText,
                                onValueChange = { totalMarksText = it },
                                modifier = Modifier.width(100.dp).background(BackgroundColor, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = TextFieldDefaults.colors(focusedContainerColor = BackgroundColor, unfocusedContainerColor = BackgroundColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { gradeTitle = ""; marksReceivedText = ""; totalMarksText = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Clear", fontSize = 16.sp) }

                    Button(
                        onClick = {
                            val received = marksReceivedText.toFloatOrNull()
                            if (received == null || received < 0f) { showBanner("Marks received must be a valid non-negative number"); return@Button }
                            val total = totalMarksText.toFloatOrNull()
                            if (total == null || total <= 0f) { showBanner("Total marks must be a positive number"); return@Button }
                            if (received > total) { showBanner("Marks received cannot exceed total marks"); return@Button }
                            scope.launch {
                                db.gradeItemDao().insert(GradeItem(courseId = course.id, type = selectedType, title = gradeTitle.trim(), marksReceived = received, totalMarks = total))
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)
                    ) { Text("Save", fontSize = 16.sp) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val dragOffset = remember { mutableFloatStateOf(0f) }
            Box(modifier = Modifier.offset(y = dragOffset.floatValue.coerceAtMost(0f).dp).draggable(
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                state = androidx.compose.foundation.gestures.rememberDraggableState { delta -> dragOffset.floatValue += delta; if (dragOffset.floatValue < -80f) showNotification = false },
                onDragStopped = { dragOffset.floatValue = 0f }
            )) {
                Surface(color = PrimaryColor, modifier = Modifier.fillMaxWidth().padding(16.dp), shadowElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(notificationMessage, color = BackgroundColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Grade History list
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AcademicsHistoryList(
    db: AppDatabase,
    onBack: () -> Unit,
    onCourseClick: (CompletedCourse) -> Unit
) {
    var courses by remember { mutableStateOf<List<CompletedCourse>>(emptyList()) }

    LaunchedEffect(Unit) {
        courses = db.completedCourseDao().getAll()
    }

    // Group by semester label
    val grouped = courses.groupBy { semesterLabel(it.year, it.semester) }
    val sortedKeys = grouped.keys.sortedWith(compareByDescending<String> {
        val parts = it.split(" ")
        val yr = parts.lastOrNull()?.toIntOrNull() ?: 0
        val sem = when (parts.firstOrNull()) { "Winter" -> 1; "Spring" -> 2; "Summer" -> 3; "Fall" -> 4; else -> 0 }
        yr * 10 + sem
    })

    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Back",
            tint = PrimaryColor,
            modifier = Modifier.padding(16.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }.size(40.dp)
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Grade History", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp, top = 4.dp))

            if (courses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No completed courses", fontSize = 18.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedKeys.forEach { semLabel ->
                        item {
                            Text(semLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp))
                        }
                        items(grouped[semLabel] ?: emptyList()) { completed ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(CardColor)).clickable { onCourseClick(completed) }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(completed.courseTitle, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(completed.submitGrade, fontSize = 14.sp, color = Color.Gray)
                                }
                                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Completed course info page
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AcademicsCompletedInfoPage(
    db: AppDatabase,
    course: CompletedCourse,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(16.dp)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Back",
            tint = PrimaryColor,
            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() }.size(40.dp)
        )

        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(course.courseTitle, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            InfoCard(buildList {
                if (!course.courseCode.isNullOrBlank()) add("Course Code" to course.courseCode!!)
                if (!course.description.isNullOrBlank()) add("Description" to course.description!!)
                add("Semester" to semesterLabel(course.year, course.semester))
                add("Credits" to course.credits.toString())
                if (course.calculatedGrade > 0f)
                    add("Calculated Grade" to "${"%.1f".format(course.calculatedGrade)}%")
                add("Submitted Grade" to course.submitGrade)
            })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            contentPadding = PaddingValues(16.dp)
        ) { Text("Delete", fontSize = 16.sp, color = Color.White) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = BackgroundColor,
            title = null,
            text = { Text("Delete this course record?", fontSize = 16.sp) },
            confirmButton = {},
            dismissButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch { db.completedCourseDao().deleteById(course.id); onDeleted() }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        contentPadding = PaddingValues(12.dp)
                    ) { Text("Delete", fontSize = 12.sp, color = Color.White) }
                    Button(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        contentPadding = PaddingValues(12.dp)
                    ) { Text("Cancel", fontSize = 12.sp, color = Color.White) }
                }
            }
        )
    }
}