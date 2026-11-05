package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.currency.AppCurrency
import com.example.data.currency.ExchangeRates
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.CommitmentStatus
import com.example.data.model.ExpenseCommitment
import com.example.data.model.Frequency
import com.example.data.model.PaymentMode
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.FinancialHealth
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double, currency: String = "$"): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "$currency ${formatter.format(amount)}"
}

fun formatDate(date: Date?): String {
    if (date == null) return ""
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es-ES"))
    return sdf.format(date)
}

// Live Exchange Rates Ticker Bar
@Composable
fun ExchangeRatesTickerBar(
    rates: ExchangeRates,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    displayCurrency: AppCurrency,
    paymentCurrency: AppCurrency,
    onOpenCurrencySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NavySurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenCurrencySettings() }
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ver: ${displayCurrency.symbol} • Pagar: ${paymentCurrency.symbol}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rates.lastUpdated,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontSize = 10.sp
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(24.dp).testTag("refresh_rates_btn")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = EmeraldLight,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar tasas",
                                tint = EmeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3 Exchange Rate Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RateChip(
                    label = "Dólar BCV",
                    rateText = "Bs. ${String.format(Locale.US, "%.2f", rates.dolarBcv)}",
                    emoji = "🏛️",
                    modifier = Modifier.weight(1f)
                )
                RateChip(
                    label = "USDT Paralelo",
                    rateText = "Bs. ${String.format(Locale.US, "%.2f", rates.usdtParalelo)}",
                    emoji = "₮",
                    modifier = Modifier.weight(1f)
                )
                RateChip(
                    label = "Euro BCV",
                    rateText = "Bs. ${String.format(Locale.US, "%.2f", rates.euroBcv)}",
                    emoji = "💶",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RateChip(
    label: String,
    rateText: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateBackground,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$emoji $label",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                fontSize = 10.sp
            )
            Text(
                text = rateText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldLight,
                fontSize = 11.sp
            )
        }
    }
}

// Colchón de Seguridad & Financial Health Banner
@Composable
fun SafetyCushionBanner(
    health: FinancialHealth,
    onConfigureCushion: () -> Unit,
    onTransferSavings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBreached = health.isSafetyCushionBreached
    val bannerColor = if (isBreached) AccentRed else EmeraldDark
    val icon = if (isBreached) Icons.Default.Warning else Icons.Default.Security

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("safety_cushion_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBreached) Color(0xFF450A0A) else SlateCard
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isBreached) AccentRed.copy(alpha = 0.2f) else EmeraldLight.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isBreached) AccentRed else EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isBreached) "¡Alerta: Colchón de Seguridad en Riesgo!" else "Colchón de Seguridad Protegido",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isBreached) AccentRed else TextPrimaryDark,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Meta recomendada: ${formatCurrency(health.safetyCushionAmount, health.displayCurrency.symbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                TextButton(onClick = onConfigureCushion) {
                    Text("Ajustar", color = EmeraldLight, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isBreached) {
                Text(
                    text = "Tu dinero disponible (${formatCurrency(health.totalAvailableInDisplay, health.displayCurrency.symbol)}) es inferior al colchón de emergencia. Si necesitas cubrir pagos, puedes disponer de tu fondo de ahorros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimaryDark,
                    fontSize = 12.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Excedente proyectado para destinar a ahorros:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = formatCurrency(health.surplusAvailableForSavings, health.displayCurrency.symbol),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTransferSavings,
                    modifier = Modifier.fillMaxWidth().testTag("transfer_savings_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBreached) "Disponer de Ahorros de Emergencia" else "Apartar Excedente a Ahorros",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Balance Overview Hero Card with Currency Switching
@Composable
fun BalanceOverviewCard(
    totalBalance: Double,
    totalSavings: Double,
    totalFixedExpenses: Double,
    totalDebts: Double,
    currencySymbol: String = "$",
    onNewIncomeClick: () -> Unit,
    onNewExpenseClick: () -> Unit,
    onNewCommitmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("balance_overview_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dinero Disponible Líquido",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = formatCurrency(totalBalance, currencySymbol),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalBalance >= 0) EmeraldLight else AccentRed
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(EmeraldLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats: Gastos Fijos vs Deudas Temporales vs Fondo de Ahorro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill(
                        label = "Gastos Fijos",
                        amount = totalFixedExpenses,
                        symbol = currencySymbol,
                        color = AccentAmber,
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = "Deudas / Cuotas",
                        amount = totalDebts,
                        symbol = currencySymbol,
                        color = AccentRed,
                        icon = Icons.Default.CreditCard,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = "Fondo Ahorro",
                        amount = totalSavings,
                        symbol = currencySymbol,
                        color = EmeraldLight,
                        icon = Icons.Default.Savings,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNewIncomeClick,
                        modifier = Modifier.weight(1f).testTag("quick_add_income_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ingreso", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onNewExpenseClick,
                        modifier = Modifier.weight(1f).testTag("quick_add_expense_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Egreso", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onNewCommitmentClick,
                        modifier = Modifier.weight(1.2f).testTag("quick_add_commitment_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309))
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Plan/Deuda", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    amount: Double,
    symbol: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateCardLight.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 9.sp)
            Text(
                text = formatCurrency(amount, symbol),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 11.sp
            )
        }
    }
}

// Account Card with currency badge and savings indicator
@Composable
fun AccountItemCard(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accCurrency = AppCurrency.fromCode(account.currency)
    val icon = when (account.type) {
        AccountType.BANK.name -> Icons.Default.AccountBalance
        AccountType.CREDIT_CARD.name -> Icons.Default.CreditCard
        AccountType.DIGITAL_WALLET.name -> Icons.Default.AccountBalanceWallet
        AccountType.SAVINGS.name -> Icons.Default.Savings
        else -> Icons.Default.Payment
    }

    val parsedColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        EmeraldLight
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_card_${account.id}")
            .clickable { onEdit() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(parsedColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = parsedColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        if (account.isSavings) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = EmeraldDark.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Ahorro",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldLight,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${accCurrency.flagEmoji} ${accCurrency.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )

                    if (account.hasPeriodicIncome && account.periodicIncomeAmount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = EmeraldLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${formatCurrency(account.periodicIncomeAmount, AppCurrency.fromCode(account.periodicIncomeCurrency).symbol)} (${account.periodicIncomeFrequency.lowercase().replaceFirstChar { it.uppercase() }})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(account.currentBalance, accCurrency.symbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (account.currentBalance >= 0) TextPrimaryDark else AccentRed
                    )
                    Text(
                        text = "Inicial: ${formatCurrency(account.initialBalance, accCurrency.symbol)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_account_${account.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar cuenta",
                        tint = TextSecondaryDark.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_account_${account.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar cuenta",
                        tint = AccentRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Transaction Row with currency badge
@Composable
fun TransactionRow(
    transaction: Transaction,
    accountName: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME.name
    val isTransfer = transaction.type == TransactionType.TRANSFER.name
    val sign = if (isIncome) "+" else "-"
    val amountColor = if (isIncome) EmeraldLight else if (isTransfer) AccentAmber else AccentRed
    val icon = if (isIncome) Icons.Default.ArrowDownward else if (isTransfer) Icons.Default.SwapHoriz else Icons.Default.ArrowUpward
    val txCurrency = AppCurrency.fromCode(transaction.currency)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(amountColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = amountColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.description.ifBlank { transaction.category },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        if (transaction.isFixedExpense) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AccentAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Fijo",
                                    color = AccentAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "$accountName • ${transaction.category} • ${formatDate(transaction.date.toDate())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$sign${formatCurrency(transaction.amount, txCurrency.symbol)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    fontSize = 14.sp
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar movimiento",
                        tint = TextSecondaryDark.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Commitment Card with Projected Cuotas & Details
@Composable
fun CommitmentCard(
    commitment: ExpenseCommitment,
    onPayClick: () -> Unit = {},
    onViewInstallmentsClick: () -> Unit = {},
    onShareAsTemplateClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: (() -> Unit)? = null,
    displayCurrency: AppCurrency = AppCurrency.USD_BCV,
    rates: ExchangeRates? = null,
    modifier: Modifier = Modifier
) {
    val isPaidOff = commitment.status == CommitmentStatus.PAID_OFF.name
    val origCurrency = AppCurrency.fromCode(commitment.currency)
    val badgeColor = if (commitment.isFixedExpense) AccentAmber else EmeraldLight

    val convertedTotal = rates?.convert(commitment.totalAmount, origCurrency, displayCurrency) ?: commitment.totalAmount
    val freqLabel = Frequency.entries.find { it.name == commitment.frequency }?.displayName ?: commitment.frequency

    val endDateStr = if (commitment.endDate != null) {
        "Fin: " + java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(commitment.endDate.toDate())
    } else {
        "Sin fecha fin definida"
    }

    val handleCardClick = {
        if (onEditClick != null) {
            onEditClick()
        } else {
            onViewInstallmentsClick()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("commitment_card_${commitment.id}")
            .clickable { handleCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Category, Type badge, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (commitment.isFixedExpense) "Gasto Fijo" else "Deuda Temporal",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${commitment.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }

                Surface(
                    color = if (isPaidOff) EmeraldDark.copy(alpha = 0.4f) else SlateCardLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPaidOff) "SALDADO" else "ACTIVO",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaidOff) EmeraldLight else AccentAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = commitment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Amount, Paid, Remaining & Frequency Info
            val convertedPaid = rates?.convert((commitment.totalAmount - commitment.remainingAmount).coerceAtLeast(0.0), origCurrency, displayCurrency) 
                ?: (commitment.totalAmount - commitment.remainingAmount).coerceAtLeast(0.0)
            val convertedRemaining = rates?.convert(commitment.remainingAmount, origCurrency, displayCurrency) 
                ?: commitment.remainingAmount

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatCurrency(convertedTotal, displayCurrency.symbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    if (displayCurrency != origCurrency) {
                        Text(
                            text = "Original: ${formatCurrency(commitment.totalAmount, origCurrency.symbol)} ${origCurrency.code}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = commitment.getScheduleSummary(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "📅 " + commitment.getNextPaymentDateText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontSize = 10.sp
                    )
                }
            }

            if (!commitment.isFixedExpense || commitment.totalAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { commitment.progressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isPaidOff) EmeraldLight else EmeraldPrimary,
                    trackColor = SlateCardLight
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Breakdown: Pagado vs Falta por pagar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pagado: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatCurrency(convertedPaid, displayCurrency.symbol),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Falta: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatCurrency(convertedRemaining, displayCurrency.symbol),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (convertedRemaining <= 0.001) EmeraldLight else AccentAmber,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
