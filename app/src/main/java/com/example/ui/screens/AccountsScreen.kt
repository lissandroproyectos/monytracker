package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Account
import com.example.data.model.Transaction
import com.example.ui.FinanceViewModel
import com.example.ui.components.AccountItemCard
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatCurrency
import com.example.data.model.TransactionType
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.data.model.AccountType
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val health by viewModel.financialHealth.collectAsStateWithLifecycle()
    val rates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val dispCurr = health.displayCurrency
    val payCurr = health.paymentCurrency

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var addAccountDefaultIsSavings by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var selectedTxAccountAndType by remember { mutableStateOf<Pair<String, String?>?>(null) } // Pair(Type, accountId?)
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val visibleAccounts = remember(accounts) { accounts.filter { !it.isClosed } }
    val savingsAccounts = remember(visibleAccounts) {
        visibleAccounts.filter { it.isSavings || it.type == AccountType.SAVINGS.name }
    }
    val incomeAndGeneralAccounts = remember(visibleAccounts) {
        visibleAccounts
            .filter { !it.isSavings && it.type != AccountType.SAVINGS.name }
            .sortedWith(
                compareByDescending<Account> { it.hasPeriodicIncome }
                    .thenBy { it.isPlanStartAccount }
            )
    }

    val totalBalance = remember(visibleAccounts, rates, dispCurr) {
        visibleAccounts.sumOf { rates.convert(it.currentBalance, com.example.data.currency.AppCurrency.fromCode(it.currency), dispCurr) }
    }
    val totalBalancePay = remember(visibleAccounts, rates, payCurr) {
        visibleAccounts.sumOf { rates.convert(it.currentBalance, com.example.data.currency.AppCurrency.fromCode(it.currency), payCurr) }
    }
    val generalPoolTotal = remember(incomeAndGeneralAccounts, rates, dispCurr) {
        incomeAndGeneralAccounts.sumOf { rates.convert(it.currentBalance, com.example.data.currency.AppCurrency.fromCode(it.currency), dispCurr) }
    }
    val generalPoolTotalPay = remember(incomeAndGeneralAccounts, rates, payCurr) {
        incomeAndGeneralAccounts.sumOf { rates.convert(it.currentBalance, com.example.data.currency.AppCurrency.fromCode(it.currency), payCurr) }
    }
    val savingsTotal = remember(savingsAccounts, rates, dispCurr) {
        savingsAccounts.sumOf { rates.convert(it.currentBalance, com.example.data.currency.AppCurrency.fromCode(it.currency), dispCurr) }
    }
    val savingsTotalPay = remember(savingsAccounts, rates, payCurr) {
        savingsAccounts.sumOf { rates.convert(it.currentBalance, com.example.data.currency.AppCurrency.fromCode(it.currency), payCurr) }
    }
    val extraTransactions = remember(transactions) {
        transactions.filter { it.commitmentId.isNullOrBlank() && it.type != TransactionType.TRANSFER.name }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("accounts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(
                            text = "Patrimonio Total en Cuentas (${dispCurr.code})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = formatCurrency(totalBalance, dispCurr.symbol),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        if (payCurr != dispCurr) {
                            Text(
                                text = "Equiv. pago (${payCurr.code}): ${formatCurrency(totalBalancePay, payCurr.symbol)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondaryDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Dual Summary Pills: Cuenta General vs Ahorros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SlateBackground
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Cuenta General", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                                Text(
                                    text = formatCurrency(generalPoolTotal, dispCurr.symbol),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight,
                                    fontSize = 14.sp
                                )
                                if (payCurr != dispCurr) {
                                    Text(
                                        text = "Pago: ${formatCurrency(generalPoolTotalPay, payCurr.symbol)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldDark.copy(alpha = 0.2f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Total en Ahorros", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                                Text(
                                    text = formatCurrency(savingsTotal, dispCurr.symbol),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight,
                                    fontSize = 14.sp
                                )
                                if (payCurr != dispCurr) {
                                    Text(
                                        text = "Pago: ${formatCurrency(savingsTotalPay, payCurr.symbol)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    if (visibleAccounts.size >= 2) {
                        OutlinedButton(
                            onClick = { showTransferDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("transfer_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transferir entre Cuentas", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // SECTION 1: CUENTAS DE INGRESOS Y CUENTA GENERAL
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cuentas de Ingresos (${incomeAndGeneralAccounts.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Cuenta General disponible: ${formatCurrency(generalPoolTotal, dispCurr.symbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldLight,
                        fontSize = 11.sp
                    )
                }
                OutlinedButton(
                    onClick = {
                        addAccountDefaultIsSavings = false
                        showAddAccountDialog = true
                    },
                    modifier = Modifier.testTag("add_account_btn"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Cuenta", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }

        if (incomeAndGeneralAccounts.isEmpty()) {
            item {
                Surface(
                    color = SlateCard.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tienes cuentas de ingresos ni operación registradas aún",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                addAccountDefaultIsSavings = false
                                showAddAccountDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Agregar Cuenta de Ingreso")
                        }
                    }
                }
            }
        } else {
            items(incomeAndGeneralAccounts, key = { it.id }) { acc ->
                AccountItemCard(
                    account = acc,
                    onEdit = { accountToEdit = acc },
                    onDelete = { accountToDelete = acc },
                    onAddIncome = {
                        selectedTxAccountAndType = Pair(TransactionType.INCOME.name, acc.id)
                    },
                    onAddExpense = {
                        selectedTxAccountAndType = Pair(TransactionType.EXPENSE.name, acc.id)
                    },
                    displayCurrency = dispCurr,
                    paymentCurrency = payCurr,
                    rates = rates
                )
            }
        }

        // SECTION 2: CUENTAS DE AHORROS (Digital, Bancaria, Efectivo Ahorro)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Cuentas de Ahorros (${savingsAccounts.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Text(
                        text = "Digital, Bancaria o Efectivo • Total: ${formatCurrency(savingsTotal, dispCurr.symbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = {
                        addAccountDefaultIsSavings = true
                        showAddAccountDialog = true
                    },
                    modifier = Modifier.testTag("add_savings_account_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLight)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("De Ahorro", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldLight)
                }
            }
        }

        if (savingsAccounts.isEmpty()) {
            item {
                Surface(
                    color = EmeraldDark.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aún no tienes cuentas de ahorro configuradas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                addAccountDefaultIsSavings = true
                                showAddAccountDialog = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Crear Cuenta de Ahorro (Digital / Banco / Efectivo)", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(savingsAccounts, key = { it.id }) { acc ->
                AccountItemCard(
                    account = acc,
                    onEdit = { accountToEdit = acc },
                    onDelete = { accountToDelete = acc },
                    onAddIncome = {
                        selectedTxAccountAndType = Pair(TransactionType.INCOME.name, acc.id)
                    },
                    onAddExpense = {
                        selectedTxAccountAndType = Pair(TransactionType.EXPENSE.name, acc.id)
                    },
                    displayCurrency = dispCurr,
                    paymentCurrency = payCurr,
                    rates = rates
                )
            }
        }

        // SECTION 3: INGRESOS Y EGRESOS EXTRA
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ingresos y Egresos Extra (${extraTransactions.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Toca un movimiento para ver, editar o eliminar",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                    if (onNavigateToTransactions != null) {
                        TextButton(
                            onClick = onNavigateToTransactions,
                            modifier = Modifier.testTag("accounts_view_all_tx_btn")
                        ) {
                            Text("Ver Historial", color = EmeraldLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedTxAccountAndType = Pair(TransactionType.INCOME.name, null) },
                        modifier = Modifier.weight(1f).testTag("add_extra_income_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("+ Ingreso Extra", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { selectedTxAccountAndType = Pair(TransactionType.EXPENSE.name, null) },
                        modifier = Modifier.weight(1f).testTag("add_extra_expense_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("- Egreso Extra", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (extraTransactions.isEmpty()) {
            item {
                Surface(
                    color = SlateCard.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No tienes ingresos ni egresos extra registrados aún.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(extraTransactions.take(8), key = { it.id }) { tx ->
                val accName = accounts.find { it.id == tx.accountId }?.name
                    ?: if (tx.accountId == "EXTERNAL") "Dinero Externo" else "Cuenta"
                TransactionRow(
                    transaction = tx,
                    accountName = accName,
                    onClick = { transactionToEdit = tx },
                    onEdit = { transactionToEdit = tx },
                    onDelete = { transactionToDelete = tx },
                    displayCurrency = dispCurr,
                    paymentCurrency = payCurr,
                    rates = rates
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            initialIsSavings = addAccountDefaultIsSavings,
            onDismiss = { showAddAccountDialog = false },
            onSave = { newAccount -> viewModel.addAccount(newAccount) }
        )
    }

    if (accountToEdit != null) {
        AddAccountDialog(
            accountToEdit = accountToEdit,
            onDismiss = { accountToEdit = null },
            onSave = { updated ->
                viewModel.updateAccount(updated)
                accountToEdit = null
            },
            onDelete = {
                accountToEdit?.let { viewModel.deleteAccount(it.id) }
                accountToEdit = null
            }
        )
    }

    if (accountToDelete != null) {
        val acc = accountToDelete!!
        val isPlanStart = acc.isPlanStartAccount
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { accountToDelete = null },
            containerColor = SlateCard,
            title = {
                Text(
                    text = if (isPlanStart) "¿Cerrar cuenta '${acc.name}'?" else "¿Eliminar cuenta?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = if (isPlanStart) {
                        "Al cerrar la cuenta de Inicio del Plan, su saldo restante (${formatCurrency(acc.currentBalance)}) pasará a $0.00 y dejará de mostrarse como cuenta activa hasta que vuelvas a inyectar capital desde el Plan."
                    } else {
                        "Se eliminará la cuenta '${acc.name}' de forma permanente."
                    },
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (isPlanStart) {
                            viewModel.closePlanStartAccount(acc)
                        } else {
                            viewModel.deleteAccount(acc.id)
                        }
                        accountToDelete = null
                    }
                ) {
                    Text(
                        text = if (isPlanStart) "Cerrar Cuenta" else "Eliminar",
                        color = com.example.ui.theme.AccentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancelar", color = TextSecondaryDark)
                }
            }
        )
    }

    if (showTransferDialog && visibleAccounts.size >= 2) {
        AddTransactionDialog(
            initialType = TransactionType.TRANSFER.name,
            accounts = visibleAccounts,
            customExpenseCategories = settings.customExpenseCategories,
            customIncomeCategories = settings.customIncomeCategories,
            rates = rates,
            displayCurrency = dispCurr,
            paymentCurrency = payCurr,
            onDismiss = { showTransferDialog = false },
            onSave = { tx -> viewModel.addTransaction(tx) },
            onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) }
        )
    }

    selectedTxAccountAndType?.let { (type, targetAccId) ->
        AddTransactionDialog(
            initialType = type,
            preselectedAccountId = targetAccId,
            accounts = accounts,
            customExpenseCategories = settings.customExpenseCategories,
            customIncomeCategories = settings.customIncomeCategories,
            rates = rates,
            displayCurrency = dispCurr,
            paymentCurrency = payCurr,
            onDismiss = { selectedTxAccountAndType = null },
            onSave = { tx -> viewModel.addTransaction(tx) },
            onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) }
        )
    }

    transactionToEdit?.let { oldTx ->
        AddTransactionDialog(
            transactionToEdit = oldTx,
            accounts = accounts,
            customExpenseCategories = settings.customExpenseCategories,
            customIncomeCategories = settings.customIncomeCategories,
            rates = rates,
            displayCurrency = dispCurr,
            paymentCurrency = payCurr,
            onDismiss = { transactionToEdit = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(oldTx, updatedTx)
                transactionToEdit = null
            },
            onDelete = { txToRemove ->
                viewModel.deleteTransaction(txToRemove)
                transactionToEdit = null
            },
            onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) }
        )
    }

    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            containerColor = SlateCard,
            title = { Text("¿Eliminar movimiento?", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = {
                Text(
                    "Se eliminará '${tx.description.ifBlank { tx.category }}' y se revertirá su impacto en el saldo de la cuenta.",
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancelar", color = TextSecondaryDark)
                }
            }
        )
    }
}
