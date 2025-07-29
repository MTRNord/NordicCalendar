package space.midnightthoughts.nordiccalendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import sh.calvin.autolinktext.rememberAutoLinkText
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.components.AppScaffold
import space.midnightthoughts.nordiccalendar.getCurrentAppLocale
import space.midnightthoughts.nordiccalendar.viewmodels.EventDetailsViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

@Composable
fun EventDetailsView(
    backStackEntry: NavBackStackEntry,
    navController: NavHostController,
) {
    val viewModel: EventDetailsViewModel = hiltViewModel()
    val event = remember(viewModel) {
        viewModel.event
    }.collectAsState(initial = null)
    val startDate = remember(event.value?.startTime) {
        Date(event.value?.startTime ?: 0L)
    }
    val endDate = remember(event.value?.endTime) {
        Date(event.value?.endTime ?: 0L)
    }
    val startCalendar = remember(startDate) {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar
    }
    val endCalendar = remember(endDate) {
        val calendar = Calendar.getInstance()
        calendar.time = endDate
        calendar
    }

    val appLocale = getCurrentAppLocale(
        LocalContext.current
    )
    val dateFormat = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", appLocale)
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm", appLocale)

    val scrollState = rememberScrollState()

    AppScaffold(
        title = event.value?.title
            ?: stringResource(
                R.string.event_details,
                backStackEntry.arguments?.getString("eventId") ?: ""
            ),
        selectedDestination = "eventDetails",
        navController = navController,
    ) { innerPadding ->
        SelectionContainer(modifier = innerPadding) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .fillMaxSize(),
            ) {
                if (event.value != null) {
                    Text(
                        // Check if the event is starting and ending on the same day or not
                        text = if (startCalendar.get(Calendar.DAY_OF_YEAR) == endCalendar.get(
                                Calendar.DAY_OF_YEAR
                            )
                            && startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR)
                        ) {
                            startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                                .format(dateFormat)
                        } else {
                            stringResource(
                                R.string.event_date_range,
                                startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                                    .format(dateFormat),
                                endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                                    .format(dateFormat)
                            )
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(
                            R.string.event_time_range,
                            startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
                                .format(timeFormat),
                            endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
                                .format(timeFormat)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(48.dp))
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (event.value?.calendar?.color != null) {
                                // Round color dot
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(16.dp)
                                        .background(
                                            color = if (event.value?.calendar?.color != null) {
                                                Color(event.value?.calendar?.color ?: 0)
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = event.value?.calendar?.name ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.organizer),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        Text(
                            text = if (event.value?.organizer?.trim().isNullOrEmpty()) {
                                stringResource(R.string.no_organizer_available)
                            } else {
                                event.value?.organizer ?: ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // TODO: map if location is a valid address
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.location),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        Text(
                            text = if (event.value?.location?.trim().isNullOrEmpty()) {
                                stringResource(R.string.no_location_available)
                            } else {
                                event.value?.location ?: ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    // TODO: Invitees and attendees and alerts

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.description),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = AnnotatedString.rememberAutoLinkText(
                                if (event.value?.description?.trim().isNullOrEmpty()) {
                                    stringResource(R.string.no_description_available)
                                } else {
                                    event.value?.description ?: ""
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        )
                    }


                } else {
                    Text(
                        stringResource(
                            R.string.event_details,
                            backStackEntry.arguments?.getString("eventId") ?: ""
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.event_not_found),
                    )
                }
            }
        }
    }
}