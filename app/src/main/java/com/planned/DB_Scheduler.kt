package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime

/**
Ordering Factors:
0) Dependency
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
    var remainingDuration: Int
)

/* Clear task intervals and regenerate */
@RequiresApi(Build.VERSION_CODES.O)
suspend fun generateTaskIntervals(db: AppDatabase) {
    // Clear task intervals, reset noIntervals
    db.taskDao().getAllIntervals().forEach { interval ->
        db.taskDao().deleteInterval(interval.id)
    }
    val allMasterTasks = db.taskDao()
        .getAllMasterTasks()
        .filter { it.status != 3 }
    allMasterTasks.forEach { task ->
        db.taskDao().update(task.copy(noIntervals = 0))
    }

    // Set manually scheduled tasks
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

/* Set manually scheduled tasks */
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

        // Calculate urgency
        val urgency = deadline?.let {
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, it.date)
            daysUntil.toInt()
        }

        // ATI scores
        val categoryScore = 0 // TODO: Category score calculation
        val eventScore = 0 // TODO: Event score calculation

        Triple(task, urgency, Triple(categoryScore, eventScore, task.id))
    }.sortedWith(compareBy(
        { it.first.priority },
        { it.second ?: Int.MAX_VALUE },
        { it.third.first },
        { it.third.second },
        { it.third.third }
    )).map {
        OrderedTask(it.first, it.first.predictedDuration)
    }.toMutableList().also { orderedList ->
        // Dependency chain ordering
        resolveDependencyChains(orderedList)
    }
}

/* Resolve dependency chains */
@RequiresApi(Build.VERSION_CODES.O)
private fun resolveDependencyChains(orderedTasks: MutableList<OrderedTask>) {
    // Map task ID to index
    val taskIndexMap = mutableMapOf<Int, Int>()
    orderedTasks.forEachIndexed { index, orderedTask ->
        taskIndexMap[orderedTask.masterTask.id] = index
    }

    // Track tasks that have been moved to avoid infinite loops
    val movedTasks = mutableSetOf<Int>()

    // Iterate through tasks, check dependencies
    var i = 0
    while (i < orderedTasks.size) {
        val currentTask = orderedTasks[i]
        val dependencyId = currentTask.masterTask.dependencyTaskId

        if (dependencyId != null) {
            val dependencyIndex = taskIndexMap[dependencyId]

            // If dependency exists and is after current task
            if (dependencyIndex != null && dependencyIndex > i) {
                // Check if already moved this dependency
                if (!movedTasks.contains(dependencyId)) {
                    // Move dependency task to right before current task
                    val dependencyTask = orderedTasks.removeAt(dependencyIndex)
                    orderedTasks.add(i, dependencyTask)

                    // Update index map
                    taskIndexMap[dependencyId] = i
                    orderedTasks.forEachIndexed { idx, task ->
                        taskIndexMap[task.masterTask.id] = idx
                    }

                    // Mark this task as moved
                    movedTasks.add(dependencyId)

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

        // Subtract all manual intervals
        val manualIntervalsOnDate = manualIntervals.filter { it.occurDate == bucket.occurDate }

        for (manualInterval in manualIntervalsOnDate) {
            val newSlots = mutableListOf<AvailableTimeSlot>()

            for (slot in currentSlots) {
                // Check if manual interval overlaps with slot
                if (doTimeRangesOverlap(
                        slot.startTime, slot.endTime,
                        manualInterval.startTime, manualInterval.endTime
                    )) {
                    // Split slot around manual interval
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

/* Subtract time range from a slot, return remaining slots */
@RequiresApi(Build.VERSION_CODES.O)
private fun subtractTimeRange(
    slot: AvailableTimeSlot,
    subtractStart: LocalTime,
    subtractEnd: LocalTime
): List<AvailableTimeSlot> {
    val result = mutableListOf<AvailableTimeSlot>()

    // If the subtraction completely covers slot, return empty
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

        // Find slot that fits this task
        var assigned = false

        for (i in availableSlots.indices) {
            val slot = availableSlots[i]
            val slotDuration = slot.durationMinutes()

            if (slotDuration >= durationNeeded) {
                // Slot can fit the entire task
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

                // Update slot
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
                // Task is breakable and slot has enough space, assign partial interval
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
            // No suitable slot found, task remains unassigned, remove from list
            tasksToProcess.removeAt(0)
        }
    }
}