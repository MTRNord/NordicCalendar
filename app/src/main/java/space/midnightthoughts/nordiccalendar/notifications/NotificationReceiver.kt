package space.midnightthoughts.nordiccalendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import space.midnightthoughts.nordiccalendar.R

/**
 * NotificationReceiver is a BroadcastReceiver that receives alarm broadcasts for calendar event reminders
 * and triggers the display of a notification using NotificationHelper.
 */
class NotificationReceiver : BroadcastReceiver() {
    /**
     * Called when the BroadcastReceiver receives an Intent broadcast.
     * Extracts event details from the intent and shows a notification.
     *
     * @param context The application context.
     * @param intent The received Intent containing event details.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra("eventId", -1)
        val eventTitle = intent.getStringExtra("eventTitle")
            ?: context.getString(R.string.notification_default_title)
        val eventDescription = intent.getStringExtra("eventDescription")
        val eventTime = intent.getLongExtra("eventTime", 0L)
        val eventEndTime = intent.getLongExtra("eventEndTime", 0L)
        val eventLocation = intent.getStringExtra("eventLocation")
        Log.d(
            "NotificationReceiver",
            "onReceive: eventId=$eventId, eventTitle=$eventTitle, eventTime=$eventTime, eventDescription=$eventDescription, eventEndTime=$eventEndTime, eventLocation=$eventLocation"
        )
        NotificationHelper.showEventNotification(
            context,
            eventId,
            eventTitle,
            eventDescription,
            eventTime,
            eventEndTime,
            eventLocation ?: ""
        )
    }
}
