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
import com.example.ui.FinanceViewModel
import com.example.ui.components.AccountItemCard
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.formatCurrency
import com.example.data.model.TransactionType
import androidx.compose.material.icons.filled.Savings
import com.example.data.model.AccountType
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
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var selectedTxAccountAndType by remember { mutableStateOf<Pair<String, String?>?>(null) } // Pair(Type, accountId?)

    val visibleAccounts = remember(accounts) { accounts.filter { !it.isPlanStartAccount } }
    val totalBalance = visibleAccounts.sumOf { it.currentBalance }
    val savingsAccounts = visibleAccounts.filter { it.isSavings || it.type == AccountType.SAVINGS.name }
    val savingsTotal = savingsAccounts.sumOf { it.currentBalance }

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
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Patrimonio en Cuentas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = formatCurrency(totalBalance),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AccentRed)
                            ) {
                                Text("- Egreso Extra", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddAccountDialog = true },
                                modifier = Modifier.weight(1f).testTag("add_account_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nueva Cuenta", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            if (visibleAccounts.size >= 2) {
                                OutlinedButton(
                                    onClick = { showTransferDialog = true },
                                    modifier = Modifier.weight(1f).testTag("transfer_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Transferir", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (savingsAccounts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDark.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldLight)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Ahorros y Colchón (${savingsAccounts.size})", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 14.sp)
                                Text("Cuentas destinadas a reserva", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark, fontSize = 11.sp)
                            }
                        }
                        Text(formatCurrency(savingsTotal), fontWeight = FontWeight.Bold, color = EmeraldLight, fontSize = 16.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Todas las Cuentas (${visibleAccounts.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        if (visibleAccounts.isEmpty()) {
            item {
                Surface(
                    color = SlateCard.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tienes cuentas registradas aún",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddAccountDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Agregar mi primera cuenta")
                        }
                    }
                }
            }
        } else {
            items(visibleAccounts, key = { it.id }) { acc ->
                AccountItemCard(
                    account = acc,
                    onEdit = { accountToEdit = acc },
                    onDelete = { accountToDelete = acc },
                    onAddIncome = {
                        selectedTxAccountAndType = Pair(TransactionType.INCOME.name, acc.id)
                    },
                    onAddExpense = {
                        selectedTxAccountAndType = Pair(TransactionType.EXPENSE.name, acc.id)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
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
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("¿Eliminar cuenta?", fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará la cuenta '${accountToDelete?.name}' de forma permanente.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        accountToDelete?.let { viewModel.deleteAccount(it.id) }
                        accountToDelete = null
                    }
                ) {
                    Text("Eliminar", color = com.example.ui.theme.AccentRed)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showTransferDialog && accounts.size >= 2) {
        AddTransactionDialog(
            initialType = TransactionType.TRANSFER.name,
            accounts = accounts,
            customExpenseCategories = settings.customExpenseCategories,
            customIncomeCategories = settings.customIncomeCategories,
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
            onDismiss = { selectedTxAccountAndType = null },
            onSave = { tx -> viewModel.addTransaction(tx) },
            onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) }
        )
    }
}
