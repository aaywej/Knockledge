package group.two.knockledge.pipeline

import group.two.knockledge.settings.SummaryLanguages
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResourcePromptProviderTest {

    private val xmlFile = File("src/main/res/values/prompts.xml").let {
        if (it.exists()) it else File("androidApp/src/main/res/values/prompts.xml")
    }

    private val parsedStrings: Map<String, String> by lazy {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        val stringNodes = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i) as Element
            map[node.getAttribute("name")] = node.textContent.trim()
        }
        map
    }

    @Test
    fun promptsXmlExistsAndIsValidXml() {
        assertTrue(xmlFile.exists(), "prompts.xml must exist on disk")
        assertTrue(parsedStrings.isNotEmpty(), "prompts.xml must contain string resources")
    }

    @Test
    fun geminiTranscribePromptMatchesContract() {
        val prompt = parsedStrings["prompt_gemini_transcribe"]
        assertNotNull(prompt)
        assertTrue(prompt.contains("Transcribe this audio verbatim"))
        assertTrue(prompt.contains("timestamped chapters"))
    }

    @Test
    fun taglishHintPromptMatchesContract() {
        val prompt = parsedStrings["prompt_taglish_hint"]
        assertNotNull(prompt)
        assertTrue(prompt.contains("Kumusta, magandang umaga po"))
        assertTrue(prompt.contains("paki-check"))
    }

    @Test
    fun summarizerSystemPromptAndRulesArePresent() {
        val systemPrompt = parsedStrings["prompt_summarizer_system"]
        assertNotNull(systemPrompt)
        assertTrue(systemPrompt.contains("%1\$s"))
        assertTrue(systemPrompt.contains("ADHD"))

        val filipino = parsedStrings["prompt_lang_rule_filipino"]
        assertNotNull(filipino)
        assertTrue(filipino.contains("Filipino (Tagalog)"))

        val english = parsedStrings["prompt_lang_rule_english"]
        assertNotNull(english)
        assertTrue(english.contains("clear, simple English"))

        val auto = parsedStrings["prompt_lang_rule_auto"]
        assertNotNull(auto)
        assertTrue(auto.contains("language the speakers mostly used"))
    }

    @Test
    fun summarizerUserPromptAndAsksArePresent() {
        val userPrompt = parsedStrings["prompt_summarizer_user"]
        assertNotNull(userPrompt)
        assertTrue(userPrompt.contains("%1\$s"))
        assertTrue(userPrompt.contains("%2\$s"))
        assertTrue(userPrompt.contains("%3\$s"))

        val askFilipino = parsedStrings["prompt_ask_filipino"]
        assertNotNull(askFilipino)
        assertTrue(askFilipino.contains("Isulat ang buod sa Filipino."))

        val askEnglish = parsedStrings["prompt_ask_english"]
        assertNotNull(askEnglish)
        assertTrue(askEnglish.contains("Write the recap in English."))

        val askAuto = parsedStrings["prompt_ask_auto"]
        assertNotNull(askAuto)
        assertTrue(askAuto.contains("Write the recap."))
    }

    @Test
    fun summarizerPromptImplementsPromptProvider() {
        val provider: PromptProvider = SummarizerPrompt
        assertTrue(provider.geminiTranscribePrompt.isNotEmpty())
        assertTrue(provider.taglishPrompt.isNotEmpty())
        assertTrue(provider.summarizerSystem(SummaryLanguages.FILIPINO).contains("Filipino (Tagalog)"))
        assertTrue(provider.summarizerUser(SummaryRequest("Test", false, 0, 60_000L)).contains("1:00"))
    }
}
