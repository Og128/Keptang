package com.keptang.data.db

/**
 * Why money moved between accounts. Neither kind is an expense: a withdrawal does not make you
 * poorer, it moves 1000 baht from the bank into your pocket, and counting it as spending would
 * double-count the coffee you later buy with it.
 */
enum class TransferKind {
    /** Money taken out of a bank account and into the cash wallet. */
    WITHDRAWAL,

    /**
     * A correction, for when you count your notes and the wallet disagrees - you will forget
     * small cash purchases, so the balance needs a way back to the truth. Signed: negative when
     * the wallet held less than Keptang thought.
     */
    ADJUSTMENT
}
