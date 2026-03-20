package com.planned

/**
Adaptive Time Intelligence:
1) Dynamic Scoring — tracks deadline miss rate and avg overtime per category/event
2) Automatic Time Padding — linear regression predicts overtime from predictedDuration
 **/

// Scoring weights
private const val WEIGHT_DEADLINE_MISS = 0.4f
private const val WEIGHT_AVG_OVERTIME  = 0.35f
private const val ROLLING_WINDOW       = 10

/* Round a float number of minutes up to the nearest 5 */
fun roundUpToNearest5(minutes: Float): Int {
    if (minutes <= 0f) return 0
    val intMinutes = minutes.toInt()
    val remainder = intMinutes % 5
    return if (remainder == 0) intMinutes else intMinutes + (5 - remainder)
}

/* Linear regression: given a list of completed tasks, predict overtime for a new task.
 * X = predictedDuration, Y = overTime
 * Returns predicted padding in minutes, rounded up to nearest 5. */
fun calculatePadding(tasks: List<MasterTask>): Int {
    if (tasks.size < 2) return 0

    val n = tasks.size.toFloat()
    val xs = tasks.map { it.predictedDuration.toFloat() }
    val ys = tasks.map { (it.overTime ?: 0).toFloat() }

    val sumX  = xs.sum()
    val sumY  = ys.sum()
    val sumXY = xs.zip(ys).sumOf { (x, y) -> (x * y).toDouble() }.toFloat()
    val sumX2 = xs.sumOf { (it * it).toDouble() }.toFloat()

    val denominator = n * sumX2 - sumX * sumX

    // All X values identical — slope undefined, use average Y instead
    if (denominator == 0f) {
        return roundUpToNearest5(sumY / n)
    }

    val slope     = (n * sumXY - sumX * sumY) / denominator
    val intercept = (sumY - slope * sumX) / n
    val avgX      = sumX / n
    val predicted = slope * avgX + intercept

    return roundUpToNearest5(predicted)
}

/* Weighted scoring formula.
 * deadlineMissCount: how many of last 10 completed tasks missed their deadline (0-10)
 * avgOvertime: average overtime in minutes across last 10 completed tasks
 * Returns a score float — higher means more struggle, needs earlier scheduling */
fun calculateScore(deadlineMissCount: Int, avgOvertime: Float): Float {
    val missRate    = deadlineMissCount / ROLLING_WINDOW.toFloat()
    val overtimeNorm = (avgOvertime / 120f).coerceIn(0f, 1f)
    return (WEIGHT_DEADLINE_MISS * missRate) + (WEIGHT_AVG_OVERTIME * overtimeNorm)
}

/* Main ATI update function — called every time a task is marked as complete.
 * Updates CategoryATI and/or EventATI records associated with the task. */
suspend fun updateATIOnTaskComplete(db: AppDatabase, task: MasterTask) {
    task.categoryId?.let { updateCategoryATI(db, it) }
    task.eventId?.let { updateEventATI(db, it) }
}

/* Update CategoryATI for a given category */
private suspend fun updateCategoryATI(db: AppDatabase, categoryId: Int) {
    val completedTasks = db.taskDao().getAllMasterTasks()
        .filter { it.categoryId == categoryId && it.status == 3 && it.allDay == null }
        .takeLast(ROLLING_WINDOW)

    if (completedTasks.isEmpty()) return

    val avgOvertime       = completedTasks.map { (it.overTime ?: 0).toFloat() }.average().toFloat()
    val deadlineMissCount = countDeadlineMisses(completedTasks)
    val predictedPadding  = calculatePadding(completedTasks)
    val score             = calculateScore(deadlineMissCount, avgOvertime)
    val tasksCompleted    = db.taskDao().getAllMasterTasks()
        .count { it.categoryId == categoryId && it.status == 3 && it.allDay == null }

    val existing = db.categoryATIDao().getById(categoryId)
    if (existing != null) {
        db.categoryATIDao().update(existing.copy(
            score = score,
            deadlineMissCount = deadlineMissCount,
            avgOvertime = avgOvertime,
            tasksCompleted = tasksCompleted,
            predictedPadding = predictedPadding
        ))
    } else {
        db.categoryATIDao().insert(CategoryATI(
            categoryId = categoryId,
            score = score,
            deadlineMissCount = deadlineMissCount,
            avgOvertime = avgOvertime,
            tasksCompleted = tasksCompleted,
            predictedPadding = predictedPadding
        ))
    }
}

/* Update EventATI for a given event */
private suspend fun updateEventATI(db: AppDatabase, eventId: Int) {
    val completedTasks = db.taskDao().getAllMasterTasks()
        .filter { it.eventId == eventId && it.status == 3 && it.allDay == null }
        .takeLast(ROLLING_WINDOW)

    if (completedTasks.isEmpty()) return

    val avgOvertime       = completedTasks.map { (it.overTime ?: 0).toFloat() }.average().toFloat()
    val deadlineMissCount = countDeadlineMisses(completedTasks)
    val predictedPadding  = calculatePadding(completedTasks)
    val score             = calculateScore(deadlineMissCount, avgOvertime)
    val tasksCompleted    = db.taskDao().getAllMasterTasks()
        .count { it.eventId == eventId && it.status == 3 && it.allDay == null }

    val existing = db.eventATIDao().getById(eventId)
    if (existing != null) {
        db.eventATIDao().update(existing.copy(
            score = score,
            deadlineMissCount = deadlineMissCount,
            avgOvertime = avgOvertime,
            tasksCompleted = tasksCompleted,
            predictedPadding = predictedPadding
        ))
    } else {
        db.eventATIDao().insert(EventATI(
            eventId = eventId,
            score = score,
            deadlineMissCount = deadlineMissCount,
            avgOvertime = avgOvertime,
            tasksCompleted = tasksCompleted,
            predictedPadding = predictedPadding
        ))
    }
}

/* Count how many tasks in the window missed their deadline */
private fun countDeadlineMisses(tasks: List<MasterTask>): Int {
    return tasks.count { it.deadlineMissed }
}
