package group.two.knockledge

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import group.two.knockledge.ai.FirebaseAiTranscriber
import group.two.knockledge.ai.FirebaseGeminiClient
import group.two.knockledge.pipeline.AudioSegment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Live Firebase AI (Gemini) transcription test — hits the real network API.
 *
 * Requires: androidApp/google-services.json, AI Logic enabled, device online.
 * Audio:    drop a 16-bit PCM WAV named sample.wav into src/androidTest/assets/.
 *           Record the two lines in TRANSCRIPT_SCRIPT (same or different voices).
 * Run:      gradlew :androidApp:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=group.two.knockledge.GeminiTranscriptionLiveTest"
 */
@RunWith(AndroidJUnit4::class)
class GeminiTranscriptionLiveTest {

    @Test
    fun realAudioTranscribesViaGemini() = runBlocking {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val wav = try {
            assets.open(WAV_ASSET).use { it.readBytes() }
        } catch (e: Exception) {
            fail("Missing $WAV_ASSET in src/androidTest/assets. Record: $TRANSCRIPT_SCRIPT")
            return@runBlocking
        }
        val segment = decodeWav(wav)

        // AI Logic attaches a Firebase Auth token; dedicated test user (anonymous auth disabled per spec)
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword("live-test@knockledge.dev", "test-password-123"))

        val result = FirebaseAiTranscriber(FirebaseGeminiClient()).process(segment)

        println("sentences=" + result.sentences.joinToString { "[${it.speaker} ${it.startTimeMs}-${it.endTimeMs}] ${it.text}" })
        println("diarized=${result.isDiarized} lang=${result.detectedLanguage}")
        assertTrue("no sentences returned", result.sentences.isNotEmpty())
        assertTrue("expected >=2 turns, got ${result.sentences.size}", result.sentences.size >= 2)
        // "meeting" proves line 1 was heard, "screen" proves line 2 was heard
        // ("tara" proved unreliable — the model normalizes Taglish to English)
        assertTrue("expected 'meeting' in: ${result.fullText}", result.fullText.contains("meeting", ignoreCase = true))
        assertTrue("expected 'screen' in: ${result.fullText}", result.fullText.contains("screen", ignoreCase = true))
    }

    private fun decodeWav(bytes: ByteArray): AudioSegment {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE") { "not a WAV file" }
        var pos = 12
        var rate = 0
        var channels = 0
        var bits = 0
        var pcm: ByteArray? = null
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val size = b.getInt(pos + 4)
            when (id) {
                "fmt " -> {
                    channels = b.getShort(pos + 10).toInt()
                    rate = b.getInt(pos + 12)
                    bits = b.getShort(pos + 22).toInt()
                }
                "data" -> pcm = bytes.copyOfRange(pos + 8, pos + 8 + size)
            }
            pos += 8 + size + (size and 1)
        }
        require(pcm != null && rate > 0) { "missing fmt/data chunk" }
        require(bits == 16) { "only 16-bit PCM supported, got $bits" }
        val frames = pcm.size / 2 / channels
        val samples = ShortArray(frames)
        for (f in 0 until frames) {
            samples[f] = pcm[f * channels * 2].toInt().or(pcm[f * channels * 2 + 1].toInt() shl 8).toShort()
        }
        return AudioSegment(samples, rate, 0)
    }

    private companion object {
        const val WAV_ASSET = "sample.wav"
        const val TRANSCRIPT_SCRIPT =
            "Speaker 1: \"Okay, tara na. Let us start the meeting now.\" " +
                "Speaker 2: \"Sige. I will share my screen in five seconds.\""
    }
}
