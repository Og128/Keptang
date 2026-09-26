package com.keptang.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R
import com.keptang.data.db.AccountEntity
import com.keptang.data.db.AccountKind
import com.keptang.data.db.PaymentMethod
import com.keptang.ui.common.InfoCard
import com.keptang.ui.common.MoneyText
import com.keptang.ui.common.labelRes
import com.keptang.ui.common.parseMoneyInput
import com.keptang.ui.theme.CategoryColors

/**
 * The profile: who is using Keptang, and the accounts they pay from.
 *
 * The screen is built around the one idea the rest of the app now depends on - you pick an
 * *account*, not a payment method. Card, QR and transfer are things a bank account offers; cash
 * is a place the money is. See [AccountKind].
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val cashBalance by viewModel.cashBalanceMinorUnits.collectAsStateWithLifecycle()
    val deleteBlockedCount by viewModel.deleteBlockedCount.collectAsStateWithLifecycle()

    var profileName by remember(settings.profileName) { mutableStateOf(settings.profileName) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showAddAccount by remember { mutableStateOf(false) }
    var showWithdrawal by remember { mutableStateOf(false) }
    var showCount by remember { mutableStateOf(false) }

    val wallet = accounts.firstOrNull { it.kind == AccountKind.CASH }
    val defaultAccountId = settings.defaultAccountId ?: accounts.firstOrNull()?.id

    Scaffold(
        // Nested inside the NavHost, which the root Scaffold has already inset.
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddAccount = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.profile_add_account))
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            item {
                IdentityCard(
                    name = profileName,
                    onNameChange = { profileName = it; viewModel.setProfileName(it) },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            if (wallet != null) {
                item {
                    WalletCard(
                        balanceMinorUnits = cashBalance ?: 0L,
                        currencyCode = settings.currencyCode,
                        onWithdraw = { showWithdrawal = true },
                        onCount = { showCount = true },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.profile_accounts_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            items(accounts, key = { it.id }) { account ->
                AccountRow(
                    account = account,
                    isDefault = account.id == defaultAccountId,
                    onClick = { editingAccount = account }
                )
            }
        }
    }

    editingAccount?.let { account ->
        AccountEditDialog(
            account = account,
            isDefault = account.id == defaultAccountId,
            onSave = { updated -> viewModel.updateAccount(updated); editingAccount = null },
            onMakeDefault = { viewModel.setDefaultAccount(account.id); editingAccount = null },
            onDelete = { viewModel.deleteAccount(account.id); editingAccount = null },
            onDismiss = { editingAccount = null }
        )
    }

    if (showAddAccount) {
        AccountCreateDialog(
            onCreate = { name ->
                viewModel.createAccount(
                    name,
                    AccountKind.BANK,
                    CategoryColors.PALETTE[accounts.size % CategoryColors.PALETTE.size]
                )
                showAddAccount = false
            },
            onDismiss = { showAddAccount = false }
        )
    }

    if (showWithdrawal) {
        WithdrawalDialog(
            bankAccounts = accounts.filter { it.kind == AccountKind.BANK },
            currencyCode = settings.currencyCode,
            onConfirm = { fromId, minorUnits ->
                viewModel.recordWithdrawal(fromId, minorUnits, settings.currencyCode, settings.timeZoneId)
                showWithdrawal = false
            },
            onDismiss = { showWithdrawal = false }
        )
    }

    if (showCount) {
        CountCashDialog(
            currencyCode = settings.currencyCode,
            onConfirm = { counted ->
                viewModel.adjustWalletTo(counted, settings.currencyCode, settings.timeZoneId)
                showCount = false
            },
            onDismiss = { showCount = false }
        )
    }

    deleteBlockedCount?.let { count ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBlocked,
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteBlocked) {
                    Text(stringResource(R.string.profile_cancel))
                }
            },
            text = { Text(stringResource(R.string.profile_delete_blocked, count)) }
        )
    }
}

@Composable
private fun IdentityCard(name: String, onNameChange: (String) -> Unit, modifier: Modifier = Modifier) {
    InfoCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text(stringResource(R.string.settings_profile_name_label)) },
                placeholder = { Text(stringResource(R.string.settings_profile_name_placeholder)) },
                modifier = Modifier.weight(1f).padding(start = 16.dp)
            )
        }
    }
}

/** The wallet gets a card rather than a row, because it is the only account with a number to show. */
@Composable
private fun WalletCard(
    balanceMinorUnits: Long,
    currencyCode: String,
    onWithdraw: () -> Unit,
    onCount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.profile_wallet_balance_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoneyText(
                amountMinorUnits = balanceMinorUnits,
                currencyCode = currencyCode,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onWithdraw) { Text(stringResource(R.string.profile_record_withdrawal)) }
                OutlinedButton(onClick = onCount) { Text(stringResource(R.string.profile_count_cash)) }
            }
        }
    }
}

@Composable
private fun AccountRow(account: AccountEntity, isDefault: Boolean, onClick: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(CategoryColors.parse(account.colorHex)))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(account.name, style = MaterialTheme.typography.bodyLarge)
                // The labels are resolved before joining: stringResource is composable and
                // joinToString's transform lambda is not.
                val methodLabels = account.paymentMethods.map { stringResource(it.labelRes()) }
                val subtitle = when (account.kind) {
                    AccountKind.CASH -> stringResource(R.string.profile_account_kind_cash)
                    AccountKind.BANK -> methodLabels.joinToString(" · ")
                        .ifBlank { stringResource(R.string.profile_account_kind_bank) }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isDefault) {
                Text(
                    stringResource(R.string.profile_default_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountEditDialog(
    account: AccountEntity,
    isDefault: Boolean,
    onSave: (AccountEntity) -> Unit,
    onMakeDefault: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(account.id) { mutableStateOf(account.name) }
    var methods by remember(account.id) { mutableStateOf(account.paymentMethods) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(account.copy(name = name, paymentMethods = methods)) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.profile_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_account_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                when (account.kind) {
                    AccountKind.CASH -> Text(
                        stringResource(R.string.profile_cash_no_methods),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    AccountKind.BANK -> {
                        Text(
                            stringResource(R.string.profile_payment_methods_label),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = method in methods,
                                    onClick = {
                                        // Kept in enum order however they are toggled, so the
                                        // picker on the expense form never reshuffles itself.
                                        methods = if (method in methods) {
                                            methods - method
                                        } else {
                                            (methods + method).sortedBy { it.ordinal }
                                        }
                                    },
                                    label = { Text(stringResource(method.labelRes())) }
                                )
                            }
                        }
                    }
                }
                if (!isDefault) {
                    TextButton(onClick = onMakeDefault, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.profile_make_default))
                    }
                }
                // The wallet is not deletable: the balance, the withdrawals and every cash
                // expense hang off it, and a database without one has nowhere to put cash.
                if (account.kind != AccountKind.CASH) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.profile_delete_account), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    )
}

/** Only bank accounts can be added: there is exactly one pocket, and it is seeded with the database. */
@Composable
private fun AccountCreateDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.profile_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
        title = { Text(stringResource(R.string.profile_add_account)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.profile_account_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun WithdrawalDialog(
    bankAccounts: List<AccountEntity>,
    currencyCode: String,
    onConfirm: (String?, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var fromId by remember(bankAccounts) { mutableStateOf(bankAccounts.firstOrNull()?.id) }
    val minorUnits = parseMoneyInput(amountText, currencyCode)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { minorUnits?.let { onConfirm(fromId, it) } },
                enabled = minorUnits != null && minorUnits > 0
            ) { Text(stringResource(R.string.profile_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
        title = { Text(stringResource(R.string.profile_record_withdrawal)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.profile_withdrawal_explainer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(currencyCode) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                if (bankAccounts.isNotEmpty()) {
                    Text(
                        stringResource(R.string.profile_withdrawal_from),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                    Column {
                        bankAccounts.forEach { account ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { fromId = account.id }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (account.id == fromId) {
                                                CategoryColors.parse(account.colorHex)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                )
                                Text(account.name, modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    )
}

/**
 * Counting the notes, not entering a correction. Asking for the amount in hand rather than the
 * difference is the only version anyone can answer without doing arithmetic first - the delta is
 * worked out in [ProfileViewModel.adjustWalletTo].
 */
@Composable
private fun CountCashDialog(currencyCode: String, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val minorUnits = parseMoneyInput(amountText, currencyCode)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { minorUnits?.let(onConfirm) }, enabled = minorUnits != null) {
                Text(stringResource(R.string.profile_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) } },
        title = { Text(stringResource(R.string.profile_count_cash)) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                label = { Text(stringResource(R.string.profile_counted_amount_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
