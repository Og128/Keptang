package com.keptang.ui.categories

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.ui.theme.CategoryColors
import com.keptang.ui.theme.CategoryIcons

@Composable
fun CategoryEditScreen(
    categoryName: String?,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CategoryEditViewModel = viewModel(factory = CategoryEditViewModel.factory(categoryName))
) {
    val existing by viewModel.existing.collectAsStateWithLifecycle()
    val isEditMode = categoryName != null

    var name by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(CategoryColors.PALETTE.first()) }
    var iconKey by remember { mutableStateOf(CategoryIcons.TAG) }

    LaunchedEffect(existing) {
        existing?.let {
            name = it.name
            colorHex = it.colorHex
            iconKey = it.iconKey
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(if (isEditMode) R.string.categories_edit_title else R.string.categories_add_title),
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.categories_name_label)) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Text(
            stringResource(R.string.categories_color_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            CategoryColors.PALETTE.forEach { swatch ->
                ColorSwatch(
                    color = CategoryColors.parse(swatch),
                    selected = swatch == colorHex,
                    onClick = { colorHex = swatch }
                )
            }
        }

        Text(
            stringResource(R.string.categories_icon_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp)
        ) {
            items(CategoryIcons.KEYS) { key ->
                IconSwatch(
                    icon = CategoryIcons.iconFor(key),
                    color = CategoryColors.parse(colorHex),
                    selected = key == iconKey,
                    onClick = { iconKey = key }
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isEditMode) {
                OutlinedButton(onClick = { viewModel.delete(onSaved) }) {
                    Text(stringResource(R.string.action_delete))
                }
            } else {
                Row {}
            }
            Row {
                OutlinedButton(onClick = onCancel, modifier = Modifier.padding(end = 8.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = { viewModel.save(name.trim(), colorHex, iconKey, onSaved) },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun IconSwatch(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) color else color.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}
