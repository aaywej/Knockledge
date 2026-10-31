package two.knockledge.ui.settings

import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import group.two.knockledge.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun defaultInitialStateMatchesAppSettingsDefaults() {
        val memoryStore = MemoryStore()
        val settings = AppSettings(memoryStore, "qwen2b")
        val viewModel = SettingsViewModel(settings, CoroutineScope(Dispatchers.Unconfined))

        val state = viewModel.uiState.value
        assertEquals(300, state.bufferDurationSeconds)
        assertEquals(5, state.minAudioSaveSeconds)
        assertEquals(6.0f, state.knockSensorThreshold, 0.001f)
        assertEquals("mic", state.inputSource)
        assertEquals("auto", state.transcriptionMode)
        assertEquals("whisper", state.transcriptionLocalModel)
        assertEquals("tl", state.transcriptionLanguage)
        assertEquals("auto", state.summaryLanguage)
        assertEquals("qwen2b", state.summaryModel)
        assertEquals("auto", state.comprehensionMode)
    }

    @Test
    fun updatingSettingsUpdatesUiState() {
        val memoryStore = MemoryStore()
        val settings = AppSettings(memoryStore, "qwen2b")
        val viewModel = SettingsViewModel(settings, CoroutineScope(Dispatchers.Unconfined))

        viewModel.setBufferDurationSeconds(120)
        assertEquals(120, viewModel.uiState.value.bufferDurationSeconds)

        viewModel.setMinAudioSaveSeconds(15)
        assertEquals(15, viewModel.uiState.value.minAudioSaveSeconds)

        viewModel.setKnockSensorThreshold(4.5f)
        assertEquals(4.5f, viewModel.uiState.value.knockSensorThreshold, 0.001f)

        viewModel.setInputSource("mic_device")
        assertEquals("mic_device", viewModel.uiState.value.inputSource)

        viewModel.setTranscriptionMode("online")
        assertEquals("online", viewModel.uiState.value.transcriptionMode)

        viewModel.setTranscriptionLocalModel("whisper_tiny")
        assertEquals("whisper_tiny", viewModel.uiState.value.transcriptionLocalModel)

        viewModel.setTranscriptionLanguage("en")
        assertEquals("en", viewModel.uiState.value.transcriptionLanguage)

        viewModel.setSummaryLanguage("en")
        assertEquals("en", viewModel.uiState.value.summaryLanguage)

        viewModel.setSummaryModel("qwen7b")
        assertEquals("qwen7b", viewModel.uiState.value.summaryModel)

        viewModel.setComprehensionMode("local")
        assertEquals("local", viewModel.uiState.value.comprehensionMode)
    }
}
