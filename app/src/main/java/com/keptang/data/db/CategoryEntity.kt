package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-editable expense category: a display name plus a color (hex, from the fixed
 * CVD-safe palette in [com.keptang.ui.theme.CategoryColors]) and an icon (a key into
 * [com.keptang.ui.theme.CategoryIcons]). [ExpenseEntity.category]/[BudgetEntity.category]
 * reference this by [name] rather than a foreign key, matching how budgets already reference
 * categories loosely.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int
)
