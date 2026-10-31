package group.two.knockledge.sensor

import group.two.knockledge.sensor.TapRT.Companion.mMaxTimeGapNs
import group.two.knockledge.sensor.TapRT.Companion.mMinTimeGapNs
import java.util.ArrayDeque

/**
 * Detects knocks on the back of the device from accelerometer samples: differentiates, filters and
 * peak-checks the signal, then pairs taps within a timing window.
 *
 * @param sizeWindowNs duration of the rolling analysis window in nanoseconds.
 */
open class TapRT @JvmOverloads constructor(
    val sizeWindowNs: Long
): EventIMURT(), BaseTapRT {

    /** Number of back taps still queued for double-tap timing. */
    fun backTapCount() = _tBackTapTimestamps.size

    /** Face of the device a tap was attributed to; only [Back] is validated by the current heuristic. */
    enum class TapClass {
        Front, Back, Left, Right, Top, Bottom, Others
    }

    /** Timing bounds for pairing taps (nanoseconds) and the default amplitude ceilings of the heuristic. */
    companion object {
        const val mMinTimeGapNs = 100000000L
        const val mMaxTimeGapNs = 500000000L
        const val DEFAULT_MAX_TAP_PEAK_THRESHOLD = 3.5f
        const val DEFAULT_MAX_TRANSVERSE_THRESHOLD = 2.5f
    }

    var evaluationListener: HeuristicEvaluationListener? = null
    var maxTapPeakThreshold: Float = DEFAULT_MAX_TAP_PEAK_THRESHOLD
    var maxTransverseThreshold: Float = DEFAULT_MAX_TRANSVERSE_THRESHOLD

    private val _lowpassKey = Lowpass1C()
    private val _highpassKey = Highpass1C()
    private val _peakDetectorPositive = PeakDetector()
    private val _peakDetectorNegative = PeakDetector()
    protected val _tBackTapTimestamps = ArrayDeque<Long>()
    private var _result = 0

    init {
        _sizeWindowNs = sizeWindowNs
        _lowpassAcc.setPara(1f)
        _highpassAcc.setPara(0.05f)
        _lowpassKey.setPara(0.2f)
        _highpassKey.setPara(0.2f)
    }

    /**
     * Prunes queued taps older than [mMaxTimeGapNs], then tries to pair the newest tap with an older
     * one. Pairs closer than [mMinTimeGapNs] are reported as
     * [RejectionReason.DEBOUNCE_TOO_FAST]; a valid pair is reported, the queue is cleared.
     *
     * @return 2 when a double tap fired, 1 while a single tap is still pending, 0 when nothing is queued.
     */
    override fun checkDoubleTapTiming(timestamp: Long): Int {
        val v0 = _tBackTapTimestamps.iterator()
        while (v0.hasNext()) {
            val v1 = v0.next()
            if (timestamp - v1 <= mMaxTimeGapNs) {
                continue
            }
            v0.remove()
            evaluationListener?.onKnockRejected(RejectionReason.TIMED_OUT)
        }
        if (_tBackTapTimestamps.isEmpty()) {
            return 0
        }

        val v6 = _tBackTapTimestamps.iterator()
        while (v6.hasNext()) {
            val v0_1 = _tBackTapTimestamps.last
            val v7 = v6.next()
            if (v0_1 - v7 <= mMinTimeGapNs) {
                evaluationListener?.onKnockRejected(RejectionReason.DEBOUNCE_TOO_FAST)
                continue
            }

            val intervalMs = (v0_1 - v7) / 1_000_000L
            evaluationListener?.onDoubleKnockTriggered(intervalMs)
            _tBackTapTimestamps.clear()
            return 2
        }

        return 1
    }

    /** High-pass filter applied to the key signal; exposed so calibration can retune its coefficient. */
    fun getHighpassKey(): Highpass1C {
        return _highpassKey
    }

    /** Low-pass filter applied to the key signal; exposed so calibration can retune its coefficient. */
    fun getLowpassKey(): Lowpass1C {
        return _lowpassKey
    }

    /** Peak detector fed the negated key signal, used to time the rebound after an impact. */
    fun getNegativePeakDetection(): PeakDetector {
        return _peakDetectorNegative
    }

    /** Peak detector fed the key signal, used to locate the impact peak. */
    fun getPositivePeakDetector(): PeakDetector {
        return _peakDetectorPositive
    }

    /** Builds the key channel from the resampled slope, feeds both peak detectors, and classifies once the window fills. */
    private fun processKeySignalHeursitic(timestamp: Long) {
        val resamplePoint = _resampleAcc.results.point
        val scaledInterval = 2500000.0f / _resampleAcc.getInterval().toFloat()
        val slopeAcc = _slopeAcc.update(resamplePoint, scaledInterval)
        val lowpassKey = _lowpassKey.update(slopeAcc.z)
        val highpassKey = _highpassKey.update(lowpassKey)
        _peakDetectorPositive.update(highpassKey)
        _peakDetectorNegative.update(-highpassKey)
        _zsAcc.add(highpassKey)
        val transverseJerk = kotlin.math.sqrt(slopeAcc.x * slopeAcc.x + slopeAcc.y * slopeAcc.y)
        _xsAcc.add(transverseJerk)
        val resampleInterval = _resampleAcc.getInterval()
        val scaledResampleInverval = (sizeWindowNs / resampleInterval).toInt()
        while(_zsAcc.size > scaledResampleInverval) {
            _zsAcc.removeFirst()
            if (_xsAcc.isNotEmpty()) {
                _xsAcc.removeFirst()
            }
        }

        if(_zsAcc.size == scaledResampleInverval) {
            recognizeTapHeuristic()
        }

        if(_result == TapClass.Back.ordinal) {
            _tBackTapTimestamps.addLast(timestamp)
        }
    }

    /** Rejects shock or transverse motion above the ceilings, then accepts only an impact at index 4 with a rebound one or two samples later. */
    private fun recognizeTapHeuristic() {
        val positiveIdMajorPeak = _peakDetectorPositive.getIdMajorPeak()
        val negativeIdMajorPeak = _peakDetectorNegative.getIdMajorPeak() - positiveIdMajorPeak
        if (positiveIdMajorPeak == 4) {
            val maxPeakAmplitude = _zsAcc.maxOfOrNull { kotlin.math.abs(it) } ?: 0f
            val maxTransverse = _xsAcc.maxOfOrNull { it } ?: 0f
            evaluationListener?.onPositivePeak(maxPeakAmplitude, positiveIdMajorPeak)
            _fv = ArrayList(_zsAcc)
            if (maxPeakAmplitude > maxTapPeakThreshold) {
                _result = TapClass.Others.ordinal
                evaluationListener?.onKnockRejected(RejectionReason.SHOCK_EXCEEDS_CEILING)
            } else if (maxTransverse > maxTransverseThreshold) {
                _result = TapClass.Others.ordinal
                evaluationListener?.onKnockRejected(RejectionReason.TRANSVERSE_MOTION_EXCESSIVE)
            } else if (maxPeakAmplitude >= 0.5f && negativeIdMajorPeak in 1..2) {
                _result = TapClass.Back.ordinal
                evaluationListener?.onSingleKnockValidated(_zsAcc.size.toLong(), negativeIdMajorPeak)
            } else {
                _result = TapClass.Others.ordinal
                evaluationListener?.onKnockRejected(RejectionReason.REBOUND_OUT_OF_BOUNDS)
            }
        }
    }

    /** Clears window state via the base reset; [justClearFv] empties the sample buffer in place instead of replacing it. */
    override fun reset(justClearFv: Boolean) {
        super.reset()
        if(justClearFv){
            _fv.clear()
        }else{
            _fv = ArrayList()
        }
    }

    /**
     * Feeds one accelerometer sample through the resampler and runs the heuristic per resampled
     * point. Samples whose [type] is not 4 are ignored, and the first accepted sample only seeds the
     * pipeline.
     */
    override fun updateData(type: Int, lastX: Float, lastY: Float, lastZ: Float, lastT: Long, interval: Long, isHeuristic: Boolean) {
        _result = TapClass.Others.ordinal
        if (type != 4) {
            if (0L == _syncTime) {
                _syncTime = lastT
                _resampleAcc.init(lastX, lastY, lastZ, lastT, interval)
                _resampleAcc.setSyncTime(_syncTime)
                _slopeAcc.init(_resampleAcc.results.point)
                _lowpassKey.init(0.0f)
                _highpassKey.init(0.0f)
                return
            }
            while (_resampleAcc.update(lastX, lastY, lastZ, lastT)) {
                processKeySignalHeursitic(lastT)
            }
        }
    }

}
