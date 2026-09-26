package com.keptang.account

import com.keptang.data.db.AccountTransferEntity
import com.keptang.data.db.ExpenseEntity

/**
 * What is left in the cash wallet: everything withdrawn into it, minus everything spent from it,
 * plus or minus whatever corrections were made after counting the notes by hand.
 *
 * Pure and in-memory, like [com.keptang.dashboard.DashboardCalculator] - the house avoids
 * aggregate SQL at this scale, and a plain function is testable on the JVM without a database.
 */
object CashBalanceCalculator {

    /**
     * @param accountId the wallet being counted
     * @param transfers every transfer touching it, in either direction
     * @param expenses every expense charged to it
     */
    fun balanceMinorUnits(
        accountId: String,
        transfers: List<AccountTransferEntity>,
        expenses: List<ExpenseEntity>
    ): Long {
        val movedIn = transfers.filter { it.toAccountId == accountId }.sumOf { it.amountMinorUnits }
        val movedOut = transfers.filter { it.fromAccountId == accountId }.sumOf { it.amountMinorUnits }
        val spent = expenses.filter { it.accountId == accountId }.sumOf { it.amountMinorUnits }
        return movedIn - movedOut - spent
    }
}
