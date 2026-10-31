package group.two.knockledge.sensor

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import javax.inject.Inject

class AndroidKnockGestureDetector @Inject constructor(app: Application) : KnockGestureDetector {
    private val sensors = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private val tap = TapRT(160_000_000L)
    private var thread: HandlerThread? = null
    private var listener: SensorEventListener? = null
    private var lastDetectionNs = 0L

    var evaluationListener: HeuristicEvaluationListener?
        get() = tap.evaluationListener
        set(value) {
            tap.evaluationListener = value
        }

    override val isListening: Boolean get() = listener != null

    override fun startListening(onKnockDetected: () -> Unit) {
        if (isListening) return
        val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        tap.getLowpassKey().setPara(1f)
        tap.getHighpassKey().setPara(0.3f)
        tap.getPositivePeakDetector().setMinNoiseTolerate(0.15f)
        tap.getPositivePeakDetector().setWindowSize(8)
        tap.getNegativePeakDetection().setMinNoiseTolerate(0.15f)
        tap.getNegativePeakDetection().setWindowSize(8)
        tap.maxTapPeakThreshold = 6.0f
        tap.maxTransverseThreshold = 2.5f
        tap.reset(true)
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val v = event.values
                tap.updateData(event.sensor.type, v[0], v[1], v[2], event.timestamp, 2_500_000L, true)
                if (tap.checkDoubleTapTiming(event.timestamp) == 2
                    && event.timestamp - lastDetectionNs >= 500_000_000L
                ) {
                    lastDetectionNs = event.timestamp
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    onKnockDetected()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val t = HandlerThread("knock-sensor").also { it.start() }
        thread = t
        listener = l
        val handler = Handler(t.looper)
        sensors.registerListener(l, accelerometer, SensorManager.SENSOR_DELAY_FASTEST, handler)
    }

    override fun stopListening() {
        listener?.let { sensors.unregisterListener(it) }
        listener = null
        thread?.quitSafely()
        thread = null
    }
}
