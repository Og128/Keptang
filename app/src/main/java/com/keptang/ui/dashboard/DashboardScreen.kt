package com.keptang.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.dashboard.CategorySpend
import com.keptang.dashboard.DashboardFilter
import com.keptang.ui.common.formatCurrencyExclusionNotice
import com.keptang.ui.common.formatMoney
import com.keptang.ui.theme.CategoryColors
import java.time.LocalDate

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryColors = categories.associate { it.name to CategoryColors.parse(it.colorHex) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.nav_dashboard), style = MaterialTheme.typography.headlineSmall)

        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 16.dp)) {
            FilterButton(
                label = stringResource(R.string.dashboard_filter_today),
                selected = currentFilter is DashboardFilter.Today,
                onClick = { viewModel.setFilter(DashboardFilter.Today) }
            )
            FilterButton(
                label = stringResource(R.string.dashboard_filter_7d),
                selected = currentFilter is DashboardFilter.Last7Days,
                onClick = { viewModel.setFilter(DashboardFilter.Last7Days) }
            )
            FilterButton(
                label = stringResource(R.string.dashboard_filter_30d),
                selected = currentFilter is DashboardFilter.Last30Days,
                onClick = { viewModel.setFilter(DashboardFilter.Last30Days) }
            )
            FilterButton(
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

        Text(
            stringResource(R.string.dashboard_total_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            formatMoney(snapshot.totalMinorUnits, snapshot.defaultCurrencyCode),
            style = MaterialTheme.typography.headlineMedium
        )
        formatCurrencyExclusionNotice(snapshot.excludedByCurrency)?.let { notice ->
            Text(notice, style = MaterialTheme.typography.bodySmall)
        }

        if (snapshot.byCategory.isEmpty()) {
            Text(
                stringResource(R.string.dashboard_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            DonutChart(
                slices = snapshot.byCategory.map { spend ->
                    (categoryColors[spend.category] ?: MaterialTheme.colorScheme.outline) to
                        spend.spentMinorUnits.toFloat() / snapshot.totalMinorUnits.toFloat()
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 16.dp, bottom = 16.dp)
            )
            snapshot.byCategory.forEach { spend ->
                LegendRow(spend, categoryColors[spend.category] ?: MaterialTheme.colorScheme.outline, snapshot.totalMinorUnits, snapshot.defaultCurrencyCode)
            }
        }
    }
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = {}, enabled = false, modifier = Modifier.padding(end = 8.dp)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.padding(end = 8.dp)) { Text(label) }
    }
}

@Composable
private fun PeriodPicker(filter: DashboardFilter.Period, onChange: (DashboardFilter.Period) -> Unit) {
    var startText by remember(filter.start) { mutableStateOf(filter.start.toString()) }
    var endText by remember(filter.endInclusive) { mutableStateOf(filter.endInclusive.toString()) }

    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        OutlinedTextField(
            value = startText,
            onValueChange = { text ->
                startText = text
                runCatching { LocalDate.parse(text) }.getOrNull()?.let { onChange(filter.copy(start = it)) }
            },
            label = { Text(stringResource(R.string.dashboard_period_start_label)) },
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        OutlinedTextField(
            value = endText,
            onValueChange = { text ->
                endText = text
                runCatching { LocalDate.parse(text) }.getOrNull()?.let { onChange(filter.copy(endInclusive = it)) }
            },
            label = { Text(stringResource(R.string.dashboard_period_end_label)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DonutChart(slices: List<Pair<Color, Float>>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var startAngle = -90f
        slices.forEach { (color, fraction) ->
            val sweep = (fraction * 360f).coerceIn(0f, 360f)
            // A 2-degree surface gap between slices, per the dataviz skill's spacer rule.
            val gap = if (slices.size > 1) 2f else 0f
            drawArc(
                color = color,
                startAngle = startAngle + gap / 2,
                sweepAngle = (sweep - gap).coerceAtLeast(0f),
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun LegendRow(spend: CategorySpend, color: Color, totalMinorUnits: Long, currencyCode: String) {
    val percent = if (totalMinorUnits > 0) (spend.spentMinorUnits * 100f / totalMinorUnits) else 0f
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Text(spend.category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            "${formatMoney(spend.spentMinorUnits, currencyCode)} · ${"%.0f".format(percent)}%",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
