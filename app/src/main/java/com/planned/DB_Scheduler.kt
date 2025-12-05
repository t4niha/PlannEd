package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime

/**
Ordering Factors:
1) Priority (1-5)
2) Urgency (Deadline)
3) Category Score (ATI - TODO)
4) Event Score (ATI - TODO)
5) Date Created (Task ID)
 **/

/* Available time slots for task scheduling */
data class AvailableTimeSlot(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun durationMinutes(): Int {
        val startMinutes = startTime.hour * 60 + startTime.minute
        val endMinutes = endTime.hour * 60 + endTime.minute
        return endMinutes - startMinutes
    }
}

/* Ordering auto-scheduled tasks */
data class OrderedTask(
    val masterTask: MasterTask,
    var remainingDuration: Int // Updated as task is broken into intervals
)

/* Clear all task intervals and regenerates them based on scheduling rules */
@RequiresApi(Build.VERSION_CODES.O)
suspend fun generateTaskIntervals(db: AppDatabase) {
    // Clear all task intervals and reset noIntervals
    db.taskDao().getAllIntervals().forEach { interval ->
        db.taskDao().deleteInterval(interval.id)
    }
    val allMasterTasks = db.taskDao()
        .getAllMasterTasks()
        .filter { it.status != 3 }
    allMasterTasks.forEach { task ->
        db.taskDao().update(task.copy(noIntervals = 0))
    }

    // Process manually scheduled tasks
    val manualTasks = allMasterTasks.filter { it.startDate != null && it.startTime != null }
    val autoTasks = allMasterTasks.filter { it.startDate == null || it.startTime == null }

    processManuallyScheduledTasks(db, manualTasks)

    // Order auto-scheduled tasks by heuristic
    val orderedAutoTasks = orderAutoScheduledTasks(db, autoTasks)

    // Get available time slots from task buckets
    val availableSlots = getAvailableTimeSlots(db)

    // Assign auto-scheduled tasks into available slots
    assignAutoScheduledTasks(db, orderedAutoTasks, availableSlots)
}

/* Process manually scheduled tasks */
@RequiresApi(Build.VERSION_CODES.O)
private suspend fun processManuallyScheduledTasks(db: AppDatabase, manualTasks: List<MasterTask>) {
    for (task in manualTasks) {
        val startTime = task.startTime!!
        val startDate = task.startDate!!
        val durationMinutes = task.predictedDuration

        // Calculate end time
        val endTime = startTime.plusMinutes(durationMinutes.toLong())

        // Create single interval
        val interval = TaskInterval(
            masterTaskId = task.id,
            intervalNo = 1,
            notes = task.notes,
            occurDate = startDate,
            startTime = startTime,
            endTime = endTime,
            status = 1,
            timeLeft = durationMinutes,
            overTime = 0
        )

        db.taskDao().insertInterval(interval)

        // Update master task
        db.taskDao().update(task.copy(noIntervals = 1))
    }
}

/* Order auto-scheduled tasks */
@RequiresApi(Build.VERSION_CODES.O)
private suspend fun orderAutoScheduledTasks(db: AppDatabase, autoTasks: List<MasterTask>): MutableList<OrderedTask> {
    val today = LocalDate.now()
    val allDeadlines = db.deadlineDao().getAll()

    return autoTasks.map { task ->
        val deadline = task.deadlineId?.let { deadlineId ->
            allDeadlines.find { it.id == deadlineId }
        }

        // Calculate urgency (days until deadline, null if no deadline)
        val urgency = deadline?.let {
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, it.date)
            daysUntil.toInt()
        }

        // Placeholder scores
        val categoryScore = 0 // TODO: Implement category score calculation
        val eventScore = 0 // TODO: Implement event score calculation

        Triple(task, urgency, Triple(categoryScore, eventScore, task.id))
    }.sortedWith(compareBy(
        { it.first.priority }, // Priority (1 = highest, 5 = lowest)
        { it.second ?: Int.MAX_VALUE }, // Urgency (fewer days = higher urgency, null = lowest)
        { it.third.first }, // Category Score placeholder
        { it.third.second }, // Event Score placeholder
        { it.third.third } // ID (lower = created first)
    )).map {
        OrderedTask(it.first, it.first.predictedDuration)
    }.toMutableList().also { orderedList ->
        // Apply dependency chain ordering
        resolveDependencyChains(orderedList)
    }
}

/* Resolve dependency chains to ensure dependent tasks are scheduled after their dependencies */
@RequiresApi(Build.VERSION_CODES.O)
private fun resolveDependencyChains(orderedTasks: MutableList<OrderedTask>) {
    // Build a map of task ID to index for quick lookup
    val taskIndexMap = mutableMapOf<Int, Int>()
    orderedTasks.forEachIndexed { index, orderedTask ->
        taskIndexMap[orderedTask.masterTask.id] = index
    }

    // Track tasks that have been moved to avoid infinite loops
    val movedTasks = mutableSetOf<Int>()

    // Iterate through tasks and check dependencies
    var i = 0
    while (i < orderedTasks.size) {
        val currentTask = orderedTasks[i]
        val dependencyId = currentTask.masterTask.dependencyTaskId

        if (dependencyId != null) {
            val dependencyIndex = taskIndexMap[dependencyId]

            // If dependency exists in the list and is after the current task
            if (dependencyIndex != null && dependencyIndex > i) {
                // Check if we've already moved this dependency to avoid loops
                if (!movedTasks.contains(dependencyId)) {
                    // Move the dependency task to be right before the current task
                    val dependencyTask = orderedTasks.removeAt(dependencyIndex)
                    orderedTasks.add(i, dependencyTask)

                    // Update index map
                    taskIndexMap[dependencyId] = i
                    orderedTasks.forEachIndexed { idx, task ->
                        taskIndexMap[task.masterTask.id] = idx
                    }

                    // Mark this task as moved
                    movedTasks.add(dependencyId)

                    // Don't increment i, recheck this position
                    continue
                }
            }
        }

        i++
    }
}

/* Get all available time slots */
@RequiresApi(Build.VERSION_CODES.O)
private suspend fun getAvailableTimeSlots(db: AppDatabase): MutableList<AvailableTimeSlot> {
    // Get all task bucket occurrences
    val bucketOccurrences = db.taskBucketDao().getAllBucketOccurrences()

    // Get all manually scheduled task intervals
    val manualIntervals = db.taskDao().getAllIntervals()

    // Convert bucket occurrences to available slots
    val availableSlots = mutableListOf<AvailableTimeSlot>()

    for (bucket in bucketOccurrences) {
        // Start with the full bucket time range
        var currentSlots = mutableListOf(
            AvailableTimeSlot(
                date = bucket.occurDate,
                startTime = bucket.startTime,
                endTime = bucket.endTime
            )
        )

        // Subtract all manual intervals on this date
        val manualIntervalsOnDate = manualIntervals.filter { it.occurDate == bucket.occurDate }

        for (manualInterval in manualIntervalsOnDate) {
            val newSlots = mutableListOf<AvailableTimeSlot>()

            for (slot in currentSlots) {
                // Check if manual interval overlaps with this slot
                if (doTimeRangesOverlap(
                        slot.startTime, slot.endTime,
                        manualInterval.startTime, manualInterval.endTime
                    )) {
                    // Split the slot around the manual interval
                    val splitSlots = subtractTimeRange(
                        slot,
                        manualInterval.startTime,
                        manualInterval.endTime
                    )
                    newSlots.addAll(splitSlots)
                } else {
                    // No overlap, keep the slot as is
                    newSlots.add(slot)
                }
            }

            currentSlots = newSlots
        }

        availableSlots.addAll(currentSlots)
    }

    // Sort by date and time
    return availableSlots.sortedWith(compareBy({ it.date }, { it.startTime })).toMutableList()
}

/* Subtract a time range from a slot, returning the remaining slots */
@RequiresApi(Build.VERSION_CODES.O)
private fun subtractTimeRange(
    slot: AvailableTimeSlot,
    subtractStart: LocalTime,
    subtractEnd: LocalTime
): List<AvailableTimeSlot> {
    val result = mutableListOf<AvailableTimeSlot>()

    // If the subtraction completely covers the slot, return empty
    if (!subtractStart.isAfter(slot.startTime) && !subtractEnd.isBefore(slot.endTime)) {
        return result
    }

    // If there's a slot before the subtraction
    if (slot.startTime.isBefore(subtractStart) && subtractStart.isBefore(slot.endTime)) {
        result.add(
            AvailableTimeSlot(
                date = slot.date,
                startTime = slot.startTime,
                endTime = minOf(subtractStart, slot.endTime)
            )
        )
    }

    // If there's a slot after the subtraction
    if (slot.startTime.isBefore(subtractEnd) && subtractEnd.isBefore(slot.endTime)) {
        result.add(
            AvailableTimeSlot(
                date = slot.date,
                startTime = maxOf(subtractEnd, slot.startTime),
                endTime = slot.endTime
            )
        )
    }

    return result
}

/* Compare LocalTime */
@RequiresApi(Build.VERSION_CODES.O)
private fun minOf(time1: LocalTime, time2: LocalTime): LocalTime {
    return if (time1.isBefore(time2)) time1 else time2
}

/* Compare LocalTime */
@RequiresApi(Build.VERSION_CODES.O)
private fun maxOf(time1: LocalTime, time2: LocalTime): LocalTime {
    return if (time1.isAfter(time2)) time1 else time2
}

/* Assign auto-scheduled tasks into available slots based on heuristic order */
@RequiresApi(Build.VERSION_CODES.O)
private suspend fun assignAutoScheduledTasks(
    db: AppDatabase,
    orderedTasks: MutableList<OrderedTask>,
    availableSlots: MutableList<AvailableTimeSlot>
) {
    val tasksToProcess = orderedTasks.toMutableList()

    while (tasksToProcess.isNotEmpty() && availableSlots.isNotEmpty()) {
        val currentTask = tasksToProcess.first()
        val task = currentTask.masterTask
        val durationNeeded = currentTask.remainingDuration

        // Try to find a slot that fits this task
        var assigned = false

        for (i in availableSlots.indices) {
            val slot = availableSlots[i]
            val slotDuration = slot.durationMinutes()

            if (slotDuration >= durationNeeded) {
                // Slot can fit the entire task (or remaining duration)
                val endTime = slot.startTime.plusMinutes(durationNeeded.toLong())

                // Get current interval count for this task
                val currentIntervals = db.taskDao().getIntervalsForTask(task.id)
                val intervalNo = currentIntervals.size + 1

                // Create interval
                val interval = TaskInterval(
                    masterTaskId = task.id,
                    intervalNo = intervalNo,
                    notes = task.notes,
                    occurDate = slot.date,
                    startTime = slot.startTime,
                    endTime = endTime,
                    status = 1,
                    timeLeft = durationNeeded,
                    overTime = 0
                )

                db.taskDao().insertInterval(interval)

                // Update master task noIntervals
                db.taskDao().update(task.copy(noIntervals = intervalNo))

                // Update the slot
                if (slotDuration == durationNeeded) {
                    // Slot is completely filled, remove it
                    availableSlots.removeAt(i)
                } else {
                    // Slot has remaining time, update it
                    availableSlots[i] = AvailableTimeSlot(
                        date = slot.date,
                        startTime = endTime,
                        endTime = slot.endTime
                    )
                }

                // Task is fully assigned
                tasksToProcess.removeAt(0)
                assigned = true
                break

            } else if (task.breakable == true && slotDuration >= MIN_INTERVAL_SIZE) {
                // Task is breakable and slot has enough space (>= MIN_INTERVAL_SIZE)
                // Assign partial interval
                val endTime = slot.endTime

                // Get current interval count for this task
                val currentIntervals = db.taskDao().getIntervalsForTask(task.id)
                val intervalNo = currentIntervals.size + 1

                // Create interval for this chunk
                val interval = TaskInterval(
                    masterTaskId = task.id,
                    intervalNo = intervalNo,
                    notes = task.notes,
                    occurDate = slot.date,
                    startTime = slot.startTime,
                    endTime = endTime,
                    status = 1,
                    timeLeft = slotDuration,
                    overTime = 0
                )

                db.taskDao().insertInterval(interval)

                // Update master task noIntervals
                db.taskDao().update(task.copy(noIntervals = intervalNo))

                // Remove this slot (completely used)
                availableSlots.removeAt(i)

                // Update task's remaining duration
                currentTask.remainingDuration -= slotDuration

                // Task is partially assigned, continue to next slot
                assigned = true
                break
            }
        }

        if (!assigned) {
            // No suitable slot found, task remains unassigned (intervalNo = 0)
            // Remove from processing list
            tasksToProcess.removeAt(0)
        }
    }
}