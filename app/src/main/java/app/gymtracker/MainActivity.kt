package app.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.gymtracker.ui.common.AppIcons
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.history.HistoryScreen
import app.gymtracker.ui.history.SessionDetailScreen
import app.gymtracker.ui.library.ExerciseDetailScreen
import app.gymtracker.ui.library.LibraryScreen
import app.gymtracker.ui.plans.DayEditorScreen
import app.gymtracker.ui.plans.PlanEditorScreen
import app.gymtracker.ui.plans.PlansScreen
import app.gymtracker.ui.progress.ProgressScreen
import app.gymtracker.ui.settings.SettingsScreen
import app.gymtracker.ui.theme.Charcoal
import app.gymtracker.ui.theme.GymTheme
import app.gymtracker.ui.today.TodayScreen
import app.gymtracker.ui.workout.WorkoutScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Charcoal.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Charcoal.toArgb()),
        )
        super.onCreate(savedInstanceState)
        val repo = (application as GymApplication).repository
        lifecycleScope.launch { repo.ensureSeeded() }
        setContent {
            GymTheme {
                CompositionLocalProvider(LocalRepo provides repo) { GymApp() }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("today", "Today", AppIcons.Dumbbell),
    Tab("plans", "Plans", Icons.Filled.DateRange),
    Tab("library", "Exercises", Icons.AutoMirrored.Filled.List),
    Tab("history", "History", AppIcons.History),
    Tab("progress", "Progress", AppIcons.TrendingUp),
)

object Routes {
    fun workout(id: Long) = "workout/$id"
    fun plan(id: Long) = "plan/$id"
    fun day(id: Long) = "day/$id"
    fun exercise(id: Long) = "exercise/$id"
    fun session(id: Long) = "session/$id"
    const val SETTINGS = "settings"
}

@Composable
fun GymApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "today",
            modifier = Modifier.padding(padding).let { if (showBar) it else it.navigationBarsPadding() },
        ) {
            composable("today") {
                TodayScreen(
                    openWorkout = { nav.navigate(Routes.workout(it)) },
                    openPlan = { nav.navigate(Routes.plan(it)) },
                    openPlans = { nav.switchTab("plans") },
                    openSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable("plans") {
                PlansScreen(openPlan = { nav.navigate(Routes.plan(it)) }, openDay = { nav.navigate(Routes.day(it)) })
            }
            composable("library") { LibraryScreen(openExercise = { nav.navigate(Routes.exercise(it)) }) }
            composable("history") { HistoryScreen(openSession = { nav.navigate(Routes.session(it)) }) }
            composable("progress") { ProgressScreen(openExercise = { nav.navigate(Routes.exercise(it)) }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable("workout/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                WorkoutScreen(
                    sessionId = it.arguments!!.getLong("id"),
                    onBack = { nav.popBackStack() },
                    onFinished = { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.session(id))
                    },
                    openExercise = { id -> nav.navigate(Routes.exercise(id)) },
                )
            }
            composable("plan/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                PlanEditorScreen(planId = it.arguments!!.getLong("id"), onBack = { nav.popBackStack() }, openDay = { id -> nav.navigate(Routes.day(id)) })
            }
            composable("day/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                DayEditorScreen(
                    dayId = it.arguments!!.getLong("id"),
                    onBack = { nav.popBackStack() },
                    openDay = { id -> nav.popBackStack(); nav.navigate(Routes.day(id)) },
                    openWorkout = { id -> nav.navigate(Routes.workout(id)) },
                )
            }
            composable("exercise/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                ExerciseDetailScreen(exerciseId = it.arguments!!.getLong("id"), onBack = { nav.popBackStack() }, openSession = { id -> nav.navigate(Routes.session(id)) })
            }
            composable("session/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                SessionDetailScreen(
                    sessionId = it.arguments!!.getLong("id"),
                    onBack = { nav.popBackStack() },
                    openWorkout = { id -> nav.popBackStack(); nav.navigate(Routes.workout(id)) },
                    openExercise = { id -> nav.navigate(Routes.exercise(id)) },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
