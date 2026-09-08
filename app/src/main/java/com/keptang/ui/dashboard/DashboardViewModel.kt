package com.keptang.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.budget.BudgetCalculator
import com.keptang.budget.BudgetSnapshot
import com.keptang.core.Defaults
import com.keptang.dashboard.DashboardCalculator
import com.keptang.dashboard.DashboardFilter
import com.keptang.dashboard.DashboardSnapshot
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.db.RecurringExpenseEntity
import com.keptang.data.repository.BudgetRepository
import com.keptang.data.repository.CategoryRepository
import com.keptang.data.repository.DashboardCard
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.RecurringExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val RECENT_EXPENSES_LIMIT = 5

class DashboardViewModel(
    expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
    recurringExpenseRepository: RecurringExpenseRepository
) : ViewModel() {

    private val filter = MutableStateFlow<DashboardFilter>(DashboardFilter.Today)
    val currentFilter: StateFlow<DashboardFilter> = filter.asStateFlow()

    fun setFilter(newFilter: DashboardFilter) {
        filter.value = newFilter
    }

    /** Shared by [snapshot] and [budgetSnapshot] so "approved expenses" is only queried/collected once, not twice per emission. */
    private val approvedExpenses: StateFlow<List<ExpenseEntity>> = expenseRepository.observeApproved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snapshot: StateFlow<DashboardSnapshot> = combine(
        approvedExpenses,
        settingsRepository.settings,
        filter
    ) { expenses, settings, currentFilter ->
        DashboardCalculator.compute(
            approvedExpenses = expenses,
            defaultCurrencyCode = settings.currencyCode,
            filter = currentFilter,
            today = LocalDate.now(ZoneId.of(settings.timeZoneId))
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardSnapshot(0L, 0, emptyList(), emptyMap(), Defaults.CURRENCY_CODE)
    )

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetSnapshot: StateFlow<BudgetSnapshot> = combine(
        budgetRepository.observeAll(),
        approvedExpenses,
        settingsRepository.settings
    ) { budgets, expenses, settings ->
        BudgetCalculator.compute(
            budgets = budgets,
            approvedExpenses = expenses,
            defaultCurrencyCode = settings.currencyCode,
            today = LocalDate.now(ZoneId.of(settings.timeZoneId))
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetSnapshot(null, null, emptyList(), Defaults.CURRENCY_CODE)
    )

    /**
     * Captures needing attention (failed or needs-review) - the single number shown by both the
     * Dashboard bell and the [ReviewCard] banner below it, so the two no longer disagree the way
     * the bell's old capture-level count and the banner's old expense-level count used to.
     */
    val attentionCount: StateFlow<Int> = ServiceLocator.attentionCount

    val recentExpenses: StateFlow<List<ExpenseEntity>> = expenseRepository.observeRecent(RECENT_EXPENSES_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringById: StateFlow<Map<String, RecurringExpenseEntity>> = recurringExpenseRepository.observeAll()
        .map { recurring -> recurring.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Visible dashboard cards (Spending/Budget/Recent), in display order - the Review banner is always pinned above these and isn't user-customizable. */
    val cardOrder: StateFlow<List<DashboardCard>> = settingsRepository.settings
        .map { it.dashboardCardOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCard.entries)

    fun setCardOrder(cards: List<DashboardCard>) {
        viewModelScope.launch { settingsRepository.setDashboardCardOrder(cards) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    ServiceLocator.expenseRepository,
                    ServiceLocator.settingsRepository,
                    ServiceLocator.categoryRepository,
                    ServiceLocator.budgetRepository,
                    ServiceLocator.recurringExpenseRepository
                )
            }
        }
    }
}
