package com.planned

import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime
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

    val startDate: LocalDate,
    val endDate: LocalDate? = null,

    val startTime: LocalTime,
    val endTime: LocalTime,

    val recurFreq: RecurrenceFrequency,
    val recurRule: RecurrenceRule,

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
    val endTime: LocalTime
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

    val startDate: LocalDate,
    val endDate: LocalDate? = null,

    val startTime: LocalTime,
    val endTime: LocalTime,

    val recurFreq: RecurrenceFrequency,
    val recurRule: RecurrenceRule,
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

    val isException: Boolean = false
)
//</editor-fold>

// Task
//<editor-fold desc="Task">

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MasterTask::class,
            parentColumns = ["id"],
            childColumns = ["dependencyTaskId"],
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
    val allDay: LocalDate? = null,
    val breakable: Boolean? = false,
    val noIntervals: Int,

    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,

    val predictedDuration: Int,
    val actualDuration: Int? = null,

    val status: Int? = 1,
    val timeLeft: Int? = null,
    val overTime: Int? = null,
    val deadlineMissed: Boolean = false,
    val completedAt: LocalDateTime? = null,

    val dependencyTaskId: Int? = null,
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

    val intervalNo: Int,
    val notes: String? = null,

    val occurDate: LocalDate,

    val startTime: LocalTime,
    val endTime: LocalTime,

    val status: Int? = 1,
    val timeLeft: Int? = null,
    val overTime: Int? = null,
    val atiPadding: Int = 0
)
//</editor-fold>

// Reminder
//<editor-fold desc="Reminder">

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
data class MasterReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val notes: String? = null,

    val startDate: LocalDate,
    val endDate: LocalDate? = null,

    val time: LocalTime? = null,
    val allDay: Boolean,

    val recurFreq: RecurrenceFrequency,
    val recurRule: RecurrenceRule,

    val categoryId: Int? = null
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MasterReminder::class,
            parentColumns = ["id"],
            childColumns = ["masterReminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReminderOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val masterReminderId: Int,

    val notes: String? = null,

    val occurDate: LocalDate,
    val time: LocalTime? = null,
    val allDay: Boolean
)
//</editor-fold>

// ATI
//<editor-fold desc="ATI">

@Entity
data class CategoryATI(
    @PrimaryKey val categoryId: Int,
    val score: Float = 0f,
    val deadlineMissCount: Int = 0,
    val avgOvertime: Float = 0f,
    val tasksCompleted: Int = 0,
    val predictedPadding: Int = 0,
    val paddingSlope: Float = 0f,
    val paddingIntercept: Float = 0f
)

@Entity
data class EventATI(
    @PrimaryKey val eventId: Int,
    val score: Float = 0f,
    val deadlineMissCount: Int = 0,
    val avgOvertime: Float = 0f,
    val tasksCompleted: Int = 0,
    val predictedPadding: Int = 0,
    val paddingSlope: Float = 0f,
    val paddingIntercept: Float = 0f
)
//</editor-fold>

// Settings
//<editor-fold desc="Settings">

@Entity
data class AppSetting(
    @PrimaryKey val id: Int = 0,
    val startWeekOnMonday: Boolean = false,
    val primaryColor: String = "#FF4D4D4D",
    val breakDuration: Int = 5,
    val breakEvery: Int = 30,
    val atiPaddingEnabled: Boolean = true,

    val notifTasksEnabled: Boolean = true,
    val notifTaskAllDayTime: Int = 25200,
    val notifEventsEnabled: Boolean = true,
    val notifEventLeadMinutes: Int = 10,
    val notifRemindersEnabled: Boolean = true,
    val notifReminderAllDayTime: Int = 25200,
    val notifDeadlinesEnabled: Boolean = true,
    val notifDeadlineTiming: String = "TIME_OF",
    val notifDeadlineLeadMinutes: Int = 10,
    val notifDeadlineTime: Int = 25200
)
//</editor-fold>

// Academics
//<editor-fold desc="Academics">

@Entity
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val courseCode: String? = null,
    val description: String? = null,
    val credits: Int = 1,
    val year: Int,
    val semester: Int,          // 1=Spring, 2=Summer, 3=Fall, 4=Winter

    val weightQuiz: Float = 0f,
    val weightMid: Float = 0f,
    val weightAssignment: Float = 0f,
    val weightProject: Float = 0f,
    val weightFinal: Float = 0f,
    val weightLab: Float = 0f,
    val weightAttendance: Float = 0f,
    val weightParticipation: Float = 0f,
    val weightReport: Float = 0f,
    val weightPresentation: Float = 0f,
    val weightHomework: Float = 0f,
    val weightPractical: Float = 0f,
    val weightTutorial: Float = 0f,
    val weightOther: Float = 0f
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GradeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val courseId: Int,
    val type: GradeItemType,
    val title: String,
    val marksReceived: Float,
    val totalMarks: Float
)

enum class GradeItemType {
    QUIZ, MID, ASSIGNMENT, PROJECT, FINAL, LAB, ATTENDANCE, PARTICIPATION,
    REPORT, PRESENTATION, HOMEWORK, PRACTICAL, TUTORIAL, OTHER
}

@Entity
data class CompletedCourse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val courseTitle: String,
    val courseCode: String? = null,
    val description: String? = null,
    val credits: Int,
    val year: Int,
    val semester: Int,

    val calculatedGrade: Float,
    val submitGrade: String
)
@Entity
data class GradingScale(
    @PrimaryKey val id: Int = 0,
    val cgpa: Float = 0f,
    val gpaAPlus: Float? = 4.0f,
    val gpaA: Float? = 4.0f,
    val gpaAMinus: Float? = 3.7f,
    val gpaBPlus: Float? = 3.3f,
    val gpaB: Float? = 3.0f,
    val gpaBMinus: Float? = 2.7f,
    val gpaCPlus: Float? = 2.3f,
    val gpaC: Float? = 2.0f,
    val gpaCMinus: Float? = 1.7f,
    val gpaDPlus: Float? = 1.3f,
    val gpaD: Float? = 1.0f,
    val gpaDMinus: Float? = 0.7f,
    val gpaF: Float? = 0.0f,
    val gpaU: Float? = 0.0f,
    val gpaP: Float? = null,
    val gpaS: Float? = null,
    val gpaW: Float? = null,
    val gpaI: Float? = null,
    val gpaN: Float? = null,
    val gpaNp: Float? = null,
    val gpaNC: Float? = null
)
//</editor-fold>

/* DAOs */
//<editor-fold desc="DAOs">

// Category
@Dao
interface CategoryDao {
    @Insert suspend fun insert(category: Category): Long
    @Query("SELECT * FROM Category ORDER BY title ASC") suspend fun getAll(): List<Category>
    @Query("SELECT * FROM Category WHERE id = :categoryId") suspend fun getCategoryById(categoryId: Int): Category?
    @Update suspend fun update(category: Category)
    @Query("DELETE FROM Category WHERE id = :categoryId") suspend fun deleteById(categoryId: Int)
}

// Event
@Dao
interface EventDao {
    @Insert suspend fun insert(event: MasterEvent): Long
    @Insert suspend fun insertOccurrence(occurrence: EventOccurrence)
    @Query("SELECT * FROM MasterEvent ORDER BY title ASC") suspend fun getAllMasterEvents(): List<MasterEvent>
    @Query("SELECT * FROM MasterEvent WHERE id = :eventId") suspend fun getMasterEventById(eventId: Int): MasterEvent?
    @Query("SELECT * FROM EventOccurrence ORDER BY occurDate, startTime") suspend fun getAllOccurrences(): List<EventOccurrence>
    @Transaction @Query("SELECT * FROM MasterEvent") suspend fun getAllEventsWithOccurrences(): List<MasterEventWithOccurrences>
    @Update suspend fun update(event: MasterEvent)
    @Update suspend fun updateOccurrence(occurrence: EventOccurrence)
    @Query("DELETE FROM EventOccurrence WHERE id = :occurrenceId") suspend fun deleteOccurrence(occurrenceId: Int)
    @Query("DELETE FROM MasterEvent WHERE id = :masterId") suspend fun deleteMasterEvent(masterId: Int)
}

// Deadline
@Dao
interface DeadlineDao {
    @Insert suspend fun insert(deadline: Deadline): Long
    @Query("SELECT * FROM Deadline ORDER BY title ASC") suspend fun getAll(): List<Deadline>
    @Query("SELECT * FROM Deadline WHERE id = :deadlineId") suspend fun getDeadlineById(deadlineId: Int): Deadline?
    @Update suspend fun update(deadline: Deadline)
    @Query("DELETE FROM Deadline WHERE id = :deadlineId") suspend fun deleteById(deadlineId: Int)
}

// Task Bucket
@Dao
interface TaskBucketDao {
    @Insert suspend fun insert(masterBucket: MasterTaskBucket): Long
    @Insert suspend fun insertOccurrence(occurrence: TaskBucketOccurrence)
    @Query("SELECT * FROM MasterTaskBucket ORDER BY startDate, startTime") suspend fun getAllMasterBuckets(): List<MasterTaskBucket>
    @Query("SELECT * FROM MasterTaskBucket WHERE id = :bucketId") suspend fun getMasterBucketById(bucketId: Int): MasterTaskBucket?
    @Query("SELECT * FROM TaskBucketOccurrence ORDER BY occurDate, startTime") suspend fun getAllBucketOccurrences(): List<TaskBucketOccurrence>
    @Transaction @Query("SELECT * FROM MasterTaskBucket") suspend fun getAllBucketsWithOccurrences(): List<MasterTaskBucketWithOccurrences>
    @Update suspend fun update(masterBucket: MasterTaskBucket)
    @Update suspend fun updateOccurrence(bucketOccurrence: TaskBucketOccurrence)
    @Query("DELETE FROM TaskBucketOccurrence WHERE id = :occurrenceId") suspend fun deleteOccurrence(occurrenceId: Int)
    @Query("DELETE FROM MasterTaskBucket WHERE id = :masterId") suspend fun deleteMasterBucket(masterId: Int)
    @Query("SELECT * FROM TaskBucketOccurrence WHERE occurDate = :date") suspend fun getOccurrencesByDate(date: LocalDate): List<TaskBucketOccurrence>
    @Update suspend fun updateBucketOccurrence(occurrence: TaskBucketOccurrence)
}

// Task
@Dao
interface TaskDao {
    @Insert suspend fun insert(task: MasterTask): Long
    @Insert suspend fun insertInterval(interval: TaskInterval)
    @Query("SELECT * FROM MasterTask") suspend fun getAllMasterTasks(): List<MasterTask>
    @Query("SELECT * FROM TaskInterval") suspend fun getAllIntervals(): List<TaskInterval>
    @Query("SELECT * FROM MasterTask WHERE id = :taskId") suspend fun getMasterTaskById(taskId: Int): MasterTask?
    @Query("SELECT * FROM TaskInterval WHERE masterTaskId = :taskId") suspend fun getIntervalsForTask(taskId: Int): List<TaskInterval>
    @Update suspend fun update(task: MasterTask)
    @Update suspend fun updateInterval(interval: TaskInterval)
    @Query("DELETE FROM TaskInterval WHERE id = :intervalId") suspend fun deleteInterval(intervalId: Int)
    @Query("DELETE FROM MasterTask WHERE id = :taskId") suspend fun deleteMasterTask(taskId: Int)
}

// Reminder
@Dao
interface ReminderDao {
    @Insert suspend fun insert(reminder: MasterReminder): Long
    @Insert suspend fun insertOccurrence(occurrence: ReminderOccurrence)
    @Query("SELECT * FROM MasterReminder ORDER BY startDate, time") suspend fun getAllMasterReminders(): List<MasterReminder>
    @Query("SELECT * FROM MasterReminder WHERE id = :reminderId") suspend fun getMasterReminderById(reminderId: Int): MasterReminder?
    @Query("SELECT * FROM ReminderOccurrence ORDER BY occurDate, time") suspend fun getAllOccurrences(): List<ReminderOccurrence>
    @Transaction @Query("SELECT * FROM MasterReminder") suspend fun getAllRemindersWithOccurrences(): List<MasterReminderWithOccurrences>
    @Update suspend fun update(reminder: MasterReminder)
    @Update suspend fun updateOccurrence(occurrence: ReminderOccurrence)
    @Query("DELETE FROM ReminderOccurrence WHERE id = :occurrenceId") suspend fun deleteOccurrence(occurrenceId: Int)
    @Query("DELETE FROM MasterReminder WHERE id = :masterId") suspend fun deleteMasterReminder(masterId: Int)
}

// CategoryATI
@Dao
interface CategoryATIDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(categoryATI: CategoryATI)
    @Query("SELECT * FROM CategoryATI WHERE categoryId = :categoryId") suspend fun getById(categoryId: Int): CategoryATI?
    @Query("SELECT * FROM CategoryATI WHERE categoryId = 0") suspend fun getNoneRecord(): CategoryATI?
    @Query("SELECT * FROM CategoryATI") suspend fun getAll(): List<CategoryATI>
    @Update suspend fun update(categoryATI: CategoryATI)
    @Query("DELETE FROM CategoryATI WHERE categoryId = :categoryId") suspend fun deleteById(categoryId: Int)
}

// EventATI
@Dao
interface EventATIDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(eventATI: EventATI)
    @Query("SELECT * FROM EventATI WHERE eventId = :eventId") suspend fun getById(eventId: Int): EventATI?
    @Query("SELECT * FROM EventATI WHERE eventId = 0") suspend fun getNoneRecord(): EventATI?
    @Query("SELECT * FROM EventATI") suspend fun getAll(): List<EventATI>
    @Update suspend fun update(eventATI: EventATI)
    @Query("DELETE FROM EventATI WHERE eventId = :eventId") suspend fun deleteById(eventId: Int)
}

// AppSetting
@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(setting: AppSetting)
    @Query("UPDATE AppSetting SET primaryColor = :color WHERE id = 0") suspend fun updatePrimaryColor(color: String)
    @Query("UPDATE AppSetting SET startWeekOnMonday = :value WHERE id = 0") suspend fun updateStartWeekOnMonday(value: Boolean)
    @Query("UPDATE AppSetting SET breakDuration = :minutes WHERE id = 0") suspend fun updateBreakDuration(minutes: Int)
    @Query("UPDATE AppSetting SET breakEvery = :minutes WHERE id = 0") suspend fun updateBreakEvery(minutes: Int)
    @Query("UPDATE AppSetting SET atiPaddingEnabled = :value WHERE id = 0") suspend fun updateAtiPaddingEnabled(value: Boolean)
    @Query("UPDATE AppSetting SET notifTasksEnabled = :value WHERE id = 0") suspend fun updateNotifTasksEnabled(value: Boolean)
    @Query("UPDATE AppSetting SET notifEventsEnabled = :value WHERE id = 0") suspend fun updateNotifEventsEnabled(value: Boolean)
    @Query("UPDATE AppSetting SET notifEventLeadMinutes = :minutes WHERE id = 0") suspend fun updateNotifEventLeadMinutes(minutes: Int)
    @Query("UPDATE AppSetting SET notifRemindersEnabled = :value WHERE id = 0") suspend fun updateNotifRemindersEnabled(value: Boolean)
    @Query("UPDATE AppSetting SET notifReminderAllDayTime = :seconds WHERE id = 0") suspend fun updateNotifReminderAllDayTime(seconds: Int)
    @Query("UPDATE AppSetting SET notifTaskAllDayTime = :seconds WHERE id = 0") suspend fun updateNotifTaskAllDayTime(seconds: Int)
    @Query("UPDATE AppSetting SET notifDeadlinesEnabled = :value WHERE id = 0") suspend fun updateNotifDeadlinesEnabled(value: Boolean)
    @Query("UPDATE AppSetting SET notifDeadlineTiming = :timing WHERE id = 0") suspend fun updateNotifDeadlineTiming(timing: String)
    @Query("UPDATE AppSetting SET notifDeadlineTime = :seconds WHERE id = 0") suspend fun updateNotifDeadlineTime(seconds: Int)
    @Query("UPDATE AppSetting SET notifDeadlineLeadMinutes = :minutes WHERE id = 0") suspend fun updateNotifDeadlineLeadMinutes(minutes: Int)
    @Query("SELECT * FROM AppSetting WHERE id = 0") suspend fun getAll(): AppSetting?
    @Query("DELETE FROM AppSetting") suspend fun deleteAll()
}

// Course
@Dao
interface CourseDao {
    @Insert suspend fun insert(course: Course): Long
    @Query("SELECT * FROM Course ORDER BY year DESC, semester DESC, title ASC") suspend fun getAll(): List<Course>
    @Query("SELECT * FROM Course WHERE id = :courseId") suspend fun getById(courseId: Int): Course?
    @Update suspend fun update(course: Course)
    @Query("DELETE FROM Course WHERE id = :courseId") suspend fun deleteById(courseId: Int)
}

// GradeItem
@Dao
interface GradeItemDao {
    @Insert suspend fun insert(item: GradeItem): Long
    @Query("SELECT * FROM GradeItem WHERE courseId = :courseId") suspend fun getByCourseId(courseId: Int): List<GradeItem>
    @Query("SELECT * FROM GradeItem WHERE id = :itemId") suspend fun getById(itemId: Int): GradeItem?
    @Update suspend fun update(item: GradeItem)
    @Query("DELETE FROM GradeItem WHERE id = :itemId") suspend fun deleteById(itemId: Int)
    @Query("DELETE FROM GradeItem WHERE courseId = :courseId") suspend fun deleteByCourseId(courseId: Int)
}

// CompletedCourse
@Dao
interface CompletedCourseDao {
    @Insert suspend fun insert(course: CompletedCourse): Long
    @Query("SELECT * FROM CompletedCourse ORDER BY year DESC, semester DESC, courseTitle ASC") suspend fun getAll(): List<CompletedCourse>
    @Query("SELECT * FROM CompletedCourse WHERE id = :id") suspend fun getById(id: Int): CompletedCourse?
    @Update suspend fun update(course: CompletedCourse)
    @Query("DELETE FROM CompletedCourse WHERE id = :id") suspend fun deleteById(id: Int)
}

// GradingScale
@Dao
interface GradingScaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(scale: GradingScale)
    @Query("SELECT * FROM GradingScale WHERE id = 0") suspend fun get(): GradingScale?
    @Update suspend fun update(scale: GradingScale)
    @Query("DELETE FROM GradingScale") suspend fun delete()
}
//</editor-fold>

/* RELATIONS */
//<editor-fold desc="Relations">

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
data class CategoryWithATI(
    @Embedded val category: Category,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val categoryATI: CategoryATI?
)
data class EventOccurrenceWithTasks(
    @Embedded val occurrence: EventOccurrence,
    @Relation(parentColumn = "masterEventId", entityColumn = "eventId")
    val tasks: List<MasterTask>
)
data class MasterTaskBucketWithOccurrences(
    @Embedded val masterBucket: MasterTaskBucket,
    @Relation(parentColumn = "id", entityColumn = "masterBucketId")
    val occurrences: List<TaskBucketOccurrence>
)
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
data class MasterTaskWithDependency(
    @Embedded val masterTask: MasterTask,
    @Relation(parentColumn = "dependencyTaskId", entityColumn = "id")
    val dependencyTask: MasterTask?
)
data class MasterReminderWithOccurrences(
    @Embedded val masterReminder: MasterReminder,
    @Relation(parentColumn = "id", entityColumn = "masterReminderId")
    val occurrences: List<ReminderOccurrence>
)
data class CategoryWithMasterReminders(
    @Embedded val category: Category,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val masterReminders: List<MasterReminder>
)
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
        MasterReminder::class, ReminderOccurrence::class,
        AppSetting::class,
        CategoryATI::class, EventATI::class,
        Course::class, GradeItem::class, CompletedCourse::class,
        GradingScale::class
    ],
    version = 17
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
    abstract fun categoryATIDao(): CategoryATIDao
    abstract fun eventATIDao(): EventATIDao
    abstract fun courseDao(): CourseDao
    abstract fun gradeItemDao(): GradeItemDao
    abstract fun completedCourseDao(): CompletedCourseDao
    abstract fun gradingScaleDao(): GradingScaleDao
}
//</editor-fold>