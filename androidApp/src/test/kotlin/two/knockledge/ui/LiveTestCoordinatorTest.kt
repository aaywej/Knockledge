package two.knockledge.ui

import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.ui.LiveTestCoordinator
import group.two.knockledge.ui.LiveTestTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTestCoordinatorTest {

    @Test
    fun testInitialState() {
        val coordinator = LiveTestCoordinator()
        val state = coordinator.uiState.value

        assertEquals(LiveTestTab.RECORD, state.activeTab)
        assertNull(state.selectedNote)
        assertNull(state.lastKnockEpochMs)
        assertEquals(0, state.knockCount)
        assertFalse(state.hasRecordAudioPermission)
    }

    @Test
    fun testSelectTab() {
        val coordinator = LiveTestCoordinator()
        coordinator.selectTab(LiveTestTab.NOTES)
        assertEquals(LiveTestTab.NOTES, coordinator.uiState.value.activeTab)

        coordinator.selectTab(LiveTestTab.SETTINGS)
        assertEquals(LiveTestTab.SETTINGS, coordinator.uiState.value.activeTab)
    }

    @Test
    fun testSelectNote() {
        val coordinator = LiveTestCoordinator()
        val testNote = Note(
            id = "note_123",
            userId = "user_456",
            title = "Test Note",
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L,
            audio = AudioMetadata(
                localPath = "/tmp/audio.wav",
                durationMs = 30000L,
                sampleRate = 16000,
                triggerType = TriggerType.MANUAL,
            )
        )

        coordinator.selectNote(testNote)
        assertEquals(testNote, coordinator.uiState.value.selectedNote)

        coordinator.selectNote(null)
        assertNull(coordinator.uiState.value.selectedNote)
    }

    @Test
    fun testKnockTriggerUpdatesTimeAndCount() {
        var mockTime = 100000L
        val coordinator = LiveTestCoordinator(clock = { mockTime })

        coordinator.onKnockTriggered()
        var state = coordinator.uiState.value
        assertEquals(100000L, state.lastKnockEpochMs)
        assertEquals(1, state.knockCount)

        mockTime = 105000L
        coordinator.onKnockTriggered()
        state = coordinator.uiState.value
        assertEquals(105000L, state.lastKnockEpochMs)
        assertEquals(2, state.knockCount)
    }

    @Test
    fun testPermissionResult() {
        val coordinator = LiveTestCoordinator()
        coordinator.onPermissionResult(true)
        assertTrue(coordinator.uiState.value.hasRecordAudioPermission)

        coordinator.onPermissionResult(false)
        assertFalse(coordinator.uiState.value.hasRecordAudioPermission)
    }
}
