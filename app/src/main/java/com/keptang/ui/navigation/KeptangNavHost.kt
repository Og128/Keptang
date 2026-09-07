package com.keptang.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate
import com.keptang.R
import com.keptang.ui.budgets.BudgetFormScreen
import com.keptang.ui.budgets.BudgetsScreen
import com.keptang.ui.capturedetail.CaptureDetailScreen
import com.keptang.ui.categories.CategoriesScreen
import com.keptang.ui.categories.CategoryEditScreen
import com.keptang.ui.dashboard.DashboardScreen
import com.keptang.ui.expenses.ExpensesScreen
import com.keptang.ui.expenses.ManualExpenseScreen
import com.keptang.ui.inbox.InboxScreen
import com.keptang.ui.settings.SettingsScreen

object Routes {
    const val INBOX = "inbox?tab={tab}"
    const val EXPENSES = "expenses"
    const val BUDGETS = "budgets"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val CAPTURE_DETAIL = "capture/{captureId}"
    const val ADD_EXPENSE = "add_expense?date={date}"
    const val EXPENSE_EDIT = "expense_edit/{expenseId}"
    const val BUDGET_ADD = "budget_add"
    const val BUDGET_EDIT = "budget_edit/{budgetId}"
    const val CATEGORIES = "categories"
    const val CATEGORY_ADD = "category_add"
    const val CATEGORY_EDIT = "category_edit/{categoryName}"

    fun inbox(showReview: Boolean = false) = if (showReview) "inbox?tab=review" else "inbox"
    fun captureDetail(captureId: String) = "capture/$captureId"
    fun addExpense(date: LocalDate?) = if (date != null) "add_expense?date=$date" else "add_expense"
    fun expenseEdit(expenseId: String) = "expense_edit/$expenseId"
    fun budgetEdit(budgetId: String) = "budget_edit/$budgetId"
    fun categoryEdit(categoryName: String) = "category_edit/${Uri.encode(categoryName)}"
}

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS = listOf(
    BottomTab(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.BarChart),
    BottomTab(Routes.EXPENSES, R.string.nav_expenses, Icons.Filled.List),
    BottomTab(Routes.BUDGETS, R.string.nav_budgets, Icons.Filled.AccountBalanceWallet),
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
                                    popUpTo(Routes.DASHBOARD)
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
            startDestination = Routes.DASHBOARD,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(
                Routes.INBOX,
                arguments = listOf(navArgument("tab") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { backStack ->
                InboxScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCapture = { id -> navController.navigate(Routes.captureDetail(id)) },
                    startOnReview = backStack.arguments?.getString("tab") == "review"
                )
            }
            composable(Routes.EXPENSES) {
                ExpensesScreen(
                    onAddExpense = { date -> navController.navigate(Routes.addExpense(date)) },
                    onEditExpense = { id -> navController.navigate(Routes.expenseEdit(id)) }
                )
            }
            composable(Routes.BUDGETS) {
                BudgetsScreen(
                    onAddBudget = { navController.navigate(Routes.BUDGET_ADD) },
                    onEditBudget = { id -> navController.navigate(Routes.budgetEdit(id)) }
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenBudgets = {
                        navController.navigate(Routes.BUDGETS) { launchSingleTop = true; popUpTo(Routes.DASHBOARD) }
                    },
                    onOpenReview = { navController.navigate(Routes.inbox(showReview = true)) },
                    onEditExpense = { id -> navController.navigate(Routes.expenseEdit(id)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenInbox = { navController.navigate(Routes.INBOX) },
                    onEditCategories = { navController.navigate(Routes.CATEGORIES) }
                )
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onAddCategory = { navController.navigate(Routes.CATEGORY_ADD) },
                    onEditCategory = { name -> navController.navigate(Routes.categoryEdit(name)) }
                )
            }
            composable(Routes.CATEGORY_ADD) {
                CategoryEditScreen(
                    categoryName = null,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.CATEGORY_EDIT) { backStack ->
                val encodedName = backStack.arguments?.getString("categoryName") ?: return@composable
                CategoryEditScreen(
                    categoryName = Uri.decode(encodedName),
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.CAPTURE_DETAIL) { backStack ->
                val captureId = backStack.arguments?.getString("captureId") ?: return@composable
                CaptureDetailScreen(captureId = captureId, onDeleted = { navController.popBackStack() })
            }
            composable(
                Routes.ADD_EXPENSE,
                arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { backStack ->
                val dateArg = backStack.arguments?.getString("date")
                ManualExpenseScreen(
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    initialDate = dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                )
            }
            composable(Routes.EXPENSE_EDIT) { backStack ->
                val expenseId = backStack.arguments?.getString("expenseId") ?: return@composable
                ManualExpenseScreen(
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    expenseId = expenseId
                )
            }
            composable(Routes.BUDGET_ADD) {
                BudgetFormScreen(
                    budgetId = null,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.BUDGET_EDIT) { backStack ->
                val budgetId = backStack.arguments?.getString("budgetId") ?: return@composable
                BudgetFormScreen(
                    budgetId = budgetId,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }

    LaunchedEffect(startCaptureId) {
        if (startCaptureId != null) {
            navController.navigate(Routes.captureDetail(startCaptureId))
        }
    }
}
