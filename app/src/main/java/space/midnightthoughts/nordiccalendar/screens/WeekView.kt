package space.midnightthoughts.nordiccalendar.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import space.midnightthoughts.nordiccalendar.util.Event
import space.midnightthoughts.nordiccalendar.viewmodels.WeekViewModel

/**
 * WeekView displays the calendar in a weekly format, showing events for the current week.
 *
 * @param modifier Modifier for styling and layout.
 * @param navController NavController for navigation actions.
 * @param weekViewModel Specialized ViewModel for week view operations.
 * @param events List of events to display.
 */
@Composable
fun WeekView(
    modifier: Modifier = Modifier,
    navController: NavController,
    weekViewModel: WeekViewModel,
    events: List<Event> = emptyList()
) {
    // TODO: Implement WeekView
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events) { event ->
            Text(
                event.title, modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        navController.navigate("eventDetails/${event.eventId}?tab=1") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
        }
    }

}