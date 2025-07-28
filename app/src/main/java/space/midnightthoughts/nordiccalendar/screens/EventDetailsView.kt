package space.midnightthoughts.nordiccalendar.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.components.AppScaffold
import space.midnightthoughts.nordiccalendar.viewmodels.EventDetailsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EventDetailsView(
    backStackEntry: NavBackStackEntry,
    navController: NavHostController,
) {
    val owner = LocalViewModelStoreOwner.current
    val context =
        androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val eventId = backStackEntry.arguments?.getString("eventId")
    val viewModel: EventDetailsViewModel = if (owner != null && eventId != null) {
        viewModel(
            viewModelStoreOwner = owner,
            key = "eventDetails_$eventId",
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return EventDetailsViewModel(
                        app = context,
                        savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("eventId" to eventId))
                    ) as T
                }
            }
        )
    } else {
        throw IllegalStateException("ViewModelStoreOwner or eventId is null")
    }
    val event = viewModel.event.collectAsState().value
    val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    AppScaffold(
        title = stringResource(R.string.event_details_title),
        selectedDestination = "eventDetails",
        navController = navController,
    ) { innerPadding ->
        Column(
            modifier = innerPadding
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (event != null) {
                Text(event.title)
                Spacer(modifier = Modifier.height(8.dp))
                if (!event.description.isNullOrBlank()) {
                    Text(event.description)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    "Start: " + dateTimeFormat.format(java.util.Date(event.startTime))
                )
                Text(
                    "Ende: " + dateTimeFormat.format(java.util.Date(event.endTime))
                )
                if (!event.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ort: ${event.location}")
                }
                if (!event.organizer.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Organisator: ${event.organizer}")
                }
            } else {
                Text(
                    stringResource(
                        R.string.event_details,
                        backStackEntry.arguments?.getString("eventId") ?: ""
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Event nicht gefunden oder wird geladen...")
            }
        }
    }
}