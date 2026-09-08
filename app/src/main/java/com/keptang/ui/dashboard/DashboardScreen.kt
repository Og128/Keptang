package com.keptang.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.budget.BudgetSnapshot
import com.keptang.dashboard.DashboardFilter
import com.keptang.ui.common.formatCurrencyExclusionNotice
import com.keptang.ui.common.formatMoney
import com.keptang.ui.common.formatPeriodRange
import com.keptang.ui.expenses.ExpenseCard
import com.keptang.ui.theme.CategoryColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    onOpenBudgets: () -> Unit = {},
    onOpenReview: () -> Unit = {},
    onEditExpense: (String) -> Unit = {},
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val budgetSnapshot by viewModel.budgetSnapshot.collectAsStateWithLifecycle()
    val reviewCount by viewModel.reviewCount.collectAsStateWithLifecycle()
    val attentionCount by viewModel.attentionCount.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.recentExpenses.collectAsStateWithLifecycle()
    val recurringById by viewModel.recurringById.collectAsStateWithLifecycle()
    val categoryColors = categories.associate { it.name to CategoryColors.parse(it.colorHex) }
    val categoriesByName = categories.associateBy { it.name }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.m_relaxed),
                contentDescription = stringResource(R.string.nav_dashboard),
                modifier = Modifier.weight(1f).height(56.dp),
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.Fit
            )
            if (attentionCount > 0) {
                BadgedBox(badge = { Badge { Text(attentionCount.toString()) } }) {
                    IconButton(onClick = onOpenReview) {
                        Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.dashboard_review_action))
                    }
                }
            }
        }

        if (reviewCount > 0) {
            ReviewCard(reviewCount, onOpenReview)
        }

        DashboardSection(modifier = Modifier.padding(top = 20.dp)) {
            Text(stringResource(R.string.dashboard_spending_label), style = MaterialTheme.typography.titleMedium)

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpendingFilterChip(
                    label = stringResource(R.string.dashboard_filter_today),
                    selected = currentFilter is DashboardFilter.Today,
                    onClick = { viewModel.setFilter(DashboardFilter.Today) }
                )
                SpendingFilterChip(
                    label = stringResource(R.string.dashboard_filter_7d),
                    selected = currentFilter is DashboardFilter.Last7Days,
                    onClick = { viewModel.setFilter(DashboardFilter.Last7Days) }
                )
                SpendingFilterChip(
                    label = stringResource(R.string.dashboard_filter_30d),
                    selected = currentFilter is DashboardFilter.Last30Days,
                    onClick = { viewModel.setFilter(DashboardFilter.Last30Days) }
                )
                SpendingFilterChip(
                    label = stringResource(R.string.dashboard_filter_period),
                    selected = currentFilter is DashboardFilter.Period,
                    onClick = {
                        val today = LocalDate.now()
                        viewModel.setFilter(DashboardFilter.Period(today.minusDays(6), today))
                    }
                )
            }

            val periodFilter = currentFilter as? DashboardFilter.Period
            if (periodFilter != null) {
                PeriodPicker(periodFilter, onChange = viewModel::setFilter)
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dashboard_total_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatMoney(snapshot.totalMinorUnits, snapshot.defaultCurrencyCode),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                VerticalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp).height(36.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.dashboard_transactions_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        snapshot.transactionCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            formatCurrencyExclusionNotice(snapshot.excludedByCurrency)?.let { notice ->
                Text(notice, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }

            if (snapshot.byCategory.isEmpty()) {
                Text(
                    stringResource(R.string.dashboard_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                val spendingSlices = snapshot.byCategory.map { spend ->
                    BudgetSlice(spend.category, categoryColors[spend.category] ?: MaterialTheme.colorScheme.outline, spend.spentMinorUnits)
                }
                Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SpendingDonut(
                        slices = spendingSlices,
                        totalMinorUnits = snapshot.totalMinorUnits,
                        currencyCode = snapshot.defaultCurrencyCode
                    )
                    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                        spendingSlices.forEach { slice ->
                            BudgetLegendRow(slice.label, slice.color, slice.amountMinorUnits, snapshot.defaultCurrencyCode)
                        }
                    }
                }
            }
        }

        DashboardSection(modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.dashboard_budget_label), style = MaterialTheme.typography.titleMedium)
            BudgetSection(budgetSnapshot = budgetSnapshot, categoryColors = categoryColors, onOpenBudgets = onOpenBudgets)
        }

        if (recentExpenses.isNotEmpty()) {
            DashboardSection(
                modifier = Modifier.padding(top = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    stringResource(R.string.dashboard_recent_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Column(Modifier.padding(top = 8.dp)) {
                    recentExpenses.forEach { expense ->
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
}

/**
 * A visually distinct block of related dashboard info. Flat color (no shadow) against a
 * lighter page background is the actual separator - a barely-there 1dp Material shadow on a
 * near-identical surface tone is what read as "messy" before.
 */
@Composable
private fun DashboardSection(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
private fun ReviewCard(reviewCount: Int, onOpenReview: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.error))
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    stringResource(R.string.dashboard_review_count, reviewCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                Button(
                    onClick = onOpenReview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.dashboard_review_action))
                }
            }
        }
    }
}

private data class BudgetSlice(val label: String, val color: Color, val amountMinorUnits: Long)

@Composable
private fun BudgetSection(budgetSnapshot: BudgetSnapshot, categoryColors: Map<String, Color>, onOpenBudgets: () -> Unit) {
    val overall = budgetSnapshot.overall
    if (overall == null && budgetSnapshot.categories.isEmpty()) {
        Column(Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.dashboard_budget_empty), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpenBudgets, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.dashboard_budget_setup_action))
            }
        }
        return
    }

    val outline = MaterialTheme.colorScheme.outline
    val categorySlices = budgetSnapshot.categories
        .map { standing ->
            BudgetSlice(
                label = standing.budget.category.orEmpty(),
                color = categoryColors[standing.budget.category] ?: outline,
                amountMinorUnits = standing.spentMinorUnits
            )
        }
        .sortedByDescending { it.amountMinorUnits }
    val categorizedSpent = categorySlices.sumOf { it.amountMinorUnits }
    val totalSpent = overall?.spentMinorUnits ?: categorizedSpent
    val unassignedSpent = (totalSpent - categorizedSpent).coerceAtLeast(0L)
    val ringTotal = overall?.budget?.amountMinorUnits ?: budgetSnapshot.categories.sumOf { it.budget.amountMinorUnits }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BudgetDonut(
            totalMinorUnits = ringTotal,
            spentMinorUnits = totalSpent,
            slices = categorySlices,
            unassignedMinorUnits = unassignedSpent,
            unassignedColor = outline,
            currencyCode = budgetSnapshot.defaultCurrencyCode
        )

        overall?.let { standing ->
            Text(
                formatPeriodRange(standing.periodStart, standing.periodEndExclusive),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
            categorySlices.forEach { slice ->
                BudgetLegendRow(slice.label, slice.color, slice.amountMinorUnits, budgetSnapshot.defaultCurrencyCode)
            }
            if (unassignedSpent > 0L) {
                BudgetLegendRow(stringResource(R.string.budgets_other_label), outline, unassignedSpent, budgetSnapshot.defaultCurrencyCode)
            }
        }

        TextButton(onClick = onOpenBudgets, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.dashboard_budget_view_all))
        }
    }
}

/** A ring split into colored arcs, drawn from -90deg (top) clockwise, with arbitrary center content. Shared by [BudgetDonut] (partial fill, headroom left as track) and [SpendingDonut] (always fully filled, a plain pie). */
@Composable
private fun SegmentedRing(
    slices: List<BudgetSlice>,
    filledFraction: Float,
    trackColor: Color,
    centerContent: @Composable () -> Unit
) {
    Box(Modifier.padding(top = 8.dp).size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidthPx = 22.dp.toPx()
            val diameter = size.minDimension - strokeWidthPx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)

            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)

            val sliceTotal = slices.sumOf { it.amountMinorUnits }
            if (sliceTotal > 0L && filledFraction > 0f) {
                val filledDegrees = filledFraction.coerceIn(0f, 1f) * 360f
                var startAngle = -90f
                slices.forEach { slice ->
                    val sweep = filledDegrees * (slice.amountMinorUnits.toFloat() / sliceTotal.toFloat())
                    if (sweep > 0f) {
                        drawArc(color = slice.color, startAngle = startAngle, sweepAngle = sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
                        startAngle += sweep
                    }
                }
            }
        }
        centerContent()
    }
}

/** The budget's ring: one arc per budgeted category (its own color), sized by its share of what's spent - plus a neutral arc for spend outside any category budget. Unfilled ring = budget headroom. */
@Composable
private fun BudgetDonut(
    totalMinorUnits: Long,
    spentMinorUnits: Long,
    slices: List<BudgetSlice>,
    unassignedMinorUnits: Long,
    unassignedColor: Color,
    currencyCode: String
) {
    val allSlices = if (unassignedMinorUnits > 0L) {
        slices + BudgetSlice("", unassignedColor, unassignedMinorUnits)
    } else {
        slices
    }
    val filledFraction = if (totalMinorUnits > 0L) (spentMinorUnits.toFloat() / totalMinorUnits.toFloat()).coerceIn(0f, 1f) else 0f

    SegmentedRing(
        slices = allSlices,
        filledFraction = filledFraction,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatMoney(spentMinorUnits, currencyCode), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.dashboard_budget_of_amount, formatMoney(totalMinorUnits, currencyCode)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The spending breakdown's ring: a fully-filled pie sliced by category (no headroom concept - the whole ring is "what was spent"). */
@Composable
private fun SpendingDonut(slices: List<BudgetSlice>, totalMinorUnits: Long, currencyCode: String) {
    SegmentedRing(
        slices = slices,
        filledFraction = 1f,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatMoney(totalMinorUnits, currencyCode), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.dashboard_total_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetLegendRow(label: String, color: Color, amountMinorUnits: Long, currencyCode: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(start = 10.dp)
        )
        Text(formatMoney(amountMinorUnits, currencyCode), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** A compact, single-line filter control - deliberately smaller and less visually loud than a button grid, since these are secondary controls next to the spend total. */
@Composable
private fun SpendingFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
    )
}

private val periodFieldFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

@Composable
private fun PeriodPicker(filter: DashboardFilter.Period, onChange: (DashboardFilter.Period) -> Unit) {
    var editing by remember { mutableStateOf<PeriodEndpoint?>(null) }

    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PeriodDateField(
            label = stringResource(R.string.dashboard_period_start_label),
            date = filter.start,
            onClick = { editing = PeriodEndpoint.START },
            modifier = Modifier.weight(1f)
        )
        PeriodDateField(
            label = stringResource(R.string.dashboard_period_end_label),
            date = filter.endInclusive,
            onClick = { editing = PeriodEndpoint.END },
            modifier = Modifier.weight(1f)
        )
    }

    val endpoint = editing
    if (endpoint != null) {
        PeriodDatePickerDialog(
            initialDate = if (endpoint == PeriodEndpoint.START) filter.start else filter.endInclusive,
            onDismiss = { editing = null },
            onConfirm = { date ->
                onChange(if (endpoint == PeriodEndpoint.START) filter.copy(start = date) else filter.copy(endInclusive = date))
                editing = null
            }
        )
    }
}

private enum class PeriodEndpoint { START, END }

@Composable
private fun PeriodDateField(label: String, date: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date.format(periodFieldFormatter), style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDatePickerDialog(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    ) {
        DatePicker(state = state)
    }
}
