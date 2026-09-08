package com.keptang.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A free-form label a user can attach to any number of expenses (see [ExpenseTagCrossRef]),
 * independent of [CategoryEntity] - a tag crosses categories (e.g. "Japan trip" spans Dining,
 * Transport, Housing) rather than replacing them.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String
)
