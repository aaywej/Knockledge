package group.two.knockledge.sensor

/** Tracks the largest amplitude inside a rolling window, using a decaying noise floor to ignore chatter. */
class PeakDetector {

    private var _amplitudeMajorPeak = 0.0f
    private var _amplitudeReference = 0.0f
    private var _idMajorPeak: Int = -1
    private var _minNoiseTolerate = 0.0f
    private var _noiseTolerate = 0f
    private var _windowSize = 0

    /** Floor amplitude treated as noise unconditionally; a fifth of the current major peak raises it further. */
    fun setMinNoiseTolerate(noise: Float) {
        _minNoiseTolerate = noise
    }

    /** Window length used to place a new major peak at its newest index. */
    fun setWindowSize(size: Int) {
        _windowSize = size
    }

    /**
     * Feeds one value: ages the current peak by one sample, raises the noise floor from a fifth of
     * that peak, and records a new major peak when the signal climbs above the floor after falling.
     */
    fun update(value: Float) {
        val adjustedPeak = _idMajorPeak - 1
        _idMajorPeak = adjustedPeak
        if (adjustedPeak < 0) {
            _amplitudeMajorPeak = 0.0f
        }
        _noiseTolerate = _minNoiseTolerate
        val amplitudeMajorPeak = _amplitudeMajorPeak
        val adjustedMajorPeak = amplitudeMajorPeak / 5.0f
        if (adjustedMajorPeak > _minNoiseTolerate) {
            _noiseTolerate = adjustedMajorPeak
        }
        val adjustedReference = _amplitudeReference - value
        val noiseTolerate = _noiseTolerate
        if (adjustedReference >= noiseTolerate) {
            _amplitudeReference = value
        } else if (adjustedReference < 0.0f && value > noiseTolerate) {
            _amplitudeReference = value
            if (value > amplitudeMajorPeak) {
                _idMajorPeak = _windowSize - 1
                _amplitudeMajorPeak = value
                return
            }
        }
    }

    /** Samples back from the newest value at which the current major peak sits, negative once it ages out of the window. */
    fun getIdMajorPeak(): Int {
        return _idMajorPeak
    }

}
