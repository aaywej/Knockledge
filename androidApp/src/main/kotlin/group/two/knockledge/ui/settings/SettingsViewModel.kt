package group.two.knockledge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import group.two.knockledge.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for managing application settings state and updates.
 */
class SettingsViewModel @Inject constructor(
    private val settings: AppSettings,
    scope: CoroutineScope? = null,
) : ViewModel(), SettingsController {

    private val coroutineScope = scope ?: viewModelScope

    /** Combined reactive state flow for all settings. */
    override val uiState: StateFlow<SettingsUiState> = combine(
        settings.bufferDurationSeconds,
        settings.minAudioSaveSeconds,
        settings.knockSensorThreshold,
        settings.inputSource,
    ) { buffer, minSave, threshold, input ->
        quadrupleOf(buffer, minSave, threshold, input)
    }.let { audioFlow ->
        combine(
            audioFlow,
            combine(
                settings.transcriptionMode,
                settings.transcriptionLocalModel,
                settings.language,
                settings.summaryLanguage,
            ) { mode, model, lang, summaryLang ->
                arrayListOf(mode, model, lang, summaryLang)
            },
            combine(
                settings.summaryModel,
                settings.comprehensionMode,
            ) { summaryModel, comprehension ->
                Pair(summaryModel, comprehension)
            }
        ) { audio, aiList, aiPair ->
            SettingsUiState(
                bufferDurationSeconds = audio.first,
                minAudioSaveSeconds = audio.second,
                knockSensorThreshold = audio.third,
                inputSource = audio.fourth,
                transcriptionMode = aiList[0],
                transcriptionLocalModel = aiList[1],
                transcriptionLanguage = aiList[2],
                summaryLanguage = aiList[3],
                summaryModel = aiPair.first,
                comprehensionMode = aiPair.second,
            )
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        SettingsUiState(
            bufferDurationSeconds = settings.bufferDurationSeconds.value,
            minAudioSaveSeconds = settings.minAudioSaveSeconds.value,
            knockSensorThreshold = settings.knockSensorThreshold.value,
            inputSource = settings.inputSource.value,
            transcriptionMode = settings.transcriptionMode.value,
            transcriptionLocalModel = settings.transcriptionLocalModel.value,
            transcriptionLanguage = settings.language.value,
            summaryLanguage = settings.summaryLanguage.value,
            summaryModel = settings.summaryModel.value,
            comprehensionMode = settings.comprehensionMode.value,
        ),
    )

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun <A, B, C, D> quadrupleOf(a: A, b: B, c: C, d: D) = Quadruple(a, b, c, d)

    /** Sets the circular buffer duration in seconds. */
    override fun setBufferDurationSeconds(seconds: Int) {
        settings.setBufferDurationSeconds(seconds)
    }

    /** Sets the minimum audio save duration in seconds. */
    override fun setMinAudioSaveSeconds(seconds: Int) {
        settings.setMinAudioSaveSeconds(seconds)
    }

    /** Sets the knock sensor acceleration threshold. */
    override fun setKnockSensorThreshold(threshold: Float) {
        settings.setKnockSensorThreshold(threshold)
    }

    /** Sets the transcription mode. */
    override fun setTranscriptionMode(mode: String) {
        settings.setTranscriptionMode(mode)
    }

    /** Sets the transcription local model ID. */
    override fun setTranscriptionLocalModel(modelId: String) {
        settings.setTranscriptionLocalModel(modelId)
    }

    /** Sets the transcription language code. */
    override fun setTranscriptionLanguage(language: String) {
        settings.setLanguage(language)
    }

    /** Sets the summary language code. */
    override fun setSummaryLanguage(language: String) {
        settings.setSummaryLanguage(language)
    }

    /** Sets the summary model ID. */
    override fun setSummaryModel(modelId: String) {
        settings.setSummaryModel(modelId)
    }

    /** Sets the comprehension mode. */
    override fun setComprehensionMode(mode: String) {
        settings.setComprehensionMode(mode)
    }

    /** Sets the input source. */
    override fun setInputSource(source: String) {
        settings.setInputSource(source)
    }
}
