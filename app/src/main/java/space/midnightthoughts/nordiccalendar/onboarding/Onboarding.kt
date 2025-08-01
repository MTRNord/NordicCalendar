package space.midnightthoughts.nordiccalendar.onboarding

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import space.midnightthoughts.nordiccalendar.R

/**
 * Data class representing a single onboarding page.
 *
 * @property imageRes Optional image resource to display.
 * @property titleRes String resource for the title.
 * @property descriptionRes String resource for the description.
 * @property permissionRequest List of permissions to request on this page.
 * @property showPermissionRequest Whether to show the permission request UI.
 */
data class OnBoardModel(
    val imageRes: Int? = null,
    val titleRes: Int,
    val descriptionRes: Int,
    val permissionRequest: List<String> = emptyList(),
    val showPermissionRequest: Boolean = false
)

/**
 * List of onboarding data models, each representing a page in the onboarding flow.
 */
val onBoardingData = listOf(
    // Explain the purpose of the app
    OnBoardModel(
        imageRes = R.drawable.calendar_illustration,
        titleRes = R.string.onboarding_intro_title,
        descriptionRes = R.string.onboarding_intro_text
    ),
    // Ask for permissions
    OnBoardModel(
        imageRes = R.drawable.permission_illustration,
        titleRes = R.string.onboarding_permissions_title,
        descriptionRes = R.string.onboarding_permissions_text,
        permissionRequest = listOf(
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.WRITE_CALENDAR
        ),
        showPermissionRequest = true
    ),
)

/**
 * Composable function that displays a single onboarding page.
 * Handles permission requests if required by the page.
 *
 * @param page The OnBoardModel representing the current onboarding page.
 * @param hasPermissions MutableState indicating if all required permissions are granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnBoardItem(page: OnBoardModel, hasPermissions: MutableState<Boolean>) {
    val permissionsState = rememberMultiplePermissionsState(
        page.permissionRequest
    ) {
        hasPermissions.value = it.values.all { granted ->
            granted
        }
    }
    Log.d("OnBoardItem", "Permissions state: ${permissionsState.allPermissionsGranted}")
    hasPermissions.value = permissionsState.allPermissionsGranted

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        page.imageRes?.let {
            Image(
                painter = painterResource(id = page.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .height(350.dp)
                    .width(350.dp)
                    .padding(bottom = 20.dp)
            )
        }
        Text(
            text = stringResource(page.titleRes), style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        )
        Text(
            text = stringResource(page.descriptionRes),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        )

        // Show permission request if needed
        if (!permissionsState.allPermissionsGranted && page.showPermissionRequest) {
            Text(
                text = stringResource(R.string.onboarding_permissions_request),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(top = 10.dp)
            )
            Button(onClick = {
                permissionsState.launchMultiplePermissionRequest()
            }) {
                Text(
                    text = stringResource(R.string.onboarding_permissions_button),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }

}