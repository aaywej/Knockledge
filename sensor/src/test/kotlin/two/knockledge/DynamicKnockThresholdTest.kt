package two.knockledge

import group.two.knockledge.sensor.AndroidKnockGestureDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicKnockThresholdTest {
    @Test
    fun setPeakThresholdUpdatesInternalTapThreshold() {
        // Test that setPeakThreshold modifies the detector's peak amplitude threshold
        val detector = AndroidKnockGestureDetector(null)
        detector.setPeakThreshold(4.0f)
        assertEquals(4.0f, detector.getPeakThreshold(), 0.001f)
        detector.setPeakThreshold(8.0f)
        assertEquals(8.0f, detector.getPeakThreshold(), 0.001f)
    }
}
