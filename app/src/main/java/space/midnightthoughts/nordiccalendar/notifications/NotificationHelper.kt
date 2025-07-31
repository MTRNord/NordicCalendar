package space.midnightthoughts.nordiccalendar.notifications

import android.app.Notification.CATEGORY_EVENT
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import space.midnightthoughts.nordiccalendar.MainActivity
import space.midnightthoughts.nordiccalendar.R

object NotificationHelper {
    private const val CHANNEL_ID = "calendar_event_reminders"
    private const val CHANNEL_NAME = "Kalender Erinnerungen"
    private const val CHANNEL_DESC = "Benachrichtigungen für Kalender-Events mit Erinnerung"

    fun showEventNotification(
        context: Context,
        eventId: Long,
        eventTitle: String,
        eventTime: Long,
        eventEndTime: Long,
        eventLocation: String
    ) {
        Log.d(
            "NotificationHelper",
            "showEventNotification: eventId=$eventId, eventTitle=$eventTitle, eventTime=$eventTime"
        )
        createNotificationChannel(context)
        // Prüfe Berechtigung für Benachrichtigungen (ab Android 13)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(
                    "NotificationHelper",
                    "POST_NOTIFICATIONS permission not granted, notification not shown"
                )
                // Keine Berechtigung, Notification nicht anzeigen
                return
            }
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val startCal = java.util.Calendar.getInstance().apply { timeInMillis = eventTime }
        val endCal = java.util.Calendar.getInstance().apply { timeInMillis = eventEndTime }
        val sameDay =
            startCal.get(java.util.Calendar.YEAR) == endCal.get(java.util.Calendar.YEAR) &&
                    startCal.get(java.util.Calendar.DAY_OF_YEAR) == endCal.get(java.util.Calendar.DAY_OF_YEAR)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
        val dateFormat = android.text.format.DateFormat.getDateFormat(context)
        val timeText = if (sameDay) {
            "${timeFormat.format(startCal.time)}-${timeFormat.format(endCal.time)}"
        } else {
            "${dateFormat.format(startCal.time)} ${timeFormat.format(startCal.time)} - ${
                dateFormat.format(
                    endCal.time
                )
            } ${timeFormat.format(endCal.time)}"
        }
        val contentText = "Beginn: $timeText"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_calendar_clock_24)
            .setContentTitle(eventTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setCategory(CATEGORY_EVENT)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.teal_700))
        // Action für Link oder Adresse
        val location = eventLocation
        if (android.util.Patterns.WEB_URL.matcher(location).matches()) {
            val linkIntent = Intent(Intent.ACTION_VIEW, location.toUri())
            val linkPendingIntent = PendingIntent.getActivity(
                context,
                (eventId + 1000).toInt(),
                linkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Öffnen", linkPendingIntent)
        } else if (location.isNotBlank()) {
            val mapsIntent = Intent(
                Intent.ACTION_VIEW,
                ("geo:0,0?q=" + java.net.URLEncoder.encode(
                    location,
                    "UTF-8"
                )).toUri()
            )
            val mapsPendingIntent = PendingIntent.getActivity(
                context,
                (eventId + 2000).toInt(),
                mapsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Route anzeigen", mapsPendingIntent)
        }

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(eventId.toInt(), builder.build())
                Log.d("NotificationHelper", "Notification sent for eventId=$eventId")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "SecurityException: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        Log.d("NotificationHelper", "Creating notification channel: $CHANNEL_ID")
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
