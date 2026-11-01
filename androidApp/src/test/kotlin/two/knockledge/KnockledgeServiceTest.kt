package two.knockledge

import group.two.knockledge.NotificationBinder
import group.two.knockledge.NotificationMode
import group.two.knockledge.NotificationModel
import group.two.knockledge.R
import group.two.knockledge.latchCacheReplaced
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnockledgeServiceTest {

    private fun model(
        canRewind: Boolean = false,
        summarizing: Boolean = false,
        savedCount: Int = 0,
        bufferedSeconds: Long = 0L,
        millisUntilUnlock: Long = 0L,
        mode: NotificationMode = NotificationMode.NORMAL,
        cacheFull: Boolean = false,
    ) = NotificationModel(
        running = true,
        startTimeMillis = 1234L,
        bufferedSeconds = bufferedSeconds,
        canRewind = canRewind,
        summarizing = summarizing,
        minSaveMillis = 10_000L,
        millisUntilUnlock = millisUntilUnlock,
        savedCount = savedCount,
        mode = mode,
        cacheFull = cacheFull,
    )

    @Test
    fun `view state shows recording title while running`() {
        val view = NotificationBinder.viewState(model())
        assertEquals(R.string.notification_title_recording, view.titleRes)
    }

    @Test
    fun `view state shows summarizing title and hides rewind while summarizing`() {
        val view = NotificationBinder.viewState(model(canRewind = true, summarizing = true))
        assertEquals(R.string.notification_title_summarizing, view.titleRes)
        assertFalse(view.rewindVisible)
    }

    @Test
    fun `rewind button visible only when rewind unlocked`() {
        assertFalse(NotificationBinder.viewState(model(canRewind = false)).rewindVisible)
        assertTrue(NotificationBinder.viewState(model(canRewind = true)).rewindVisible)
    }

    @Test
    fun `saved count passes through to badge`() {
        assertEquals(0, NotificationBinder.viewState(model(savedCount = 0)).savedCount)
        assertEquals(3, NotificationBinder.viewState(model(savedCount = 3)).savedCount)
        assertEquals(120, NotificationBinder.viewState(model(savedCount = 120)).savedCount)
    }

    @Test
    fun `status line shows unlock countdown until rewind ready`() {
        val locked = NotificationBinder.viewState(model(millisUntilUnlock = 4_000L))
        assertEquals(R.string.notification_status_locked, locked.statusRes)
        assertEquals(4_000L, locked.statusDurationMillis)

        val ready = NotificationBinder.viewState(model(canRewind = true, bufferedSeconds = 30L))
        assertEquals(R.string.notification_status_ready, ready.statusRes)
        assertEquals(30_000L, ready.statusDurationMillis)

        val summarizing = NotificationBinder.viewState(model(summarizing = true))
        assertEquals(R.string.notification_title_summarizing, summarizing.statusRes)
    }

    @Test
    fun `normal mode keeps icon buttons and hides view and confirm controls`() {
        val view = NotificationBinder.viewState(model(mode = NotificationMode.NORMAL))
        assertTrue(view.stopVisible)
        assertFalse(view.viewVisible)
        assertFalse(view.confirmVisible)
        assertTrue(view.badgeVisible)
    }

    @Test
    fun `cache dot is filling while window not full, replacing once full`() {
        val filling = NotificationBinder.viewState(model(cacheFull = false))
        assertEquals(R.drawable.bg_notification_cache_filling, filling.cacheDotRes)
        assertEquals(R.string.notification_cache_filling, filling.cacheDescRes)
        assertTrue(filling.cacheDotVisible)

        val replacing = NotificationBinder.viewState(model(cacheFull = true))
        assertEquals(R.drawable.bg_notification_cache_replacing, replacing.cacheDotRes)
        assertEquals(R.string.notification_cache_replacing, replacing.cacheDescRes)
        assertTrue(replacing.cacheDotVisible)
    }

    @Test
    fun `cache replaced latch is false before window fills and when window is zero`() {
        assertFalse(latchCacheReplaced(false, bufferedMillis = 100L, windowMillis = 1_000L))
        assertFalse(latchCacheReplaced(false, bufferedMillis = 100L, windowMillis = 0L))
        assertFalse(latchCacheReplaced(false, bufferedMillis = 0L, windowMillis = 0L))
    }

    @Test
    fun `cache replaced latch turns true at first fill and stays latched`() {
        assertTrue(latchCacheReplaced(false, bufferedMillis = 1_000L, windowMillis = 1_000L))
        // After pruning, buffered drops below the window again: the latch holds.
        assertTrue(latchCacheReplaced(true, bufferedMillis = 400L, windowMillis = 1_000L))
    }

    @Test
    fun `cache dot hidden in saved and confirm stop modes`() {
        assertFalse(
            NotificationBinder.viewState(model(mode = NotificationMode.SAVED)).cacheDotVisible,
        )
        assertFalse(
            NotificationBinder.viewState(model(mode = NotificationMode.CONFIRM_STOP)).cacheDotVisible,
        )
    }

    @Test
    fun `saved mode shows saved title status and view button, hides rewind`() {
        val view = NotificationBinder.viewState(
            model(canRewind = true, savedCount = 2, mode = NotificationMode.SAVED),
        )
        assertEquals(R.string.notification_title_saved, view.titleRes)
        assertEquals(R.string.notification_status_saved, view.statusRes)
        assertFalse(view.rewindVisible)
        assertTrue(view.viewVisible)
        assertTrue(view.stopVisible)
        assertFalse(view.confirmVisible)
        assertEquals(2, view.savedCount)
    }

    @Test
    fun `confirm stop mode shows confirm controls and hides rewind view and badge`() {
        val view = NotificationBinder.viewState(
            model(canRewind = true, mode = NotificationMode.CONFIRM_STOP),
        )
        assertEquals(R.string.notification_title_stop_confirm, view.titleRes)
        assertEquals(R.string.notification_status_stop_discard, view.statusRes)
        assertTrue(view.confirmVisible)
        assertFalse(view.rewindVisible)
        assertFalse(view.viewVisible)
        assertFalse(view.stopVisible)
        assertFalse(view.badgeVisible)
    }
}
