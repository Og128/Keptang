package com.keptang.ui.expenses

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.db.RecurringExpenseEntity
import com.keptang.ui.common.formatDateHeader
import com.keptang.ui.common.formatMoney
import com.keptang.ui.common.localDateOf
import com.keptang.ui.theme.CategoryColors
import com.keptang.ui.theme.CategoryIcons
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ExpensesViewMode { LIST, CALENDAR, RECURRING }

private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

@Composable
fun ExpensesScreen(
    onAddExpense: (LocalDate?) -> Unit,
    onEditExpense: (String) -> Unit,
    onEditRecurring: (String) -> Unit,
    viewModel: ExpensesViewModel = viewModel(factory = ExpensesViewModel.Factory)
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val categoriesByName by viewModel.categoriesByName.collectAsStateWithLifecycle()
    val recurringById by viewModel.recurringById.collectAsStateWithLifecycle()
    val timeZoneId by viewModel.timeZoneId.collectAsStateWithLifecycle()
    val allTagNames by viewModel.allTagNames.collectAsStateWithLifecycle()
    val tagsByExpenseId by viewModel.tagsByExpenseId.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(ExpensesViewMode.LIST) }
    var selectedCalendarDate by remember { mutableStateOf<LocalDate?>(null) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.m_reading),
                contentDescription = stringResource(R.string.nav_expenses),
                modifier = Modifier.height(56.dp),
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.Fit
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(52.dp),
                placeholder = { Text(stringResource(R.string.expenses_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            ViewModePill(viewMode, onSelect = { viewMode = it })
        }

        val searchedExpenses = expenses.filter {
            searchQuery.isBlank() || it.merchant?.contains(searchQuery, ignoreCase = true) == true
        }

        when (viewMode) {
            ExpensesViewMode.LIST -> {
                MonthNavigator(
                    displayedMonth = displayedMonth,
                    onPrevious = { displayedMonth = displayedMonth.minusMonths(1) },
                    onNext = { displayedMonth = displayedMonth.plusMonths(1) },
                    onReset = { displayedMonth = YearMonth.now() }
                )
                CategoryFilterRow(
                    categories = categoriesByName.values.toList(),
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
                if (allTagNames.isNotEmpty()) {
                    TagFilterRow(
                        tags = allTagNames,
                        selected = selectedTag,
                        onSelect = { selectedTag = it }
                    )
                }
                val monthExpenses = searchedExpenses.filter {
                    YearMonth.from(localDateOf(it.occurredAtEpochMillis, it.timeZoneId)) == displayedMonth &&
                        (selectedCategory == null || it.category == selectedCategory) &&
                        (selectedTag == null || selectedTag in tagsByExpenseId[it.id].orEmpty())
                }
                when {
                    searchedExpenses.isEmpty() -> EmptyState(stringResource(R.string.expenses_empty))
                    monthExpenses.isEmpty() -> EmptyState(stringResource(R.string.expenses_empty_period))
                    else -> ExpensesLedger(monthExpenses, categoriesByName, recurringById, timeZoneId, onEditExpense)
                }
            }
            ExpensesViewMode.CALENDAR -> ExpenseCalendarView(
                searchedExpenses,
                categoriesByName,
                selectedCalendarDate,
                onDateSelected = { selectedCalendarDate = it },
                onEditExpense = onEditExpense,
                onAddExpense = onAddExpense
            )
            ExpensesViewMode.RECURRING -> RecurringExpensesSection(onEditRecurring = onEditRecurring)
        }
    }
}

private data class ViewModeOption(val mode: ExpensesViewMode, val icon: androidx.compose.ui.graphics.vector.ImageVector, val contentDescription: String)

/** One rounded pill holding all three view modes as segments - the active segment gets a filled background, the other two stay bare icons in the same container. Reads as one grouped control instead of three separate buttons. */
@Composable
private fun ViewModePill(viewMode: ExpensesViewMode, onSelect: (ExpensesViewMode) -> Unit) {
    val options = listOf(
        ViewModeOption(ExpensesViewMode.LIST, Icons.AutoMirrored.Filled.List, stringResource(R.string.expenses_view_list)),
        ViewModeOption(ExpensesViewMode.CALENDAR, Icons.Filled.CalendarMonth, stringResource(R.string.expenses_view_calendar)),
        ViewModeOption(ExpensesViewMode.RECURRING, Icons.Filled.Repeat, stringResource(R.string.recurring_view_recurring))
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        options.forEach { option ->
            val selected = viewMode == option.mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(option.mode) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    option.icon,
                    contentDescription = option.contentDescription,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MonthNavigator(
    displayedMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable(onClick = onReset)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                displayedMonth.format(monthYearFormatter),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.calendar_previous_month))
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.calendar_next_month))
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<CategoryEntity>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.expenses_filter_all_categories)) }
        )
        categories.forEach { category ->
            FilterChip(
                selected = selected == category.name,
                onClick = { onSelect(if (selected == category.name) null else category.name) },
                label = { Text(category.name) }
            )
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            FilterChip(
                selected = selected == tag,
                onClick = { onSelect(if (selected == tag) null else tag) },
                leadingIcon = { Icon(Icons.Filled.Sell, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(tag) }
            )
        }
    }
}

@Composable
private fun ExpensesLedger(
    expenses: List<ExpenseEntity>,
    categoriesByName: Map<String, CategoryEntity>,
    recurringById: Map<String, RecurringExpenseEntity>,
    timeZoneId: String,
    onEditExpense: (String) -> Unit
) {
    val today = LocalDate.now(ZoneId.of(timeZoneId))
    val todayLabel = stringResource(R.string.calendar_today_action)
    val yesterdayLabel = stringResource(R.string.ledger_date_yesterday)
    val grouped = expenses.groupBy { localDateOf(it.occurredAtEpochMillis, it.timeZoneId) }
        .toSortedMap(compareByDescending { it })

    LazyColumn(Modifier.fillMaxSize()) {
        grouped.forEach { (date, dayExpenses) ->
            item(key = "header_$date") {
                DateHeaderBar(formatDateHeader(date, today, todayLabel, yesterdayLabel))
            }
            for (expense in dayExpenses) {
                item(key = expense.id) {
                    ExpenseCard(
                        expense,
                        categoriesByName[expense.category],
                        recurring = expense.recurringExpenseId?.let { recurringById[it] },
                        onClick = { onEditExpense(expense.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateHeaderBar(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
internal fun CategoryTag(category: CategoryEntity?) {
    val color = category?.let { CategoryColors.parse(it.colorHex) } ?: MaterialTheme.colorScheme.outline
    val label = category?.name ?: stringResource(R.string.manual_add_default_category)
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
internal fun ExpenseCard(expense: ExpenseEntity, category: CategoryEntity?, recurring: RecurringExpenseEntity? = null, onClick: () -> Unit) {
    val color = category?.let { CategoryColors.parse(it.colorHex) } ?: MaterialTheme.colorScheme.outline
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.1f))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(category, color, isRecurring = recurring != null)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    expense.merchant?.takeIf { it.isNotBlank() } ?: stringResource(R.string.expenses_merchant_placeholder),
                    style = MaterialTheme.typography.bodyLarge
                )
                val extra = listOfNotNull(
                    recurring?.let { recurringFrequencyLabel(it) },
                    expense.account,
                    expense.paymentMethod
                ).joinToString(" · ")
                if (extra.isNotBlank()) {
                    Text(
                        extra,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Text(
                "-" + formatMoney(expense.amountMinorUnits, expense.currencyCode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

/** A small colored circle carrying just the category's icon - replaces the name pill in the ledger, where the row's own tint already says which category it is. A tiny repeat glyph badges the corner when the expense came from a recurring definition. */
@Composable
internal fun CategoryIconBadge(category: CategoryEntity?, color: Color, isRecurring: Boolean = false) {
    val icon = category?.let { CategoryIcons.iconFor(it.iconKey) } ?: Icons.Filled.Sell
    val description = category?.name ?: stringResource(R.string.manual_add_default_category)
    Box(modifier = Modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, tint = color, modifier = Modifier.size(20.dp))
        }
        if (isRecurring) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = stringResource(R.string.recurring_view_recurring),
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
