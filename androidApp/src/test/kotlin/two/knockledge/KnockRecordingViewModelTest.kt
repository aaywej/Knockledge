package group.two.knockledge

import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import group.two.knockledge.ui.KnockRecordingViewModel
import group.two.knockledge.ui.RecordingBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KnockRecordingViewModelTest {
    private class FakeBackend(var canSave: Boolean = true) : RecordingBackend {
        val flow = MutableStateFlow(KnockledgeState())
        override val state: StateFlow<KnockledgeState> = flow
        var saves = 0
        var starts = 0
        var stops = 0
        override fun start() { starts++; flow.value = flow.value.copy(isRunning = true) }
        override fun stop() { stops++; flow.value = flow.value.copy(isRunning = false) }
        override fun save(): Boolean {
            if (!canSave) return false
            saves++
            flow.value = flow.value.copy(pendingRewinds = 1)
            return true
        }
    }

    private fun vm(backend: FakeBackend, settings: AppSettings = AppSettings(MemoryStore(), "m")) =
        KnockRecordingViewModel(backend, settings, CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startAndStopReflectInState() {
        val b = FakeBackend()
        val vm = vm(b)
        assertFalse(vm.uiState.value.isRecording)
        vm.startSession()
        assertTrue(vm.uiState.value.isRecording)
        assertTrue(vm.uiState.value.isGestureActive)
        vm.stopSession()
        assertFalse(vm.uiState.value.isRecording)
        assertEquals(1, b.starts)
        assertEquals(1, b.stops)
    }

    @Test
    fun manualSaveInvokesBackendAndShowsSaving() {
        val b = FakeBackend()
        val vm = vm(b)
        assertNull(vm.uiState.value.lastSaveSuccess)
        vm.triggerManualSave()
        assertEquals(1, b.saves)
        assertTrue(vm.uiState.value.isSavingSnapshot)
        assertEquals(true, vm.uiState.value.lastSaveSuccess)
        b.flow.value = b.flow.value.copy(pendingRewinds = 0)
        assertFalse(vm.uiState.value.isSavingSnapshot)
    }

    @Test
    fun rejectedManualSaveReportsFailure() {
        val vm = vm(FakeBackend(canSave = false))
        vm.triggerManualSave()
        assertEquals(false, vm.uiState.value.lastSaveSuccess)
        assertFalse(vm.uiState.value.isSavingSnapshot)
    }

    @Test
    fun bufferDurationIsClampedAndExposed() {
        val settings = AppSettings(MemoryStore(), "m")
        val vm = vm(FakeBackend(), settings)
        assertEquals(300, vm.uiState.value.maxBufferSeconds)
        vm.setBufferDurationSeconds(120)
        assertEquals(120, vm.uiState.value.maxBufferSeconds)
        vm.setBufferDurationSeconds(9999)
        assertEquals(300, vm.uiState.value.maxBufferSeconds)
        vm.setBufferDurationSeconds(1)
        assertEquals(60, vm.uiState.value.maxBufferSeconds)
    }

    @Test
    fun bufferedSecondsFollowRecorder() {
        val b = FakeBackend()
        val vm = vm(b)
        b.flow.value = b.flow.value.copy(bufferedMillis = 12_900L)
        assertEquals(12, vm.uiState.value.bufferedSeconds)
    }
}
