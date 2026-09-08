package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** The many-to-many link between an expense and a [TagEntity], keyed by tag name rather than id since tags have no other data. */
@Entity(
    tableName = "expense_tags",
    primaryKeys = ["expense_id", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expense_id"), Index("tag")]
)
data class ExpenseTagCrossRef(
    @ColumnInfo(name = "expense_id") val expenseId: String,
    @ColumnInfo(name = "tag") val tag: String
)
