package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.FinanceViewModel
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredTransactions = transactions.filter { tx ->
        val matchesType = when (filterType) {
            "INCOME" -> tx.type == TransactionType.INCOME.name
            "EXPENSE" -> tx.type == TransactionType.EXPENSE.name
            "TRANSFER" -> tx.type == TransactionType.TRANSFER.name
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("transactions_screen"),
        containerColor = SlateBackground,
        floatingActionButton = {
            if (accounts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = TextPrimaryDark,
                    modifier = Modifier.testTag("fab_add_tx")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo movimiento")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Ingresos", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatCurrency(totalIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldLight)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Egresos", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatCurrency(totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AccentRed)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por descripción o categoría...") },
                    modifier = Modifier.fillMaxWidth().testTag("tx_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        "ALL" to "Todos (${transactions.size})",
                        "EXPENSE" to "Egresos",
                        "INCOME" to "Ingresos",
                        "TRANSFER" to "Transferencias"
                    )
                    items(filterOptions) { (key, label) ->
                        val isSelected = filterType == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else SlateCard,
                            modifier = Modifier
                                .clickable { filterType = key }
                                .testTag("filter_$key")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NavySurface else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
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
                                text = if (searchQuery.isNotBlank()) "No se encontraron resultados para '$searchQuery'"
                                       else "Sin movimientos en este filtro",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    val accName = accounts.find { it.id == tx.accountId }?.name ?: "Cuenta"
                    TransactionRow(
                        transaction = tx,
                        accountName = accName,
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showAddDialog && accounts.isNotEmpty()) {
        AddTransactionDialog(
            initialType = if (filterType == "INCOME") TransactionType.INCOME.name else TransactionType.EXPENSE.name,
            accounts = accounts,
            onDismiss = { showAddDialog = false },
            onSave = { tx -> viewModel.addTransaction(tx) }
        )
    }
}
