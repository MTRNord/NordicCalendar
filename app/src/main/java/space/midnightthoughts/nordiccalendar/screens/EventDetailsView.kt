package space.midnightthoughts.nordiccalendar.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import sh.calvin.autolinktext.rememberAutoLinkText
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
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return EventDetailsViewModel(
                        app = context,
                        savedStateHandle = SavedStateHandle(mapOf("eventId" to eventId))
                    ) as T
                }
            }
        )
    } else {
        throw IllegalStateException("ViewModelStoreOwner or eventId is null")
    }
    val event = remember(viewModel) {
        viewModel.event
    }.collectAsState(initial = null)
    val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

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
                    .padding(24.dp)
                    .fillMaxSize(),
            ) {
                if (event.value != null) {
                    Text(
                        dateTimeFormat.format(
                            java.util.Date(
                                event.value?.startTime ?: 0L
                            )
                        ) + " - " + dateTimeFormat.format(
                            java.util.Date(event.value?.endTime ?: 0L)
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!event.value?.location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = AnnotatedString.rememberAutoLinkText(
                                event.value?.location ?: ""
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (!event.value?.organizer.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            event.value?.organizer ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    if (!event.value?.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = AnnotatedString.rememberAutoLinkText(
                                event.value?.description ?: ""
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
                    Text("Event nicht gefunden oder wird geladen...")
                }
            }
        }
    }
}