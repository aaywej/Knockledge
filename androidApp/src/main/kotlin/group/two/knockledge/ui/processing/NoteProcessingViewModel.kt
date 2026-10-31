package group.two.knockledge.ui.processing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.ChapterMarkerInput
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.SentenceInput
import group.two.knockledge.data.SummaryInput
import group.two.knockledge.data.TranscriptInput
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.notification.AiNotificationNotifier
import group.two.knockledge.notification.NoopAiNotificationNotifier
import group.two.knockledge.pipeline.PipelineResult
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.RewindStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import group.two.knockledge.data.model.TranscriptionEngineType as ModelEngineType
import group.two.knockledge.pipeline.TranscriptionEngineType as PipelineEngineType

/**
 * ViewModel coordinating on-demand AI transcription and summarization for notes.
 */
class NoteProcessingViewModel(
    private val notes: NoteRepository,
    private val auth: AuthRepository,
    private val pipeline: RewindPipeline,
    private val notifier: AiNotificationNotifier = NoopAiNotificationNotifier,
    coroutineScope: CoroutineScope? = null,
) : ViewModel(), NoteProcessingController {

    private val scope = coroutineScope ?: viewModelScope

    private val _processingState = MutableStateFlow(NoteProcessingUiState())
    override val processingState: StateFlow<NoteProcessingUiState> = _processingState.asStateFlow()

    override fun processNote(noteId: String) {
        // Deduplication guard: do not re-trigger if processing is active
        if (_processingState.value.isProcessing) return

        _processingState.update {
            NoteProcessingUiState(
                noteId = noteId,
                isProcessing = true,
                state = NoteAiProcessingState.TRANSCRIBING,
                stage = RewindStage.Preprocessing,
                errorMessage = null,
                isSuccess = false,
            )
        }

        scope.launch(Dispatchers.Default) {
            try {
                val user = auth.currentUser.value
                if (user == null) {
                    _processingState.update {
                        it.copy(
                            isProcessing = false,
                            state = NoteAiProcessingState.FAILED,
                            errorMessage = "User not authenticated.",
                        )
                    }
                    return@launch
                }

                val note = notes.getNote(noteId, user.uid)
                if (note == null) {
                    _processingState.update {
                        it.copy(
                            isProcessing = false,
                            state = NoteAiProcessingState.FAILED,
                            errorMessage = "Note not found.",
                        )
                    }
                    return@launch
                }

                val audioFile = File(note.audio.localPath)
                if (!audioFile.exists() || !audioFile.isFile) {
                    _processingState.update {
                        it.copy(
                            isProcessing = false,
                            state = NoteAiProcessingState.FAILED,
                            errorMessage = "Audio file missing.",
                        )
                    }
                    return@launch
                }

                val pcm = readPcm(audioFile)

                val result = pipeline.run(
                    pcm = pcm,
                    sampleRate = note.audio.sampleRate,
                    durationMillis = note.audio.durationMs,
                    onStage = { stage ->
                        _processingState.update {
                            it.copy(
                                stage = stage,
                                state = if (stage == RewindStage.Summarizing) {
                                    NoteAiProcessingState.SUMMARIZING
                                } else {
                                    NoteAiProcessingState.TRANSCRIBING
                                },
                            )
                        }
                    },
                )

                if (result is PipelineResult.InsufficientSpeech) {
                    _processingState.update {
                        it.copy(
                            isProcessing = false,
                            state = NoteAiProcessingState.FAILED,
                            errorMessage = result.message,
                        )
                    }
                    return@launch
                }
                val success = result as PipelineResult.Success
                val transcript = success.transcript

                notes.attachTranscript(
                    noteId,
                    user.uid,
                    TranscriptInput(
                        fullText = transcript.fullText,
                        sentences = transcript.sentences.map {
                            SentenceInput(it.speaker, it.startTimeMs, it.endTimeMs, it.text, it.confidence)
                        },
                        isDiarized = transcript.isDiarized,
                        detectedLanguage = transcript.detectedLanguage,
                        engineType = ModelEngineType.valueOf(transcript.engineType.name),
                    ),
                )

                val chapterInputs = if (transcript.chapters.isNotEmpty()) {
                    transcript.chapters.map { ChapterMarkerInput(it.startTimeMs, it.title, it.keyBullets) }
                } else {
                    success.summary.chapters.map { ChapterMarkerInput(it.startTimeMs, it.title, it.keyBullets) }
                }

                notes.attachSummary(
                    noteId,
                    user.uid,
                    SummaryInput(
                        title = success.summary.title,
                        executiveSummary = success.summary.gist,
                        keyPoints = success.summary.points,
                        chapterMarkers = chapterInputs,
                        generationMode = if (transcript.engineType == PipelineEngineType.CLOUD_FIREBASE_AI) {
                            AiEngineType.CLOUD_FIREBASE_AI
                        } else {
                            AiEngineType.LOCAL_ON_DEVICE
                        },
                    ),
                )

                notifier.notifyAiComplete(success.summary.title, success.summary.gist)

                _processingState.update {
                    it.copy(
                        isProcessing = false,
                        state = NoteAiProcessingState.COMPLETED,
                        isSuccess = true,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _processingState.update {
                    it.copy(
                        isProcessing = false,
                        state = NoteAiProcessingState.FAILED,
                        errorMessage = t.message ?: "AI processing failed.",
                    )
                }
            }
        }
    }

    override fun resetState() {
        if (_processingState.value.isProcessing) return
        _processingState.value = NoteProcessingUiState()
    }

    private fun readPcm(wav: File): ShortArray {
        val bytes = wav.readBytes()
        val offset = if (bytes.size >= FileLocalAudioRingBuffer.WAV_HEADER_BYTES) {
            FileLocalAudioRingBuffer.WAV_HEADER_BYTES
        } else {
            0
        }
        val pcmSize = (bytes.size - offset) / 2
        return ShortArray(pcmSize.coerceAtLeast(0)).also {
            if (it.isNotEmpty()) {
                ByteBuffer.wrap(bytes, offset, it.size * 2)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(it)
            }
        }
    }
}
