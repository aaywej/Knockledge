package group.two.knockledge

import group.two.knockledge.ai.AiControls
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.data.RecentsRepository
import group.two.knockledge.models.ModelProvider
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RegexSensitiveFilter
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.SensitiveFilter
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Transcriber

/**
 * The app's dependency aggregate. Dagger builds the real one (see di/KnockledgeComponent);
 * the secondary constructor keeps previews and tests working without it: pass a microphone
 * and an "in progress" surface and get placeholders for everything else.
 */
class KnockledgeGraph(
    val controller: KnockledgeController,
    val recents: RecentsRepository,
    /** On-device AI model store; null where the platform has none yet. */
    val models: ModelProvider?,
    /** Settings and model picker; null in previews. */
    val ai: AiControls?,
) {
    constructor(
        audioSource: AudioSource,
        hooks: SessionHooks,
        transcriber: Transcriber = PlaceholderTranscriber(),
        filter: SensitiveFilter = RegexSensitiveFilter(),
        summarizer: Summarizer = PlaceholderSummarizer(),
        recents: RecentsRepository = InMemoryRecentsRepository(),
        buffer: LocalAudioRingBuffer,
    ) : this(
        controller = KnockledgeController(
            audioSource = audioSource,
            hooks = hooks,
            pipeline = RewindPipeline(transcriber, filter, summarizer),
            recents = recents,
            buffer = buffer,
        ),
        recents = recents,
        models = null,
        ai = null,
    )
}
