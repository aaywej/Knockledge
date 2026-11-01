package group.two.knockledge.audio

import group.two.knockledge.data.model.ChapterMarker
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChapterNavigationTest {

    private val chapters = listOf(
        ChapterMarker(startTimeMs = 0L, title = "Intro", keyBullets = listOf("Start")),
        ChapterMarker(startTimeMs = 30000L, title = "Topic A", keyBullets = listOf("Main topic")),
        ChapterMarker(startTimeMs = 90000L, title = "Conclusion", keyBullets = listOf("Summary"))
    )

    private fun computeActiveChapter(
        positionMs: Long,
        chapterMarkers: List<ChapterMarker>
    ): ChapterMarker? {
        if (chapterMarkers.isEmpty()) return null
        return chapterMarkers
            .filter { it.startTimeMs <= positionMs }
            .maxByOrNull { it.startTimeMs }
    }

    @Test
    fun activeChapterIsCalculatedCorrectlyByPosition() {
        // At 0ms, active chapter is Intro
        assertEquals("Intro", computeActiveChapter(0L, chapters)?.title)

        // At 15000ms (15s), still Intro
        assertEquals("Intro", computeActiveChapter(15000L, chapters)?.title)

        // At 30000ms (30s), active chapter becomes Topic A
        assertEquals("Topic A", computeActiveChapter(30000L, chapters)?.title)

        // At 60000ms (60s), still Topic A
        assertEquals("Topic A", computeActiveChapter(60000L, chapters)?.title)

        // At 90000ms (90s), active chapter becomes Conclusion
        assertEquals("Conclusion", computeActiveChapter(90000L, chapters)?.title)

        // Beyond 90000ms, still Conclusion
        assertEquals("Conclusion", computeActiveChapter(120000L, chapters)?.title)
    }

    @Test
    fun activeChapterReturnsNullWhenNoChapters() {
        assertNull(computeActiveChapter(1000L, emptyList()))
    }

    @Test
    fun activeChapterReturnsNullWhenPositionBeforeAllChapters() {
        val lateChapters = listOf(
            ChapterMarker(startTimeMs = 10000L, title = "Late Start", keyBullets = listOf("Late"))
        )
        assertNull(computeActiveChapter(5000L, lateChapters))
        assertEquals("Late Start", computeActiveChapter(10000L, lateChapters)?.title)
    }
}
