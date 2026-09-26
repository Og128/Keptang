package com.keptang.account

import com.keptang.data.db.AccountEntity
import com.keptang.data.db.PaymentMethod
import com.keptang.data.repository.AccountRepository
import com.keptang.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Turns what the parser heard - an account name and a payment method, both free text - into the
 * account and [PaymentMethod] an expense is actually stored with.
 *
 * It lives here rather than in [com.keptang.parser] on purpose: the parser is plain Kotlin with
 * no database behind it, and it cannot know which accounts exist. It only reports what was said.
 */
class AccountResolver(
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository
) {

    data class Resolution(val accountId: String?, val paymentMethod: PaymentMethod?)

    /**
     * @param spokenAccount what [com.keptang.parser.AccountExtractor] heard ("HSBC"), or null
     * @param spokenMethod what [com.keptang.parser.PaymentMethodExtractor] heard ("Cash", "PromptPay"), or null
     *
     * "coffee 50 baht cash" wins over the default account: hearing cash means the money came out
     * of your pocket, whichever bank you usually pay from.
     */
    suspend fun resolve(spokenAccount: String?, spokenMethod: String?): Resolution {
        if (isCash(spokenMethod)) {
            return Resolution(accountRepository.cashWallet()?.id, paymentMethod = null)
        }
        val named = spokenAccount?.takeIf { it.isNotBlank() }?.let { accountRepository.findByName(it) }
        val account = named ?: defaultAccount()
        return Resolution(account?.id, methodFor(account, spokenMethod))
    }

    /** The account new expenses land on when nothing else was said. */
    suspend fun defaultAccount(): AccountEntity? {
        val stored = settingsRepository.settings.first().defaultAccountId
        return stored?.let { accountRepository.getById(it) } ?: accountRepository.getAll().firstOrNull()
    }

    /**
     * Drops a method the account does not offer instead of recording something impossible -
     * "paid by card" on a wallet that only holds notes is a mishearing, not a fact.
     */
    private fun methodFor(account: AccountEntity?, spokenMethod: String?): PaymentMethod? {
        val parsed = parseSpokenMethod(spokenMethod) ?: return null
        return parsed.takeIf { account == null || it in account.paymentMethods }
    }

    private companion object {

        /**
         * The vocabulary [com.keptang.parser.PaymentMethodExtractor] emits. Kept as a mapping here
         * rather than making the parser speak [PaymentMethod] directly, since the parser has no
         * word for "cash is an account, not a method" - that distinction only exists once there
         * is a wallet to put the expense on.
         */
        fun parseSpokenMethod(spoken: String?): PaymentMethod? = when (spoken?.lowercase()) {
            "card" -> PaymentMethod.CARD
            "promptpay" -> PaymentMethod.QR
            "bank transfer" -> PaymentMethod.TRANSFER
            else -> null
        }

        fun isCash(spoken: String?): Boolean = spoken?.equals("cash", ignoreCase = true) == true
    }
}
