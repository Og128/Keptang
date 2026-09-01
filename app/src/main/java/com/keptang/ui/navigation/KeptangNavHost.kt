package com.keptang.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keptang.R
import com.keptang.ui.capturedetail.CaptureDetailScreen
import com.keptang.ui.expenses.ExpensesScreen
import com.keptang.ui.inbox.InboxScreen
import com.keptang.ui.review.ReviewScreen
import com.keptang.ui.settings.SettingsScreen

object Routes {
    const val INBOX = "inbox"
    const val EXPENSES = "expenses"
    const val REVIEW = "review"
    const val SETTINGS = "settings"
    const val CAPTURE_DETAIL = "capture/{captureId}"

    fun captureDetail(captureId: String) = "capture/$captureId"
}

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS = listOf(
    BottomTab(Routes.INBOX, R.string.nav_inbox, Icons.Filled.Inbox),
    BottomTab(Routes.EXPENSES, R.string.nav_expenses, Icons.Filled.List),
    BottomTab(Routes.REVIEW, R.string.nav_review, Icons.Filled.RateReview),
    BottomTab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)

@Composable
fun KeptangNavHost(navController: NavHostController = rememberNavController(), startCaptureId: String? = null) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == null || BOTTOM_TABS.any { it.route == currentRoute }) {
                NavigationBar {
                    BOTTOM_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    launchSingleTop = true
                                    popUpTo(Routes.INBOX)
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.INBOX,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Routes.INBOX) {
                InboxScreen(onOpenCapture = { id -> navController.navigate(Routes.captureDetail(id)) })
            }
            composable(Routes.EXPENSES) { ExpensesScreen() }
            composable(Routes.REVIEW) { ReviewScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.CAPTURE_DETAIL) { backStack ->
                val captureId = backStack.arguments?.getString("captureId") ?: return@composable
                CaptureDetailScreen(captureId = captureId, onDeleted = { navController.popBackStack() })
            }
        }
    }

    LaunchedEffect(startCaptureId) {
        if (startCaptureId != null) {
            navController.navigate(Routes.captureDetail(startCaptureId))
        }
    }
}
