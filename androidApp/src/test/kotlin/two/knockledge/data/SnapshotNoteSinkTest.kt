package group.two.knockledge.data

import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.AuthProvider
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.UserProfile
import group.two.knockledge.data.repository.SnapshotNoteSink
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptSentence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnapshotNoteSinkTest {

    private class FakeAuth(private val profile: UserProfile?) : AuthRepository {
        override val currentUser: StateFlow<UserProfile?> = MutableStateFlow(profile)

        override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> = TODO()

        override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = TODO()

        override suspend fun registerWithEmail(
            displayName: String,
            email: String,
            password: String,
        ): Result<UserProfile> = TODO()

        override suspend fun signOut() = Unit
    }

    private class FakeNotes : NoteRepository {
        val created = mutableListOf<AudioSnapshotInput>()
        val transcripts = mutableListOf<Pair<String, TranscriptInput>>()
        val summaries = mutableListOf<Pair<String, SummaryInput>>()
        private var next = 0

        override fun observeNotes(userId: String): Flow<List<Note>> = flowOf(emptyList())

        override suspend fun getNote(id: String, userId: String): Note? = null

        override suspend fun createFromSnapshot(snapshot: AudioSnapshotInput, userId: String): Note {
            created += snapshot
            return Note(
                id = "note-${next++}",
                userId = userId,
                title = "t",
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
                audio = group.two.knockledge.data.model.AudioMetadata(
                    snapshot.file.absolutePath, null, snapshot.durationMs, snapshot.sampleRate, snapshot.triggerType,
                ),
            )
        }

        private fun stub(id: String, userId: String) = Note(
            id = id,
            userId = userId,
            title = "t",
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
            audio = group.two.knockledge.data.model.AudioMetadata(
                "/tmp/$id.wav", null, 60_000L, 16_000, group.two.knockledge.data.model.TriggerType.GESTURE,
            ),
        )

        override suspend fun attachTranscript(noteId: String, userId: String, transcript: TranscriptInput): Note {
            transcripts += noteId to transcript
            return stub(noteId, userId)
        }

        override suspend fun attachSummary(noteId: String, userId: String, summary: SummaryInput): Note {
            summaries += noteId to summary
            return stub(noteId, userId)
        }

        override suspend fun deleteNote(id: String, userId: String) = Unit

        override suspend fun syncPending(userId: String) = Unit
    }

    private val user = UserProfile("u1", "a@b.c", "Ana", null, AuthProvider.GOOGLE, 1L)

    private val transcript = TranscriptResult(
        sentences = listOf(TranscriptSentence("Speaker 1", 0L, 3_200L, "Good morning.", 0.9f)),
        fullText = "Speaker 1: Good morning.",
        isDiarized = true,
        detectedLanguage = "en",
        engineType = group.two.knockledge.pipeline.TranscriptionEngineType.CLOUD_FIREBASE_AI,
    )

    private val summary = Summary("Recap", "A greeting.", listOf("Said hi"), 0)

    private fun wav() = File.createTempFile("snap", ".wav")

    @Test
    fun createsNoteAndAttachesTranscriptAndSummary() = runBlocking {
        val notes = FakeNotes()
        SnapshotNoteSink(notes, FakeAuth(user)).record(wav(), 1_000L, 61_000L, TriggerType.GESTURE, transcript, summary)

        assertEquals(1, notes.created.size)
        assertEquals(60_000L, notes.created[0].durationMs)
        assertEquals(group.two.knockledge.data.model.TriggerType.GESTURE, notes.created[0].triggerType)
        assertEquals("note-0", notes.transcripts[0].first)
        assertEquals("Good morning.", notes.transcripts[0].second.sentences[0].text)
        assertTrue(notes.transcripts[0].second.isDiarized)
        assertEquals(
            group.two.knockledge.data.model.TranscriptionEngineType.CLOUD_FIREBASE_AI,
            notes.transcripts[0].second.engineType,
        )
        assertEquals("note-0", notes.summaries[0].first)
        assertEquals("A greeting.", notes.summaries[0].second.executiveSummary)
        assertEquals(AiEngineType.CLOUD_FIREBASE_AI, notes.summaries[0].second.generationMode)
    }

    @Test
    fun skipsEverythingWhenSignedOut() = runBlocking {
        val notes = FakeNotes()
        SnapshotNoteSink(notes, FakeAuth(null)).record(wav(), 1_000L, 61_000L, TriggerType.MANUAL, transcript, summary)

        assertTrue(notes.created.isEmpty())
        assertTrue(notes.transcripts.isEmpty())
        assertTrue(notes.summaries.isEmpty())
    }
}
