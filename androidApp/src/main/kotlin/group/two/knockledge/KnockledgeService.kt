package group.two.knockledge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Transient UI modes of the session notification, owned by [KnockledgeService]. */
internal enum class NotificationMode {
    /** Regular recording view. */
    NORMAL,

    /** Brief feedback shown after a Rewind saves a clip. */
    SAVED,

    /** Two-step confirmation shown before a notification Stop ends the session. */
    CONFIRM_STOP,
}

/** Immutable state rendered by [KnockledgeService] into the notification's [RemoteViews]. */
internal data class NotificationModel(
    val running: Boolean,
    /** Chronometer base: [SystemClock.elapsedRealtime] when the session started. */
    val startTimeMillis: Long,
    val bufferedSeconds: Long,
    val canRewind: Boolean,
    val summarizing: Boolean,
    val minSaveMillis: Long,
    val millisUntilUnlock: Long,
    /** Clips saved so far in the active session. */
    val savedCount: Int,
    val mode: NotificationMode,
    /** True once the circular window is full and oldest audio is being overwritten. */
    val cacheFull: Boolean,
)

/** Resolved values for one notification layout bind. */
internal data class NotificationViewState(
    val titleRes: Int,
    val statusRes: Int,
    /** Duration argument for [statusRes]; ignored by strings without a placeholder. */
    val statusDurationMillis: Long,
    val savedCount: Int,
    val badgeVisible: Boolean,
    val rewindVisible: Boolean,
    val viewVisible: Boolean,
    val stopVisible: Boolean,
    val confirmVisible: Boolean,
    /** Cache indicator dot drawable, its content description, and visibility. */
    val cacheDotRes: Int,
    val cacheDescRes: Int,
    val cacheDotVisible: Boolean,
)

/**
 * Sticky cache-full latch: the ring buffer prunes a chunk right after the window
 * fills, so [bufferedMillis] drops below [windowMillis] again. Once the window has
 * been full once, stay "replacing" for the rest of the session.
 */
internal fun latchCacheReplaced(previous: Boolean, bufferedMillis: Long, windowMillis: Long): Boolean =
    previous || (windowMillis > 0L && bufferedMillis >= windowMillis)

/**
 * Pure mapping from [NotificationModel] to [NotificationViewState], so the binding
 * decisions are unit-testable on the JVM without a device.
 */
internal object NotificationBinder {
    /** Computes the title, status line, badge count, and button visibility for [model]. */
    fun viewState(model: NotificationModel): NotificationViewState {
        val (statusRes, statusMillis) = when {
            model.mode == NotificationMode.SAVED -> R.string.notification_status_saved to 0L
            model.mode == NotificationMode.CONFIRM_STOP -> R.string.notification_status_stop_discard to 0L
            model.summarizing -> R.string.notification_title_summarizing to 0L
            model.canRewind -> R.string.notification_status_ready to model.bufferedSeconds * 1000L
            else -> R.string.notification_status_locked to model.millisUntilUnlock
        }
        return NotificationViewState(
            titleRes = when {
                model.mode == NotificationMode.SAVED -> R.string.notification_title_saved
                model.mode == NotificationMode.CONFIRM_STOP -> R.string.notification_title_stop_confirm
                model.summarizing -> R.string.notification_title_summarizing
                else -> R.string.notification_title_recording
            },
            statusRes = statusRes,
            statusDurationMillis = statusMillis,
            savedCount = model.savedCount.coerceAtLeast(0),
            badgeVisible = model.mode != NotificationMode.CONFIRM_STOP,
            rewindVisible = model.mode == NotificationMode.NORMAL &&
                model.canRewind && !model.summarizing,
            viewVisible = model.mode == NotificationMode.SAVED,
            stopVisible = model.mode != NotificationMode.CONFIRM_STOP,
            confirmVisible = model.mode == NotificationMode.CONFIRM_STOP,
            cacheDotRes = if (model.cacheFull) {
                R.drawable.bg_notification_cache_replacing
            } else {
                R.drawable.bg_notification_cache_filling
            },
            cacheDescRes = if (model.cacheFull) {
                R.string.notification_cache_replacing
            } else {
                R.string.notification_cache_filling
            },
            cacheDotVisible = model.mode == NotificationMode.NORMAL,
        )
    }
}

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

    /** Transient notification mode; combined into the model so changes re-post the notification. */
    private val mode = MutableStateFlow(NotificationMode.NORMAL)

    /** Reverts [mode] back to [NotificationMode.NORMAL]; replaced on every mode change. */
    private var revertJob: Job? = null

    /** Chronometer base for the session; captured once on the first start intent. */
    private var sessionStartMillis = 0L

    /** Latched once the circular window first fills; resets with the session. */
    private var cacheReplaced = false
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
            ACTION_STOP -> setTransientMode(NotificationMode.CONFIRM_STOP, STOP_CONFIRM_MILLIS)
            ACTION_CONFIRM_STOP -> controller.stop()
            ACTION_CANCEL_STOP -> revertToNormal()
            ACTION_REWIND -> {
                revertJob?.cancel()
                // Only flash "Saved" when a save is actually possible; rewind() is a
                // no-op until the minimum buffer is ready, so don't claim a clip.
                val canSave = controller.state.value.canRewind
                controller.rewind(TriggerType.MANUAL)
                if (canSave) {
                    setTransientMode(NotificationMode.SAVED, SAVED_FEEDBACK_MILLIS)
                } else {
                    mode.value = NotificationMode.NORMAL
                }
            }
            else -> {
                if (sessionStartMillis == 0L) {
                    sessionStartMillis = SystemClock.elapsedRealtime()
                }
                val state = controller.state.value
                val first = build(
                    NotificationModel(
                        running = true,
                        startTimeMillis = sessionStartMillis,
                        bufferedSeconds = 0L,
                        canRewind = false,
                        summarizing = false,
                        minSaveMillis = state.minSaveMillis,
                        millisUntilUnlock = state.millisUntilUnlock,
                        savedCount = 0,
                        mode = mode.value,
                        cacheFull = false,
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
            combine(controller.state, controller.savedClipCount, mode) { state, saved, m ->
                Triple(state, saved, m)
            }
                .map { (it, saved, m) ->
                    cacheReplaced = latchCacheReplaced(
                        cacheReplaced, it.bufferedMillis, it.windowMillis,
                    )
                    NotificationModel(
                        running = it.isRunning,
                        startTimeMillis = sessionStartMillis,
                        bufferedSeconds = it.bufferedMillis / 1000L,
                        canRewind = it.canRewind,
                        summarizing = it.isSummarizing,
                        minSaveMillis = it.minSaveMillis,
                        millisUntilUnlock = it.millisUntilUnlock,
                        savedCount = saved,
                        mode = m,
                        cacheFull = cacheReplaced,
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
        val view = NotificationBinder.viewState(model)
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // The confirm prompt gets its own collapsed layout: the recording row is too
        // cramped for two text buttons inside the ~64dp compact height.
        val compactLayout = if (model.mode == NotificationMode.CONFIRM_STOP) {
            R.layout.notification_confirm_stop_compact
        } else {
            R.layout.notification_recording_compact
        }
        val compact = bindViews(
            RemoteViews(packageName, compactLayout), model, view, openApp,
        )
        val expanded = bindViews(
            RemoteViews(packageName, R.layout.notification_recording_expanded), model, view, openApp,
        )

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_knockledge)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .setContentTitle(getString(view.titleRes))
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    /** Fills [views] with the model's title, chronometer, save count, status, and actions. */
    private fun bindViews(
        views: RemoteViews,
        model: NotificationModel,
        view: NotificationViewState,
        openApp: PendingIntent,
    ): RemoteViews {
        views.setTextViewText(R.id.notification_title, getString(view.titleRes))
        views.setImageViewResource(R.id.notification_cache_dot, view.cacheDotRes)
        views.setCharSequence(
            R.id.notification_cache_dot, "setContentDescription", getString(view.cacheDescRes),
        )
        views.setViewVisibility(
            R.id.notification_cache_dot,
            if (view.cacheDotVisible) View.VISIBLE else View.GONE,
        )
        views.setChronometer(R.id.notification_chronometer, model.startTimeMillis, null, model.running)
        views.setViewVisibility(
            R.id.notification_saved_count,
            if (view.badgeVisible) View.VISIBLE else View.GONE,
        )
        views.setTextViewText(
            R.id.notification_saved_count,
            getString(R.string.notification_save_count_format, view.savedCount),
        )
        val status = when (view.statusRes) {
            R.string.notification_status_ready, R.string.notification_status_locked ->
                getString(view.statusRes, formatDuration(view.statusDurationMillis))
            R.string.notification_status_saved -> getString(view.statusRes, view.savedCount)
            else -> getString(view.statusRes)
        }
        views.setTextViewText(R.id.notification_status, status)
        views.setViewVisibility(
            R.id.btn_notification_rewind,
            if (view.rewindVisible) View.VISIBLE else View.GONE,
        )
        views.setViewVisibility(
            R.id.btn_notification_view,
            if (view.viewVisible) View.VISIBLE else View.GONE,
        )
        views.setViewVisibility(
            R.id.btn_notification_stop,
            if (view.stopVisible) View.VISIBLE else View.GONE,
        )
        views.setViewVisibility(
            R.id.notification_confirm_row,
            if (view.confirmVisible) View.VISIBLE else View.GONE,
        )
        views.setOnClickPendingIntent(R.id.btn_notification_rewind, actionIntent(ACTION_REWIND, 1))
        views.setOnClickPendingIntent(R.id.btn_notification_stop, actionIntent(ACTION_STOP, 2))
        views.setOnClickPendingIntent(R.id.btn_notification_view, openApp)
        views.setOnClickPendingIntent(
            R.id.btn_notification_confirm_stop, actionIntent(ACTION_CONFIRM_STOP, 3),
        )
        views.setOnClickPendingIntent(
            R.id.btn_notification_cancel_stop, actionIntent(ACTION_CANCEL_STOP, 4),
        )
        return views
    }

    /** Shows [newMode] on the notification, then reverts to NORMAL after [revertMillis]. */
    private fun setTransientMode(newMode: NotificationMode, revertMillis: Long) {
        revertJob?.cancel()
        mode.value = newMode
        revertJob = scope.launch {
            delay(revertMillis)
            mode.value = NotificationMode.NORMAL
        }
    }

    /** Cancels any pending revert and returns the notification to NORMAL right away. */
    private fun revertToNormal() {
        revertJob?.cancel()
        mode.value = NotificationMode.NORMAL
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this, requestCode,
        Intent(this, KnockledgeService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Cancels the state collector and drops the projection flag. */
    override fun onDestroy() {
        DeviceAudioConsent.foregroundReady = false
        sessionStartMillis = 0L
        cacheReplaced = false
        mode.value = NotificationMode.NORMAL
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "knockledge_session"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "group.two.knockledge.action.STOP"
        const val ACTION_CONFIRM_STOP = "group.two.knockledge.action.CONFIRM_STOP"
        const val ACTION_CANCEL_STOP = "group.two.knockledge.action.CANCEL_STOP"
        const val ACTION_REWIND = "group.two.knockledge.action.REWIND"
        const val SAVED_FEEDBACK_MILLIS = 1500L
        const val STOP_CONFIRM_MILLIS = 5000L
    }
}
