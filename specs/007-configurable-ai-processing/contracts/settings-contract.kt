package group.two.knockledge.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract extension for AppSettings to support configurable automatic AI processing.
 */
interface AiProcessingSettingsContract {
    /**
     * Whether transcription and summarization execute automatically upon saving a circular buffer.
     * Defaults to false.
     */
    val autoAiProcessingEnabled: StateFlow<Boolean>

    /**
     * Updates the automatic AI processing preference.
     *
     * @param enabled True to auto-process captures, false to require manual user trigger.
     */
    fun setAutoAiProcessingEnabled(enabled: Boolean)
}
