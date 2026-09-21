package com.keptang.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.keptang.ui.theme.MascotOutlineWidth

/**
 * The app's standard container, drawn the way the mascots are: a flat fill inside a single ink
 * outline, one corner radius, and no shadow anywhere.
 *
 * The outline is doing the work elevation used to. Both animals are line art with a heavy black
 * contour, so a container that separates itself with a tonal step or a soft shadow reads as
 * belonging to a different drawing than the mascot sitting next to it.
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val border = BorderStroke(MascotOutlineWidth, MaterialTheme.colorScheme.outline)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
    }
}
