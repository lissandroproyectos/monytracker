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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.currency.ExchangeRates
import com.example.data.model.CommitmentStatus
import com.example.data.model.ExpenseCommitment
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.FinanceViewModel
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PlannedEvent(
    val title: String,
    val dueDate: Date,
    val amount: Double,
    val currency: AppCurrency,
    val isFixed: Boolean,
    val isIncome: Boolean = false,
    val isPaid: Boolean,
    val installmentInfo: String? = null
)

@Composable
fun CalendarTimelineScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val commitments by viewModel.commitments.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val rates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val health by viewModel.financialHealth.collectAsStateWithLifecycle()

    var selectedMonthOffset by remember { mutableStateOf(0) } // 0 = current month, 1 = next, etc.
    var viewMode by remember { mutableStateOf("MONTHLY") } // "MONTHLY" or "CYCLE"

    val cal = Calendar.getInstance().apply {
        add(Calendar.MONTH, selectedMonthOffset)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val monthStart = cal.time
    val monthEndCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }
    val monthEnd = monthEndCal.time

    val monthName = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(monthStart)
        .replaceFirstChar { it.uppercase() }

    // Build all planned events for this month
    val plannedEvents = mutableListOf<PlannedEvent>()

    // 1. Installments from commitments
    commitments.forEach { c ->
        val commCurr = AppCurrency.fromCode(c.currency)
        c.getProjectedInstallments().forEach { inst ->
            val instDate = inst.dueDate.toDate()
            if (instDate.after(monthStart) && instDate.before(monthEnd)) {
                plannedEvents.add(
                    PlannedEvent(
                        title = c.title,
                        dueDate = instDate,
                        amount = inst.amount,
                        currency = commCurr,
                        isFixed = c.isFixedExpense,
                        isIncome = false,
                        isPaid = inst.isPaid,
                        installmentInfo = "Cuota ${inst.number}/${c.totalInstallments}"
                    )
                )
            }
        }
    }

    // 2. Scheduled or recorded incomes
    transactions.filter { it.type == TransactionType.INCOME.name }.forEach { tx ->
        val txDate = tx.date.toDate()
        if (txDate.after(monthStart) && txDate.before(monthEnd)) {
            plannedEvents.add(
                PlannedEvent(
                    title = tx.description.ifBlank { "Ingreso de Nómina/Venta" },
                    dueDate = txDate,
                    amount = tx.amount,
                    currency = AppCurrency.fromCode(tx.currency),
                    isFixed = false,
                    isIncome = true,
                    isPaid = true
                )
            )
        }
    }

    // 3. Periodic incomes configured on accounts
    accounts.filter { it.hasPeriodicIncome && it.periodicIncomeAmount > 0 }.forEach { acc ->
        val incCurr = AppCurrency.fromCode(acc.periodicIncomeCurrency)
        val step = (cal.clone() as Calendar)
        when (acc.periodicIncomeFrequency) {
            "QUINCENAL" -> {
                val d15 = (step.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 15) }
                if (d15.time.after(monthStart) && d15.time.before(monthEnd)) {
                    plannedEvents.add(
                        PlannedEvent(
                            title = "${acc.periodicIncomeDescription} (${acc.name})",
                            dueDate = d15.time,
                            amount = acc.periodicIncomeAmount,
                            currency = incCurr,
                            isFixed = false,
                            isIncome = true,
                            isPaid = d15.time.before(Date())
                        )
                    )
                }
                val dEnd = (step.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
                if (dEnd.time.after(monthStart) && dEnd.time.before(monthEnd)) {
                    plannedEvents.add(
                        PlannedEvent(
                            title = "${acc.periodicIncomeDescription} (${acc.name})",
                            dueDate = dEnd.time,
                            amount = acc.periodicIncomeAmount,
                            currency = incCurr,
                            isFixed = false,
                            isIncome = true,
                            isPaid = dEnd.time.before(Date())
                        )
                    )
                }
            }
            "SEMANAL" -> {
                val w = (step.clone() as Calendar)
                while (w.time.before(monthEnd)) {
                    if (w.time.after(monthStart)) {
                        plannedEvents.add(
                            PlannedEvent(
                                title = "${acc.periodicIncomeDescription} (${acc.name})",
                                dueDate = w.time,
                                amount = acc.periodicIncomeAmount,
                                currency = incCurr,
                                isFixed = false,
                                isIncome = true,
                                isPaid = w.time.before(Date())
                            )
                        )
                    }
                    w.add(Calendar.DAY_OF_YEAR, 7)
                }
            }
            else -> { // MENSUAL
                val day = acc.periodicIncomeDayOfMonth.coerceIn(1, step.getActualMaximum(Calendar.DAY_OF_MONTH))
                val dMonth = (step.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                if (dMonth.time.after(monthStart) && dMonth.time.before(monthEnd)) {
                    plannedEvents.add(
                        PlannedEvent(
                            title = "${acc.periodicIncomeDescription} (${acc.name})",
                            dueDate = dMonth.time,
                            amount = acc.periodicIncomeAmount,
                            currency = incCurr,
                            isFixed = false,
                            isIncome = true,
                            isPaid = dMonth.time.before(Date())
                        )
                    )
                }
            }
        }
    }

    plannedEvents.sortBy { it.dueDate }

    // "De un ingreso al siguiente": cycle computation
    val incomeEvents = plannedEvents.filter { it.isIncome }
    val nextExpectedIncome = incomeEvents.firstOrNull { it.dueDate.after(Date()) }

    val pendingBeforeNextIncome = if (nextExpectedIncome != null) {
        plannedEvents.filter { !it.isIncome && !it.isPaid && it.dueDate.before(nextExpectedIncome.dueDate) }
    } else {
        plannedEvents.filter { !it.isIncome && !it.isPaid }
    }

    val totalPendingInCycle = pendingBeforeNextIncome.sumOf {
        rates.convert(it.amount, it.currency, health.displayCurrency)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("calendar_timeline_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month navigator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedMonthOffset-- }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Mes anterior", tint = EmeraldLight)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = monthName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        IconButton(onClick = { selectedMonthOffset++ }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Mes siguiente", tint = EmeraldLight)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // View Mode Toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (viewMode == "MONTHLY") EmeraldPrimary else SlateBackground,
                            modifier = Modifier.weight(1f).clickable { viewMode = "MONTHLY" }
                        ) {
                            Text(
                                text = "Lista Mensual",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == "MONTHLY") NavySurface else TextSecondaryDark,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (viewMode == "CYCLE") AccentAmber else SlateBackground,
                            modifier = Modifier.weight(1.3f).clickable { viewMode = "CYCLE" }
                        ) {
                            Text(
                                text = "Ciclo Entre Ingresos",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == "CYCLE") NavySurface else TextSecondaryDark,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (viewMode == "CYCLE") {
            // "De un ingreso al siguiente" special cycle view
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventRepeat, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pendiente antes del Próximo Ingreso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (nextExpectedIncome != null)
                                "Próximo cobro previsto: ${formatDate(nextExpectedIncome.dueDate)} (${formatCurrency(nextExpectedIncome.amount, nextExpectedIncome.currency.symbol)})"
                            else "Planifica tus compromisos de este ciclo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Compromisos a pagar en este ciclo:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = formatCurrency(totalPendingInCycle, health.displayCurrency.symbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                        }
                    }
                }
            }

            if (pendingBeforeNextIncome.isEmpty()) {
                item {
                    Surface(
                        color = SlateCard.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No tienes pagos pendientes antes de tu próximo ingreso 🎉", style = MaterialTheme.typography.bodyMedium, color = EmeraldLight)
                        }
                    }
                }
            } else {
                items(pendingBeforeNextIncome) { event ->
                    PlannedEventRow(event = event, displayCurrency = health.displayCurrency, rates = rates)
                }
            }
        } else {
            // General Monthly List
            if (plannedEvents.isEmpty()) {
                item {
                    Surface(
                        color = SlateCard.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No hay movimientos ni cuotas registradas para este mes", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                        }
                    }
                }
            } else {
                items(plannedEvents) { event ->
                    PlannedEventRow(event = event, displayCurrency = health.displayCurrency, rates = rates)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun PlannedEventRow(
    event: PlannedEvent,
    displayCurrency: AppCurrency,
    rates: ExchangeRates
) {
    val amountInDisp = rates.convert(event.amount, event.currency, displayCurrency)
    val icon = if (event.isIncome) Icons.Default.TrendingUp else if (event.isFixed) Icons.Default.CalendarMonth else Icons.Default.CreditCard
    val iconColor = if (event.isIncome) EmeraldLight else if (event.isFixed) AccentAmber else AccentRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            fontSize = 14.sp
                        )
                        if (event.installmentInfo != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SlateCardLight,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = event.installmentInfo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldLight,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${formatDate(event.dueDate)} • ${if (event.isIncome) "Ingreso" else if (event.isFixed) "Gasto Fijo" else "Deuda/Cuota"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (event.isIncome) "+" else "-"}${formatCurrency(event.amount, event.currency.symbol)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (event.isIncome) EmeraldLight else TextPrimaryDark,
                    fontSize = 13.sp
                )
                if (event.currency != displayCurrency) {
                    Text(
                        text = "≈ ${formatCurrency(amountInDisp, displayCurrency.symbol)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontSize = 10.sp
                    )
                }
                if (event.isPaid) {
                    Text(
                        text = "PAGADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
