package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId

/* RECURRENCE LOGIC */
//<editor-fold desc="Recurrence">

enum class RecurrenceFrequency {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val daysOfWeek: List<Int>? = null,       // 1=Mon ... 7=Sun
    val dayOfMonth: Int? = null              // Dates 1–31
)
//</editor-fold>

/* TYPE CONVERTERS */
//<editor-fold desc="Converters">

@RequiresApi(Build.VERSION_CODES.O)
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? =
        date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDate(millis: Long?): LocalDate? =
        millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): Long? =
        dateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDateTime(millis: Long?): LocalDateTime? =
        millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
}

class RecurrenceConverter {
    @TypeConverter
    fun fromRule(rule: RecurrenceRule?): String? =
        rule?.let { com.google.gson.Gson().toJson(it) }

    @TypeConverter
    fun toRule(json: String?): RecurrenceRule? =
        json?.let { com.google.gson.Gson().fromJson(it, RecurrenceRule::class.java) }
}
//</editor-fold>

/* ENTITIES */
//<editor-fold desc="Entities">

// Category
@Entity
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String?
)

// Event
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
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String?,
    val color: String?,

    val startDate: LocalDate,            // first day of occurrence
    val endDate: LocalDate?,             // optional end date

    val startTime: LocalDateTime,
    val endTime: LocalDateTime,

    val recurrence: RecurrenceRule?,                // master recurrence rule
    val parentRecurrenceId: Int? = null,            // null if master, else points to master
    val recurrenceInstanceDate: LocalDate? = null,  // actual date of this occurrence
    val isException: Boolean = false,               // edited instance exception

    val categoryId: Int?,
    val linkedDeadlineId: Int?
)

// Deadline
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Deadline(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String,
    val notes: String?,
    val eventId: Int?
)

// TaskBucket
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["exclusiveToEventId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TaskBucket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,

    val recurrence: RecurrenceRule?,
    val parentRecurrenceId: Int? = null,
    val recurrenceInstanceDate: LocalDate? = null,
    val isException: Boolean = false,

    val startDate: LocalDate?,
    val endDate: LocalDate?,

    val startTime: LocalDateTime,
    val endTime: LocalDateTime,

    val categoryId: Int?,
    val exclusiveToEventId: Int? // null = not exclusive
)

// Task
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = TaskBucket::class,
            parentColumns = ["id"],
            childColumns = ["bucketId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Event::class,
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
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String?,

    val startDateTime: LocalDateTime?,
    val predictedDuration: Int,
    val actualDuration: Int?,

    val priority: Int,
    val breakable: Boolean,

    val eventId: Int?,
    val deadlineId: Int?,
    val bucketId: Int?,
    val categoryId: Int?,

    val status: String,
    val manuallyAssigned: Boolean
)
//</editor-fold>

/* ATI MODULE */
//<editor-fold desc="ATI">

// EventATI
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
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

data class TaskBucketWithTasks(
    @Embedded val bucket: TaskBucket,
    @Relation(parentColumn = "id", entityColumn = "bucketId")
    val tasks: List<Task>
)

data class EventWithTasks(
    @Embedded val event: Event,
    @Relation(parentColumn = "id", entityColumn = "eventId")
    val tasks: List<Task>
)
//</editor-fold>

/* DAOs */
//<editor-fold desc="DAOs">

// Category
@Dao
interface CategoryDao {
    // Create
    @Insert suspend fun insert(category: Category)

    // Read
    @Query("SELECT * FROM Category") suspend fun getAll(): List<Category>
    @Query("SELECT * FROM Category WHERE id = :categoryId") suspend fun getById(categoryId: Int): Category?

    // Fetch all events linked to this category
    @Transaction
    @Query("SELECT * FROM Event WHERE categoryId = :categoryId")
    suspend fun getEventsForCategory(categoryId: Int): List<Event>

    // Update
    @Update suspend fun update(category: Category)

    // Delete
    @Query("DELETE FROM Category WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Int)
}

// Event
@Dao
interface EventDao {
    // Create
    @Insert suspend fun insert(event: Event)

    // Read
    @Query("SELECT * FROM Event") suspend fun getAll(): List<Event>
    @Query("SELECT * FROM Event WHERE id = :eventId") suspend fun getById(eventId: Int): Event?

    // Fetch event with tasks
    @Transaction
    @Query("SELECT * FROM Event WHERE id = :eventId")
    suspend fun getEventWithTasks(eventId: Int): EventWithTasks

    // Update
    @Update suspend fun update(event: Event)

    // Delete this occurrence only
    @Query("DELETE FROM Event WHERE id = :eventId")
    suspend fun deleteThisEvent(eventId: Int)

    // Delete this and following occurrences
    @Query("""
    DELETE FROM Event 
    WHERE id = :eventId 
       OR (parentRecurrenceId = :parentId AND recurrenceInstanceDate >= :date)
    """)
    suspend fun deleteThisAndFollowingEvents(eventId: Int, parentId: Int, date: LocalDate)

    // Delete all occurrences including master
    @Query("""
    DELETE FROM Event 
    WHERE id = :masterId OR parentRecurrenceId = :masterId
    """)
    suspend fun deleteAllEvents(masterId: Int)

    // Search by date
    @Query("SELECT * FROM Event WHERE recurrenceInstanceDate = :date")
    suspend fun getByDate(date: LocalDate): List<Event>
}

// Deadline
@Dao
interface DeadlineDao {
    // Create
    @Insert suspend fun insert(deadline: Deadline)

    // Read
    @Query("SELECT * FROM Deadline") suspend fun getAll(): List<Deadline>
    @Query("SELECT * FROM Deadline WHERE id = :deadlineId") suspend fun getById(deadlineId: Int): Deadline?

    // Update
    @Update suspend fun update(deadline: Deadline)

    // Delete
    @Query("DELETE FROM Deadline WHERE id = :deadlineId")
    suspend fun deleteById(deadlineId: Int)

    // Search by event
    @Query("SELECT * FROM Deadline WHERE eventId = :eventId")
    suspend fun getByEvent(eventId: Int): List<Deadline>
}

// TaskBucket
@Dao
interface TaskBucketDao {
    // Create
    @Insert suspend fun insert(bucket: TaskBucket)

    // Read
    @Query("SELECT * FROM TaskBucket") suspend fun getAll(): List<TaskBucket>
    @Query("SELECT * FROM TaskBucket WHERE id = :bucketId") suspend fun getById(bucketId: Int): TaskBucket?

    // Fetch buckets with tasks
    @Transaction
    @Query("SELECT * FROM TaskBucket")
    suspend fun getBucketsWithTasks(): List<TaskBucketWithTasks>

    // Fetch exclusive / non-exclusive
    @Query("SELECT * FROM TaskBucket WHERE exclusiveToEventId = :eventId")
    suspend fun getExclusiveBucketsForEvent(eventId: Int): List<TaskBucket>
    @Query("SELECT * FROM TaskBucket WHERE exclusiveToEventId IS NULL")
    suspend fun getNonExclusiveBuckets(): List<TaskBucket>

    // Update
    @Update suspend fun update(bucket: TaskBucket)

    // Delete this occurrence only
    @Query("DELETE FROM TaskBucket WHERE id = :bucketId")
    suspend fun deleteThisBucket(bucketId: Int)

    // Delete this and following occurrences
    @Query("""
    DELETE FROM TaskBucket 
    WHERE id = :bucketId 
       OR (parentRecurrenceId = :parentId AND recurrenceInstanceDate >= :date)
    """)
    suspend fun deleteThisAndFollowingBuckets(bucketId: Int, parentId: Int, date: LocalDate)

    // Delete all occurrences including master
    @Query("""
    DELETE FROM TaskBucket 
    WHERE id = :masterId OR parentRecurrenceId = :masterId
    """)
    suspend fun deleteAllBuckets(masterId: Int)


    // Search by date
    @Query("SELECT * FROM TaskBucket WHERE recurrenceInstanceDate = :date")
    suspend fun getByDate(date: LocalDate): List<TaskBucket>
}

// Task
@Dao
interface TaskDao {
    // Create
    @Insert suspend fun insert(task: Task)

    // Read
    @Query("SELECT * FROM Task") suspend fun getAll(): List<Task>
    @Query("SELECT * FROM Task WHERE id = :taskId") suspend fun getById(taskId: Int): Task?

    // Fetch all tasks for a bucket
    @Query("SELECT * FROM Task WHERE bucketId = :bucketId")
    suspend fun getTasksForBucket(bucketId: Int): List<Task>

    // Update
    @Update suspend fun update(task: Task)

    // Delete
    @Query("DELETE FROM Task WHERE id = :taskId")
    suspend fun deleteById(taskId: Int)

    // Search tasks by date range
    @Query("SELECT * FROM Task WHERE date(startDateTime) = :date")
    suspend fun getTasksByDate(date: LocalDate): List<Task>
}

// Event ATI
@Dao
interface EventATIDao {
    @Insert suspend fun insert(ati: EventATI)
    @Query("SELECT * FROM EventATI WHERE eventId = :eventId")
    suspend fun get(eventId: Int): EventATI?
}

// User ATI
@Dao
interface UserATIDao {
    @Insert suspend fun insert(ati: UserATI)
    @Query("SELECT * FROM UserATI LIMIT 1")
    suspend fun get(): UserATI?
}
//</editor-fold>

/* DATABASE */
//<editor-fold desc="Database">

@Database(
    entities = [
        Category::class, Event::class, Task::class, TaskBucket::class, Deadline::class,
        EventATI::class, UserATI::class
    ],
    version = 1
)
@TypeConverters(Converters::class, RecurrenceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun taskBucketDao(): TaskBucketDao
    abstract fun taskDao(): TaskDao
    abstract fun eventATIDao(): EventATIDao
    abstract fun userATIDao(): UserATIDao
}
//</editor-fold>