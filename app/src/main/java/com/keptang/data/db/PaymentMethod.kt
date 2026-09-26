package com.keptang.data.db

/**
 * How money left a [AccountKind.BANK] account. Cash is deliberately absent: the moment you
 * withdraw, the money is no longer at the bank, so spending it is an expense on the wallet
 * account rather than a K-bank expense paid "in cash". An expense on a [AccountKind.CASH]
 * account carries no payment method at all.
 */
enum class PaymentMethod {
    CARD,
    QR,
    TRANSFER;

    companion object {
        /** Every method a new bank account offers until the user prunes the list. */
        val BANK_DEFAULTS = listOf(CARD, QR, TRANSFER)

        fun parseList(stored: String?): List<PaymentMethod> =
            stored?.split(',')
                ?.mapNotNull { name -> entries.firstOrNull { it.name == name.trim() } }
                .orEmpty()

        fun encodeList(methods: List<PaymentMethod>): String = methods.joinToString(",") { it.name }
    }
}
