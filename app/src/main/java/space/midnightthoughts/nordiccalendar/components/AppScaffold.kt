package space.midnightthoughts.nordiccalendar.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffoldContent(
    title: String?,
    isBackButtonVisible: Boolean,
    navController: NavController,
    onBackClick: (() -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
    onMenuClick: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (title == null) {
                        Text(stringResource(R.string.app_name))
                    } else {
                        Text(title)
                    }
                },
                navigationIcon = {
                    if (isBackButtonVisible) {
                        IconButton(
                            onClick = {
                                onBackClick?.invoke() ?: navController.popBackStack()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    } else if (onMenuClick != null) {
                        IconButton(
                            onClick = { onMenuClick() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.menu)
                            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String? = null,
    selectedDestination: String,
    navController: NavController,
    floatingActionButton: (@Composable () -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDrawerDestination = selectedDestination in listOf("calendar", "settings", "about")
    val isBackButtonVisible by remember {
        derivedStateOf {
            navController.previousBackStackEntry != null && !isDrawerDestination
        }
    }

    if (isDrawerDestination) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                SidebarDrawer(
                    selectedDestination = selectedDestination,
                    navController = navController,
                    drawerState = drawerState,
                )
            }
        ) {
            AppScaffoldContent(
                title = title,
                isBackButtonVisible = isBackButtonVisible,
                navController = navController,
                onBackClick = {
                    scope.launch { drawerState.close() }
                    onBackClick?.invoke() ?: navController.popBackStack()
                },
                floatingActionButton = floatingActionButton,
                content = content,
                onMenuClick = { scope.launch { drawerState.open() } }
            )
        }
    } else {
        AppScaffoldContent(
            title = title,
            isBackButtonVisible = true,
            navController = navController,
            onBackClick = onBackClick,
            floatingActionButton = floatingActionButton,
            content = content
        )
    }
}
