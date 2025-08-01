package space.midnightthoughts.nordiccalendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra("eventId", -1)
        val eventTitle = intent.getStringExtra("eventTitle") ?: "Kalenderereignis"
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
