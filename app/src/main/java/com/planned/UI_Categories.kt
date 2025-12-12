package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun Categories(db: AppDatabase) {
    var currentView by remember { mutableStateOf("list") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    when (currentView) {
        "list" -> CategoriesList(
            db = db,
            onCategoryClick = { category ->
                selectedCategory = category
                currentView = "info"
            }
        )
        "info" -> selectedCategory?.let { category ->
            CategoryInfoPage(
                db = db,
                category = category,
                onBack = {
                    currentView = "list"
                    selectedCategory = null
                },
                onUpdate = { currentView = "update" }
            )
        }
        "update" -> selectedCategory?.let { category ->
            CategoryUpdateForm(
                db = db,
                category = category,
                onBack = { currentView = "info" },
                onSaveSuccess = { updatedCategory ->
                    selectedCategory = updatedCategory
                    currentView = "info"
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoriesList(
    db: AppDatabase,
    onCategoryClick: (Category) -> Unit
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(Unit) {
        categories = CategoryManager.getAll(db)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Text(
            "Categories",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No categories",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    CategoryListItem(
                        category = category,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    onClick: () -> Unit
) {
    val categoryColor = Converters.toColor(category.color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(CardColor))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(INNER_CIRCLE_SIZE)
                .background(categoryColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = category.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryInfoPage(
    db: AppDatabase,
    category: Category,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var currentCategory by remember { mutableStateOf(category) }
    var eventCount by remember { mutableIntStateOf(0) }
    var deadlineCount by remember { mutableIntStateOf(0) }
    var taskCount by remember { mutableIntStateOf(0) }
    var reminderCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(category.id) {
        currentCategory = db.categoryDao().getCategoryById(category.id) ?: category

        // Related items
        val events = db.eventDao().getAllMasterEvents().filter { it.categoryId == category.id }
        val deadlines = db.deadlineDao().getAll().filter { it.categoryId == category.id }
        val tasks = db.taskDao().getAllMasterTasks().filter { it.categoryId == category.id }
        val reminders = db.reminderDao().getAllMasterReminders().filter { it.categoryId == category.id }

        eventCount = events.size
        deadlineCount = deadlines.size
        taskCount = tasks.size
        reminderCount = reminders.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Text(text = currentCategory.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            if (!currentCategory.notes.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(text = currentCategory.notes!!, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Color display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Color: ", fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Converters.toColor(currentCategory.color))
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Usage statistics
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoField("Events", eventCount.toString())
                InfoField("Deadlines", deadlineCount.toString())
                InfoField("Tasks", taskCount.toString())
                InfoField("Reminders", reminderCount.toString())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        CategoryManager.delete(db, currentCategory.id)
                        onBack()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Delete", fontSize = 16.sp, color = Color.White)
            }
            Button(
                onClick = onUpdate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Update", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryUpdateForm(
    db: AppDatabase,
    category: Category,
    onBack: () -> Unit,
    onSaveSuccess: (Category) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf(category.title) }
    var notes by remember { mutableStateOf(category.notes ?: "") }
    var color by remember { mutableStateOf(Converters.toColor(category.color)) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryColor)
                .clickable { onBack() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Back", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            CategoryForm(
                title = title,
                onTitleChange = { title = it },
                notes = notes,
                onNotesChange = { notes = it },
                color = color,
                onColorChange = { color = it },
                resetTrigger = resetTrigger
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        title = category.title
                        notes = category.notes ?: ""
                        color = Converters.toColor(category.color)
                        resetTrigger++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Reset", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (title.isBlank()) return@Button
                        scope.launch {
                            val updatedCategory = category.copy(
                                title = title,
                                notes = notes.ifBlank { null },
                                color = Converters.fromColor(color)
                            )
                            CategoryManager.update(db, updatedCategory)
                            val refreshedCategory = db.categoryDao().getCategoryById(category.id) ?: updatedCategory
                            onSaveSuccess(refreshedCategory)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Save", fontSize = 16.sp)
                }
            }
        }
    }
}