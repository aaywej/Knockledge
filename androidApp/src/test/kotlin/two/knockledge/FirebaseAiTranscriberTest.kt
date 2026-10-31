package group.two.knockledge

import group.two.knockledge.ai.FirebaseAiTranscriber
import group.two.knockledge.ai.GeminiClient
import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FakeGemini(val json: String) : GeminiClient {
    var wavSize = 0
    override suspend fun generate(wav: ByteArray, prompt: String): String {
        wavSize = wav.size
        return json
    }
}

class FirebaseAiTranscriberTest {
    private val segment = AudioSegment(ShortArray(16_000), 16_000, 1_000)

    @Test
    fun parsesDiarizedJson() = runBlocking {
        val gemini = FakeGemini(
            """{"detectedLanguage":"taglish","sentences":[
            {"speaker":"Speaker 1","startTimeMs":0,"endTimeMs":1500,"text":"Okay, \"tara\" na","confidence":0.9},
            {"speaker":"Speaker 2","startTimeMs":1500,"endTimeMs":2500,"text":"Sige"}]}"""
        )
        val r = FirebaseAiTranscriber(gemini).process(segment)
        assertEquals(TranscriptionEngineType.CLOUD_FIREBASE_AI, r.engineType)
        assertTrue(r.isDiarized)
        assertEquals("taglish", r.detectedLanguage)
        assertEquals(listOf("Speaker 1", "Speaker 2"), r.sentences.map { it.speaker })
        assertEquals("Okay, \"tara\" na Sige", r.fullText)
        assertEquals(0.9f, r.sentences[0].confidence)
        assertEquals(1.0f, r.sentences[1].confidence)
        assertEquals(44 + 32_000, gemini.wavSize)
    }

    @Test
    fun singleSpeakerIsNotDiarized() = runBlocking {
        val gemini = FakeGemini(
            """{"detectedLanguage":"en","sentences":[{"speaker":"Speaker 1","startTimeMs":0,"endTimeMs":5,"text":"hi"}]}"""
        )
        assertEquals(false, FirebaseAiTranscriber(gemini).process(segment).isDiarized)
    }

    @Test
    fun malformedJsonThrows() {
        assertFailsWith<Exception> { runBlocking { FirebaseAiTranscriber(FakeGemini("not json")).process(segment) } }
    }
}
