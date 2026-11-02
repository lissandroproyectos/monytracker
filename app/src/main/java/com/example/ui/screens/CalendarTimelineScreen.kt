package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.currency.AppCurrency
import com.example.data.currency.ExchangeRates
import com.example.data.model.Account
import com.example.data.model.CommitmentStatus
import com.example.data.model.ExpenseCommitment
import com.example.data.model.Frequency
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
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PlannedEvent(
    val id: String = "",
    val title: String,
    val dueDate: Date,
    val amount: Double,
    val currency: AppCurrency,
    val isFixed: Boolean,
    val isIncome: Boolean = false,
    val isPaid: Boolean,
    val installmentInfo: String? = null,
    val commitment: ExpenseCommitment? = null,
    val installmentNumber: Long? = null
)

@Composable
fun CalendarTimelineScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val commitments by viewModel.commitments.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val rates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val health by viewModel.financialHealth.collectAsStateWithLifecycle()

    // Plan range: Start and End Dates
    var startDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        )
    }

    var endDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time
        )
    }

    // Filter status: ALL, PENDING, PAID
    var statusFilter by remember { mutableStateOf("ALL") }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) }

    fun showPicker(current: Date, onSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance().apply { time = current }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val sel = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time
                onSelected(sel)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Dynamic calculation of all planned events within [startDate, endDate]
    val plannedEvents = remember(commitments, transactions, accounts, startDate, endDate) {
        val events = mutableListOf<PlannedEvent>()
        val startMs = startDate.time
        val endMs = endDate.time

        // 1. Commitments (Debts & Fixed Expenses)
        commitments.forEach { c ->
            val commCurr = AppCurrency.fromCode(c.currency)
            if (!c.isFixedExpense) {
                // Installment Debts
                c.getProjectedInstallments().forEach { inst ->
                    val instDate = inst.dueDate.toDate()
                    if (instDate.time in startMs..endMs) {
                        events.add(
                            PlannedEvent(
                                id = "comm_${c.id}_${inst.number}",
                                title = c.title,
                                dueDate = instDate,
                                amount = inst.amount,
                                currency = commCurr,
                                isFixed = false,
                                isIncome = false,
                                isPaid = inst.isPaid,
                                installmentInfo = "Cuota ${inst.number}/${c.totalInstallments}",
                                commitment = c,
                                installmentNumber = inst.number
                            )
                        )
                    }
                }
            } else {
                // Fixed Expenses recurring projection
                val freq = Frequency.entries.find { it.name == c.frequency } ?: Frequency.MONTHLY
                val baseCal = Calendar.getInstance().apply { time = c.dueDate.toDate() }
                val startRangeCal = Calendar.getInstance().apply { time = startDate }
                val endRangeCal = Calendar.getInstance().apply { time = endDate }

                // Determine recurring step
                val iterCal = Calendar.getInstance().apply {
                    time = baseCal.time
                    while (time.before(startDate)) {
                        when (freq) {
                            Frequency.WEEKLY -> add(Calendar.DAY_OF_YEAR, 7)
                            Frequency.BIWEEKLY -> add(Calendar.DAY_OF_YEAR, 15)
                            Frequency.MONTHLY -> add(Calendar.MONTH, 1)
                            Frequency.CUSTOM -> add(Calendar.DAY_OF_YEAR, c.customDays.coerceAtLeast(1))
                        }
                    }
                }

                while (!iterCal.time.after(endDate)) {
                    val occurrenceDate = iterCal.time
                    if (occurrenceDate.time in startMs..endMs) {
                        val isPaidInCycle = occurrenceDate.before(Date()) && c.status == CommitmentStatus.PAID_OFF.name
                        events.add(
                            PlannedEvent(
                                id = "fixed_${c.id}_${occurrenceDate.time}",
                                title = c.title,
                                dueDate = occurrenceDate,
                                amount = if (c.installmentAmount > 0) c.installmentAmount else c.totalAmount,
                                currency = commCurr,
                                isFixed = true,
                                isIncome = false,
                                isPaid = isPaidInCycle,
                                installmentInfo = "Gasto Fijo",
                                commitment = c,
                                installmentNumber = null
                            )
                        )
                    }
                    when (freq) {
                        Frequency.WEEKLY -> iterCal.add(Calendar.DAY_OF_YEAR, 7)
                        Frequency.BIWEEKLY -> iterCal.add(Calendar.DAY_OF_YEAR, 15)
                        Frequency.MONTHLY -> iterCal.add(Calendar.MONTH, 1)
                        Frequency.CUSTOM -> iterCal.add(Calendar.DAY_OF_YEAR, c.customDays.coerceAtLeast(1))
                    }
                }
            }
        }

        // 2. Periodic Incomes configured on Accounts
        accounts.filter { it.hasPeriodicIncome && it.periodicIncomeAmount > 0 }.forEach { acc ->
            val incCurr = AppCurrency.fromCode(acc.periodicIncomeCurrency)
            val iterCal = Calendar.getInstance().apply { time = startDate }

            while (!iterCal.time.after(endDate)) {
                when (acc.periodicIncomeFrequency) {
                    "QUINCENAL" -> {
                        val d15 = (iterCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 15) }
                        if (d15.time.time in startMs..endMs) {
                            events.add(
                                PlannedEvent(
                                    id = "inc_quin_15_${acc.id}_${d15.timeInMillis}",
                                    title = "${acc.periodicIncomeDescription.ifBlank { "Ingreso Quincenal" }} (${acc.name})",
                                    dueDate = d15.time,
                                    amount = acc.periodicIncomeAmount,
                                    currency = incCurr,
                                    isFixed = false,
                                    isIncome = true,
                                    isPaid = d15.time.before(Date())
                                )
                            )
                        }
                        val dEnd = (iterCal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        }
                        if (dEnd.time.time in startMs..endMs) {
                            events.add(
                                PlannedEvent(
                                    id = "inc_quin_end_${acc.id}_${dEnd.timeInMillis}",
                                    title = "${acc.periodicIncomeDescription.ifBlank { "Ingreso Quincenal" }} (${acc.name})",
                                    dueDate = dEnd.time,
                                    amount = acc.periodicIncomeAmount,
                                    currency = incCurr,
                                    isFixed = false,
                                    isIncome = true,
                                    isPaid = dEnd.time.before(Date())
                                )
                            )
                        }
                        iterCal.add(Calendar.MONTH, 1)
                    }
                    "SEMANAL" -> {
                        if (iterCal.time.time in startMs..endMs) {
                            events.add(
                                PlannedEvent(
                                    id = "inc_sem_${acc.id}_${iterCal.timeInMillis}",
                                    title = "${acc.periodicIncomeDescription.ifBlank { "Ingreso Semanal" }} (${acc.name})",
                                    dueDate = iterCal.time,
                                    amount = acc.periodicIncomeAmount,
                                    currency = incCurr,
                                    isFixed = false,
                                    isIncome = true,
                                    isPaid = iterCal.time.before(Date())
                                )
                            )
                        }
                        iterCal.add(Calendar.DAY_OF_YEAR, 7)
                    }
                    else -> { // MENSUAL
                        val day = acc.periodicIncomeDayOfMonth.coerceIn(1, iterCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        val dMonth = (iterCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                        if (dMonth.time.time in startMs..endMs) {
                            events.add(
                                PlannedEvent(
                                    id = "inc_mes_${acc.id}_${dMonth.timeInMillis}",
                                    title = "${acc.periodicIncomeDescription.ifBlank { "Ingreso Mensual" }} (${acc.name})",
                                    dueDate = dMonth.time,
                                    amount = acc.periodicIncomeAmount,
                                    currency = incCurr,
                                    isFixed = false,
                                    isIncome = true,
                                    isPaid = dMonth.time.before(Date())
                                )
                            )
                        }
                        iterCal.add(Calendar.MONTH, 1)
                    }
                }
            }
        }

        // 3. Transactions recorded in this range
        transactions.forEach { tx ->
            val txDate = tx.date.toDate()
            if (txDate.time in startMs..endMs) {
                val isInc = tx.type == TransactionType.INCOME.name
                val txCurr = AppCurrency.fromCode(tx.currency)
                events.add(
                    PlannedEvent(
                        id = "tx_${tx.id}",
                        title = tx.description.ifBlank { if (isInc) "Ingreso Registrado" else "Egreso Registrado" },
                        dueDate = txDate,
                        amount = tx.amount,
                        currency = txCurr,
                        isFixed = tx.isFixedExpense,
                        isIncome = isInc,
                        isPaid = true,
                        installmentInfo = if (isInc) "Cobrado" else "Pagado"
                    )
                )
            }
        }

        // Distinct by ID and sort by dueDate
        events.distinctBy { it.id }.sortedBy { it.dueDate }
    }

    // Filtered events by status (ALL, PENDING, PAID)
    val filteredEvents = remember(plannedEvents, statusFilter) {
        when (statusFilter) {
            "PENDING" -> plannedEvents.filter { !it.isPaid }
            "PAID" -> plannedEvents.filter { it.isPaid }
            else -> plannedEvents
        }
    }

    // Totals in display currency
    val totalIncome = plannedEvents.filter { it.isIncome }.sumOf {
        rates.convert(it.amount, it.currency, health.displayCurrency)
    }
    val totalExpenses = plannedEvents.filter { !it.isIncome }.sumOf {
        rates.convert(it.amount, it.currency, health.displayCurrency)
    }
    val netBalance = totalIncome - totalExpenses

    val totalPendingExpenses = plannedEvents.filter { !it.isIncome && !it.isPaid }.sumOf {
        rates.convert(it.amount, it.currency, health.displayCurrency)
    }
    val totalPaidExpenses = plannedEvents.filter { !it.isIncome && it.isPaid }.sumOf {
        rates.convert(it.amount, it.currency, health.displayCurrency)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("calendar_timeline_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Plan Header & Range Selection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Plan de Pagos & Flujo de Caja",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }

                    Text(
                        text = "Configura el inicio y fin para calcular ingresos y egresos previstos:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )

                    // Start and End Date buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SlateCardLight,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showPicker(startDate) { newStart ->
                                        startDate = newStart
                                        if (endDate.before(newStart)) {
                                            endDate = Calendar.getInstance().apply {
                                                time = newStart
                                                add(Calendar.MONTH, 1)
                                            }.time
                                        }
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Fecha Inicio", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(dateFormat.format(startDate), fontWeight = FontWeight.Bold, color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SlateCardLight,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showPicker(endDate) { newEnd ->
                                        endDate = newEnd
                                        if (startDate.after(newEnd)) {
                                            startDate = Calendar.getInstance().apply {
                                                time = newEnd
                                                add(Calendar.MONTH, -1)
                                            }.time
                                        }
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Fecha Fin", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(dateFormat.format(endDate), fontWeight = FontWeight.Bold, color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Quick range presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            modifier = Modifier.weight(1f).clickable {
                                startDate = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                }.time
                                endDate = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                }.time
                            }
                        ) {
                            Text("Este Mes", modifier = Modifier.padding(vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = EmeraldLight, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            modifier = Modifier.weight(1f).clickable {
                                startDate = Calendar.getInstance().time
                                endDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 15) }.time
                            }
                        ) {
                            Text("15 Días", modifier = Modifier.padding(vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            modifier = Modifier.weight(1f).clickable {
                                startDate = Calendar.getInstance().time
                                endDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }.time
                            }
                        ) {
                            Text("30 Días", modifier = Modifier.padding(vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            modifier = Modifier.weight(1f).clickable {
                                startDate = Calendar.getInstance().time
                                endDate = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.time
                            }
                        ) {
                            Text("3 Meses", modifier = Modifier.padding(vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Summary Card for the Period
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ingresos en Periodo", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Text(
                                text = "+${formatCurrency(totalIncome, health.displayCurrency.symbol)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Egresos en Periodo", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Text(
                                text = "-${formatCurrency(totalExpenses, health.displayCurrency.symbol)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Saldo Estimado", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Text(
                                text = formatCurrency(netBalance, health.displayCurrency.symbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) EmeraldLight else AccentAmber
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pendiente por pagar: ${formatCurrency(totalPendingExpenses, health.displayCurrency.symbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Listo: ${formatCurrency(totalPaidExpenses, health.displayCurrency.symbol)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldLight
                            )
                        }
                    }
                }
            }
        }

        // Filter chips: Todos, Pendientes, Listos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (statusFilter == "ALL") EmeraldPrimary else SlateCard,
                    modifier = Modifier.weight(1f).clickable { statusFilter = "ALL" }
                ) {
                    Text(
                        text = "Todos (${plannedEvents.size})",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (statusFilter == "ALL") NavySurface else TextSecondaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (statusFilter == "PENDING") AccentAmber else SlateCard,
                    modifier = Modifier.weight(1.1f).clickable { statusFilter = "PENDING" }
                ) {
                    Text(
                        text = "Pendientes (${plannedEvents.count { !it.isPaid }})",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (statusFilter == "PENDING") NavySurface else TextSecondaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (statusFilter == "PAID") EmeraldLight else SlateCard,
                    modifier = Modifier.weight(1f).clickable { statusFilter = "PAID" }
                ) {
                    Text(
                        text = "Listos (${plannedEvents.count { it.isPaid }})",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (statusFilter == "PAID") NavySurface else TextSecondaryDark
                    )
                }
            }
        }

        // Events List
        if (filteredEvents.isEmpty()) {
            item {
                Surface(
                    color = SlateCard.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (statusFilter == "PENDING") "¡No tienes pagos pendientes en este rango! 🎉"
                            else if (statusFilter == "PAID") "No hay movimientos listos en este rango"
                            else "No hay movimientos previstos en este rango de fechas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        } else {
            items(filteredEvents, key = { it.id }) { event ->
                PlannedEventRow(
                    event = event,
                    displayCurrency = health.displayCurrency,
                    rates = rates,
                    accounts = accounts,
                    onToggleReady = {
                        if (event.commitment != null && event.installmentNumber != null) {
                            viewModel.toggleInstallment(
                                commitment = event.commitment,
                                installmentNumber = event.installmentNumber,
                                markAsPaid = !event.isPaid,
                                payFromAccount = accounts.firstOrNull()
                            )
                        } else if (!event.isIncome && event.commitment != null) {
                            // Fixed expense occurrence toggle
                            viewModel.addTransaction(
                                Transaction(
                                    userId = viewModel.currentUserId,
                                    accountId = accounts.firstOrNull()?.id ?: "",
                                    type = TransactionType.EXPENSE.name,
                                    amount = event.amount,
                                    currency = event.currency.name,
                                    category = event.commitment.category,
                                    description = "Pago de ${event.title}",
                                    date = Timestamp(event.dueDate),
                                    commitmentId = event.commitment.id,
                                    isFixedExpense = true
                                )
                            )
                        } else if (event.isIncome) {
                            // Register income as realized
                            viewModel.addTransaction(
                                Transaction(
                                    userId = viewModel.currentUserId,
                                    accountId = accounts.firstOrNull()?.id ?: "",
                                    type = TransactionType.INCOME.name,
                                    amount = event.amount,
                                    currency = event.currency.name,
                                    description = event.title,
                                    date = Timestamp(event.dueDate)
                                )
                            )
                        }
                    }
                )
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
    rates: ExchangeRates,
    accounts: List<Account>,
    onToggleReady: () -> Unit
) {
    val amountInDisp = rates.convert(event.amount, event.currency, displayCurrency)
    val icon = if (event.isIncome) Icons.Default.TrendingUp else if (event.isFixed) Icons.Default.CalendarMonth else Icons.Default.CreditCard
    val iconColor = if (event.isIncome) EmeraldLight else if (event.isFixed) AccentAmber else AccentRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isPaid) SlateCard.copy(alpha = 0.65f) else SlateCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Checkbox / Mark as Ready button
                IconButton(
                    onClick = onToggleReady,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (event.isPaid) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Listo",
                            tint = EmeraldLight,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Pendiente - Toca para marcar como listo",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (event.isPaid) TextSecondaryDark else TextPrimaryDark,
                            fontSize = 13.sp
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
                    color = if (event.isIncome) EmeraldLight else if (event.isPaid) TextSecondaryDark else TextPrimaryDark,
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
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (event.isPaid) EmeraldDark.copy(alpha = 0.35f) else AccentAmber.copy(alpha = 0.2f),
                    modifier = Modifier.padding(top = 2.dp).clickable { onToggleReady() }
                ) {
                    Text(
                        text = if (event.isPaid) "✓ LISTO" else "PENDIENTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (event.isPaid) EmeraldLight else AccentAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
