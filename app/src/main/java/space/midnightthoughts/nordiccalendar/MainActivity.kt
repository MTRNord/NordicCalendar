package space.midnightthoughts.nordiccalendar

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.components.AppScaffold
import space.midnightthoughts.nordiccalendar.onboarding.OnBoardItem
import space.midnightthoughts.nordiccalendar.onboarding.onBoardingData
import space.midnightthoughts.nordiccalendar.screens.CalendarScreen
import space.midnightthoughts.nordiccalendar.screens.EventDetailsView
import space.midnightthoughts.nordiccalendar.screens.SettingsView
import space.midnightthoughts.nordiccalendar.ui.theme.NordicCalendarTheme
import space.midnightthoughts.nordiccalendar.util.OnboardingPrefs

/**
 * Destinations for the app's navigation.
 */
sealed class Destinations(val route: String) {
    object Intro : Destinations("intro")

    /**
     * Calendar destination with optional parameters for tab and date.
     * @param tab The index of the selected tab (default is 0).
     * @param date The date to display in the calendar (optional).
     */
    object Calendar : Destinations("calendar?tab={tab}&date={date}")
    object Settings : Destinations("settings")
    object About : Destinations("about")
    object EventDetails : Destinations("eventDetails/{eventId}")
}

/**
 * MainActivity is the entry point of the Nordic Calendar app.
 * It sets up the navigation and UI components, including onboarding and calendar views.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * onCreate is called when the activity is created.
     * It initializes the UI and sets up navigation.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0-exp"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.1.0-exp"
        }
        //OnboardingPrefs.resetOnboarding(this) // Reset onboarding for testing purposes
        val showOnboarding = OnboardingPrefs.isOnboardingNeeded(this, currentVersion)

        val navigateToEventId = intent.getLongExtra("navigateToEventId", -1)

        setContent {
            NordicCalendarTheme {
                val navController = rememberNavController()

                val permissionsState = rememberMultiplePermissionsState(
                    listOf(
                        android.Manifest.permission.READ_CALENDAR,
                        android.Manifest.permission.WRITE_CALENDAR
                    ),
                )

                if (!permissionsState.allPermissionsGranted && !showOnboarding) {
                    LaunchedEffect(Unit) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                } else {
                    // Initialize calendar services when permissions are granted
                    LaunchedEffect(permissionsState.allPermissionsGranted) {
                        if (!showOnboarding) {
                            (application as NordicCalendarApp).initializeCalendarServices()
                        }
                    }

                    LaunchedEffect(navigateToEventId) {
                        if (navigateToEventId > 0) {
                            navController.navigate("eventDetails/$navigateToEventId")
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = if (showOnboarding) Destinations.Intro.route else Destinations.Calendar.route,
                        // See https://developer.android.com/develop/ui/compose/system/predictive-back-setup for more info
                        popExitTransition = {
                            scaleOut(
                                targetScale = 0.9f,
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = 0.5f,
                                    pivotFractionY = 0.5f
                                )
                            )
                        },
                        popEnterTransition = {
                            EnterTransition.None
                        },
                    ) {
                        composable(Destinations.Intro.route) {
                            IntroScreen(navController) { // Callback nach Abschluss
                                OnboardingPrefs.setOnboardingDone(this@MainActivity, currentVersion)
                                // Initialize calendar services after onboarding completion
                                (application as NordicCalendarApp).initializeCalendarServices()
                                navController.navigate(Destinations.Calendar.route) {
                                    popUpTo(Destinations.Intro.route) {
                                        inclusive = true
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        composable(
                            route = Destinations.Calendar.route,
                            arguments = listOf(
                                navArgument("tab") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument("date") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            CalendarView(
                                navController = navController
                            )
                        }
                        composable(
                            route = "eventDetails/{eventId}?tab={tab}",
                            arguments = listOf(
                                navArgument("eventId") { type = NavType.LongType },
                                navArgument("tab") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            ),
                            deepLinks = listOf(
                                navDeepLink {
                                    uriPattern = "nordiccalendar://eventdetails/{eventId}"
                                }
                            )
                        ) { backStackEntry ->
                            EventDetailsView(
                                backStackEntry = backStackEntry,
                                navController = navController
                            )
                        }
                        composable(Destinations.Settings.route) { backStackEntry ->
                            SettingsView(
                                navController = navController
                            )
                        }
                        composable(Destinations.About.route) { backStackEntry ->
                            AboutView(
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }

}

/**
 * IntroScreen displays the onboarding screens for the app.
 * It allows users to navigate through the onboarding items and finish the onboarding process.
 *
 * @param navController The NavHostController for navigation actions.
 * @param onFinish Optional callback when the user finishes the onboarding.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun IntroScreen(navController: NavHostController, onFinish: (() -> Unit)? = null) {
    val navigateToHome: () -> Unit = {
        onFinish?.invoke() ?: navController.navigate(Destinations.Calendar.route) {
            popUpTo(Destinations.Calendar.route) {
                inclusive = true
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { onBoardingData.size })
    val coroutineScope = rememberCoroutineScope()

    // Keep state if the user has allowed permmissions to hide the finish button using mutable state
    val hasPermissions = remember { mutableStateOf(false) }

    NordicCalendarTheme {
        Scaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    OnBoardItem(onBoardingData[page], hasPermissions)
                }


                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)

                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        repeat(onBoardingData.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .width(if (isSelected) 18.dp else 8.dp)
                                    .height(if (isSelected) 8.dp else 8.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF707784),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .background(
                                        color = if (isSelected) Color(0xFF3B6C64) else Color(
                                            0xFFFFFFFF
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }


                    if (pagerState.currentPage < pagerState.pageCount - 1 || (pagerState.currentPage == pagerState.pageCount - 1 && hasPermissions.value)) Text(
                        text = if (pagerState.currentPage == pagerState.pageCount - 1) stringResource(
                            R.string.onboarding_finish
                        ) else stringResource(R.string.onboarding_next),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        modifier = Modifier.clickable {
                            Log.d(
                                "Pager",
                                "Current Page: ${pagerState.currentPage}, Page Count: ${pagerState.pageCount}"
                            )
                            if (pagerState.currentPage == pagerState.pageCount - 1) {
                                navigateToHome()
                            }
                            if (pagerState.currentPage < pagerState.pageCount - 1) {
                                val nextPage = pagerState.currentPage + 1
                                coroutineScope.launch { pagerState.animateScrollToPage(nextPage) }
                            }

                        }
                    )

                }
            }
        }
    }
}

/**
 * CalendarView displays the main calendar screen with a floating action button to add events.
 * It uses the CalendarScreen composable to render the calendar UI.
 *
 * @param navController The NavHostController for navigation actions.
 */
@Composable
fun CalendarView(
    navController: NavHostController
) {

    AppScaffold(
        title = stringResource(R.string.app_name),
        selectedDestination = "calendar",
        navController = navController,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Event hinzufügen (später) */ },
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.event_add),
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.event_add),
                    )
                },
            )
        }
    ) { innerPadding ->
        CalendarScreen(
            modifier = innerPadding,
            navController = navController,
        )
    }
}

/**
 * AboutView displays information about the app and its version.
 * It provides a simple text view with the app name and version.
 *
 * @param navController The NavHostController for navigation actions.
 */
@Composable
fun AboutView(navController: NavHostController) {

    AppScaffold(
        title = stringResource(R.string.about) + " Nordic Calendar",
        selectedDestination = "about",
        navController = navController,
    ) { innerPadding ->
        Column(
            modifier = innerPadding
                .fillMaxSize()
        ) {
            Text(
                stringResource(R.string.about) + " Nordic Calendar",
                modifier = Modifier.padding(16.dp)
            )
            // Weitere Infos hier
        }
    }
}