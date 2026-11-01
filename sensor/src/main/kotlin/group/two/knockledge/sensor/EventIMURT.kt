package group.two.knockledge.sensor

import java.util.ArrayDeque

/** Shared pipeline state for the tap detectors: filters, resampling and the per-axis rolling windows. */
open class EventIMURT {

    protected var _fv = ArrayList<Float>()
    protected var _highpassAcc = Highpass3C()
    protected var _lowpassAcc = Lowpass3C()
    protected var _resampleAcc = Resample3C()
    protected var _sizeWindowNs = 0L
    protected var _slopeAcc = Slope3C()
    protected var _syncTime = 0L
    protected var _xsAcc = ArrayDeque<Float>()
    protected var _ysAcc = ArrayDeque<Float>()
    protected var _zsAcc = ArrayDeque<Float>()

    /** Clears the rolling windows and the sync time; filter state is left untouched. */
    fun reset() {
        _xsAcc.clear()
        _ysAcc.clear()
        _zsAcc.clear()
        _syncTime = 0L
    }

}
