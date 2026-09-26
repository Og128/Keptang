package com.keptang.account

import com.keptang.data.db.AccountTransferEntity
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.db.PaymentMethod
import com.keptang.data.db.ReviewStatus
import com.keptang.data.db.TransferKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CashBalanceCalculatorTest {

    private val wallet = "acc-cash"
    private val bank = "acc-kbank"

    private fun withdrawal(amountMinorUnits: Long, from: String? = bank, to: String = wallet) =
        AccountTransferEntity(
            id = "t-$amountMinorUnits-$from-$to",
            fromAccountId = from,
            toAccountId = to,
            amountMinorUnits = amountMinorUnits,
            currencyCode = "THB",
            occurredAtEpochMillis = 0L,
            timeZoneId = "Asia/Bangkok",
            kind = TransferKind.WITHDRAWAL,
            createdAtEpochMillis = 0L
        )

    private fun adjustment(deltaMinorUnits: Long, to: String = wallet) =
        withdrawal(deltaMinorUnits, from = null, to = to).copy(
            id = "adj-$deltaMinorUnits",
            kind = TransferKind.ADJUSTMENT
        )

    private fun expense(
        id: String,
        amountMinorUnits: Long,
        accountId: String? = wallet,
        paymentMethod: PaymentMethod? = null
    ) = ExpenseEntity(
        id = id,
        captureId = "capture-$id",
        amountMinorUnits = amountMinorUnits,
        currencyCode = "THB",
        occurredAtEpochMillis = 0L,
        timeZoneId = "Asia/Bangkok",
        category = "Coffee",
        accountId = accountId,
        paymentMethod = paymentMethod,
        description = null,
        confidence = 1f,
        reviewStatus = ReviewStatus.APPROVED,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L
    )

    @Test
    fun `an empty wallet is worth nothing`() {
        assertEquals(0L, CashBalanceCalculator.balanceMinorUnits(wallet, emptyList(), emptyList()))
    }

    @Test
    fun `withdrawing fills the wallet`() {
        assertEquals(
            100_000L,
            CashBalanceCalculator.balanceMinorUnits(wallet, listOf(withdrawal(100_000L)), emptyList())
        )
    }

    /** The point of the whole design: 1000 in, a 50 coffee out, 950 left - not 1050 spent. */
    @Test
    fun `spending cash draws the wallet down rather than counting twice`() {
        val balance = CashBalanceCalculator.balanceMinorUnits(
            wallet,
            listOf(withdrawal(100_000L)),
            listOf(expense("e1", 5_000L))
        )

        assertEquals(95_000L, balance)
    }

    @Test
    fun `expenses on other accounts leave the wallet untouched`() {
        val balance = CashBalanceCalculator.balanceMinorUnits(
            wallet,
            listOf(withdrawal(100_000L)),
            listOf(expense("card", 5_000L, accountId = bank, paymentMethod = PaymentMethod.CARD))
        )

        assertEquals(100_000L, balance)
    }

    @Test
    fun `a negative adjustment takes money off a wallet that was overstated`() {
        val balance = CashBalanceCalculator.balanceMinorUnits(
            wallet,
            listOf(withdrawal(100_000L), adjustment(-3_000L)),
            emptyList()
        )

        assertEquals(97_000L, balance)
    }

    @Test
    fun `moving cash back into the bank empties the wallet again`() {
        val balance = CashBalanceCalculator.balanceMinorUnits(
            wallet,
            listOf(withdrawal(100_000L), withdrawal(100_000L, from = wallet, to = bank)),
            emptyList()
        )

        assertEquals(0L, balance)
    }

    /**
     * Nothing stops the wallet going negative, and nothing should: it means expenses were logged
     * that the withdrawals do not account for, which is information, not an error to hide.
     */
    @Test
    fun `the wallet is allowed to go negative`() {
        val balance = CashBalanceCalculator.balanceMinorUnits(
            wallet,
            emptyList(),
            listOf(expense("e1", 5_000L))
        )

        assertEquals(-5_000L, balance)
    }
}
