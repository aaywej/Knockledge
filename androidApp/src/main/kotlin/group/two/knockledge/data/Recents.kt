package group.two.knockledge.data

import group.two.knockledge.data.model.Note
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.settings.KeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A saved rewind. Summary-only view of a rewind; audio and transcripts live in NoteRepository. */
data class Recent(
    val id: Long,
    val title: String,
    val gist: String,
    val points: List<String>,
    val redactedCount: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
)

/** Store of saved rewinds, listed newest first. */
interface RecentsRepository {
    /** Saved rewinds, newest first. */
    val recents: StateFlow<List<Recent>>
    /** Saves a finished summary and returns the stored recent. */
    suspend fun add(summary: Summary, startedAtMillis: Long, endedAtMillis: Long): Recent
    /** Removes the recent with this id; unknown ids are ignored. */
    suspend fun delete(id: Long)
}

/**
 * Lives for the app process only. Swap for an encrypted local database
 * (Room KMP or SQLDelight) when persistence is decided (spec open question 4).
 */
class InMemoryRecentsRepository : RecentsRepository {
    private val items = MutableStateFlow<List<Recent>>(emptyList())
    private var nextId = 1L

    override val recents: StateFlow<List<Recent>> = items.asStateFlow()

    /** Saves a finished summary and returns the stored recent. */
    override suspend fun add(summary: Summary, startedAtMillis: Long, endedAtMillis: Long): Recent {
        val recent = Recent(
            id = nextId++,
            title = summary.title,
            gist = summary.gist,
            points = summary.points,
            redactedCount = summary.redactedCount,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
        )
        items.update { listOf(recent) + it }
        return recent
    }

    /** Removes the recent with this id; unknown ids are ignored. */
    override suspend fun delete(id: Long) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}

/**
 * Keeps tapes across app restarts in the platform key-value store (SharedPreferences /
 * NSUserDefaults, both private to the app and excluded from cloud backup on Android).
 * Only summaries are stored here: title, gist, points, counts and times. Audio and transcripts go to NoteRepository.
 */
class StoredRecentsRepository(
    private val store: KeyValueStore,
    private val maxKept: Int = 500,
) : RecentsRepository {
    private val lock = Mutex()
    private val items = MutableStateFlow(load())
    private var nextId = (items.value.maxOfOrNull { it.id } ?: 0L) + 1L

    override val recents: StateFlow<List<Recent>> = items.asStateFlow()

    /** Persists the recent, keeping only the newest maxKept entries. */
    override suspend fun add(summary: Summary, startedAtMillis: Long, endedAtMillis: Long): Recent = lock.withLock {
        val recent = Recent(
            id = nextId++,
            title = summary.title,
            gist = summary.gist,
            points = summary.points,
            redactedCount = summary.redactedCount,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
        )
        items.update { (listOf(recent) + it).take(maxKept) }
        save()
        recent
    }

    /** Removes the recent and rewrites the store. */
    override suspend fun delete(id: Long) = lock.withLock {
        items.update { list -> list.filterNot { it.id == id } }
        save()
    }

    private fun save() = store.putString(KEY, RecentsCodec.encode(items.value))

    private fun load(): List<Recent> = store.getString(KEY)?.let { RecentsCodec.decode(it) }.orEmpty()

    private companion object {
        const val KEY = "tapes_v1"
    }
}

/**
 * Plain-text format for saved tapes, so no serialization library is needed.
 * Records, fields and points are split by ASCII separator characters, which never occur in
 * speech summaries (they are stripped from the text before saving, just in case).
 */
object RecentsCodec {
    private const val RECORD = '\u001E'
    private const val FIELD = '\u001F'
    private const val POINT = '\u001D'
    private const val VERSION = "1"

    /** Renders recents in the plain-text store format. */
    fun encode(list: List<Recent>): String = list.joinToString(RECORD.toString()) { r ->
        listOf(
            VERSION,
            r.id.toString(),
            clean(r.title),
            clean(r.gist),
            r.points.joinToString(POINT.toString()) { clean(it) },
            r.redactedCount.toString(),
            r.startedAtMillis.toString(),
            r.endedAtMillis.toString(),
        ).joinToString(FIELD.toString())
    }

    /** Parses stored text; records in another version or with broken fields are dropped. */
    fun decode(text: String): List<Recent> {
        if (text.isEmpty()) return emptyList()
        return text.split(RECORD).mapNotNull { record ->
            val f = record.split(FIELD)
            if (f.size < 8 || f[0] != VERSION) return@mapNotNull null
            Recent(
                id = f[1].toLongOrNull() ?: return@mapNotNull null,
                title = f[2],
                gist = f[3],
                points = if (f[4].isEmpty()) emptyList() else f[4].split(POINT),
                redactedCount = f[5].toIntOrNull() ?: 0,
                startedAtMillis = f[6].toLongOrNull() ?: 0L,
                endedAtMillis = f[7].toLongOrNull() ?: 0L,
            )
        }
    }

    private fun clean(s: String) = s.filterNot { it == RECORD || it == FIELD || it == POINT }
}

/** Read-through view of NoteRepository for the signed-in user; add() needs an audio snapshot, so use NoteRepository. */
@OptIn(ExperimentalCoroutinesApi::class)
class NoteBackedRecentsRepository(
    private val notes: NoteRepository,
    private val auth: AuthRepository,
    scope: CoroutineScope,
) : RecentsRepository {
    private fun Note.recentId() = id.hashCode().toLong()

    override val recents: StateFlow<List<Recent>> = auth.currentUser
        .flatMapLatest { u -> u?.let { notes.observeNotes(it.uid) } ?: emptyFlow() }
        .map { list ->
            list.mapNotNull { n ->
                n.summary?.let {
                    Recent(
                        n.recentId(), n.title, it.executiveSummary, it.keyPoints, 0,
                        n.createdAtEpochMs - n.audio.durationMs, n.createdAtEpochMs,
                    )
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Always throws: notes are created through NoteRepository.createFromSnapshot. */
    override suspend fun add(summary: Summary, startedAtMillis: Long, endedAtMillis: Long): Recent =
        throw UnsupportedOperationException("Create notes via NoteRepository.createFromSnapshot")

    /** Deletes the note behind this recent; the id is the note id hashed, so the first match is looked up. */
    override suspend fun delete(id: Long) {
        val uid = auth.currentUser.value?.uid ?: return
        notes.observeNotes(uid).first().firstOrNull { it.recentId() == id }?.let { notes.deleteNote(it.id, uid) }
    }
}