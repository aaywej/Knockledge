package group.two.knockledge.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import group.two.knockledge.MainActivity
import group.two.knockledge.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for posting device notifications when AI processing (transcription & summarization) completes.
 */
interface AiNotificationNotifier {
    /**
     * Shows a device notification when AI processing completes.
     *
     * @param title The title of the generated summary or note.
     * @param summaryText Executive summary or details to display in the notification body.
     */
    fun notifyAiComplete(title: String, summaryText: String)
}

/**
 * Default no-op implementation of [AiNotificationNotifier] for test and non-Android environments.
 */
object NoopAiNotificationNotifier : AiNotificationNotifier {
    override fun notifyAiComplete(title: String, summaryText: String) = Unit
}

/**
 * Android system implementation posting notifications via [NotificationManager].
 */
@Singleton
class AndroidAiNotificationNotifier @Inject constructor(
    private val context: Context,
) : AiNotificationNotifier {

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun notifyAiComplete(title: String, summaryText: String) {
        createNotificationChannel()

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val displayTitle = title.ifBlank { context.getString(R.string.notification_ai_complete_title) }
        val displayText = summaryText.ifBlank { context.getString(R.string.notification_ai_complete_fallback) }

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_knockledge)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setStyle(Notification.BigTextStyle().bigText(displayText))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            (NOTIFICATION_ID_OFFSET + (System.currentTimeMillis() % 100000)).toInt(),
            notification,
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_ai_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_ai_desc)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "ai_processing_completion"
        private const val NOTIFICATION_ID_OFFSET = 2000
    }
}
