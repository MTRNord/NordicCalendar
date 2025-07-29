package space.midnightthoughts.nordiccalendar.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.textFieldPreference
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.components.AppScaffold
import space.midnightthoughts.nordiccalendar.viewmodels.SettingsViewModel

@Composable
fun SettingsView(navController: NavHostController) {
    val viewModel: SettingsViewModel = hiltViewModel()

    AppScaffold(
        title = stringResource(R.string.settings),
        selectedDestination = "settings",
        navController = navController,
    ) { innerPadding ->
        ProvidePreferenceLocals {
            LazyColumn(
                modifier = innerPadding
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                textFieldPreference(
                    key = "nominatim_url",
                    defaultValue = "https://nominatim.openstreetmap.org/search",
                    title = { Text(text = stringResource(R.string.nominatim_url_label)) },
                    textToValue = { it },
                    summary = { Text(text = stringResource(R.string.nominatim_url_hint)) },
                )
            }
        }
    }
}