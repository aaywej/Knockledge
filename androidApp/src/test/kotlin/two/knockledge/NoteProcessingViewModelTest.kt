package two.knockledge

import group.two.knockledge.data.AudioSnapshotInput
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.SummaryInput
import group.two.knockledge.data.TranscriptInput
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.AuthProvider
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.data.model.UserProfile
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.ui.processing.NoteAiProcessingState
import group.two.knockledge.ui.processing.NoteProcessingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteProcessingViewModelTest {

    private class FakeAuth(user: UserProfile?) : AuthRepository {
        override val currentUser: StateFlow<UserProfile?> = MutableStateFlow(user)
        override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> = TODO()
        override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = TODO()
        override suspend fun registerWithEmail(displayName: String, email: String, password: String): Result<UserProfile> = TODO()
        override suspend fun signOut() = Unit
    }

    private class FakeNotes(private val note: Note?) : NoteRepository {
        var attachedTranscript: TranscriptInput? = null
        var attachedSummary: SummaryInput? = null

        override fun observeNotes(userId: String): Flow<List<Note>> = flowOf(note?.let { listOf(it) } ?: emptyList())
        override suspend fun getNote(id: String, userId: String): Note? = if (id == note?.id) note else null
        override suspend fun createFromSnapshot(snapshot: AudioSnapshotInput, userId: String): Note = TODO()

        override suspend fun attachTranscript(noteId: String, userId: String, transcript: TranscriptInput): Note {
            attachedTranscript = transcript
            return note!!
        }

        override suspend fun attachSummary(noteId: String, userId: String, summary: SummaryInput): Note {
            attachedSummary = summary
            return note!!
        }

        override suspend fun deleteNote(id: String, userId: String) = Unit
        override suspend fun syncPending(userId: String) = Unit
    }

    private val user = UserProfile("u1", "test@example.com", "Test", null, AuthProvider.GOOGLE, 1L)
    private var tempWavFile: File? = null

    private fun createDummyWavFile(): File {
        val file = File.createTempFile("dummy_audio", ".wav")
        val pcm = ShortArray(16_000) { 2_000 }
        val bytes = ByteArray(44 + pcm.size * 2)
        ByteBuffer.wrap(bytes, 44, pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm)
        file.writeBytes(bytes)
        tempWavFile = file
        return file
    }

    @AfterTest
    fun cleanup() {
        tempWavFile?.delete()
    }

    private suspend fun awaitIdle(vm: NoteProcessingViewModel) =
        withTimeout(5_000) { while (vm.processingState.value.isProcessing) delay(10) }

    @Test
    fun processNoteTransitionsToCompletedAndAttachesTranscriptAndSummary() = runBlocking {
        val wav = createDummyWavFile()
        val note = Note(
            id = "note-123",
            userId = user.uid,
            title = "Test Note",
            createdAtEpochMs = 1_000L,
            updatedAtEpochMs = 1_000L,
            audio = AudioMetadata(wav.absolutePath, null, 1_000L, 16_000, TriggerType.GESTURE),
        )
        val notesRepo = FakeNotes(note)
        val authRepo = FakeAuth(user)
        val pipeline = RewindPipeline(
            transcriber = PlaceholderTranscriber(),
            preprocessor = DefaultAudioPreprocessor(),
            summarizer = PlaceholderSummarizer(),
        )

        val vm = NoteProcessingViewModel(
            notes = notesRepo,
            auth = authRepo,
            pipeline = pipeline,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )

        assertEquals(NoteAiProcessingState.IDLE, vm.processingState.value.state)

        vm.processNote("note-123")
        awaitIdle(vm)

        val state = vm.processingState.value
        assertEquals(NoteAiProcessingState.COMPLETED, state.state)
        assertTrue(state.isSuccess)
        assertFalse(state.isProcessing)
        assertEquals(null, state.errorMessage)

        assertEquals("note-123", state.noteId)
        assertTrue(notesRepo.attachedTranscript != null)
        assertTrue(notesRepo.attachedSummary != null)
    }

    @Test
    fun processNoteDeduplicatesConcurrentTriggers() = runBlocking {
        val wav = createDummyWavFile()
        val note = Note(
            id = "note-123",
            userId = user.uid,
            title = "Test Note",
            createdAtEpochMs = 1_000L,
            updatedAtEpochMs = 1_000L,
            audio = AudioMetadata(wav.absolutePath, null, 1_000L, 16_000, TriggerType.GESTURE),
        )
        val notesRepo = FakeNotes(note)
        val authRepo = FakeAuth(user)
        val pipeline = RewindPipeline(
            transcriber = PlaceholderTranscriber(),
            preprocessor = DefaultAudioPreprocessor(),
            summarizer = PlaceholderSummarizer(),
        )

        val vm = NoteProcessingViewModel(
            notes = notesRepo,
            auth = authRepo,
            pipeline = pipeline,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )

        vm.processNote("note-123")

        vm.processNote("note-123")
        awaitIdle(vm)

        assertEquals(NoteAiProcessingState.COMPLETED, vm.processingState.value.state)
    }

    @Test
    fun processNoteSetsFailedStateWhenAudioFileIsMissing() = runBlocking {
        val note = Note(
            id = "note-missing",
            userId = user.uid,
            title = "Missing Note",
            createdAtEpochMs = 1_000L,
            updatedAtEpochMs = 1_000L,
            audio = AudioMetadata("/tmp/non_existent_audio_file.wav", null, 1_000L, 16_000, TriggerType.GESTURE),
        )
        val notesRepo = FakeNotes(note)
        val authRepo = FakeAuth(user)
        val pipeline = RewindPipeline(
            transcriber = PlaceholderTranscriber(),
            preprocessor = DefaultAudioPreprocessor(),
            summarizer = PlaceholderSummarizer(),
        )

        val vm = NoteProcessingViewModel(
            notes = notesRepo,
            auth = authRepo,
            pipeline = pipeline,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
        )

        vm.processNote("note-missing")
        awaitIdle(vm)

        val state = vm.processingState.value
        assertEquals(NoteAiProcessingState.FAILED, state.state)
        assertFalse(state.isProcessing)
        assertEquals("Audio file missing.", state.errorMessage)
    }
}
