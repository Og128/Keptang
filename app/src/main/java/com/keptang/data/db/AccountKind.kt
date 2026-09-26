package com.keptang.data.db

/**
 * What an account physically is, which decides whether Keptang tracks a balance for it.
 *
 * [BANK] accounts are labels: their real balance lives in the bank's own app, and keeping a
 * second copy honest here would mean logging every salary, transfer and fee. [CASH] is the
 * opposite - nothing tells you what is left in your pocket - so the wallet is the one account
 * Keptang actually counts, fed by withdrawals and drawn down by cash expenses.
 */
enum class AccountKind {
    BANK,
    CASH
}
