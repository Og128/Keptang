package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One category the user taught the parser by correcting an expense by hand: "massage" means
 * Entertainment *here*, whatever it might mean to someone else.
 *
 * The keyword is the primary key and is stored already normalised (lowercase, trimmed), so
 * re-teaching the same word simply overwrites the previous answer - the latest correction is
 * always the one that counts. Not a foreign key to `categories`: a rename there would silently
 * strand these rows, and the parser treats categories as free text anyway
 * (see ADR-0001).
 */
@Entity(tableName = "learned_categories")
data class LearnedCategoryEntity(
    @PrimaryKey @ColumnInfo(name = "keyword") val keyword: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long
)
