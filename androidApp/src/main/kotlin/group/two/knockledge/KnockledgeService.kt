package group.two.knockledge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import group.two.knockledge.audio.DeviceAudioConsent
import group.two.knockledge.core.formatDuration
import group.two.knockledge.data.TriggerType
import group.two.knockledge.sensor.KnockGestureDetector
import group.two.knockledge.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Microphone foreground service. Its ongoing notification is the Android version of
 * the "Knockledge in progress" lock screen widget: status, unlock progress, Rewind and Stop.
 *
 * Note: since Android 14 users can swipe away foreground service notifications.
 * Recording continues; the notification comes back on the next update.
 */
class KnockledgeService : Service() {

    @Inject lateinit var controller: KnockledgeController
    @Inject lateinit var knock: KnockGestureDetector
    @Inject lateinit var settings: AppSettings

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private val notifications by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    /** Not bound: the notification is the only interface. */
    override fun onBind(intent: Intent?): IBinder? = null

    /** Creates the low-importance, lock-screen visible notification channel. */
    override fun onCreate() {
        super.onCreate()
        (application as KnockledgeApplication).component.inject(this)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Knockledge session",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows while Knockledge is listening."
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notifications.createNotificationChannel(channel)
    }

    /**
     * Runs the Stop or Rewind notification action, or else enters the foreground with the mic
     * (plus media projection if phone audio was granted) and starts publishing state.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> controller.stop()
            ACTION_REWIND -> controller.rewind(TriggerType.MANUAL)
            else -> {
                val state = controller.state.value
                val first = build(
                    NotificationModel(
                        true, 0L,
                        canRewind = false,
                        summarizing = false,
                        minSaveMillis = state.minSaveMillis,
                        millisUntilUnlock = state.millisUntilUnlock,
                    )
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // The mediaProjection type is only allowed after the user said yes to the
                    // screen-capture prompt for this session (phone audio on).
                    val withProjection = DeviceAudioConsent.grantedThisSession
                    val types = if (withProjection) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    }
                    runCatching { startForeground(NOTIFICATION_ID, first, types) }
                        .onFailure { startForeground(NOTIFICATION_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) }
                        .onSuccess { if (withProjection) DeviceAudioConsent.foregroundReady = true }
                } else {
                    startForeground(NOTIFICATION_ID, first)
                }
                observe()
            }
        }
        // If the process dies, cached chunks are wiped on the next start. Do not restart.
        return START_NOT_STICKY
    }

    private fun observe() {
        if (observeJob != null) return
        scope.launch {
            settings.knockSensorThreshold.collect { knock.setPeakThreshold(it) }
        }
        observeJob = scope.launch {
            controller.state
                .map {
                    NotificationModel(
                        running = it.isRunning,
                        bufferedSeconds = it.bufferedMillis / 1000L,
                        canRewind = it.canRewind,
                        summarizing = it.isSummarizing,
                        minSaveMillis = it.minSaveMillis,
                        millisUntilUnlock = it.millisUntilUnlock,
                    )
                }
                .distinctUntilChanged()
                .collect { model ->
                    if (!model.running) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        notifications.notify(NOTIFICATION_ID, build(model))
                    }
                }
        }
    }

    private fun build(model: NotificationModel): Notification {
        val bufferedMillis = model.bufferedSeconds * 1000L
        val text = when {
            model.summarizing -> "Summarizing..."
            model.canRewind -> "${formatDuration(bufferedMillis)} in memory. Rewind is ready."
            else -> "Rewind unlocks in ${formatDuration(model.millisUntilUnlock)}"
        }
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val icon = Icon.createWithResource(this, R.drawable.ic_stat_knockledge)

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_knockledge)
            .setContentTitle("Knockledge in progress")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setColor(0xFFD7372B.toInt())

        if (!model.canRewind && !model.summarizing) {
            builder.setProgress((model.minSaveMillis / 1000L).toInt(), model.bufferedSeconds.toInt(), false)
        }
        if (model.canRewind) {
            builder.addAction(Notification.Action.Builder(icon, "Rewind", actionIntent(ACTION_REWIND, 1)).build())
        }
        builder.addAction(Notification.Action.Builder(icon, "Stop", actionIntent(ACTION_STOP, 2)).build())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this, requestCode,
        Intent(this, KnockledgeService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Cancels the state collector and drops the projection flag. */
    override fun onDestroy() {
        DeviceAudioConsent.foregroundReady = false
        scope.cancel()
        super.onDestroy()
    }

    private data class NotificationModel(
        val running: Boolean,
        val bufferedSeconds: Long,
        val canRewind: Boolean,
        val summarizing: Boolean,
        val minSaveMillis: Long,
        val millisUntilUnlock: Long,
    )

    private companion object {
        const val CHANNEL_ID = "knockledge_session"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "group.two.knockledge.action.STOP"
        const val ACTION_REWIND = "group.two.knockledge.action.REWIND"
    }
}
