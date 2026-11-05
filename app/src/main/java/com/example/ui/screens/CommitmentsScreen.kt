package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.currency.AppCurrency
import com.example.data.model.CommitmentStatus
import com.example.data.model.ExpenseCommitment
import com.example.ui.FinanceViewModel
import com.example.ui.components.AddCommitmentDialog
import com.example.ui.components.CommitmentCard
import com.example.ui.components.PayCommitmentDialog
import com.example.ui.components.ShareTemplateDialog
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

enum class CommitmentSortOption(val displayName: String) {
    START_DATE_DESC("Fecha Inicio (Reciente primero)"),
    START_DATE_ASC("Fecha Inicio (Antigua primero)"),
    REMAINING_DESC("Mayor Pago Pendiente"),
    REMAINING_ASC("Menor Pago Pendiente"),
    PAID_DESC("Mayor Pago Realizado"),
    NAME_ASC("Nombre (A-Z)")
}

@Composable
fun CommitmentsScreen(
    viewModel: FinanceViewModel,
    onViewInstallments: (ExpenseCommitment) -> Unit,
    onOpenDebtTypes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val commitments by viewModel.commitments.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val health by viewModel.financialHealth.collectAsStateWithLifecycle()
    val rates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf("DEBTS") } // DEBTS, FIXED, ALL, PAID_OFF
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(CommitmentSortOption.START_DATE_DESC) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var commitmentToEdit by remember { mutableStateOf<ExpenseCommitment?>(null) }
    var commitmentToDelete by remember { mutableStateOf<ExpenseCommitment?>(null) }
    var commitmentToPay by remember { mutableStateOf<ExpenseCommitment?>(null) }
    var commitmentToShare by remember { mutableStateOf<ExpenseCommitment?>(null) }

    val filteredAndSortedCommitments = remember(commitments, filterType, searchQuery, sortOption) {
        commitments.filter { comm ->
            val matchesFilter = when (filterType) {
                "DEBTS" -> !comm.isFixedExpense && comm.status == CommitmentStatus.ACTIVE.name
                "FIXED" -> comm.isFixedExpense && comm.status == CommitmentStatus.ACTIVE.name
                "PAID_OFF" -> comm.status == CommitmentStatus.PAID_OFF.name
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                comm.title.contains(searchQuery, ignoreCase = true) ||
                comm.category.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }.sortedWith { c1, c2 ->
            when (sortOption) {
                CommitmentSortOption.START_DATE_DESC -> c2.startDate.compareTo(c1.startDate)
                CommitmentSortOption.START_DATE_ASC -> c1.startDate.compareTo(c2.startDate)
                CommitmentSortOption.REMAINING_DESC -> c2.remainingAmount.compareTo(c1.remainingAmount)
                CommitmentSortOption.REMAINING_ASC -> c1.remainingAmount.compareTo(c2.remainingAmount)
                CommitmentSortOption.PAID_DESC -> {
                    val paid1 = (c1.totalAmount - c1.remainingAmount).coerceAtLeast(0.0)
                    val paid2 = (c2.totalAmount - c2.remainingAmount).coerceAtLeast(0.0)
                    paid2.compareTo(paid1)
                }
                CommitmentSortOption.NAME_ASC -> c1.title.lowercase().compareTo(c2.title.lowercase())
            }
        }
    }

    val totalDebts = commitments.filter { !it.isFixedExpense && it.status == CommitmentStatus.ACTIVE.name }
        .sumOf { rates.convert(it.remainingAmount, AppCurrency.fromCode(it.currency), health.displayCurrency) }

    val totalFixed = commitments.filter { it.isFixedExpense && it.status == CommitmentStatus.ACTIVE.name }
        .sumOf { c ->
            val recurringAmt = if (c.installmentAmount > 0) c.installmentAmount else (if (c.totalInstallments > 0) c.totalAmount / c.totalInstallments else c.totalAmount)
            rates.convert(recurringAmt, AppCurrency.fromCode(c.currency), health.displayCurrency)
        }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("commitments_screen"),
        containerColor = SlateBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = TextPrimaryDark,
                modifier = Modifier.testTag("fab_add_commitment")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo compromiso")
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
            // Header Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Column {
                            Text(
                                text = "Control Financiero",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "Gastos Fijos vs Deudas",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Two distinct cards: Deudas vs Gastos Fijos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = if (filterType == "DEBTS") AccentRed.copy(alpha = 0.1f) else SlateBackground,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { filterType = "DEBTS" }
                                    .border(
                                        width = if (filterType == "DEBTS") 1.5.dp else 0.dp,
                                        color = if (filterType == "DEBTS") AccentRed else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Deudas y Cuotas", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatCurrency(totalDebts, health.displayCurrency.symbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentRed
                                    )
                                    Text("Pasivos temporales", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 9.sp)
                                }
                            }

                            Surface(
                                color = if (filterType == "FIXED") AccentAmber.copy(alpha = 0.1f) else SlateBackground,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { filterType = "FIXED" }
                                    .border(
                                        width = if (filterType == "FIXED") 1.5.dp else 0.dp,
                                        color = if (filterType == "FIXED") AccentAmber else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gastos Fijos", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatCurrency(totalFixed, health.displayCurrency.symbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentAmber
                                    )
                                    Text("Egresos recurrentes", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Search & Sort Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por título o categoría...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = TextSecondaryDark)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("commitments_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Sort Dropdown Button
                    Box {
                        Surface(
                            modifier = Modifier
                                .clickable { sortDropdownExpanded = true }
                                .testTag("commitments_sort_btn"),
                            shape = RoundedCornerShape(12.dp),
                            color = SlateCard
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = EmeraldLight, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ordenar", style = MaterialTheme.typography.labelMedium, color = EmeraldLight, fontWeight = FontWeight.Bold)
                            }
                        }

                        DropdownMenu(
                            expanded = sortDropdownExpanded,
                            onDismissRequest = { sortDropdownExpanded = false }
                        ) {
                            CommitmentSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == option) EmeraldLight else TextPrimaryDark
                                        )
                                    },
                                    onClick = {
                                        sortOption = option
                                        sortDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Segment Tabs (Deudas, Gastos Fijos, Saldados, Todos)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filterOptions = listOf(
                        "DEBTS" to "💳 Deudas (${commitments.count { !it.isFixedExpense && it.status == CommitmentStatus.ACTIVE.name }})",
                        "FIXED" to "🏠 Gastos Fijos (${commitments.count { it.isFixedExpense && it.status == CommitmentStatus.ACTIVE.name }})",
                        "PAID_OFF" to "✅ Saldados",
                        "ALL" to "Todos (${commitments.size})"
                    )
                    items(filterOptions) { (key, label) ->
                        val isSelected = filterType == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else SlateCard,
                            modifier = Modifier
                                .clickable { filterType = key }
                                .testTag("filter_comm_$key")
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

            if (filteredAndSortedCommitments.isEmpty()) {
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
                                text = if (searchQuery.isNotEmpty()) "No se encontraron compromisos que coincidan con \"$searchQuery\""
                                else if (filterType == "DEBTS") "No tienes deudas activas registradas"
                                else "No hay compromisos en esta sección",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (filterType == "DEBTS") "+ Registrar Nueva Deuda o Cuotas" else "+ Registrar Gasto Fijo")
                            }
                        }
                    }
                }
            } else {
                items(filteredAndSortedCommitments, key = { it.id }) { comm ->
                    CommitmentCard(
                        commitment = comm,
                        onEditClick = { commitmentToEdit = comm },
                        onViewInstallmentsClick = { commitmentToEdit = comm },
                        onPayClick = { commitmentToPay = comm },
                        onShareAsTemplateClick = { commitmentToShare = comm },
                        onDeleteClick = { commitmentToDelete = comm },
                        displayCurrency = health.displayCurrency,
                        rates = rates
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCommitmentDialog(
            accounts = accounts,
            templates = emptyList(),
            customCategories = settings.customExpenseCategories,
            initialIsFixed = (filterType == "FIXED"),
            onDismiss = { showAddDialog = false },
            onSave = { newCommitment ->
                viewModel.addCommitment(newCommitment)
                showAddDialog = false
            },
            onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) }
        )
    }

    commitmentToEdit?.let { comm ->
        AddCommitmentDialog(
            accounts = accounts,
            templates = emptyList(),
            customCategories = settings.customExpenseCategories,
            commitmentToEdit = comm,
            onDismiss = { commitmentToEdit = null },
            onSave = { updated ->
                viewModel.updateCommitment(updated)
                commitmentToEdit = null
            },
            onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) },
            onDelete = {
                viewModel.deleteCommitment(comm.id)
                commitmentToEdit = null
            }
        )
    }

    commitmentToDelete?.let { comm ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { commitmentToDelete = null },
            title = { Text("¿Eliminar compromiso?", fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará '${comm.title}' y su historial.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCommitment(comm.id)
                        commitmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { commitmentToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    commitmentToPay?.let { comm ->
        PayCommitmentDialog(
            commitment = comm,
            accounts = accounts,
            onDismiss = { commitmentToPay = null },
            onConfirm = { payment, account ->
                viewModel.payCommitment(payment, comm, account)
            }
        )
    }

    commitmentToShare?.let { comm ->
        ShareTemplateDialog(
            commitment = comm,
            onDismiss = { commitmentToShare = null },
            onPublish = { template ->
                viewModel.publishMarketplaceTemplate(template)
            }
        )
    }
}
