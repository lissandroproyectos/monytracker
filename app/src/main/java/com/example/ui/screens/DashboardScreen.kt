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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.currency.AppCurrency
import com.example.data.model.Account
import com.example.data.model.ExpenseCommitment
import com.example.data.model.Transaction
import com.example.ui.FinanceViewModel
import com.example.ui.components.BalanceOverviewCard
import com.example.ui.components.CashflowProjectionCard
import com.example.ui.components.CommitmentCard
import com.example.ui.components.ExchangeRatesTickerBar
import com.example.ui.components.SafetyCushionBanner
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCommitments: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNewIncome: () -> Unit,
    onNewExpense: () -> Unit,
    onNewCommitment: () -> Unit,
    onPayCommitment: (ExpenseCommitment) -> Unit,
    onViewInstallments: (ExpenseCommitment) -> Unit,
    onOpenCurrencySettings: () -> Unit,
    onTransferSavings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val health by viewModel.financialHealth.collectAsStateWithLifecycle()
    val rates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val isRefreshingRates by viewModel.isRefreshingRates.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val commitments by viewModel.commitments.collectAsStateWithLifecycle()
    val projectedPoints by viewModel.projectedCashflow.collectAsStateWithLifecycle()
    val projectionMonths by viewModel.projectionMonths.collectAsStateWithLifecycle()

    val activeCommitments = commitments.filter { it.status == "ACTIVE" }
    val recentTransactions = transactions.take(4)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Rates Ticker (Dolar BCV, USDT Paralelo, Euro BCV)
        item {
            ExchangeRatesTickerBar(
                rates = rates,
                isRefreshing = isRefreshingRates,
                onRefresh = { viewModel.refreshRates() },
                displayCurrency = health.displayCurrency,
                paymentCurrency = health.paymentCurrency,
                onOpenCurrencySettings = onOpenCurrencySettings
            )
        }

        // Safety Cushion (Colchón) Health Banner & Surplus Alert
        item {
            SafetyCushionBanner(
                health = health,
                onConfigureCushion = onOpenCurrencySettings,
                onTransferSavings = onTransferSavings
            )
        }

        // Hero Balance Overview
        item {
            BalanceOverviewCard(
                totalBalance = health.totalAvailableInDisplay,
                totalSavings = health.totalSavingsInDisplay,
                totalFixedExpenses = health.totalFixedExpensesInDisplay,
                totalDebts = health.totalDebtsInDisplay,
                currencySymbol = health.displayCurrency.symbol,
                onNewIncomeClick = onNewIncome,
                onNewExpenseClick = onNewExpense,
                onNewCommitmentClick = onNewCommitment
            )
        }

        // Cashflow Projection Graph
        item {
            CashflowProjectionCard(
                points = projectedPoints,
                selectedPeriodMonths = projectionMonths,
                onPeriodSelected = { viewModel.setProjectionPeriodMonths(it) },
                currencySymbol = health.displayCurrency.symbol
            )
        }

        // Accounts section
        if (accounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("starter_accounts_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(EmeraldLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldLight)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Configura tus cuentas multi-moneda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gestiona Bolívares (Pago Móvil/Banco), Dólares BCV, Cripto USDT y Fondo de Ahorro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.createInitialAccountsIfEmpty() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("create_starter_accounts_btn")
                        ) {
                            Text("Generar Cuentas Recomendadas", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mis Cuentas (${accounts.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        TextButton(onClick = onNavigateToAccounts) {
                            Text("Ver todas", color = EmeraldLight)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldLight)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(accounts, key = { it.id }) { acc ->
                            AccountMiniCard(account = acc, onClick = onNavigateToAccounts)
                        }
                    }
                }
            }
        }

        // Active Commitments & Cuotas Section
        if (activeCommitments.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gastos y Cuotas en Curso (${activeCommitments.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    TextButton(onClick = onNavigateToCommitments) {
                        Text("Ver todos", color = EmeraldLight)
                    }
                }
            }

            items(activeCommitments.take(2), key = { it.id }) { comm ->
                CommitmentCard(
                    commitment = comm,
                    onEditClick = { onViewInstallments(comm) },
                    onViewInstallmentsClick = { onViewInstallments(comm) },
                    onPayClick = { onPayCommitment(comm) },
                    onShareAsTemplateClick = {},
                    displayCurrency = health.displayCurrency,
                    rates = rates
                )
            }
        }

        // Recent Movements
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Movimientos Recientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                if (recentTransactions.isNotEmpty()) {
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("Historial", color = EmeraldLight)
                    }
                }
            }
        }

        if (recentTransactions.isEmpty()) {
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
                            text = "No hay movimientos registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
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

@Composable
fun AccountMiniCard(
    account: Account,
    onClick: () -> Unit
) {
    val accCurrency = AppCurrency.fromCode(account.currency)
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        EmeraldLight
    }

    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() }
            .testTag("mini_card_${account.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                )
                Text(
                    text = "${accCurrency.flagEmoji} ${accCurrency.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(account.currentBalance, accCurrency.symbol),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (account.currentBalance >= 0) EmeraldLight else Color(0xFFEF4444),
                fontSize = 13.sp
            )
        }
    }
}
