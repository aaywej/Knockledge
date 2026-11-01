package two.knockledge

import group.two.knockledge.pipeline.CompositeSummarizer
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CompositeSummarizerTest {
    private class MockSummarizer(val label: String) : Summarizer {
        override suspend fun summarize(request: SummaryRequest): Summary {
            return Summary(title = label, gist = "gist", points = listOf("p1"), redactedCount = 0)
        }
    }

    @Test
    fun routesComprehensionAccordingToMode() = runBlocking {
        val online = MockSummarizer("cloud")
        val offline = MockSummarizer("device")
        var mode = "auto"
        var isOnline = true

        val composite = CompositeSummarizer(
            online = online,
            offline = offline,
            isOnline = { isOnline },
            mode = { mode }
        )

        val req = SummaryRequest("sample text", false, 0, 5000L)

        // Local mode -> device
        mode = "local"
        assertEquals("device", composite.summarize(req).title)

        // Online mode -> cloud
        mode = "online"
        assertEquals("cloud", composite.summarize(req).title)

        // Auto mode while offline -> device
        mode = "auto"
        isOnline = false
        assertEquals("device", composite.summarize(req).title)

        // Auto mode while online -> cloud
        isOnline = true
        assertEquals("cloud", composite.summarize(req).title)
    }
}
