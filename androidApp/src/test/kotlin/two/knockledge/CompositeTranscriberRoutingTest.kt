package two.knockledge

import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.CompositeTranscriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptionEngine
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CompositeTranscriberRoutingTest {
    private class FakeEngine(private val type: TranscriptionEngineType) : TranscriptionEngine {
        var callCount = 0
        override val engineType: TranscriptionEngineType get() = type
        override suspend fun process(segment: AudioSegment): TranscriptResult {
            callCount++
            return TranscriptResult(emptyList(), "test", false, "en", type)
        }
    }

    @Test
    fun routesAccordingToMode() = runBlocking {
        val online = FakeEngine(TranscriptionEngineType.CLOUD_FIREBASE_AI)
        val offline = FakeEngine(TranscriptionEngineType.LOCAL_WHISPER)
        var currentMode = "auto"
        var isOnline = true

        val transcriber = CompositeTranscriber(
            online = online,
            offline = offline,
            isOnline = { isOnline },
            mode = { currentMode }
        )

        val pcm = ShortArray(16000) { 100 }

        // Mode: local -> must call offline even if online is available
        currentMode = "local"
        val r1 = transcriber.transcribe(pcm, 16000)
        assertEquals(TranscriptionEngineType.LOCAL_WHISPER, r1.engineType)
        assertEquals(1, offline.callCount)
        assertEquals(0, online.callCount)

        // Mode: online -> must call online
        currentMode = "online"
        val r2 = transcriber.transcribe(pcm, 16000)
        assertEquals(TranscriptionEngineType.CLOUD_FIREBASE_AI, r2.engineType)
        assertEquals(1, online.callCount)

        // Mode: auto while offline -> falls back to offline
        currentMode = "auto"
        isOnline = false
        val r3 = transcriber.transcribe(pcm, 16000)
        assertEquals(TranscriptionEngineType.LOCAL_WHISPER, r3.engineType)
        assertEquals(2, offline.callCount)
    }
}
