package com.planned

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId

// Type Converters
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

/* ENTITIES */

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
    val date: LocalDate,
    val time: LocalDateTime,
    val color: String?,
    val recurrence: String?,
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
    val recurrence: String?,
    val date: LocalDate?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val categoryId: Int?,
    val exclusiveToEventId: Int? // null = not exclusive, otherwise linked to an Event
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

    val predictedDuration: Int,
    val actualDuration: Int?,
    val priority: Int,
    val breakable: Boolean,

    val startDateTime: LocalDateTime?,
    val endDateTime: LocalDateTime?,

    val eventId: Int?,
    val deadlineId: Int?,
    val bucketId: Int?,
    val categoryId: Int?,

    val status: String,
    val manuallyAssigned: Boolean
)

/* ATI MODULE */

// Event-Specific
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

// General User-Wide
@Entity
data class UserATI(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val productivityByTimeJson: String?,
    val productivityByDayJson: String?,

    val reschedulingByTimeJson: String?,
    val reschedulingByDayJson: String?,

    val commonPaddingMinutes: Int?
)

/* RELATIONS */
data class TaskBucketWithTasks(
    @Embedded val bucket: TaskBucket,
    @Relation(
        parentColumn = "id",
        entityColumn = "bucketId"
    )
    val tasks: List<Task>
)

data class EventWithTasks(
    @Embedded val event: Event,
    @Relation(
        parentColumn = "id",
        entityColumn = "eventId"
    )
    val tasks: List<Task>
)

/* DAOs */

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

    // Delete
    @Query("DELETE FROM Event WHERE id = :eventId")
    suspend fun deleteById(eventId: Int)

    // Search by date
    @Query("SELECT * FROM Event WHERE date = :date")
    suspend fun getByDate(date: LocalDate): List<Event>
}


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

    // Delete
    @Query("DELETE FROM TaskBucket WHERE id = :bucketId")
    suspend fun deleteById(bucketId: Int)
}


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
    @Query("SELECT * FROM Task WHERE startDateTime >= :from AND endDateTime <= :to")
    suspend fun getTasksByDateRange(from: LocalDateTime, to: LocalDateTime): List<Task>
}


@Dao
interface EventATIDao {
    @Insert suspend fun insert(ati: EventATI)
    @Query("SELECT * FROM EventATI WHERE eventId = :eventId")
    suspend fun get(eventId: Int): EventATI?
}

@Dao
interface UserATIDao {
    @Insert suspend fun insert(ati: UserATI)
    @Query("SELECT * FROM UserATI LIMIT 1")
    suspend fun get(): UserATI?
}

/* DATABASE */
@Database(
    entities = [
        Category::class, Event::class, Task::class, TaskBucket::class, Deadline::class,
        EventATI::class, UserATI::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun deadlineDao(): DeadlineDao
    abstract fun taskBucketDao(): TaskBucketDao
    abstract fun taskDao(): TaskDao
    abstract fun eventATIDao(): EventATIDao
    abstract fun userATIDao(): UserATIDao
}