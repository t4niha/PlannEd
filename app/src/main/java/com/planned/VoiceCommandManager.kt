package com.planned

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

// ─────────────────────────────────────────────────────────────────
// Voice phases
// ─────────────────────────────────────────────────────────────────
enum class VoicePhase {
    IDLE, LISTENING, THINKING, SPEAKING, ERROR
}

// ─────────────────────────────────────────────────────────────────
// A fully-resolved pending action
// ─────────────────────────────────────────────────────────────────
data class VoicePendingAction(
    val actionType: String,
    val entityLabel: String,
    val summaryFields: List<Pair<String, String>>,  // used for CREATE, DELETE, and EDIT
    val changedFields: Set<String>,                 // field labels that changed — for highlighting in UI
    val replyText: String,
    val execute: suspend () -> Unit
)

data class VoiceResult(
    val userText: String,
    val replyText: String,
    val actionTaken: String? = null
)

// ─────────────────────────────────────────────────────────────────
// VoiceCommandManager
// ─────────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
object VoiceCommandManager {

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile var onPhaseChange: (VoicePhase) -> Unit = {}
    @Volatile var onPendingAction: (VoicePendingAction) -> Unit = {}
    @Volatile var onResult: (VoiceResult) -> Unit = {}

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false

    private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

    // ── TTS ───────────────────────────────────────────────────────
    fun initTts(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object :
                    android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        managerScope.launch { onPhaseChange(VoicePhase.IDLE) }
                    }
                    override fun onError(utteranceId: String?) {
                        managerScope.launch { onPhaseChange(VoicePhase.IDLE) }
                    }
                })
                isTtsReady = true
            }
        }
    }

    fun releaseTts() {
        tts?.stop(); tts?.shutdown(); tts = null; isTtsReady = false
        speechRecognizer?.destroy(); speechRecognizer = null
    }

    fun speakOut(text: String) {
        if (isTtsReady) {
            onPhaseChange(VoicePhase.SPEAKING)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "planned_vc")
        } else {
            onPhaseChange(VoicePhase.IDLE)
        }
    }

    fun cancelSpeech() { tts?.stop(); onPhaseChange(VoicePhase.IDLE) }

    // ── Listening ─────────────────────────────────────────────────
    @androidx.annotation.MainThread
    fun startListening(context: Context, db: AppDatabase) {
        cancelSpeech()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { onPhaseChange(VoicePhase.ERROR); return }
        onPhaseChange(VoicePhase.LISTENING)
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 600000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600000L)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                onPhaseChange(VoicePhase.ERROR)
                CoroutineScope(Dispatchers.Main).launch { onPhaseChange(VoicePhase.IDLE) }
            }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (spoken.isNullOrBlank()) { onPhaseChange(VoicePhase.ERROR); return }
                onPhaseChange(VoicePhase.THINKING)
                CoroutineScope(Dispatchers.Main).launch { handleSpokenCommand(context, db, spoken) }
            }
        })
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() { speechRecognizer?.stopListening() }

    // ── Helpers ───────────────────────────────────────────────────
    private fun parseRecurFreq(v: String) =
        runCatching { RecurrenceFrequency.valueOf(v.uppercase()) }.getOrDefault(RecurrenceFrequency.NONE)

    private fun parseRecurRule(j: JSONObject): RecurrenceRule {
        val dow = if (j.has("recur_days_of_week")) j.getJSONArray("recur_days_of_week")
            .let { a -> (0 until a.length()).map { a.getInt(it) } } else null
        val dom = if (j.has("recur_days_of_month")) j.getJSONArray("recur_days_of_month")
            .let { a -> (0 until a.length()).map { a.getInt(it) } } else null
        val mad = if (j.has("recur_month") && j.has("recur_day"))
            Pair(j.getInt("recur_day"), j.getInt("recur_month")) else null
        return RecurrenceRule(daysOfWeek = dow, daysOfMonth = dom, monthAndDay = mad)
    }

    private fun hexToColor(hex: String): Color =
        runCatching { Converters.toColor(hex) }.getOrDefault(Color(0xFF888780))

    private fun fmtRecur(freq: RecurrenceFrequency, rule: RecurrenceRule) =
        freq.name.lowercase().replaceFirstChar { it.uppercase() } + when (freq) {
            RecurrenceFrequency.WEEKLY  -> rule.daysOfWeek?.sorted()?.joinToString(", ") { d ->
                when (d) { 1->"Mo";2->"Tu";3->"We";4->"Th";5->"Fr";6->"Sa";7->"Su";else->"" }
            }?.let { " ($it)" } ?: ""
            RecurrenceFrequency.MONTHLY -> rule.daysOfMonth?.sorted()?.joinToString(", ")?.let { " ($it)" } ?: ""
            RecurrenceFrequency.YEARLY  -> rule.monthAndDay?.let {
                " (${java.time.Month.of(it.second).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())} ${it.first})"
            } ?: ""
            else -> ""
        }

    private fun fmtDur(min: Int): String {
        val h = min / 60; val m = min % 60
        return when { h > 0 && m > 0 -> "${h}h ${m}m"; h > 0 -> "${h}h"; else -> "${m}m" }
    }

    // Helper: format a field value for edit — shows "old → new" if changed, else just value
    private fun fmtEdit(oldVal: String, newVal: String) =
        if (oldVal != newVal) "$oldVal → $newVal" else newVal

    // ── Context snapshot ──────────────────────────────────────────
    private suspend fun buildContextSnapshot(db: AppDatabase): String {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        val allMasterTasks = db.taskDao().getAllMasterTasks()
        val allEvents = db.eventDao().getAllMasterEvents()
        val allReminders = db.reminderDao().getAllMasterReminders()
        val allDeadlines = db.deadlineDao().getAll()
        val categories = db.categoryDao().getAll()
        val buckets = db.taskBucketDao().getAllMasterBuckets()

        val taskLines = db.taskDao().getAllIntervals().filter { it.occurDate == today }.mapNotNull { interval ->
            val m = allMasterTasks.find { it.id == interval.masterTaskId } ?: return@mapNotNull null
            val s = when (m.status) { 3->"done";2->"in-progress";else->"pending" }
            "  • Task \"${m.title}\" (id=${m.id}) ${interval.startTime}–${interval.endTime} [$s]"
        }
        val eventLines = db.eventDao().getAllOccurrences().filter { it.occurDate == today }.mapNotNull { occ ->
            val m = allEvents.find { it.id == occ.masterEventId } ?: return@mapNotNull null
            "  • Event \"${m.title}\" (id=${m.id}) ${occ.startTime}–${occ.endTime}"
        }
        val reminderLines = db.reminderDao().getAllOccurrences().filter { it.occurDate == today }.mapNotNull { occ ->
            val m = allReminders.find { it.id == occ.masterReminderId } ?: return@mapNotNull null
            "  • Reminder \"${m.title}\" (id=${m.id}) at ${occ.time?.toString() ?: "all-day"}"
        }
        val deadlineLines = allDeadlines
            .filter { !it.date.isBefore(today) && !it.date.isAfter(today.plusDays(7)) }
            .map { "  • Deadline \"${it.title}\" (id=${it.id}) on ${it.date} at ${it.time}" }

        fun fmtRuleInline(freq: RecurrenceFrequency, rule: RecurrenceRule): String = when (freq) {
            RecurrenceFrequency.NONE    -> "once"
            RecurrenceFrequency.DAILY   -> "daily"
            RecurrenceFrequency.WEEKLY  -> "weekly on ${rule.daysOfWeek?.sorted()?.joinToString(",") { d ->
                when (d) { 1->"Mon";2->"Tue";3->"Wed";4->"Thu";5->"Fri";6->"Sat";7->"Sun";else->"" }
            } ?: "?"}"
            RecurrenceFrequency.MONTHLY -> "monthly on day(s) ${rule.daysOfMonth?.sorted()?.joinToString(",") ?: "?"}"
            RecurrenceFrequency.YEARLY  -> rule.monthAndDay?.let {
                "yearly on ${java.time.Month.of(it.second).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())} ${it.first}"
            } ?: "yearly"
        }

        return buildString {
            appendLine("TODAY: $todayStr"); appendLine()
            if (taskLines.isNotEmpty())     { appendLine("TASKS TODAY:");                     taskLines.forEach     { appendLine(it) }; appendLine() }
            if (eventLines.isNotEmpty())    { appendLine("EVENTS TODAY:");                    eventLines.forEach    { appendLine(it) }; appendLine() }
            if (reminderLines.isNotEmpty()) { appendLine("REMINDERS TODAY:");                 reminderLines.forEach { appendLine(it) }; appendLine() }
            if (deadlineLines.isNotEmpty()) { appendLine("UPCOMING DEADLINES (next 7 days):"); deadlineLines.forEach { appendLine(it) }; appendLine() }

            appendLine("ALL TASKS (pending):")
            allMasterTasks.filter { it.status != 3 }.forEach { t ->
                val catT  = t.categoryId?.let { categories.find { c -> c.id == it }?.title } ?: "None"
                val evT   = t.eventId?.let   { allEvents.find   { e -> e.id == it }?.title } ?: "None"
                val dlT   = t.deadlineId?.let { allDeadlines.find { d -> d.id == it }?.title } ?: "None"
                val sched = if (t.startDate != null && t.startTime != null) "${t.startDate} at ${t.startTime}" else "auto-scheduled"
                appendLine("  • \"${t.title}\" (id=${t.id}, duration=${t.predictedDuration}min, breakable=${t.breakable ?: false}, schedule=$sched, deadline=$dlT, event=$evT, category=$catT, notes=${t.notes ?: "—"})")
            }
            appendLine()

            appendLine("ALL EVENTS:")
            allEvents.forEach { e ->
                val catT = e.categoryId?.let { categories.find { c -> c.id == it }?.title } ?: "None"
                appendLine("  • \"${e.title}\" (id=${e.id}, date=${e.startDate}, endDate=${e.endDate ?: "N/A"}, time=${e.startTime}–${e.endTime}, recurrence=${fmtRuleInline(e.recurFreq, e.recurRule)}, category=$catT, notes=${e.notes ?: "—"})")
            }
            appendLine()

            appendLine("ALL REMINDERS:")
            allReminders.forEach { r ->
                val catT    = r.categoryId?.let { categories.find { c -> c.id == it }?.title } ?: "None"
                val timeStr = if (r.allDay) "all-day" else r.time?.toString() ?: "—"
                appendLine("  • \"${r.title}\" (id=${r.id}, date=${r.startDate}, endDate=${r.endDate ?: "N/A"}, time=$timeStr, recurrence=${fmtRuleInline(r.recurFreq, r.recurRule)}, category=$catT, notes=${r.notes ?: "—"})")
            }
            appendLine()

            appendLine("ALL DEADLINES:")
            allDeadlines.forEach { d ->
                val catT = d.categoryId?.let { categories.find { c -> c.id == it }?.title } ?: "None"
                val evT  = d.eventId?.let   { allEvents.find   { e -> e.id == it }?.title } ?: "None"
                appendLine("  • \"${d.title}\" (id=${d.id}, date=${d.date}, time=${d.time}, event=$evT, category=$catT, notes=${d.notes ?: "—"})")
            }
            appendLine()

            appendLine("ALL CATEGORIES: ${categories.joinToString(", ") { "\"${it.title}\" (id=${it.id})" }}")
            appendLine("ALL TASK BUCKETS: ${buckets.joinToString(", ") { "\"${it.startTime}–${it.endTime} ${fmtRuleInline(it.recurFreq, it.recurRule)}\" (id=${it.id})" }}")
        }
    }

    // ── Gemini ────────────────────────────────────────────────────
    private suspend fun callGemini(systemPrompt: String, userMessage: String): String =
        withTimeout(20_000L) {
            withContext(Dispatchers.IO) {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; setRequestProperty("Content-Type", "application/json")
                    doOutput = true; connectTimeout = 15_000; readTimeout = 30_000
                }
                val body = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", org.json.JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
                    })
                    put("contents", org.json.JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", org.json.JSONArray().put(JSONObject().apply { put("text", userMessage) }))
                    }))
                    put("generationConfig", JSONObject().apply { put("responseMimeType", "application/json") })
                }.toString()
                conn.outputStream.use { it.write(body.toByteArray()) }
                JSONObject(conn.inputStream.bufferedReader().readText())
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            }
        }

    // ── Build pending action ──────────────────────────────────────
    suspend fun buildPendingAction(context: Context, db: AppDatabase, json: JSONObject): VoicePendingAction? {
        val action  = json.optString("action", "REPLY")
        val reply   = json.optString("reply", "Done.")
        val today   = LocalDate.now()
        val defDate = today.plusDays(1)
        val defTime = LocalTime.of(10, 0)
        val defEndT = LocalTime.of(11, 0)

        when (action) {

            // ── TASK ──────────────────────────────────────────────
            "CREATE_TASK" -> {
                val title     = json.getString("title")
                val dur       = json.optInt("duration_minutes", 60)
                val breakable = json.optBoolean("breakable", false)
                val sdStr     = json.optString("start_date", "")
                val stStr     = json.optString("start_time", "")
                val notes     = json.optString("notes", "").ifBlank { null }
                val eventId   = json.optInt("event_id", -1).takeIf { it != -1 }
                val dlId      = json.optInt("deadline_id", -1).takeIf { it != -1 }
                val sd = if (sdStr.isNotBlank()) LocalDate.parse(sdStr) else null
                val st = if (stStr.isNotBlank()) LocalTime.parse(stStr) else null
                val resolvedDl    = dlId?.let { db.deadlineDao().getDeadlineById(it) }
                val resolvedEvId  = resolvedDl?.eventId ?: eventId
                val resolvedEvent = resolvedEvId?.let { db.eventDao().getMasterEventById(it) }
                val catId = when {
                    resolvedEvent != null -> resolvedEvent.categoryId
                    resolvedDl    != null -> resolvedDl.categoryId
                    else                  -> json.optInt("category_id", -1).takeIf { it != -1 }
                }
                val catT     = catId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val evT      = resolvedEvId?.let { db.eventDao().getMasterEventById(it)?.title } ?: "None"
                val dlT      = dlId?.let { db.deadlineDao().getDeadlineById(it)?.title } ?: "None"
                val schedStr = if (sd != null && st != null) "${sd.format(dateFmt)} at ${st.format(timeFmt)}" else "Auto-scheduled"
                return VoicePendingAction(action, "Task", listOf(
                    "Title"     to title,
                    "Duration"  to fmtDur(dur),
                    "Breakable" to if (breakable) "Yes" else "No",
                    "Schedule"  to schedStr,
                    "Deadline"  to dlT,
                    "Event"     to evT,
                    "Category"  to catT,
                    "Notes"     to (notes ?: "—")
                ), emptySet(), reply) {
                    TaskManager.insert(context=context, db=db, title=title, notes=notes, allDay=null,
                        breakable=breakable, startDate=sd, startTime=st, predictedDuration=dur,
                        categoryId=catId, eventId=resolvedEvId, deadlineId=dlId, dependencyTaskId=null)
                }
            }

            "EDIT_TASK" -> {
                val taskId    = json.getInt("task_id")
                val task      = db.taskDao().getMasterTaskById(taskId) ?: return null
                val title     = json.optString("title", "").ifBlank { task.title }
                val dur       = if (json.has("duration_minutes") && !json.isNull("duration_minutes")) json.getInt("duration_minutes") else task.predictedDuration
                val breakable = if (json.has("breakable") && !json.isNull("breakable")) json.getBoolean("breakable") else (task.breakable ?: false)
                val sdStr     = json.optString("start_date", "")
                val stStr     = json.optString("start_time", "")
                val notes     = json.optString("notes", "").ifBlank { task.notes }
                val evId      = json.optInt("event_id", -1).takeIf { it != -1 } ?: task.eventId
                val dlId      = json.optInt("deadline_id", -1).takeIf { it != -1 } ?: task.deadlineId
                val nsd       = if (sdStr.isNotBlank()) LocalDate.parse(sdStr) else task.startDate
                val nst       = if (stStr.isNotBlank()) LocalTime.parse(stStr) else task.startTime
                val resolvedDl    = dlId?.let { db.deadlineDao().getDeadlineById(it) }
                val resolvedEvId  = resolvedDl?.eventId ?: evId
                val resolvedEvent = resolvedEvId?.let { db.eventDao().getMasterEventById(it) }
                val catId = when {
                    resolvedEvent != null -> resolvedEvent.categoryId
                    resolvedDl    != null -> resolvedDl.categoryId
                    else                  -> json.optInt("category_id", -1).takeIf { it != -1 } ?: task.categoryId
                }
                val oldCat   = task.categoryId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val newCat   = catId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val oldEv    = task.eventId?.let { db.eventDao().getMasterEventById(it)?.title } ?: "None"
                val newEv    = resolvedEvId?.let { db.eventDao().getMasterEventById(it)?.title } ?: "None"
                val oldDl    = task.deadlineId?.let { db.deadlineDao().getDeadlineById(it)?.title } ?: "None"
                val newDl    = dlId?.let { db.deadlineDao().getDeadlineById(it)?.title } ?: "None"
                val oldSch   = if (task.startDate != null && task.startTime != null) "${task.startDate.format(dateFmt)} at ${task.startTime.format(timeFmt)}" else "Auto-scheduled"
                val newSch   = if (nsd != null && nst != null) "${nsd.format(dateFmt)} at ${nst.format(timeFmt)}" else "Auto-scheduled"
                val oldBreak = if (task.breakable == true) "Yes" else "No"
                val newBreak = if (breakable) "Yes" else "No"
                val changed = mutableSetOf<String>()
                if (title != task.title)               changed.add("Title")
                if (dur != task.predictedDuration)     changed.add("Duration")
                if (oldBreak != newBreak)              changed.add("Breakable")
                if (oldSch != newSch)                  changed.add("Schedule")
                if (oldDl != newDl)                    changed.add("Deadline")
                if (oldEv != newEv)                    changed.add("Event")
                if (oldCat != newCat)                  changed.add("Category")
                if (notes != task.notes)               changed.add("Notes")
                return VoicePendingAction(action, "Task", listOf(
                    "Title"     to fmtEdit(task.title,                          title),
                    "Duration"  to fmtEdit(fmtDur(task.predictedDuration),      fmtDur(dur)),
                    "Breakable" to fmtEdit(oldBreak,                            newBreak),
                    "Schedule"  to fmtEdit(oldSch,                              newSch),
                    "Deadline"  to fmtEdit(oldDl,                               newDl),
                    "Event"     to fmtEdit(oldEv,                               newEv),
                    "Category"  to fmtEdit(oldCat,                              newCat),
                    "Notes"     to fmtEdit(task.notes ?: "—",                   notes ?: "—")
                ), changed, reply) {
                    TaskManager.update(context=context, db=db, task=task.copy(
                        title=title, predictedDuration=dur, breakable=breakable,
                        startDate=nsd, startTime=nst, notes=notes,
                        categoryId=catId, eventId=resolvedEvId, deadlineId=dlId))
                }
            }

            "DELETE_TASK" -> {
                val taskId   = json.getInt("task_id")
                val task     = db.taskDao().getMasterTaskById(taskId) ?: return null
                val catT     = task.categoryId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val evT      = task.eventId?.let { db.eventDao().getMasterEventById(it)?.title } ?: "None"
                val dlT      = task.deadlineId?.let { db.deadlineDao().getDeadlineById(it)?.title } ?: "None"
                val schedStr = if (task.startDate != null && task.startTime != null) "${task.startDate.format(dateFmt)} at ${task.startTime.format(timeFmt)}" else "Auto-scheduled"
                return VoicePendingAction(action, "Task", listOf(
                    "Title"     to task.title,
                    "Duration"  to fmtDur(task.predictedDuration),
                    "Breakable" to if (task.breakable == true) "Yes" else "No",
                    "Schedule"  to schedStr,
                    "Deadline"  to dlT,
                    "Event"     to evT,
                    "Category"  to catT,
                    "Notes"     to (task.notes ?: "—")
                ), emptySet(), reply) { TaskManager.delete(context=context, db=db, taskId=taskId) }
            }

            // ── EVENT ─────────────────────────────────────────────
            "CREATE_EVENT" -> {
                val title    = json.getString("title")
                val sd       = LocalDate.parse(json.optString("start_date", "").ifBlank { defDate.toString() })
                val ed       = json.optString("end_date", "").ifBlank { null }?.let { LocalDate.parse(it) }
                val st       = LocalTime.parse(json.optString("start_time", "").ifBlank { defTime.toString() })
                val et       = LocalTime.parse(json.optString("end_time", "").ifBlank { defEndT.toString() })
                val notes    = json.optString("notes", "").ifBlank { null }
                val catId    = json.optInt("category_id", -1).takeIf { it != -1 }
                val colorHex = json.optString("color", "").ifBlank { null }
                val color    = colorHex?.let { hexToColor(it) }
                val rf       = parseRecurFreq(json.optString("recur_freq", "NONE")); val rr = parseRecurRule(json)
                val catT     = catId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                return VoicePendingAction(action, "Event", listOf(
                    "Title"      to title,
                    "Date"       to sd.format(dateFmt),
                    "End Date"   to (ed?.format(dateFmt) ?: "N/A"),
                    "Start Time" to st.format(timeFmt),
                    "End Time"   to et.format(timeFmt),
                    "Recurrence" to fmtRecur(rf, rr),
                    "Color"      to (colorHex ?: "Default"),
                    "Category"   to catT,
                    "Notes"      to (notes ?: "—")
                ), emptySet(), reply) {
                    EventManager.insert(context=context, db=db, title=title, notes=notes, color=color,
                        startDate=sd, endDate=ed, startTime=st, endTime=et,
                        recurFreq=rf, recurRule=rr, categoryId=catId)
                }
            }

            "EDIT_EVENT" -> {
                val evId     = json.getInt("event_id")
                val ev       = db.eventDao().getMasterEventById(evId) ?: return null
                val title    = json.optString("title", "").ifBlank { ev.title }
                val sdStr    = json.optString("start_date", ""); val edStr = json.optString("end_date", "")
                val stStr    = json.optString("start_time", ""); val etStr = json.optString("end_time", "")
                val notes    = json.optString("notes", "").ifBlank { ev.notes }
                val catId    = json.optInt("category_id", -1).takeIf { it != -1 } ?: ev.categoryId
                val colorHex = json.optString("color", "").ifBlank { null }
                val newColor = colorHex?.let { Converters.fromColor(hexToColor(it)) } ?: ev.color
                val rf       = if (json.has("recur_freq")) parseRecurFreq(json.getString("recur_freq")) else ev.recurFreq
                val rr       = if (json.has("recur_freq")) parseRecurRule(json) else ev.recurRule
                val nsd      = if (sdStr.isNotBlank()) LocalDate.parse(sdStr) else ev.startDate
                val ned      = if (edStr.isNotBlank()) LocalDate.parse(edStr) else ev.endDate
                val nst      = if (stStr.isNotBlank()) LocalTime.parse(stStr) else ev.startTime
                val net      = if (etStr.isNotBlank()) LocalTime.parse(etStr) else ev.endTime
                val oldC     = ev.categoryId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val newC     = catId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val changed  = mutableSetOf<String>()
                if (title != ev.title)        changed.add("Title")
                if (nsd != ev.startDate)      changed.add("Date")
                if (ned != ev.endDate)        changed.add("End Date")
                if (nst != ev.startTime)      changed.add("Start Time")
                if (net != ev.endTime)        changed.add("End Time")
                if (rf != ev.recurFreq)       changed.add("Recurrence")
                if (newColor != ev.color)     changed.add("Color")
                if (oldC != newC)             changed.add("Category")
                if (notes != ev.notes)        changed.add("Notes")
                return VoicePendingAction(action, "Event", listOf(
                    "Title"      to fmtEdit(ev.title,                                    title),
                    "Date"       to fmtEdit(ev.startDate.format(dateFmt),                nsd.format(dateFmt)),
                    "End Date"   to fmtEdit(ev.endDate?.format(dateFmt) ?: "N/A",        ned?.format(dateFmt) ?: "N/A"),
                    "Start Time" to fmtEdit(ev.startTime.format(timeFmt),                nst.format(timeFmt)),
                    "End Time"   to fmtEdit(ev.endTime.format(timeFmt),                  net.format(timeFmt)),
                    "Recurrence" to fmtEdit(fmtRecur(ev.recurFreq, ev.recurRule),        fmtRecur(rf, rr)),
                    "Color"      to fmtEdit(ev.color ?: "Default",                       colorHex ?: "Default"),
                    "Category"   to fmtEdit(oldC,                                        newC),
                    "Notes"      to fmtEdit(ev.notes ?: "—",                             notes ?: "—")
                ), changed, reply) {
                    EventManager.update(context=context, db=db, event=ev.copy(
                        title=title, startDate=nsd, endDate=ned, startTime=nst, endTime=net,
                        notes=notes, categoryId=catId, recurFreq=rf, recurRule=rr, color=newColor))
                }
            }

            "DELETE_EVENT" -> {
                val evId = json.getInt("event_id")
                val ev   = db.eventDao().getMasterEventById(evId) ?: return null
                val catT = ev.categoryId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                return VoicePendingAction(action, "Event", listOf(
                    "Title"      to ev.title,
                    "Date"       to ev.startDate.format(dateFmt),
                    "End Date"   to (ev.endDate?.format(dateFmt) ?: "N/A"),
                    "Start Time" to ev.startTime.format(timeFmt),
                    "End Time"   to ev.endTime.format(timeFmt),
                    "Recurrence" to fmtRecur(ev.recurFreq, ev.recurRule),
                    "Color"      to (ev.color ?: "Default"),
                    "Category"   to catT,
                    "Notes"      to (ev.notes ?: "—")
                ), emptySet(), reply) { EventManager.delete(context=context, db=db, eventId=evId) }
            }

            // ── REMINDER ──────────────────────────────────────────
            "CREATE_REMINDER" -> {
                val title  = json.getString("title")
                val sd     = LocalDate.parse(json.optString("start_date", "").ifBlank { defDate.toString() })
                val ed     = json.optString("end_date", "").ifBlank { null }?.let { LocalDate.parse(it) }
                val tStr   = json.optString("time", "").ifBlank { null }
                val notes  = json.optString("notes", "").ifBlank { null }
                val catId  = json.optInt("category_id", -1).takeIf { it != -1 }
                val rf     = parseRecurFreq(json.optString("recur_freq", "NONE")); val rr = parseRecurRule(json)
                val t      = tStr?.let { LocalTime.parse(it) }; val allDay = t == null
                val catT   = catId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                return VoicePendingAction(action, "Reminder", listOf(
                    "Title"      to title,
                    "Date"       to sd.format(dateFmt),
                    "End Date"   to (ed?.format(dateFmt) ?: "N/A"),
                    "Time"       to (if (allDay) "All Day" else t!!.format(timeFmt)),
                    "Recurrence" to fmtRecur(rf, rr),
                    "Category"   to catT,
                    "Notes"      to (notes ?: "—")
                ), emptySet(), reply) {
                    ReminderManager.insert(context=context, db=db, title=title, notes=notes,
                        startDate=sd, endDate=ed, time=t, allDay=allDay,
                        recurFreq=rf, recurRule=rr, categoryId=catId)
                }
            }

            "EDIT_REMINDER" -> {
                val rId   = json.getInt("reminder_id")
                val rem   = db.reminderDao().getMasterReminderById(rId) ?: return null
                val title = json.optString("title", "").ifBlank { rem.title }
                val sdStr = json.optString("start_date", ""); val edStr = json.optString("end_date", "")
                val tStr  = json.optString("time", "")
                val notes = json.optString("notes", "").ifBlank { rem.notes }
                val catId = json.optInt("category_id", -1).takeIf { it != -1 } ?: rem.categoryId
                val rf    = if (json.has("recur_freq")) parseRecurFreq(json.getString("recur_freq")) else rem.recurFreq
                val rr    = if (json.has("recur_freq")) parseRecurRule(json) else rem.recurRule
                val nsd   = if (sdStr.isNotBlank()) LocalDate.parse(sdStr) else rem.startDate
                val ned   = if (edStr.isNotBlank()) LocalDate.parse(edStr) else rem.endDate
                val nt    = if (tStr.isNotBlank()) LocalTime.parse(tStr) else rem.time
                val nad   = if (tStr.isNotBlank()) false else rem.allDay
                val oldC  = rem.categoryId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val newC  = catId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val oldTS = if (rem.allDay) "All Day" else rem.time?.format(timeFmt) ?: "—"
                val newTS = if (nad) "All Day" else nt?.format(timeFmt) ?: "—"
                val changed = mutableSetOf<String>()
                if (title != rem.title)       changed.add("Title")
                if (nsd != rem.startDate)     changed.add("Date")
                if (ned != rem.endDate)       changed.add("End Date")
                if (oldTS != newTS)           changed.add("Time")
                if (rf != rem.recurFreq)      changed.add("Recurrence")
                if (oldC != newC)             changed.add("Category")
                if (notes != rem.notes)       changed.add("Notes")
                return VoicePendingAction(action, "Reminder", listOf(
                    "Title"      to fmtEdit(rem.title,                                   title),
                    "Date"       to fmtEdit(rem.startDate.format(dateFmt),               nsd.format(dateFmt)),
                    "End Date"   to fmtEdit(rem.endDate?.format(dateFmt) ?: "N/A",       ned?.format(dateFmt) ?: "N/A"),
                    "Time"       to fmtEdit(oldTS,                                       newTS),
                    "Recurrence" to fmtEdit(fmtRecur(rem.recurFreq, rem.recurRule),      fmtRecur(rf, rr)),
                    "Category"   to fmtEdit(oldC,                                        newC),
                    "Notes"      to fmtEdit(rem.notes ?: "—",                            notes ?: "—")
                ), changed, reply) {
                    ReminderManager.update(context=context, db=db, reminder=rem.copy(
                        title=title, startDate=nsd, endDate=ned, time=nt, allDay=nad,
                        notes=notes, categoryId=catId, recurFreq=rf, recurRule=rr))
                }
            }

            "DELETE_REMINDER" -> {
                val rId  = json.getInt("reminder_id")
                val rem  = db.reminderDao().getMasterReminderById(rId) ?: return null
                val catT = rem.categoryId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                return VoicePendingAction(action, "Reminder", listOf(
                    "Title"      to rem.title,
                    "Date"       to rem.startDate.format(dateFmt),
                    "End Date"   to (rem.endDate?.format(dateFmt) ?: "N/A"),
                    "Time"       to (if (rem.allDay) "All Day" else rem.time?.format(timeFmt) ?: "—"),
                    "Recurrence" to fmtRecur(rem.recurFreq, rem.recurRule),
                    "Category"   to catT,
                    "Notes"      to (rem.notes ?: "—")
                ), emptySet(), reply) { ReminderManager.delete(context=context, db=db, reminderId=rId) }
            }

            // ── DEADLINE ──────────────────────────────────────────
            "CREATE_DEADLINE" -> {
                val title     = json.getString("title")
                val d         = LocalDate.parse(json.optString("date", "").ifBlank { defDate.toString() })
                val t         = LocalTime.parse(json.optString("time", "").ifBlank { defTime.toString() })
                val notes     = json.optString("notes", "").ifBlank { null }
                val evId      = json.optInt("event_id", -1).takeIf { it != -1 }
                val autoTask  = json.optBoolean("auto_schedule_task", false)
                val taskDur   = json.optInt("task_duration_minutes", 60)
                val taskBreak = json.optBoolean("task_breakable", false)
                val taskSdStr = json.optString("task_start_date", "")
                val taskStStr = json.optString("task_start_time", "")
                val taskSd    = if (taskSdStr.isNotBlank()) LocalDate.parse(taskSdStr) else null
                val taskSt    = if (taskStStr.isNotBlank()) LocalTime.parse(taskStStr) else null
                val resolvedEvent = evId?.let { db.eventDao().getMasterEventById(it) }
                val catId = resolvedEvent?.categoryId ?: json.optInt("category_id", -1).takeIf { it != -1 }
                val catT  = catId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val evT   = evId?.let { db.eventDao().getMasterEventById(it)?.title } ?: "None"
                val summaryFields = mutableListOf(
                    "Title"    to title,
                    "Date"     to d.format(dateFmt),
                    "Time"     to t.format(timeFmt),
                    "Event"    to evT,
                    "Category" to catT,
                    "Notes"    to (notes ?: "—")
                )
                if (autoTask) {
                    val taskSchedStr = if (taskSd != null && taskSt != null) "${taskSd.format(dateFmt)} at ${taskSt.format(timeFmt)}" else "Auto-scheduled"
                    summaryFields.add("Auto-create Task" to "Yes, ${fmtDur(taskDur)}${if (taskBreak) ", breakable" else ""}, $taskSchedStr")
                }
                return VoicePendingAction(action, "Deadline", summaryFields, emptySet(), reply) {
                    val insertedDlId = DeadlineManager.insert(context=context, db=db, title=title,
                        notes=notes, date=d, time=t, categoryId=catId, eventId=evId)
                    if (autoTask) {
                        TaskManager.insert(context=context, db=db, title=title, notes=notes, allDay=null,
                            breakable=taskBreak, startDate=taskSd, startTime=taskSt, predictedDuration=taskDur,
                            categoryId=catId, eventId=evId, deadlineId=insertedDlId, dependencyTaskId=null)
                    }
                }
            }

            "EDIT_DEADLINE" -> {
                val dlId  = json.getInt("deadline_id")
                val dl    = db.deadlineDao().getDeadlineById(dlId) ?: return null
                val title = json.optString("title", "").ifBlank { dl.title }
                val dStr  = json.optString("date", ""); val tStr = json.optString("time", "")
                val notes = json.optString("notes", "").ifBlank { dl.notes }
                val evId  = json.optInt("event_id", -1).takeIf { it != -1 } ?: dl.eventId
                val resolvedEvForCat = evId?.let { db.eventDao().getMasterEventById(it) }
                val catId = resolvedEvForCat?.categoryId ?: dl.categoryId
                val nd    = if (dStr.isNotBlank()) LocalDate.parse(dStr) else dl.date
                val nt    = if (tStr.isNotBlank()) LocalTime.parse(tStr) else dl.time
                val oldC  = dl.categoryId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val newC  = catId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val oldE  = dl.eventId?.let { db.eventDao().getMasterEventById(it)?.title } ?: "None"
                val newE  = evId?.let { db.eventDao().getMasterEventById(it)?.title } ?: "None"
                val changed = mutableSetOf<String>()
                if (title != dl.title)  changed.add("Title")
                if (nd != dl.date)      changed.add("Date")
                if (nt != dl.time)      changed.add("Time")
                if (oldE != newE)       changed.add("Event")
                if (oldC != newC)       changed.add("Category")
                if (notes != dl.notes)  changed.add("Notes")
                return VoicePendingAction(action, "Deadline", listOf(
                    "Title"    to fmtEdit(dl.title,               title),
                    "Date"     to fmtEdit(dl.date.format(dateFmt), nd.format(dateFmt)),
                    "Time"     to fmtEdit(dl.time.format(timeFmt), nt.format(timeFmt)),
                    "Event"    to fmtEdit(oldE,                   newE),
                    "Category" to fmtEdit(oldC,                   newC),
                    "Notes"    to fmtEdit(dl.notes ?: "—",        notes ?: "—")
                ), changed, reply) {
                    DeadlineManager.update(context=context, db=db,
                        deadline=dl.copy(title=title, date=nd, time=nt, notes=notes, categoryId=catId, eventId=evId))
                }
            }

            "DELETE_DEADLINE" -> {
                val dlId = json.getInt("deadline_id")
                val dl   = db.deadlineDao().getDeadlineById(dlId) ?: return null
                val catT = dl.categoryId?.let { db.categoryDao().getCategoryById(it)?.title } ?: "None"
                val evT  = dl.eventId?.let { db.eventDao().getMasterEventById(it)?.title } ?: "None"
                return VoicePendingAction(action, "Deadline", listOf(
                    "Title"    to dl.title,
                    "Date"     to dl.date.format(dateFmt),
                    "Time"     to dl.time.format(timeFmt),
                    "Event"    to evT,
                    "Category" to catT,
                    "Notes"    to (dl.notes ?: "—")
                ), emptySet(), reply) { DeadlineManager.delete(context=context, db=db, deadlineId=dlId) }
            }

            // ── CATEGORY ──────────────────────────────────────────
            "CREATE_CATEGORY" -> {
                val title    = json.getString("title")
                val notes    = json.optString("notes", "").ifBlank { null }
                val colorHex = json.optString("color", "").ifBlank { null }
                val color    = if (colorHex != null) hexToColor(colorHex) else Color(0xFF888780)
                return VoicePendingAction(action, "Category", listOf(
                    "Title" to title,
                    "Color" to (colorHex ?: "Default"),
                    "Notes" to (notes ?: "—")
                ), emptySet(), reply) { CategoryManager.insert(db=db, title=title, notes=notes, color=color) }
            }

            "EDIT_CATEGORY" -> {
                val catId       = json.getInt("category_id")
                val cat         = db.categoryDao().getCategoryById(catId) ?: return null
                val title       = json.optString("title", "").ifBlank { cat.title }
                val notes       = json.optString("notes", "").ifBlank { cat.notes }
                val colorHex    = json.optString("color", "").ifBlank { null }
                val newColorStr = if (colorHex != null) Converters.fromColor(hexToColor(colorHex)) else cat.color
                val changed = mutableSetOf<String>()
                if (title != cat.title)         changed.add("Title")
                if (newColorStr != cat.color)   changed.add("Color")
                if (notes != cat.notes)         changed.add("Notes")
                return VoicePendingAction(action, "Category", listOf(
                    "Title" to fmtEdit(cat.title,        title),
                    "Color" to fmtEdit(cat.color,        colorHex ?: cat.color),
                    "Notes" to fmtEdit(cat.notes ?: "—", notes ?: "—")
                ), changed, reply) {
                    CategoryManager.update(db=db, category=cat.copy(title=title, notes=notes, color=newColorStr))
                }
            }

            "DELETE_CATEGORY" -> {
                val catId = json.getInt("category_id")
                val cat   = db.categoryDao().getCategoryById(catId) ?: return null
                return VoicePendingAction(action, "Category", listOf(
                    "Title" to cat.title,
                    "Color" to cat.color,
                    "Notes" to (cat.notes ?: "—")
                ), emptySet(), reply) { CategoryManager.delete(context=context, db=db, categoryId=catId) }
            }

            // ── TASK BUCKET ───────────────────────────────────────
            "CREATE_TASK_BUCKET" -> {
                val sd = LocalDate.parse(json.optString("start_date", "").ifBlank { defDate.toString() })
                val ed = json.optString("end_date", "").ifBlank { null }?.let { LocalDate.parse(it) }
                val st = LocalTime.parse(json.optString("start_time", "").ifBlank { defTime.toString() })
                val et = LocalTime.parse(json.optString("end_time", "").ifBlank { defEndT.toString() })
                val rf = parseRecurFreq(json.optString("recur_freq", "NONE")); val rr = parseRecurRule(json)
                return VoicePendingAction(action, "Task Bucket", listOf(
                    "Date"       to sd.format(dateFmt),
                    "End Date"   to (ed?.format(dateFmt) ?: "N/A"),
                    "Start Time" to st.format(timeFmt),
                    "End Time"   to et.format(timeFmt),
                    "Recurrence" to fmtRecur(rf, rr)
                ), emptySet(), reply) {
                    TaskBucketManager.insert(context=context, db=db, startDate=sd, endDate=ed,
                        startTime=st, endTime=et, recurFreq=rf, recurRule=rr)
                }
            }

            "EDIT_TASK_BUCKET" -> {
                val bId   = json.getInt("bucket_id")
                val b     = db.taskBucketDao().getMasterBucketById(bId) ?: return null
                val sdStr = json.optString("start_date", ""); val edStr = json.optString("end_date", "")
                val stStr = json.optString("start_time", ""); val etStr = json.optString("end_time", "")
                val rf    = if (json.has("recur_freq")) parseRecurFreq(json.getString("recur_freq")) else b.recurFreq
                val rr    = if (json.has("recur_freq")) parseRecurRule(json) else b.recurRule
                val nsd   = if (sdStr.isNotBlank()) LocalDate.parse(sdStr) else b.startDate
                val ned   = if (edStr.isNotBlank()) LocalDate.parse(edStr) else b.endDate
                val nst   = if (stStr.isNotBlank()) LocalTime.parse(stStr) else b.startTime
                val net   = if (etStr.isNotBlank()) LocalTime.parse(etStr) else b.endTime
                val changed = mutableSetOf<String>()
                if (nsd != b.startDate)  changed.add("Date")
                if (ned != b.endDate)    changed.add("End Date")
                if (nst != b.startTime)  changed.add("Start Time")
                if (net != b.endTime)    changed.add("End Time")
                if (rf != b.recurFreq)   changed.add("Recurrence")
                return VoicePendingAction(action, "Task Bucket", listOf(
                    "Date"       to fmtEdit(b.startDate.format(dateFmt),               nsd.format(dateFmt)),
                    "End Date"   to fmtEdit(b.endDate?.format(dateFmt) ?: "N/A",       ned?.format(dateFmt) ?: "N/A"),
                    "Start Time" to fmtEdit(b.startTime.format(timeFmt),               nst.format(timeFmt)),
                    "End Time"   to fmtEdit(b.endTime.format(timeFmt),                 net.format(timeFmt)),
                    "Recurrence" to fmtEdit(fmtRecur(b.recurFreq, b.recurRule),        fmtRecur(rf, rr))
                ), changed, reply) {
                    TaskBucketManager.update(context=context, db=db,
                        bucket=b.copy(startDate=nsd, endDate=ned, startTime=nst, endTime=net, recurFreq=rf, recurRule=rr))
                }
            }

            "DELETE_TASK_BUCKET" -> {
                val bId = json.getInt("bucket_id")
                val b   = db.taskBucketDao().getMasterBucketById(bId) ?: return null
                return VoicePendingAction(action, "Task Bucket", listOf(
                    "Date"       to b.startDate.format(dateFmt),
                    "End Date"   to (b.endDate?.format(dateFmt) ?: "N/A"),
                    "Start Time" to b.startTime.format(timeFmt),
                    "End Time"   to b.endTime.format(timeFmt),
                    "Recurrence" to fmtRecur(b.recurFreq, b.recurRule)
                ), emptySet(), reply) { TaskBucketManager.delete(context=context, db=db, bucketId=bId) }
            }

            // ── SETTINGS ──────────────────────────────────────────
            "CHANGE_SETTING" -> {
                val sName   = json.getString("setting_name")
                val valBool = if (json.has("value_boolean") && !json.isNull("value_boolean")) json.getBoolean("value_boolean") else null
                val valInt  = if (json.has("value_int") && !json.isNull("value_int")) json.getInt("value_int") else null
                val valStr  = valBool?.toString() ?: valInt?.toString() ?: "—"
                return VoicePendingAction(action, "Setting", listOf(
                    "Setting"   to sName,
                    "New Value" to valStr
                ), emptySet(), reply) {
                    when (sName) {
                        "startWeekOnMonday"     -> valBool?.let { SettingsManager.setStartWeek(db, it) }
                        "atiPaddingEnabled"     -> valBool?.let { SettingsManager.setAtiPaddingEnabled(db, it); generateTaskIntervals(context, db) }
                        "breakDuration"         -> valInt?.let  { SettingsManager.setBreakDuration(db, it) }
                        "breakEvery"            -> valInt?.let  { SettingsManager.setBreakEvery(db, it) }
                        "notifTasksEnabled"     -> valBool?.let { SettingsManager.setNotifTasksEnabled(db, it) }
                        "notifEventsEnabled"    -> valBool?.let { SettingsManager.setNotifEventsEnabled(db, it) }
                        "notifRemindersEnabled" -> valBool?.let { SettingsManager.setNotifRemindersEnabled(db, it) }
                        "notifDeadlinesEnabled" -> valBool?.let { SettingsManager.setNotifDeadlinesEnabled(db, it) }
                    }
                }
            }
        }
        return null
    }

    // ── Main handler ──────────────────────────────────────────────
    suspend fun handleSpokenCommand(context: Context, db: AppDatabase, spoken: String) {
        try {
            val snapshot = buildContextSnapshot(db)
            val today    = LocalDate.now()

            val systemPrompt = """
You are PlannEd Assistant, the AI voice assistant built into the PlannEd productivity app.
The user has just spoken a command. Your job is to understand it and return a JSON response.

CURRENT APP STATE:
$snapshot

RULES:
1. Always respond with a single valid JSON object — no markdown, no extra text.
2. Decide which action fits best from the list below.
3. For update and delete actions, match the user's spoken name to an entity ID from the app state lists above. Pick the closest match. If genuinely ambiguous, use REPLY to ask for clarification.
4. Only include fields the user explicitly mentioned. Omit all other optional fields — defaults are handled by the app.

DATE/TIME RULES:
- All dates: ISO format YYYY-MM-DD. Today is $today.
- All times: HH:mm 24h. "5am"→"05:00", "3:30pm"→"15:30", "noon"→"12:00", "midnight"→"00:00".
- Relative dates: "tomorrow"→${today.plusDays(1)}, compute "next Monday" etc. correctly relative to today.
- Duration: convert to minutes ("2 hours"→120, "90 minutes"→90, "half an hour"→30).
- Default duration if not mentioned: 60 minutes.

RECURRENCE RULES:
- recur_freq: one of NONE, DAILY, WEEKLY, MONTHLY, YEARLY
- For WEEKLY: include recur_days_of_week as array of ints (1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun)
- For MONTHLY: include recur_days_of_month as array of ints (1-31)
- For YEARLY: include recur_month (1-12) and recur_day (1-31)

SUPPORTED ACTIONS AND THEIR JSON SCHEMAS:

CREATE_TASK: { "action":"CREATE_TASK", "title":"string", "duration_minutes":number, "breakable":boolean, "start_date":"YYYY-MM-DD or omit for auto-schedule", "start_time":"HH:mm or omit for auto-schedule", "notes":"string or omit", "category_id":number_or_-1, "event_id":number_or_-1, "deadline_id":number_or_-1, "reply":"short spoken confirmation" }
EDIT_TASK:   { "action":"EDIT_TASK", "task_id":number, "title":"omit if unchanged", "duration_minutes":number_or_null, "breakable":boolean_or_omit, "start_date":"YYYY-MM-DD or omit if unchanged", "start_time":"HH:mm or omit if unchanged", "notes":"string or omit if unchanged", "category_id":number_or_-1, "event_id":number_or_-1, "deadline_id":number_or_-1, "reply":"short spoken confirmation" }
DELETE_TASK: { "action":"DELETE_TASK", "task_id":number, "reply":"short spoken confirmation" }

CREATE_EVENT: { "action":"CREATE_EVENT", "title":"string", "start_date":"YYYY-MM-DD", "end_date":"YYYY-MM-DD or omit", "start_time":"HH:mm", "end_time":"HH:mm", "notes":"string or omit", "color":"hex or omit", "category_id":number_or_-1, "recur_freq":"NONE|DAILY|WEEKLY|MONTHLY|YEARLY", "recur_days_of_week":[], "recur_days_of_month":[], "recur_month":number_if_YEARLY, "recur_day":number_if_YEARLY, "reply":"short spoken confirmation" }
EDIT_EVENT:  { "action":"EDIT_EVENT", "event_id":number, "title":"omit if unchanged", "start_date":"omit if unchanged", "end_date":"omit if unchanged", "start_time":"omit if unchanged", "end_time":"omit if unchanged", "notes":"omit if unchanged", "color":"hex or omit if unchanged", "category_id":number_or_-1, "recur_freq":"omit if unchanged", "reply":"short spoken confirmation" }
DELETE_EVENT: { "action":"DELETE_EVENT", "event_id":number, "reply":"short spoken confirmation" }

CREATE_REMINDER: { "action":"CREATE_REMINDER", "title":"string", "start_date":"YYYY-MM-DD", "end_date":"YYYY-MM-DD or omit", "time":"HH:mm or omit for all-day", "notes":"string or omit", "category_id":number_or_-1, "recur_freq":"NONE|DAILY|WEEKLY|MONTHLY|YEARLY", "recur_days_of_week":[], "recur_days_of_month":[], "reply":"short spoken confirmation" }
EDIT_REMINDER: { "action":"EDIT_REMINDER", "reminder_id":number, "title":"omit if unchanged", "start_date":"omit if unchanged", "end_date":"omit if unchanged", "time":"omit if unchanged", "notes":"omit if unchanged", "category_id":number_or_-1, "recur_freq":"omit if unchanged", "reply":"short spoken confirmation" }
DELETE_REMINDER: { "action":"DELETE_REMINDER", "reminder_id":number, "reply":"short spoken confirmation" }

CREATE_DEADLINE: { "action":"CREATE_DEADLINE", "title":"string", "date":"YYYY-MM-DD", "time":"HH:mm", "notes":"string or omit", "category_id":number_or_-1, "event_id":number_or_-1, "auto_schedule_task":boolean, "task_duration_minutes":number, "task_breakable":boolean, "task_start_date":"YYYY-MM-DD or omit for auto-schedule", "task_start_time":"HH:mm or omit for auto-schedule", "reply":"short spoken confirmation" }
EDIT_DEADLINE:  { "action":"EDIT_DEADLINE", "deadline_id":number, "title":"omit if unchanged", "date":"omit if unchanged", "time":"omit if unchanged", "notes":"omit if unchanged", "category_id":number_or_-1, "event_id":number_or_-1, "reply":"short spoken confirmation" }
DELETE_DEADLINE: { "action":"DELETE_DEADLINE", "deadline_id":number, "reply":"short spoken confirmation" }

CREATE_CATEGORY: { "action":"CREATE_CATEGORY", "title":"string", "notes":"string or omit", "color":"hex or omit for default", "reply":"short spoken confirmation" }
EDIT_CATEGORY:   { "action":"EDIT_CATEGORY", "category_id":number, "title":"omit if unchanged", "notes":"omit if unchanged", "color":"hex or omit if unchanged", "reply":"short spoken confirmation" }
DELETE_CATEGORY: { "action":"DELETE_CATEGORY", "category_id":number, "reply":"short spoken confirmation" }

CREATE_TASK_BUCKET: { "action":"CREATE_TASK_BUCKET", "start_date":"YYYY-MM-DD", "end_date":"YYYY-MM-DD or omit", "start_time":"HH:mm", "end_time":"HH:mm", "recur_freq":"NONE|DAILY|WEEKLY|MONTHLY|YEARLY", "recur_days_of_week":[], "recur_days_of_month":[], "reply":"short spoken confirmation" }
EDIT_TASK_BUCKET:  { "action":"EDIT_TASK_BUCKET", "bucket_id":number, "start_date":"omit if unchanged", "end_date":"omit if unchanged", "start_time":"omit if unchanged", "end_time":"omit if unchanged", "recur_freq":"omit if unchanged", "reply":"short spoken confirmation" }
DELETE_TASK_BUCKET: { "action":"DELETE_TASK_BUCKET", "bucket_id":number, "reply":"short spoken confirmation" }

CHANGE_SETTING: { "action":"CHANGE_SETTING", "setting_name":"one of: startWeekOnMonday, atiPaddingEnabled, breakDuration, breakEvery, notifTasksEnabled, notifEventsEnabled, notifRemindersEnabled, notifDeadlinesEnabled", "value_boolean":boolean_or_null, "value_int":number_or_null, "reply":"short spoken confirmation" }
QUERY_SCHEDULE: { "action":"QUERY_SCHEDULE", "reply":"natural spoken answer about their schedule based on the app state above" }
REPLY:          { "action":"REPLY", "reply":"your spoken response" }

Keep all reply strings short, friendly, and natural — they will be read aloud by TTS.
Do NOT include markdown in reply strings.
""".trimIndent()

            val rawResponse = callGemini(systemPrompt, spoken)
            val cleanJson   = rawResponse.replace(Regex("^```json\\s*", RegexOption.MULTILINE), "").replace(Regex("^```\\s*", RegexOption.MULTILINE), "").trim()
            val json        = JSONObject(cleanJson)
            val actionStr   = json.optString("action", "REPLY")
            val replyText   = json.optString("reply", "")

            if (actionStr == "QUERY_SCHEDULE" || actionStr == "REPLY") {
                val result = VoiceResult(userText = spoken, replyText = replyText, actionTaken = null)
                withContext(Dispatchers.Main) { onResult(result); speakOut(replyText) }
                return
            }

            val pending = buildPendingAction(context, db, json)
            if (pending == null) {
                val result = VoiceResult(userText = spoken, replyText = "I couldn't find that item. Please try again.", actionTaken = null)
                withContext(Dispatchers.Main) { onResult(result); speakOut(result.replyText) }
                return
            }

            withContext(Dispatchers.Main) {
                onPhaseChange(VoicePhase.IDLE)
                onPendingAction(pending)
            }

        } catch (e: TimeoutCancellationException) {
            withContext(Dispatchers.Main) {
                val errResult = VoiceResult(userText = spoken, replyText = "The request timed out. Please try again.", actionTaken = null)
                onResult(errResult); speakOut(errResult.replyText)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                val errResult = VoiceResult(userText = spoken, replyText = "Sorry, something went wrong. Please try again.", actionTaken = null)
                onResult(errResult); speakOut(errResult.replyText)
            }
        }
    }
}