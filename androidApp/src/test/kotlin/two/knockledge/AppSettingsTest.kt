package two.knockledge

import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultSettingsValuesArePopulatedCorrectly() {
        val store = MemoryStore()
        val settings = AppSettings(store, defaultSummaryModel = "qwen2b")

        assertEquals(5, settings.minAudioSaveSeconds.value)
        assertEquals(6.0f, settings.knockSensorThreshold.value, 0.001f)
        assertEquals("auto", settings.transcriptionMode.value)
        assertEquals("whisper", settings.transcriptionLocalModel.value)
        assertEquals("auto", settings.comprehensionMode.value)
        assertEquals("qwen2b", settings.summaryModel.value)
    }

    @Test
    fun settersUpdateAndPersistValues() {
        val store = MemoryStore()
        val settings = AppSettings(store, defaultSummaryModel = "qwen2b")

        settings.setMinAudioSaveSeconds(15)
        assertEquals(15, settings.minAudioSaveSeconds.value)
        assertEquals("15", store.getString("min_audio_save_seconds"))

        settings.setKnockSensorThreshold(4.0f)
        assertEquals(4.0f, settings.knockSensorThreshold.value, 0.001f)
        assertEquals("4.0", store.getString("knock_sensor_threshold"))

        settings.setTranscriptionMode("online")
        assertEquals("online", settings.transcriptionMode.value)
        assertEquals("online", store.getString("transcription_mode"))

        settings.setTranscriptionLocalModel("whisper_tiny")
        assertEquals("whisper_tiny", settings.transcriptionLocalModel.value)
        assertEquals("whisper_tiny", store.getString("transcription_local_model"))

        settings.setComprehensionMode("local")
        assertEquals("local", settings.comprehensionMode.value)
        assertEquals("local", store.getString("comprehension_mode"))
    }
}
