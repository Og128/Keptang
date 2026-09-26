package com.keptang.data.repository

import com.keptang.account.CashBalanceCalculator
import com.keptang.core.Defaults
import com.keptang.data.db.AccountDao
import com.keptang.data.db.AccountEntity
import com.keptang.data.db.AccountKind
import com.keptang.data.db.AccountTransferDao
import com.keptang.data.db.AccountTransferEntity
import com.keptang.data.db.ExpenseDao
import com.keptang.data.db.PaymentMethod
import com.keptang.data.db.TransferKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

/** Raised when an account still carrying expenses is deleted, mirroring how categories are guarded. */
class AccountInUseException(val expenseCount: Int) : IllegalStateException(
    "Account still has $expenseCount expense(s)"
)

class AccountRepository(
    private val accountDao: AccountDao,
    private val transferDao: AccountTransferDao,
    private val expenseDao: ExpenseDao
) {

    fun observeAll(): Flow<List<AccountEntity>> = accountDao.observeAll()

    fun observeBankAccounts(): Flow<List<AccountEntity>> =
        accountDao.observeAll().map { accounts -> accounts.filter { it.kind == AccountKind.BANK } }

    suspend fun getAll(): List<AccountEntity> = accountDao.getAll()

    suspend fun getById(id: String): AccountEntity? = id.let { accountDao.getById(it) }

    /**
     * The wallet, which every database has: seeded on install and by
     * [com.keptang.data.db.KeptangDatabase.MIGRATION_10_11]. Null only if someone deleted it,
     * which the Profile screen does not allow.
     */
    suspend fun cashWallet(): AccountEntity? = accountDao.getFirstOfKind(AccountKind.CASH)

    /** Resolves an account spoken or typed by name ("on HSBC"), for [com.keptang.parser.AccountExtractor] output. */
    suspend fun findByName(name: String): AccountEntity? = accountDao.getByName(name.trim())

    suspend fun create(
        name: String,
        kind: AccountKind,
        colorHex: String,
        paymentMethods: List<PaymentMethod> = if (kind == AccountKind.BANK) PaymentMethod.BANK_DEFAULTS else emptyList()
    ): AccountEntity {
        val account = AccountEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            kind = kind,
            colorHex = colorHex,
            paymentMethods = paymentMethods,
            sortOrder = (accountDao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        )
        accountDao.upsert(account)
        return account
    }

    suspend fun update(account: AccountEntity) = accountDao.upsert(account.copy(name = account.name.trim()))

    /** @throws AccountInUseException when expenses still point at it, so a year of history is never orphaned silently. */
    suspend fun delete(accountId: String) {
        val inUse = accountDao.countExpenses(accountId)
        if (inUse > 0) throw AccountInUseException(inUse)
        accountDao.deleteById(accountId)
    }

    fun observeTransfersFor(accountId: String): Flow<List<AccountTransferEntity>> =
        transferDao.observeForAccount(accountId)

    /**
     * Records taking money out of [fromAccountId] and into [toAccountId]. Deliberately not an
     * expense: see [TransferKind].
     */
    suspend fun recordWithdrawal(
        fromAccountId: String?,
        toAccountId: String,
        amountMinorUnits: Long,
        currencyCode: String = Defaults.CURRENCY_CODE,
        occurredAtEpochMillis: Long = Instant.now().toEpochMilli(),
        timeZoneId: String = Defaults.TIME_ZONE_ID,
        note: String? = null
    ): AccountTransferEntity = record(
        fromAccountId, toAccountId, amountMinorUnits, currencyCode,
        occurredAtEpochMillis, timeZoneId, TransferKind.WITHDRAWAL, note
    )

    /**
     * Recalls the wallet to what is actually in your pocket. [deltaMinorUnits] is signed and
     * negative when Keptang overstated the balance, which is the common direction - forgotten
     * small cash purchases, not forgotten income.
     */
    suspend fun recordAdjustment(
        accountId: String,
        deltaMinorUnits: Long,
        currencyCode: String = Defaults.CURRENCY_CODE,
        occurredAtEpochMillis: Long = Instant.now().toEpochMilli(),
        timeZoneId: String = Defaults.TIME_ZONE_ID,
        note: String? = null
    ): AccountTransferEntity = record(
        null, accountId, deltaMinorUnits, currencyCode,
        occurredAtEpochMillis, timeZoneId, TransferKind.ADJUSTMENT, note
    )

    suspend fun deleteTransfer(id: String) = transferDao.deleteById(id)

    /** Live balance of [accountId], recomputed whenever a transfer or one of its expenses changes. */
    fun observeBalance(accountId: String): Flow<Long> =
        combine(
            transferDao.observeForAccount(accountId),
            expenseDao.observeByAccount(accountId)
        ) { transfers, expenses ->
            CashBalanceCalculator.balanceMinorUnits(accountId, transfers, expenses)
        }

    private suspend fun record(
        fromAccountId: String?,
        toAccountId: String,
        amountMinorUnits: Long,
        currencyCode: String,
        occurredAtEpochMillis: Long,
        timeZoneId: String,
        kind: TransferKind,
        note: String?
    ): AccountTransferEntity {
        val transfer = AccountTransferEntity(
            id = UUID.randomUUID().toString(),
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amountMinorUnits = amountMinorUnits,
            currencyCode = currencyCode,
            occurredAtEpochMillis = occurredAtEpochMillis,
            timeZoneId = timeZoneId,
            kind = kind,
            note = note?.takeIf { it.isNotBlank() },
            createdAtEpochMillis = Instant.now().toEpochMilli()
        )
        transferDao.upsert(transfer)
        return transfer
    }
}
