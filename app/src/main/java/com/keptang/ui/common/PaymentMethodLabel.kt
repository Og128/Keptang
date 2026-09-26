package com.keptang.ui.common

import androidx.annotation.StringRes
import com.keptang.R
import com.keptang.data.db.PaymentMethod

/** The one place [PaymentMethod] turns into words, so the ledger, the form and the filters agree. */
@StringRes
fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CARD -> R.string.payment_method_card
    PaymentMethod.QR -> R.string.payment_method_qr
    PaymentMethod.TRANSFER -> R.string.payment_method_transfer
}
