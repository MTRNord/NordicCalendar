package space.midnightthoughts.nordiccalendar.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.midnightthoughts.nordiccalendar.util.Calendar

@Composable
fun SidebarDrawer(
    calendars: List<Calendar>,
    selectedCalendars: List<Calendar>,
    onCalendarToggle: (Calendar) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onCalendarClick: () -> Unit,
    selectedDestination: String
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            NavigationDrawerItem(
                label = { Text("Kalender") },
                selected = selectedDestination == "calendar",
                onClick = onCalendarClick,
                modifier = Modifier.fillMaxWidth(),
                colors = NavigationDrawerItemDefaults.colors()
            )
            NavigationDrawerItem(
                label = { Text("Einstellungen") },
                selected = selectedDestination == "settings",
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                colors = NavigationDrawerItemDefaults.colors()
            )
            NavigationDrawerItem(
                label = { Text("Über") },
                selected = selectedDestination == "about",
                onClick = onAboutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = NavigationDrawerItemDefaults.colors()
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(Modifier.height(8.dp))
            Text("Kalenderauswahl", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            calendars.forEach { calendar ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedCalendars.contains(calendar),
                        onCheckedChange = { onCalendarToggle(calendar) }
                    )
                    Text(calendar.name, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
