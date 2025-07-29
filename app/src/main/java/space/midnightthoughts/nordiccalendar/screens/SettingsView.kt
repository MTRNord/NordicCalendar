package space.midnightthoughts.nordiccalendar.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.components.AppScaffold
import space.midnightthoughts.nordiccalendar.viewmodels.SettingsViewModel

@Composable
fun SettingsView(navController: NavHostController) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val nominatimUrl = remember(viewModel) {
        viewModel.nominatimUrl
    }.collectAsState()
    val textFieldValue: MutableState<String> = remember { mutableStateOf(nominatimUrl.value) }

    val isValidUrl = remember(textFieldValue) {
        Patterns.WEB_URL.matcher(textFieldValue.value).matches()
    }
    AppScaffold(
        title = stringResource(R.string.settings),
        selectedDestination = "settings",
        navController = navController,

        ) { innerPadding ->
        Column(
            modifier = innerPadding
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textFieldValue.value,
                onValueChange = { value: String ->
                    textFieldValue.value = value
                },
                label = { Text(stringResource(R.string.nominatim_url_label)) },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.nominatim_url_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                isError = !isValidUrl && textFieldValue.value.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.setNominatimUrl(textFieldValue.value) },
                enabled = textFieldValue.value.isNotBlank() && textFieldValue != nominatimUrl && isValidUrl,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}