package space.midnightthoughts.nordiccalendar.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    calendars: List<Calendar>,
    selectedCalendars: List<Calendar>,
    selectedDestination: String,
    onCalendarToggle: (Calendar) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onCalendarClick: () -> Unit,
    floatingActionButton: (@Composable () -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(
                calendars = calendars,
                selectedCalendars = selectedCalendars,
                onCalendarToggle = onCalendarToggle,
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onSettingsClick()
                },
                onAboutClick = {
                    scope.launch { drawerState.close() }
                    onAboutClick()
                },
                onCalendarClick = {
                    scope.launch { drawerState.close() }
                    onCalendarClick()
                },
                selectedDestination = selectedDestination
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Zurück"
                                )
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menü")
                            }
                        }
                    }
                )
            },
            floatingActionButton = floatingActionButton ?: { },
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}
