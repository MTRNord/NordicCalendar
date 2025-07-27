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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

data class OnBoardModel(
    val imageRes: Int? = null,
    val title: String,
    val description: String,
    val permissionRequest: List<String> = emptyList(),
    val showPermissionRequest: Boolean = false
)

val onBoardingData = listOf(
    // Explain the purpose of the app
    OnBoardModel(
        title = "Welcome to Nordic Calendar",
        description = "A simple open source calendar app for Android that helps you keep track of Nordic holidays and events."
    ),
    // Ask for permissions
    OnBoardModel(
        title = "Permissions Required",
        description = "To provide you with the best experience, we need access to your calendar permissions. " +
                "This allows us to add Nordic holidays and events directly to your calendar.",
        permissionRequest = listOf(
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.WRITE_CALENDAR
        ),
        showPermissionRequest = true
    ),
)

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
            text = page.title, style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        )
        Text(
            text = page.description,
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
                text = "Please grant the required permissions to continue.",
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
                    text = "Request Permissions",
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