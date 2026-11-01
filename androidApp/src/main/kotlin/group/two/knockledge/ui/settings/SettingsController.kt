package group.two.knockledge.ui.settings

import kotlinx.coroutines.flow.StateFlow

/** Immutable settings state for the settings screen. */
data class SettingsUiState(
    val bufferDurationSeconds: Int = 300,
    val minAudioSaveSeconds: Int = 5,
    val knockSensorThreshold: Float = 6.0f,
    val inputSource: String = "mic",
    val transcriptionMode: String = "auto",
    val transcriptionLocalModel: String = "whisper",
    val transcriptionLanguage: String = "tl",
    val summaryLanguage: String = "auto",
    val summaryModel: String = "qwen2b",
    val comprehensionMode: String = "auto",
)

/** Contract for updating application settings from the settings screen. */
interface SettingsController {
    /** Current settings UI state. */
    val uiState: StateFlow<SettingsUiState>

    /** Sets the circular buffer duration in seconds. */
    fun setBufferDurationSeconds(seconds: Int)

    /** Sets the minimum audio save duration in seconds. */
    fun setMinAudioSaveSeconds(seconds: Int)

    /** Sets the knock sensor acceleration threshold. */
    fun setKnockSensorThreshold(threshold: Float)

    /** Sets the transcription mode (auto, online, local). */
    fun setTranscriptionMode(mode: String)

    /** Sets the transcription local model ID. */
    fun setTranscriptionLocalModel(modelId: String)

    /** Sets the transcription language code. */
    fun setTranscriptionLanguage(language: String)

    /** Sets the summary language code. */
    fun setSummaryLanguage(language: String)

    /** Sets the summary model ID. */
    fun setSummaryModel(modelId: String)

    /** Sets the comprehension mode (auto, online, local). */
    fun setComprehensionMode(mode: String)

    /** Sets the input source (mic, mic_device). */
    fun setInputSource(source: String)
}
