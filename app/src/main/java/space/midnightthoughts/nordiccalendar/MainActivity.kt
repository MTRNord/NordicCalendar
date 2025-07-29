package space.midnightthoughts.nordiccalendar

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.components.AppScaffold
import space.midnightthoughts.nordiccalendar.onboarding.OnBoardItem
import space.midnightthoughts.nordiccalendar.onboarding.onBoardingData
import space.midnightthoughts.nordiccalendar.screens.CalendarScreen
import space.midnightthoughts.nordiccalendar.screens.EventDetailsView
import space.midnightthoughts.nordiccalendar.ui.theme.NordicCalendarTheme
import space.midnightthoughts.nordiccalendar.util.OnboardingPrefs
import space.midnightthoughts.nordiccalendar.viewmodels.CalendarViewModel

sealed class Destinations(val route: String) {
    object Intro : Destinations("intro")
    object Calendar : Destinations("calendar?tab={tab}") // Route mit optionalem Tab-Argument
    object Settings : Destinations("settings")
    object About : Destinations("about")
    object EventDetails : Destinations("eventDetails/{eventId}")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0-exp"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.1.0-exp"
        }
        val showOnboarding = OnboardingPrefs.isOnboardingNeeded(this, currentVersion)

        setContent {
            NordicCalendarTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = if (showOnboarding) Destinations.Intro.route else Destinations.Calendar.route
                ) {
                    composable(Destinations.Intro.route) {
                        IntroScreen(navController) { // Callback nach Abschluss
                            OnboardingPrefs.setOnboardingDone(this@MainActivity, currentVersion)
                            navController.navigate(Destinations.Calendar.route) {
                                popUpTo(Destinations.Intro.route) { inclusive = true }
                            }
                        }
                    }
                    composable(
                        route = Destinations.Calendar.route,
                        arguments = listOf(
                            navArgument("tab") {
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        )
                    ) { backStackEntry ->
                        CalendarView(
                            backStackEntry = backStackEntry,
                            navController = navController
                        )
                    }
                    composable(Destinations.EventDetails.route) { backStackEntry ->
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

// Pass Navigation Actions: Create a function to handle navigation and pass it to screens.
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun IntroScreen(navController: NavHostController, onFinish: (() -> Unit)? = null) {
    val navigateToHome: () -> Unit = {
        onFinish?.invoke() ?: navController.navigate(Destinations.Calendar.route)
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
                        text = if (pagerState.currentPage == pagerState.pageCount - 1) "Finish" else "Next",
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

@Composable
fun CalendarView(
    backStackEntry: NavBackStackEntry,
    navController: NavHostController
) {
    val calendarViewModel: CalendarViewModel = hiltViewModel()

    val selectedTab = remember(calendarViewModel) {
        calendarViewModel.selectedTab
    }.collectAsState(initial = 0)
    val tabArg = backStackEntry.arguments?.getInt("tab")

    LaunchedEffect(tabArg) {
        if (tabArg != null) {
            calendarViewModel.setTab(tabArg)
        }
    }

    AppScaffold(
        title = stringResource(R.string.app_name),
        selectedDestination = "calendar",
        navController = navController,
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Event hinzufügen (später) */ }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.event_add)
                )
            }
        }
    ) { innerPadding ->
        CalendarScreen(
            selectedTab = selectedTab.value,
            onTabSelected = { calendarViewModel.setTab(it) },
            modifier = innerPadding,
            navController = navController,
        )
    }
}


@Composable
fun SettingsView(navController: NavHostController) {
    AppScaffold(
        title = stringResource(R.string.settings),
        selectedDestination = "settings",
        navController = navController,

        ) { innerPadding ->
        Column(
            modifier = innerPadding
                .fillMaxSize()
        ) {
            Text(
                stringResource(R.string.settings),
                modifier = Modifier.padding(16.dp)
            )
            // Weitere Einstellungen hier
        }
    }
}

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