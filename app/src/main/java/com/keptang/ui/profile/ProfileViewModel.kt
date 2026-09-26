package com.keptang.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.AccountEntity
import com.keptang.data.db.AccountKind
import com.keptang.data.db.PaymentMethod
import com.keptang.data.repository.AccountInUseException
import com.keptang.data.repository.AccountRepository
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val accounts: StateFlow<List<AccountEntity>> = accountRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * The one balance Keptang keeps. Bank accounts deliberately have none - see [AccountKind] -
     * so this follows whichever account is the wallet rather than being offered per row.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val cashBalanceMinorUnits: StateFlow<Long?> = accounts
        .map { list -> list.firstOrNull { it.kind == AccountKind.CASH } }
        .flatMapLatest { wallet ->
            if (wallet == null) flowOf(null) else accountRepository.observeBalance(wallet.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Set when a delete is refused because the account still carries expenses, so the UI can say why. */
    private val _deleteBlockedCount = MutableStateFlow<Int?>(null)
    val deleteBlockedCount: StateFlow<Int?> = _deleteBlockedCount.asStateFlow()

    fun setProfileName(name: String) = viewModelScope.launch { settingsRepository.setProfileName(name) }

    fun setDefaultAccount(accountId: String) = viewModelScope.launch {
        settingsRepository.setDefaultAccountId(accountId)
    }

    fun createAccount(name: String, kind: AccountKind, colorHex: String) = viewModelScope.launch {
        accountRepository.create(name, kind, colorHex)
    }

    fun updateAccount(account: AccountEntity) = viewModelScope.launch { accountRepository.update(account) }

    fun setPaymentMethods(account: AccountEntity, methods: List<PaymentMethod>) = viewModelScope.launch {
        accountRepository.update(account.copy(paymentMethods = methods))
    }

    fun deleteAccount(accountId: String) = viewModelScope.launch {
        try {
            accountRepository.delete(accountId)
        } catch (e: AccountInUseException) {
            _deleteBlockedCount.value = e.expenseCount
        }
    }

    fun dismissDeleteBlocked() { _deleteBlockedCount.value = null }

    /** Records taking [amountMinorUnits] out of [fromAccountId] and into the wallet. */
    fun recordWithdrawal(fromAccountId: String?, amountMinorUnits: Long, currencyCode: String, timeZoneId: String) =
        viewModelScope.launch {
            val wallet = accountRepository.cashWallet() ?: return@launch
            accountRepository.recordWithdrawal(
                fromAccountId = fromAccountId,
                toAccountId = wallet.id,
                amountMinorUnits = amountMinorUnits,
                currencyCode = currencyCode,
                timeZoneId = timeZoneId
            )
        }

    /** Recalls the wallet to [countedMinorUnits], the amount actually in your pocket. */
    fun adjustWalletTo(countedMinorUnits: Long, currencyCode: String, timeZoneId: String) = viewModelScope.launch {
        val wallet = accountRepository.cashWallet() ?: return@launch
        val current = cashBalanceMinorUnits.value ?: 0L
        val delta = countedMinorUnits - current
        if (delta == 0L) return@launch
        accountRepository.recordAdjustment(
            accountId = wallet.id,
            deltaMinorUnits = delta,
            currencyCode = currencyCode,
            timeZoneId = timeZoneId
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ProfileViewModel(ServiceLocator.settingsRepository, ServiceLocator.accountRepository)
            }
        }
    }
}
