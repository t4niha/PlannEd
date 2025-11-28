package com.planned

import androidx.room.*
import java.time.LocalDate
import java.time.LocalTime

/* ENTITIES */

// Category
//<editor-fold desc="Category">

@Entity
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val notes: String? = null,
    val color: String
)
//</editor-fold>

// Event
//<editor-fold desc="Event">

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class MasterEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val notes: String? = null,
    val color: String? = null,

    val startDate: LocalDate,               // date when event occurs
    val endDate: LocalDate? = null,         // for recurring events only, date when occurrences stop

    val startTime: LocalTime,
    val endTime: LocalTime,

    val recurFreq: RecurrenceFrequency,     // only once / daily / weekly / monthly / yearly
    val recurRule: RecurrenceRule,          // none / days of week 1-7 / date 1-31 / date DD-MM

    val categoryId: Int? = null,
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MasterEvent::class,
            parentColumns = ["id"],
            childColumns = ["masterEventId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EventOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val masterEventId: Int,

    val notes: String? = null,

    val occurDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,

    val isException: Boolean = false      //  back-end access only, if individually changed
)
//</editor-fold>

// Deadline
//<editor-fold desc="Deadline">

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MasterEvent::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Deadline(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val notes: String? = null,

    val date: LocalDate,
    val time: LocalTime,

    val categoryId: Int? = null,
    val eventId: Int? = null
)
//</editor-fold>

// Task Bucket
//<editor-fold desc="Bucket">

@Entity
data class MasterTaskBucket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val startDate: LocalDate,               // date when event occurs
    val endDate: LocalDate? = null,         // for recurring events only, date when occurrences stop

    val startTime: LocalTime,
    val endTime: LocalTime,

    val recurFreq: RecurrenceFrequency,     // only once / daily / weekly / monthly / yearly
    val recurRule: RecurrenceRule,          // none / days of week 1-7 / date 1-31 / date DD-MM
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MasterTaskBucket::class,
            parentColumns = ["id"],
            childColumns = ["masterBucketId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskBucketOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val masterBucketId: Int,

    val occurDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,

    val isException: Boolean = false      //  back-end access only, if individually changed
)
//</editor-fold>

// Task
//<editor-fold desc="Task">

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MasterTaskBucket::class,
            parentColumns = ["id"],
            childColumns = ["bucketId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MasterEvent::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Deadline::class,
            parentColumns = ["id"],
            childColumns = ["deadlineId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class MasterTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val notes: String? = null,
    val priority: Int,                      // levels 1-5
    val breakable: Boolean? = false,        // can be checked true
    val noIntervals: Int,                   // how many intervals, 1 if not breakable

    val startDate: LocalDate? = null,       // null = auto, otherwise manual
    val startTime: LocalTime? = null,       // available only if startDate not null

    val predictedDuration: Int,
    val actualDuration: Int? = null,        // back-end access to this only, total final duration

    val status: Int? = 1,                   // 1 = not started, 2 = in progress, 3 = completed
    val timeLeft: Int? = null,              // auto calculated (sum of occurrences)
    val overTime: Int? = null,              // auto-calculated (sum of occurrences)

    val bucketId: Int? = null,              // back-end access to this only
    val eventId: Int? = null,
    val deadlineId: Int? = null,
    val categoryId: Int? = null
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MasterTask::class,
            parentColumns = ["id"],
            childColumns = ["masterTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskInterval(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val masterTaskId: Int,

    val intervalNo: Int,                    // interval number out of all intervals
    val notes: String? = null,

    val occurDate: LocalDate,

    val startTime: LocalTime,               // updates as task progresses (pomodoro)
    val endTime: LocalTime,

    val status: Int? = 1,
    val timeLeft: Int? = null,              // auto calculated (pomodoro)
    val overTime: Int? = null,              // auto-calculated (pomodoro)
)
//</editor-fold>

// Reminder
//<editor-fold desc="Category">

@Entity
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val notes: String? = null,
    val color: String,

    val date: LocalDate,
    val time: LocalTime,                // available if all day false
    val allDay: Boolean? = false        // can be checked true
)
//</editor-fold>

// AppSetting
//<editor-fold desc="Settings">

@Entity
data class AppSetting(
    @PrimaryKey val id: Int = 0,
    val startWeekOnMonday: Boolean,
    val primaryColor: String,
    val showDeveloper: Boolean
)
//</editor-fold>

/* ATI MODULE */
//<editor-fold desc="ATI">

// EventATI
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MasterEvent::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EventATI(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int,
    val avgActualVsPredicted: Float?,
    val avgDelayMinutes: Int?,
    val rescheduleCount: Int?,
    val bestTimesJson: String?,
    val worstTimesJson: String?
)

// UserATI
@Entity
data class UserATI(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productivityByTimeJson: String?,
    val productivityByDayJson: String?,
    val reschedulingByTimeJson: String?,
    val reschedulingByDayJson: String?,
    val commonPaddingMinutes: Int?
)
//</editor-fold>

/* RELATIONS */
//<editor-fold desc="Relations">

// Category
data class CategoryWithMasterEvents(
    @Embedded val category: Category,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val masterEvents: List<MasterEvent>
)
data class CategoryWithDeadlines(
    @Embedded val category: Category,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val deadlines: List<Deadline>
)
data class CategoryWithMasterTasks(
    @Embedded val category: Category,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val masterTasks: List<MasterTask>
)

// MasterEvent
data class MasterEventWithOccurrences(
    @Embedded val masterEvent: MasterEvent,
    @Relation(parentColumn = "id", entityColumn = "masterEventId")
    val occurrences: List<EventOccurrence>
)
data class MasterEventWithDeadlines(
    @Embedded val masterEvent: MasterEvent,
    @Relation(parentColumn = "id", entityColumn = "eventId")
    val deadlines: List<Deadline>
)
data class MasterEventWithTasks(
    @Embedded val masterEvent: MasterEvent,
    @Relation(parentColumn = "id", entityColumn = "eventId")
    val masterTasks: List<MasterTask>
)
data class MasterEventWithATI(
    @Embedded val masterEvent: MasterEvent,
    @Relation(parentColumn = "id", entityColumn = "eventId")
    val eventATI: EventATI?
)

// EventOccurrence
data class EventOccurrenceWithTasks(
    @Embedded val occurrence: EventOccurrence,
    @Relation(parentColumn = "masterEventId", entityColumn = "eventId")
    val tasks: List<MasterTask>
)

// MasterTaskBucket
data class MasterTaskBucketWithOccurrences(
    @Embedded val masterBucket: MasterTaskBucket,
    @Relation(parentColumn = "id", entityColumn = "masterBucketId")
    val occurrences: List<TaskBucketOccurrence>
)

// MasterTask
data class MasterTaskWithIntervals(
    @Embedded val masterTask: MasterTask,
    @Relation(parentColumn = "id", entityColumn = "masterTaskId")
    val intervals: List<TaskInterval>
)
data class MasterTaskWithEvent(
    @Embedded val masterTask: MasterTask,
    @Relation(parentColumn = "eventId", entityColumn = "id")
    val event: MasterEvent?
)
data class MasterTaskWithDeadline(
    @Embedded val masterTask: MasterTask,
    @Relation(parentColumn = "deadlineId", entityColumn = "id")
    val deadline: Deadline?
)
data class MasterTaskWithBucket(
    @Embedded val masterTask: MasterTask,
    @Relation(parentColumn = "bucketId", entityColumn = "id")
    val bucket: MasterTaskBucket?
)
//</editor-fold>

/* DAOs */
//<editor-fold desc="DAOs">

// Category
@Dao
interface CategoryDao {
    // Insert new category
    @Insert suspend fun insert(category: Category)

    // Fetch all categories
    @Query("SELECT * FROM Category") suspend fun getAll(): List<Category>

    // Fetch category by ID
    @Query("SELECT * FROM Category WHERE id = :categoryId") suspend fun getById(categoryId: Int): Category?

    // Fetch all master events linked to this category
    @Transaction
    @Query("SELECT * FROM MasterEvent WHERE categoryId = :categoryId")
    suspend fun getEventsForCategory(categoryId: Int): List<MasterEvent>

    // Update existing category
    @Update suspend fun update(category: Category)

    // Delete category by ID
    @Query("DELETE FROM Category WHERE id = :categoryId") suspend fun deleteById(categoryId: Int)
}

// Event
@Dao
interface EventDao {
    // Insert new master event
    @Insert suspend fun insert(event: MasterEvent)

    // Insert new event occurrence
    @Insert suspend fun insertOccurrence(occurrence: EventOccurrence)

    // Fetch all master events ordered by date and time
    @Query("SELECT * FROM MasterEvent ORDER BY startDate, startTime")
    suspend fun getAllMasterEvents(): List<MasterEvent>

    // Fetch all event occurrences ordered by date and time
    @Query("SELECT * FROM EventOccurrence ORDER BY occurDate, startTime")
    suspend fun getAllOccurrences(): List<EventOccurrence>

    // Fetch master events along with their occurrences
    @Transaction
    @Query("SELECT * FROM MasterEvent")
    suspend fun getAllEventsWithOccurrences(): List<MasterEventWithOccurrences>

    // Update master event
    @Update suspend fun update(event: MasterEvent)

    // Update event occurrence
    @Update suspend fun updateOccurrence(occurrence: EventOccurrence)

    // Delete event occurrence
    @Query("DELETE FROM EventOccurrence WHERE id = :occurrenceId")
    suspend fun deleteOccurrence(occurrenceId: Int)

    // Delete master event
    @Query("DELETE FROM MasterEvent WHERE id = :masterId")
    suspend fun deleteMasterEvent(masterId: Int)
}

// Deadline
@Dao
interface DeadlineDao {
    // Insert new deadline
    @Insert suspend fun insert(deadline: Deadline)

    // Fetch all deadlines
    @Query("SELECT * FROM Deadline") suspend fun getAll(): List<Deadline>

    // Fetch deadline by ID
    @Query("SELECT * FROM Deadline WHERE id = :deadlineId") suspend fun getById(deadlineId: Int): Deadline?

    // Update deadline
    @Update suspend fun update(deadline: Deadline)

    // Delete deadline by ID
    @Query("DELETE FROM Deadline WHERE id = :deadlineId") suspend fun deleteById(deadlineId: Int)

    // Fetch all deadlines for a specific event
    @Query("SELECT * FROM Deadline WHERE eventId = :eventId") suspend fun getByEvent(eventId: Int): List<Deadline>
}

// Task Bucket
@Dao
interface TaskBucketDao {
    // Insert new master task bucket
    @Insert suspend fun insert(masterBucket: MasterTaskBucket)

    // Insert new task bucket occurrence
    @Insert suspend fun insertOccurrence(bucketOccurrence: TaskBucketOccurrence)

    // Fetch all master buckets ordered by date and time
    @Query("SELECT * FROM MasterTaskBucket ORDER BY startDate, startTime")
    suspend fun getAllMasterBuckets(): List<MasterTaskBucket>

    // Fetch all bucket occurrences ordered by date and time
    @Query("SELECT * FROM TaskBucketOccurrence ORDER BY occurDate, startTime")
    suspend fun getAllBucketOccurrences(): List<TaskBucketOccurrence>

    // Fetch master buckets along with their occurrences
    @Transaction
    @Query("SELECT * FROM MasterTaskBucket")
    suspend fun getAllBucketsWithOccurrences(): List<MasterTaskBucketWithOccurrences>

    // Update master bucket
    @Update suspend fun update(masterBucket: MasterTaskBucket)

    // Update bucket occurrence
    @Update suspend fun updateOccurrence(bucketOccurrence: TaskBucketOccurrence)

    // Delete bucket occurrence
    @Query("DELETE FROM TaskBucketOccurrence WHERE id = :occurrenceId")
    suspend fun deleteOccurrence(occurrenceId: Int)

    // Delete master bucket
    @Query("DELETE FROM MasterTaskBucket WHERE id = :masterId")
    suspend fun deleteMasterBucket(masterId: Int)
}

// Task
@Dao
interface TaskDao {
    // Insert new master task
    @Insert suspend fun insert(task: MasterTask)

    // Insert new task interval
    @Insert suspend fun insertInterval(interval: TaskInterval)

    // Fetch all master tasks
    @Query("SELECT * FROM MasterTask") suspend fun getAllMasterTasks(): List<MasterTask>

    // Fetch all task intervals
    @Query("SELECT * FROM TaskInterval") suspend fun getAllIntervals(): List<TaskInterval>

    // Fetch master task by ID
    @Query("SELECT * FROM MasterTask WHERE id = :taskId") suspend fun getMasterTaskById(taskId: Int): MasterTask?

    // Fetch all intervals for a specific master task
    @Query("SELECT * FROM TaskInterval WHERE masterTaskId = :taskId") suspend fun getIntervalsForTask(taskId: Int): List<TaskInterval>

    // Update master task
    @Update suspend fun update(task: MasterTask)

    // Update task interval
    @Update suspend fun updateInterval(interval: TaskInterval)

    // Delete task interval
    @Query("DELETE FROM TaskInterval WHERE id = :intervalId")
    suspend fun deleteInterval(intervalId: Int)

    // Delete master task
    @Query("DELETE FROM MasterTask WHERE id = :taskId")
    suspend fun deleteMasterTask(taskId: Int)
}

// Reminder
@Dao
interface ReminderDao {
    // Insert new reminder
    @Insert
    suspend fun insert(reminder: Reminder)

    // Fetch all reminders
    @Query("SELECT * FROM Reminder ORDER BY date, time")
    suspend fun getAll(): List<Reminder>

    // Fetch reminder by ID
    @Query("SELECT * FROM Reminder WHERE id = :reminderId")
    suspend fun getById(reminderId: Int): Reminder?

    // Update reminder
    @Update
    suspend fun update(reminder: Reminder)

    // Delete reminder
    @Query("DELETE FROM Reminder WHERE id = :reminderId")
    suspend fun deleteById(reminderId: Int)
}

// AppSetting
@Dao
interface SettingsDao {
    // Insert all settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: AppSetting)

    // Field Updates
    @Query("UPDATE AppSetting SET primaryColor = :color WHERE id = 0")
    suspend fun updatePrimaryColor(color: String)
    @Query("UPDATE AppSetting SET startWeekOnMonday = :value WHERE id = 0")
    suspend fun updateStartWeekOnMonday(value: Boolean)
    @Query("UPDATE AppSetting SET showDeveloper = :value WHERE id = 0")
    suspend fun updateShowDeveloper(value: Boolean)

    // Fetch all settings
    @Query("SELECT * FROM AppSetting WHERE id = 0")
    suspend fun getAll(): AppSetting?

    // Delete all settings
    @Query("DELETE FROM AppSetting")
    suspend fun deleteAll()
}
//</editor-fold>

/* DATABASE */
//<editor-fold desc="Database">

@Database(
    entities = [
        Category::class,
        MasterEvent::class, EventOccurrence::class,
        Deadline::class,
        MasterTaskBucket::class, TaskBucketOccurrence::class,
        MasterTask::class, TaskInterval::class,
        Reminder::class,
        AppSetting::class,
        EventATI::class, UserATI::class
    ],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun taskBucketDao(): TaskBucketDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun settingsDao(): SettingsDao
}
//</editor-fold>