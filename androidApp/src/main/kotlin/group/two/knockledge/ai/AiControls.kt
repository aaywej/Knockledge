package group.two.knockledge.ai

import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelEntry
import group.two.knockledge.models.ModelFile
import group.two.knockledge.models.ModelStore
import group.two.knockledge.models.SummaryModelIds
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.InputSources
import group.two.knockledge.settings.SummaryLanguages
import group.two.knockledge.settings.TranscriptionLanguages

/** One row in the "Select model" picker. */
data class SummaryModelOption(
    val id: String,
    val label: String,
    val detail: String,
    /** False when it cannot be chosen (Apple Intelligence off or unsupported). */
    val enabled: Boolean,
    /** A model file still has to be downloaded before it works. */
    val needsDownload: Boolean,
)

/** One row in a language or input-source picker; [id] is the value saved in Settings. */
data class LanguageOption(val id: String, val label: String, val detail: String)

/** What Settings and the model picker read and change. Shared by both apps. */
/**
 * Controls AI-related settings and interactions.
 */
class AiControls(
    val settings: AppSettings,
    private val models: ModelStore,
    private val system: SystemModelEngine?,
    private val totalRamBytes: Long,
    /** False where the platform cannot capture its own playback. */
    val supportsDeviceAudio: Boolean = false,
) {
    /** Every summary model the picker can offer: Qwen first, then the built-in model when present. */
    /**
 * Returns a list of summary model options for the picker.
 */
fun summaryOptions(): List<SummaryModelOption> {
        val options = mutableListOf<SummaryModelOption>()
        system?.let { apple ->
            val reason = apple.unavailableReason()
            options += SummaryModelOption(
                id = SummaryModelIds.APPLE,
                label = apple.label,
                detail = reason ?: "Built in · Apple Intelligence · English only (Filipino recaps use Qwen)",
                enabled = reason == null,
                needsDownload = false,
            )
        }
        // Qwen first: it is the default and the one that handles Filipino.
        val qwen = listOf(
            qwenOption(ModelCatalog.qwen2b, "Best quality · Filipino and English"),
            qwenOption(ModelCatalog.qwen08b, "Faster and lighter"),
        )
        return qwen + options
    }

    /**
 * Creates a SummaryModelOption for a Qwen model.
 */
private fun qwenOption(file: ModelFile, note: String): SummaryModelOption {
        val installed = models.pathOf(file) != null
        val lowRam = file == ModelCatalog.qwen2b && ModelCatalog.summarizerFor(totalRamBytes) != file
        val detail = buildString {
            append(if (installed) "On device · " else "Download · ")
            append(gigabytes(file.approxBytes))
            append(" · ")
            append(if (lowRam) "may be slow on this phone" else note)
        }
        return SummaryModelOption(file.id, file.label, detail, enabled = true, needsDownload = !installed)
    }

    /** Saves [id] as the summary model and re-reads which model files are installed. */
    /**
 * Selects the summary model by its ID and refreshes the model store.
 */
fun selectSummaryModel(id: String) {
        settings.setSummaryModel(id)
        models.refresh()
    }

    /** Spoken language choices for Whisper. */
    val languageOptions: List<LanguageOption> = listOf(
        LanguageOption(TranscriptionLanguages.FILIPINO, "Filipino / Taglish", "Best for Tagalog and mixed speech"),
        LanguageOption(TranscriptionLanguages.ENGLISH, "English", "Best when everyone speaks English"),
        LanguageOption(TranscriptionLanguages.AUTO, "Auto-detect", "Whisper guesses; may turn Tagalog into English"),
    )

    /** Saves [id] as the spoken language; the next transcription uses it. */
    /**
 * Sets the transcription language.
 */
fun selectLanguage(id: String) = settings.setLanguage(id)

    /** Language choices for the written recap, independent of the spoken language. */
    val summaryLanguageOptions: List<LanguageOption> = listOf(
        LanguageOption(SummaryLanguages.AUTO, "Match the speakers", "English, Filipino or Taglish, whichever was used most"),
        LanguageOption(SummaryLanguages.FILIPINO, "Filipino", "Recaps always in Filipino (Tagalog)"),
        LanguageOption(SummaryLanguages.ENGLISH, "English", "Recaps always in English"),
    )

    /** Saves [id] as the recap language; the next summary uses it. */
    /**
 * Sets the summary language.
 */
fun selectSummaryLanguage(id: String) = settings.setSummaryLanguage(id)

    /** What to record: the microphone alone, or the microphone mixed with phone playback. */
    val inputOptions: List<LanguageOption> = listOf(
        LanguageOption(InputSources.MIC, "Microphone", "Voices in the room"),
        LanguageOption(
            InputSources.MIC_AND_DEVICE,
            "Microphone + phone audio",
            "Also hears videos and online classes playing on this phone. Calls cannot be captured: " +
                "the system keeps call audio private and mutes other apps' mics during a call.",
        ),
    )

    /** Saves [id] as the input source; it applies from the next recording. */
    /**
 * Sets the input source for recording.
 */
fun selectInput(id: String) = settings.setInputSource(id)

    /** Every catalogue model with its install state, for the storage list in Settings. */
    /**
 * Returns all model entries with their installation state.
 */
fun modelEntries(): List<ModelEntry> = models.entries()

    /** Deletes [file]'s download, partial or complete, and re-reads what is stored. */
    /**
 * Removes the specified model file from storage.
 */
fun removeModel(file: ModelFile) = models.remove(file)

    companion object {
        /** Renders [bytes] as "N MB" or "N.N GB" for the picker's detail line. */
        fun gigabytes(bytes: Long): String {
            val mb = bytes / 1_000_000L
            return if (mb >= 1000) "${mb / 1000}.${(mb % 1000) / 100} GB" else "$mb MB"
        }
    }
}

