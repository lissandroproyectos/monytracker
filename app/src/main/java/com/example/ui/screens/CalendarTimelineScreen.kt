package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.components.ConfirmPaymentDialog
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
    val isSavingsSweep: Boolean = false,
    val isPaid: Boolean,
    val installmentInfo: String? = null,
    val commitment: ExpenseCommitment? = null,
    val installmentNumber: Long? = null,
    val availableBalanceAfter: Double = 0.0,
    val sweepExcessAmount: Double = 0.0,
    val transactionId: String? = null
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
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Screen Mode: 0 = Calendario Mensual, 1 = Plan de Pagos & Proyección
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog state for editing cushion
    var showCushionDialog by remember { mutableStateOf(false) }
    var cushionInput by remember { mutableStateOf("") }

    // Total initial available money in liquid operational accounts
    val totalAvailableNow = remember(accounts, rates, health.displayCurrency) {
        accounts.filter { !it.isSavings }.sumOf { acc ->
            rates.convert(acc.currentBalance, AppCurrency.fromCode(acc.currency), health.displayCurrency)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
            .testTag("calendar_timeline_screen")
    ) {
        // Tab Row switcher at top
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SlateCard,
            contentColor = EmeraldLight,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = EmeraldLight,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Calendario Mensual", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                },
                selectedContentColor = EmeraldLight,
                unselectedContentColor = TextSecondaryDark
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Plan & Proyección", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                },
                selectedContentColor = EmeraldLight,
                unselectedContentColor = TextSecondaryDark
            )
        }

        if (selectedTab == 0) {
            // VIEW 1: CALENDARIO MENSUAL
            MonthlyCalendarView(
                viewModel = viewModel,
                commitments = commitments,
                transactions = transactions,
                accounts = accounts,
                rates = rates,
                displayCurrency = health.displayCurrency
            )
        } else {
            // VIEW 2: PLAN DE PAGOS & PROYECCIÓN DE FLUJO DE CAJA
            PaymentPlanProjectionView(
                viewModel = viewModel,
                commitments = commitments,
                transactions = transactions,
                accounts = accounts,
                rates = rates,
                displayCurrency = health.displayCurrency,
                initialAvailable = totalAvailableNow,
                cushionAmount = settings.safetyCushionAmount,
                onEditCushion = {
                    cushionInput = settings.safetyCushionAmount.toString()
                    showCushionDialog = true
                }
            )
        }
    }

    // Cushion edit dialog
    if (showCushionDialog) {
        AlertDialog(
            onDismissRequest = { showCushionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldLight)
                    Spacer(Modifier.width(8.dp))
                    Text("Colchón de Seguridad", color = TextPrimaryDark, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Define el monto mínimo de dinero disponible que siempre deseas mantener intacto. Todo dinero por encima de este valor antes de cada cobro se apartará para ahorro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    OutlinedTextField(
                        value = cushionInput,
                        onValueChange = { cushionInput = it },
                        label = { Text("Monto del Colchón (${health.displayCurrency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldLight,
                            unfocusedBorderColor = SlateCardLight,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = cushionInput.toDoubleOrNull() ?: settings.safetyCushionAmount
                        viewModel.updateSafetyCushion(amount)
                        showCushionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Guardar", color = NavySurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCushionDialog = false }) {
                    Text("Cancelar", color = TextSecondaryDark)
                }
            },
            containerColor = SlateCard
        )
    }
}

// ------------------------------------------------------------------------------------------------
// VIEW 1: CALENDARIO MENSUAL
// ------------------------------------------------------------------------------------------------
@Composable
fun MonthlyCalendarView(
    viewModel: FinanceViewModel,
    commitments: List<ExpenseCommitment>,
    transactions: List<Transaction>,
    accounts: List<Account>,
    rates: ExchangeRates,
    displayCurrency: AppCurrency
) {
    var calendarMonth by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        )
    }

    var selectedDayOfMonth by remember { mutableStateOf<Int?>(null) }
    var statusFilter by remember { mutableStateOf("PENDING") } // PENDING, PAID, ALL
    var pendingPaymentEvent by remember { mutableStateOf<PlannedEvent?>(null) }

    val monthStart = remember(calendarMonth) {
        (calendarMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time
    }

    val monthEnd = remember(calendarMonth) {
        (calendarMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.time
    }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES")) }
    val monthTitle = remember(calendarMonth) {
        monthFormat.format(calendarMonth.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    // Compute all planned events for this month
    val monthEvents = remember(commitments, transactions, accounts, monthStart, monthEnd) {
        generatePlannedEvents(
            commitments = commitments,
            transactions = transactions,
            accounts = accounts,
            startDate = monthStart,
            endDate = monthEnd
        )
    }

    // Map of day of month -> events on that day
    val eventsByDay = remember(monthEvents) {
        monthEvents.groupBy { event ->
            val cal = Calendar.getInstance().apply { time = event.dueDate }
            cal.get(Calendar.DAY_OF_MONTH)
        }
    }

    // Events for selected day (or whole month)
    val dayEvents = remember(monthEvents, selectedDayOfMonth) {
        if (selectedDayOfMonth != null) {
            monthEvents.filter { event ->
                val cal = Calendar.getInstance().apply { time = event.dueDate }
                cal.get(Calendar.DAY_OF_MONTH) == selectedDayOfMonth
            }
        } else monthEvents
    }

    // Filter events by status
    val displayedEvents = remember(dayEvents, statusFilter) {
        dayEvents.filter { event ->
            when (statusFilter) {
                "PENDING" -> !event.isPaid
                "PAID" -> event.isPaid
                else -> true
            }
        }
    }

    // Totals for this month in display currency
    val totalIncomeMonth = monthEvents.filter { it.isIncome }.sumOf {
        rates.convert(it.amount, it.currency, displayCurrency)
    }
    val totalExpensesMonth = monthEvents.filter { !it.isIncome }.sumOf {
        rates.convert(it.amount, it.currency, displayCurrency)
    }
    val monthNetBalance = totalIncomeMonth - totalExpensesMonth

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Selector Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                selectedDayOfMonth = null
                                calendarMonth = (calendarMonth.clone() as Calendar).apply {
                                    add(Calendar.MONTH, -1)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior", tint = EmeraldLight)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "${monthEvents.size} movimientos previstos",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                selectedDayOfMonth = null
                                calendarMonth = (calendarMonth.clone() as Calendar).apply {
                                    add(Calendar.MONTH, 1)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente", tint = EmeraldLight)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Monthly Calendar Grid
                    MonthCalendarGrid(
                        calendarMonth = calendarMonth,
                        eventsByDay = eventsByDay,
                        selectedDay = selectedDayOfMonth,
                        onDaySelected = { day ->
                            selectedDayOfMonth = if (selectedDayOfMonth == day) null else day
                        }
                    )
                }
            }
        }

        // Monthly Summary Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ingresos Mes", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                        Text(
                            text = "+${formatCurrency(totalIncomeMonth, displayCurrency.symbol)}",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Egresos Mes", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                        Text(
                            text = "-${formatCurrency(totalExpensesMonth, displayCurrency.symbol)}",
                            fontWeight = FontWeight.Bold,
                            color = AccentRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Balance Neto", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                        Text(
                            text = formatCurrency(monthNetBalance, displayCurrency.symbol),
                            fontWeight = FontWeight.Bold,
                            color = if (monthNetBalance >= 0) EmeraldLight else AccentAmber,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Filter chips and Selected day badge
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedDayOfMonth != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Día $selectedDayOfMonth de $monthTitle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        TextButton(onClick = { selectedDayOfMonth = null }) {
                            Text("Ver todo el mes", color = TextSecondaryDark, fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (statusFilter == "PENDING") AccentAmber else SlateCard,
                        modifier = Modifier.weight(1.1f).clickable { statusFilter = "PENDING" }
                    ) {
                        Text(
                            text = "Pendientes (${dayEvents.count { !it.isPaid }})",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
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
                            text = "Listos (${dayEvents.count { it.isPaid }})",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (statusFilter == "PAID") NavySurface else TextSecondaryDark
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (statusFilter == "ALL") EmeraldPrimary else SlateCard,
                        modifier = Modifier.weight(1f).clickable { statusFilter = "ALL" }
                    ) {
                        Text(
                            text = "Todos (${dayEvents.size})",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (statusFilter == "ALL") NavySurface else TextSecondaryDark
                        )
                    }
                }
            }
        }

        // List of events for the Month
        if (displayedEvents.isEmpty()) {
            item {
                Surface(
                    color = SlateCard.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedDayOfMonth != null) "No hay movimientos registrados para el día $selectedDayOfMonth."
                            else "No hay movimientos previstos en $monthTitle.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedEvents, key = { it.id }) { event ->
                PlannedEventItemCard(
                    event = event,
                    displayCurrency = displayCurrency,
                    rates = rates,
                    showBalanceAfter = false,
                    onToggleReady = {
                        if (event.isPaid) {
                            toggleEventReady(viewModel, event, accounts)
                        } else {
                            pendingPaymentEvent = event
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    pendingPaymentEvent?.let { event ->
        ConfirmPaymentDialog(
            title = event.title,
            suggestedAmount = event.amount,
            currencyCode = event.currency.name,
            dueDate = event.dueDate,
            accounts = accounts,
            isIncome = event.isIncome,
            onDismiss = { pendingPaymentEvent = null },
            onConfirm = { account: Account?, paidDate: Date, amount: Double, notes: String ->
                if (event.commitment != null && event.installmentNumber != null) {
                    viewModel.toggleInstallment(
                        commitment = event.commitment,
                        installmentNumber = event.installmentNumber,
                        markAsPaid = true,
                        payFromAccount = account,
                        paidDate = paidDate
                    )
                } else if (!event.isIncome && event.commitment != null) {
                    viewModel.addTransaction(
                        Transaction(
                            userId = viewModel.currentUserId,
                            accountId = account?.id ?: "",
                            type = TransactionType.EXPENSE.name,
                            amount = amount,
                            currency = event.currency.name,
                            category = event.commitment.category,
                            description = "Pago de ${event.title}" + if (notes.isNotBlank()) " ($notes)" else "",
                            date = Timestamp(paidDate),
                            commitmentId = event.commitment.id,
                            isFixedExpense = true
                        )
                    )
                } else if (event.isIncome) {
                    viewModel.addTransaction(
                        Transaction(
                            userId = viewModel.currentUserId,
                            accountId = account?.id ?: "",
                            type = TransactionType.INCOME.name,
                            amount = amount,
                            currency = event.currency.name,
                            description = event.title + if (notes.isNotBlank()) " ($notes)" else "",
                            date = Timestamp(paidDate)
                        )
                    )
                }
                pendingPaymentEvent = null
            }
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Month Calendar Grid Component
// ------------------------------------------------------------------------------------------------
@Composable
fun MonthCalendarGrid(
    calendarMonth: Calendar,
    eventsByDay: Map<Int, List<PlannedEvent>>,
    selectedDay: Int?,
    onDaySelected: (Int) -> Unit
) {
    val daysOfWeek = listOf("L", "M", "M", "J", "V", "S", "D")
    val cal = (calendarMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // In Java Calendar, Sunday is 1, Monday is 2, ..., Saturday is 7
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Map to Monday = 0, Sunday = 6
    val offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.YEAR) == calendarMonth.get(Calendar.YEAR) &&
            today.get(Calendar.MONTH) == calendarMonth.get(Calendar.MONTH)
    val todayDayNumber = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else -1

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Day of week header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Days Grid
        val totalCells = offset + maxDays
        val rows = (totalCells + 6) / 7

        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - offset + 1

                    if (dayNum in 1..maxDays) {
                        val eventsOnThisDay = eventsByDay[dayNum].orEmpty()
                        val hasIncome = eventsOnThisDay.any { it.isIncome }
                        val hasExpense = eventsOnThisDay.any { !it.isIncome }
                        val isSelected = selectedDay == dayNum
                        val isToday = todayDayNumber == dayNum

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> EmeraldPrimary
                                        isToday -> SlateCardLight
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDaySelected(dayNum) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NavySurface else if (isToday) EmeraldLight else TextPrimaryDark
                                )

                                // Indicators
                                if (eventsOnThisDay.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (hasIncome) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) NavySurface else EmeraldLight)
                                            )
                                        }
                                        if (hasExpense) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) NavySurface else AccentRed)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty cell
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// VIEW 2: PLAN DE PAGOS & PROYECCIÓN DE FLUJO DE CAJA (SIMULACIÓN PASO A PASO)
// ------------------------------------------------------------------------------------------------
@Composable
fun PaymentPlanProjectionView(
    viewModel: FinanceViewModel,
    commitments: List<ExpenseCommitment>,
    transactions: List<Transaction>,
    accounts: List<Account>,
    rates: ExchangeRates,
    displayCurrency: AppCurrency,
    initialAvailable: Double,
    cushionAmount: Double,
    onEditCushion: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES")) }

    // Start and End dates for the projection simulation
    var startDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
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
                add(Calendar.MONTH, 2)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.time
        )
    }

    // Toggle: Savings sweep before each income if available funds exceed cushion
    var enableSavingsSweep by remember { mutableStateOf(true) }
    var pendingPaymentEvent by remember { mutableStateOf<PlannedEvent?>(null) }

    fun showPicker(current: Date, onSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance().apply { time = current }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val sel = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                onSelected(sel)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // 1. Gather raw events within [startDate, endDate]
    val rawEvents = remember(commitments, transactions, accounts, startDate, endDate) {
        generatePlannedEvents(
            commitments = commitments,
            transactions = transactions,
            accounts = accounts,
            startDate = startDate,
            endDate = endDate
        )
    }

    // 2. Perform step-by-step cash flow simulation with cushion check & excess savings sweep
    val simulatedTimeline = remember(rawEvents, initialAvailable, cushionAmount, enableSavingsSweep, rates, displayCurrency) {
        val results = mutableListOf<PlannedEvent>()
        var runningAvailable = initialAvailable

        // Group or sort chronologically
        val sorted = rawEvents.sortedBy { it.dueDate }

        sorted.forEach { event ->
            val eventAmountInDisp = rates.convert(event.amount, event.currency, displayCurrency)

            if (event.isIncome) {
                // RULE: Before each income, check if running available exceeds cushion
                if (enableSavingsSweep && runningAvailable > cushionAmount) {
                    val excess = kotlin.math.round((runningAvailable - cushionAmount) * 100.0) / 100.0
                    if (excess >= 1.0) { // meaningful threshold
                        runningAvailable = kotlin.math.round((runningAvailable - excess) * 100.0) / 100.0
                        results.add(
                            PlannedEvent(
                                id = "sweep_before_${event.id}",
                                title = "Cuota de Ahorro Sugerida (Excedente del Colchón)",
                                dueDate = event.dueDate,
                                amount = excess,
                                currency = displayCurrency,
                                isFixed = false,
                                isIncome = false,
                                isSavingsSweep = true,
                                isPaid = false,
                                installmentInfo = "Excedente resguardado",
                                availableBalanceAfter = runningAvailable,
                                sweepExcessAmount = excess
                            )
                        )
                    }
                }

                // Add income to available pool
                runningAvailable = kotlin.math.round((runningAvailable + eventAmountInDisp) * 100.0) / 100.0
                results.add(
                    event.copy(availableBalanceAfter = runningAvailable)
                )
            } else {
                // Deduct expense from available pool
                runningAvailable = kotlin.math.round((runningAvailable - eventAmountInDisp) * 100.0) / 100.0
                results.add(
                    event.copy(availableBalanceAfter = runningAvailable)
                )
            }
        }

        results
    }

    val finalAvailableBalance = simulatedTimeline.lastOrNull()?.availableBalanceAfter ?: initialAvailable
    val totalProjectedSavingsSweeps = simulatedTimeline.filter { it.isSavingsSweep }.sumOf { it.amount }

    // -----------------------------------------------------------------------------------------
    // CÁLCULO DE MONTO PARA INICIO DEL PLAN (Desde inicio de plan hasta el 1er ingreso)
    // -----------------------------------------------------------------------------------------
    val firstIncomeEvent = remember(rawEvents, startDate) {
        rawEvents.firstOrNull { it.isIncome && !it.dueDate.before(startDate) }
    }

    val preIncomeEvents = remember(rawEvents, startDate, firstIncomeEvent) {
        if (firstIncomeEvent != null) {
            rawEvents.filter { !it.isIncome && !it.dueDate.before(startDate) && !it.dueDate.after(firstIncomeEvent.dueDate) }
        } else {
            rawEvents.filter { !it.isIncome && !it.dueDate.before(startDate) }
        }
    }

    val pendingPreIncomeExpenses = remember(preIncomeEvents, rates, displayCurrency) {
        val sum = preIncomeEvents.filter { !it.isPaid }.sumOf { rates.convert(it.amount, it.currency, displayCurrency) }
        kotlin.math.round(sum * 100.0) / 100.0
    }

    val totalPreIncomeExpenses = remember(preIncomeEvents, rates, displayCurrency) {
        val sum = preIncomeEvents.sumOf { rates.convert(it.amount, it.currency, displayCurrency) }
        kotlin.math.round(sum * 100.0) / 100.0
    }

    val amountNeededForZero = pendingPreIncomeExpenses
    val amountNeededForCushion = kotlin.math.round((pendingPreIncomeExpenses + cushionAmount) * 100.0) / 100.0
    val deficitForZero = kotlin.math.round((amountNeededForZero - initialAvailable).coerceAtLeast(0.0) * 100.0) / 100.0
    val deficitForCushion = kotlin.math.round((amountNeededForCushion - initialAvailable).coerceAtLeast(0.0) * 100.0) / 100.0
    val balanceAtFirstIncome = kotlin.math.round((initialAvailable - pendingPreIncomeExpenses) * 100.0) / 100.0

    var showPreIncomeDetails by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Plan Range Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Proyección de Flujo de Caja",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }

                    Text(
                        text = "Simula cómo evolucionará tu dinero disponible en las cuentas desde la fecha de inicio hasta el fin:",
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

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            modifier = Modifier.weight(1f).clickable {
                                startDate = Calendar.getInstance().time
                                endDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }.time
                            }
                        ) {
                            Text("1 Mes", modifier = Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            modifier = Modifier.weight(1f).clickable {
                                startDate = Calendar.getInstance().time
                                endDate = Calendar.getInstance().apply { add(Calendar.MONTH, 2) }.time
                            }
                        ) {
                            Text("2 Meses", modifier = Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            modifier = Modifier.weight(1f).clickable {
                                startDate = Calendar.getInstance().time
                                endDate = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }.time
                            }
                        ) {
                            Text("3 Meses", modifier = Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SlateBackground,
                            modifier = Modifier.weight(1f).clickable {
                                startDate = Calendar.getInstance().time
                                endDate = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }.time
                            }
                        ) {
                            Text("6 Meses", modifier = Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Card: Cálculo de Monto para Inicio del Plan (Hasta el 1er Ingreso)
        item {
            val planStartAcc = accounts.firstOrNull { it.isPlanStartAccount }
            PlanInitialCapitalCard(
                startDate = startDate,
                firstIncomeEvent = firstIncomeEvent,
                preIncomeEvents = preIncomeEvents,
                pendingPreIncomeExpenses = pendingPreIncomeExpenses,
                totalPreIncomeExpenses = totalPreIncomeExpenses,
                amountNeededForZero = amountNeededForZero,
                amountNeededForCushion = amountNeededForCushion,
                initialAvailable = initialAvailable,
                cushionAmount = cushionAmount,
                deficitForZero = deficitForZero,
                deficitForCushion = deficitForCushion,
                balanceAtFirstIncome = balanceAtFirstIncome,
                displayCurrency = displayCurrency,
                rates = rates,
                dateFormat = dateFormat,
                showDetails = showPreIncomeDetails,
                onToggleDetails = { showPreIncomeDetails = !showPreIncomeDetails },
                planStartAccount = planStartAcc,
                onInjectInitialCapital = { targetAmount ->
                    viewModel.injectPlanStartCapital(targetAmount, displayCurrency.name)
                }
            )
        }

        // Available Money & Safety Cushion Control Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardLight)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dinero Disponible Inicial (Cuentas)", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Text(
                                text = formatCurrency(initialAvailable, displayCurrency.symbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (initialAvailable >= cushionAmount) EmeraldLight else AccentAmber
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SlateBackground,
                            modifier = Modifier.clickable { onEditCushion() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text("Colchón", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 9.sp)
                                    Text(
                                        formatCurrency(cushionAmount, displayCurrency.symbol),
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TextSecondaryDark, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Cushion Sweep Toggle Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ahorrar excedente antes de cada ingreso",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Genera una cuota de ahorro si el dinero supera el colchón de ${formatCurrency(cushionAmount, displayCurrency.symbol)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = enableSavingsSweep,
                                onCheckedChange = { enableSavingsSweep = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldLight,
                                    checkedTrackColor = EmeraldDark
                                )
                            )
                        }
                    }

                    // Simulation summary stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dinero Final Proyectado", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                            Text(
                                text = formatCurrency(finalAvailableBalance, displayCurrency.symbol),
                                fontWeight = FontWeight.Bold,
                                color = if (finalAvailableBalance >= cushionAmount) EmeraldLight else if (finalAvailableBalance >= 0) AccentAmber else AccentRed,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (totalProjectedSavingsSweeps > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ahorro Acumulado Sugerido", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
                                Text(
                                    text = "+${formatCurrency(totalProjectedSavingsSweeps, displayCurrency.symbol)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA78BFA),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Chronological Flow of Events
        item {
            Text(
                text = "Evolución Movimiento a Movimiento (${simulatedTimeline.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryDark
            )
        }

        // Simulation Timeline Items
        if (simulatedTimeline.isEmpty()) {
            item {
                Surface(
                    color = SlateCard.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No hay ingresos ni egresos previstos en este rango de fechas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(simulatedTimeline, key = { it.id }) { event ->
                if (event.isSavingsSweep) {
                    // Savings Sweep Special Card
                    SavingsSweepCard(
                        event = event,
                        cushionAmount = cushionAmount,
                        displayCurrency = displayCurrency,
                        onApplySweep = {
                            viewModel.applySavingsSweep(
                                amount = event.amount,
                                currency = event.currency,
                                note = "Excedente antes de cobro"
                            )
                        }
                    )
                } else {
                    // Regular simulated event with available money after movement
                    PlannedEventItemCard(
                        event = event,
                        displayCurrency = displayCurrency,
                        rates = rates,
                        showBalanceAfter = true,
                        cushionAmount = cushionAmount,
                        onToggleReady = {
                            if (event.isPaid) {
                                toggleEventReady(viewModel, event, accounts)
                            } else {
                                pendingPaymentEvent = event
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    pendingPaymentEvent?.let { event ->
        ConfirmPaymentDialog(
            title = event.title,
            suggestedAmount = event.amount,
            currencyCode = event.currency.name,
            dueDate = event.dueDate,
            accounts = accounts,
            isIncome = event.isIncome,
            onDismiss = { pendingPaymentEvent = null },
            onConfirm = { account: Account?, paidDate: Date, amount: Double, notes: String ->
                if (event.commitment != null && event.installmentNumber != null) {
                    viewModel.toggleInstallment(
                        commitment = event.commitment,
                        installmentNumber = event.installmentNumber,
                        markAsPaid = true,
                        payFromAccount = account,
                        paidDate = paidDate
                    )
                } else if (!event.isIncome && event.commitment != null) {
                    viewModel.addTransaction(
                        Transaction(
                            userId = viewModel.currentUserId,
                            accountId = account?.id ?: "",
                            type = TransactionType.EXPENSE.name,
                            amount = amount,
                            currency = event.currency.name,
                            category = event.commitment.category,
                            description = "Pago de ${event.title}" + if (notes.isNotBlank()) " ($notes)" else "",
                            date = Timestamp(paidDate),
                            commitmentId = event.commitment.id,
                            isFixedExpense = true
                        )
                    )
                } else if (event.isIncome) {
                    viewModel.addTransaction(
                        Transaction(
                            userId = viewModel.currentUserId,
                            accountId = account?.id ?: "",
                            type = TransactionType.INCOME.name,
                            amount = amount,
                            currency = event.currency.name,
                            description = event.title + if (notes.isNotBlank()) " ($notes)" else "",
                            date = Timestamp(paidDate)
                        )
                    )
                }
                pendingPaymentEvent = null
            }
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Card for Savings Sweep (Cuota de Ahorro por exceder colchón)
// ------------------------------------------------------------------------------------------------
@Composable
fun SavingsSweepCard(
    event: PlannedEvent,
    cushionAmount: Double,
    displayCurrency: AppCurrency,
    onApplySweep: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)) // Deep indigo
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF818CF8).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "💡 Cuota de Ahorro Sugerida",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC),
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatDate(event.dueDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = "-${formatCurrency(event.amount, displayCurrency.symbol)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA5B4FC),
                    fontSize = 14.sp
                )
            }

            Text(
                text = "Tu dinero disponible superaba el colchón de ${formatCurrency(cushionAmount, displayCurrency.symbol)}. Se aconseja resguardar este excedente en tu Fondo de Ahorro antes de que entre el nuevo ingreso.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dinero disponible protegido: ${formatCurrency(event.availableBalanceAfter, displayCurrency.symbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldLight,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF6366F1),
                    modifier = Modifier.clickable { onApplySweep() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apartar a Ahorro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Standard Planned Event Row Card
// ------------------------------------------------------------------------------------------------
@Composable
fun PlannedEventItemCard(
    event: PlannedEvent,
    displayCurrency: AppCurrency,
    rates: ExchangeRates,
    showBalanceAfter: Boolean,
    cushionAmount: Double = 0.0,
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
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox / Mark as Ready button
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(17.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${formatDate(event.dueDate)} • ${if (event.isIncome) "Ingreso" else if (event.isFixed) "Gasto Fijo" else "Deuda/Cuota"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            fontSize = 10.sp
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
                            fontSize = 9.sp
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
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Step-by-step Available Money tracker
            if (showBalanceAfter) {
                val bal = event.availableBalanceAfter
                val statusColor = when {
                    bal >= cushionAmount -> EmeraldLight
                    bal >= 0 -> AccentAmber
                    else -> AccentRed
                }
                val statusText = when {
                    bal >= cushionAmount -> "✓ Colchón seguro"
                    bal >= 0 -> "⚠️ Tocando colchón"
                    else -> "🚨 Déficit previsto"
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dinero disponible después:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatCurrency(bal, displayCurrency.symbol),
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "($statusText)",
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Helper: Generate planned events between two dates
// ------------------------------------------------------------------------------------------------
private fun generatePlannedEvents(
    commitments: List<ExpenseCommitment>,
    transactions: List<Transaction>,
    accounts: List<Account>,
    startDate: Date,
    endDate: Date
): List<PlannedEvent> {
    val events = mutableListOf<PlannedEvent>()
    val startMs = startDate.time
    val endMs = endDate.time

    // 1. Commitments (Debts & Fixed Expenses)
    commitments.forEach { c ->
        val commCurr = AppCurrency.fromCode(c.currency)
        val commitmentEndLimitMs = c.endDate?.toDate()?.time

        if (!c.isFixedExpense) {
            // Installment Debts
            c.getProjectedInstallments().forEach { inst ->
                val instDate = inst.dueDate.toDate()
                val instMs = instDate.time
                if (instMs in startMs..endMs && (commitmentEndLimitMs == null || instMs <= commitmentEndLimitMs)) {
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
            // Fixed Expenses: if custom installments were saved/planned, use them
            if (c.installments.isNotEmpty()) {
                c.getProjectedInstallments().forEach { inst ->
                    val instDate = inst.dueDate.toDate()
                    val instMs = instDate.time
                    if (instMs in startMs..endMs && (commitmentEndLimitMs == null || instMs <= commitmentEndLimitMs)) {
                        val isPaidInCycle = inst.isPaid || (instDate.before(Date()) && c.status == CommitmentStatus.PAID_OFF.name)
                        events.add(
                            PlannedEvent(
                                id = "fixed_${c.id}_${inst.number}_${instMs}",
                                title = c.title,
                                dueDate = instDate,
                                amount = inst.amount,
                                currency = commCurr,
                                isFixed = true,
                                isIncome = false,
                                isPaid = isPaidInCycle,
                                installmentInfo = "Gasto Fijo #${inst.number}",
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
                val baseDayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)

                val iterCal = Calendar.getInstance().apply {
                    time = baseCal.time
                    while (time.before(startDate)) {
                        when (freq) {
                            Frequency.WEEKLY -> add(Calendar.DAY_OF_YEAR, 7)
                            Frequency.CASHEA -> add(Calendar.DAY_OF_YEAR, 14)
                            Frequency.BIWEEKLY -> add(Calendar.DAY_OF_YEAR, 15)
                            Frequency.MONTHLY -> {
                                add(Calendar.MONTH, 1)
                                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                            }
                            Frequency.BIMONTHLY -> {
                                add(Calendar.MONTH, 2)
                                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                            }
                            Frequency.QUARTERLY -> {
                                add(Calendar.MONTH, 3)
                                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                            }
                            Frequency.FOUR_MONTHLY -> {
                                add(Calendar.MONTH, 4)
                                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                            }
                            Frequency.SEMIANNUAL -> {
                                add(Calendar.MONTH, 6)
                                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                            }
                            Frequency.ANNUAL -> {
                                add(Calendar.YEAR, 1)
                                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                            }
                            Frequency.CUSTOM -> add(Calendar.DAY_OF_YEAR, c.customDays.coerceAtLeast(1))
                        }
                    }
                }

                while (!iterCal.time.after(endDate) && (commitmentEndLimitMs == null || iterCal.time.time <= commitmentEndLimitMs)) {
                    val occurrenceDate = iterCal.time
                    val occMs = occurrenceDate.time
                    if (occMs in startMs..endMs && (commitmentEndLimitMs == null || occMs <= commitmentEndLimitMs)) {
                        val isPaidInCycle = occurrenceDate.before(Date()) && c.status == CommitmentStatus.PAID_OFF.name
                        events.add(
                            PlannedEvent(
                                id = "fixed_${c.id}_${occMs}",
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
                        Frequency.CASHEA -> iterCal.add(Calendar.DAY_OF_YEAR, 14)
                        Frequency.BIWEEKLY -> iterCal.add(Calendar.DAY_OF_YEAR, 15)
                        Frequency.MONTHLY -> {
                            iterCal.add(Calendar.MONTH, 1)
                            val maxDay = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            iterCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                        }
                        Frequency.BIMONTHLY -> {
                            iterCal.add(Calendar.MONTH, 2)
                            val maxDay = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            iterCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                        }
                        Frequency.QUARTERLY -> {
                            iterCal.add(Calendar.MONTH, 3)
                            val maxDay = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            iterCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                        }
                        Frequency.FOUR_MONTHLY -> {
                            iterCal.add(Calendar.MONTH, 4)
                            val maxDay = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            iterCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                        }
                        Frequency.SEMIANNUAL -> {
                            iterCal.add(Calendar.MONTH, 6)
                            val maxDay = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            iterCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                        }
                        Frequency.ANNUAL -> {
                            iterCal.add(Calendar.YEAR, 1)
                            val maxDay = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            iterCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
                        }
                        Frequency.CUSTOM -> iterCal.add(Calendar.DAY_OF_YEAR, c.customDays.coerceAtLeast(1))
                    }
                }
            }
        }
    }

    // 2. Periodic Incomes configured on Accounts
    accounts.filter { it.hasPeriodicIncome && it.periodicIncomeAmount > 0 }.forEach { acc ->
        val incCurr = AppCurrency.fromCode(acc.periodicIncomeCurrency)
        val iterCal = Calendar.getInstance().apply { time = startDate }

        while (!iterCal.time.after(endDate)) {
            val incomeDesc = acc.periodicIncomeDescription.ifBlank { "Ingreso" }
            when (acc.periodicIncomeFrequency) {
                "QUINCENAL" -> {
                    val d15 = (iterCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 15) }
                    if (d15.time.time in startMs..endMs) {
                        val matchingTx = transactions.firstOrNull { tx ->
                            tx.type == TransactionType.INCOME.name &&
                            (tx.accountId == acc.id || tx.description.contains(acc.name) || tx.description.contains(incomeDesc)) &&
                            isSameDay(tx.date.toDate(), d15.time)
                        }
                        events.add(
                            PlannedEvent(
                                id = "inc_quin_15_${acc.id}_${d15.timeInMillis}",
                                title = "$incomeDesc (${acc.name})",
                                dueDate = d15.time,
                                amount = acc.periodicIncomeAmount,
                                currency = incCurr,
                                isFixed = false,
                                isIncome = true,
                                isPaid = matchingTx != null,
                                transactionId = matchingTx?.id
                            )
                        )
                    }
                    val dEnd = (iterCal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    if (dEnd.time.time in startMs..endMs) {
                        val matchingTx = transactions.firstOrNull { tx ->
                            tx.type == TransactionType.INCOME.name &&
                            (tx.accountId == acc.id || tx.description.contains(acc.name) || tx.description.contains(incomeDesc)) &&
                            isSameDay(tx.date.toDate(), dEnd.time)
                        }
                        events.add(
                            PlannedEvent(
                                id = "inc_quin_end_${acc.id}_${dEnd.timeInMillis}",
                                title = "$incomeDesc (${acc.name})",
                                dueDate = dEnd.time,
                                amount = acc.periodicIncomeAmount,
                                currency = incCurr,
                                isFixed = false,
                                isIncome = true,
                                isPaid = matchingTx != null,
                                transactionId = matchingTx?.id
                            )
                        )
                    }
                    iterCal.add(Calendar.MONTH, 1)
                }
                "SEMANAL" -> {
                    if (iterCal.time.time in startMs..endMs) {
                        val matchingTx = transactions.firstOrNull { tx ->
                            tx.type == TransactionType.INCOME.name &&
                            (tx.accountId == acc.id || tx.description.contains(acc.name) || tx.description.contains(incomeDesc)) &&
                            isSameDay(tx.date.toDate(), iterCal.time)
                        }
                        events.add(
                            PlannedEvent(
                                id = "inc_sem_${acc.id}_${iterCal.timeInMillis}",
                                title = "$incomeDesc (${acc.name})",
                                dueDate = iterCal.time,
                                amount = acc.periodicIncomeAmount,
                                currency = incCurr,
                                isFixed = false,
                                isIncome = true,
                                isPaid = matchingTx != null,
                                transactionId = matchingTx?.id
                            )
                        )
                    }
                    iterCal.add(Calendar.DAY_OF_YEAR, 7)
                }
                else -> { // MENSUAL
                    val day = acc.periodicIncomeDayOfMonth.coerceIn(1, iterCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    val dMonth = (iterCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                    if (dMonth.time.time in startMs..endMs) {
                        val matchingTx = transactions.firstOrNull { tx ->
                            tx.type == TransactionType.INCOME.name &&
                            (tx.accountId == acc.id || tx.description.contains(acc.name) || tx.description.contains(incomeDesc)) &&
                            isSameDay(tx.date.toDate(), dMonth.time)
                        }
                        events.add(
                            PlannedEvent(
                                id = "inc_mes_${acc.id}_${dMonth.timeInMillis}",
                                title = "$incomeDesc (${acc.name})",
                                dueDate = dMonth.time,
                                amount = acc.periodicIncomeAmount,
                                currency = incCurr,
                                isFixed = false,
                                isIncome = true,
                                isPaid = matchingTx != null,
                                transactionId = matchingTx?.id
                            )
                        )
                    }
                    iterCal.add(Calendar.MONTH, 1)
                }
            }
        }
    }

    // 3. Transactions recorded in this range (only add if not already matched to a planned event)
    val matchedTxIds = events.mapNotNull { it.transactionId }.toSet()
    transactions.forEach { tx ->
        if (tx.id !in matchedTxIds) {
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
                        installmentInfo = if (isInc) "Cobrado" else "Pagado",
                        transactionId = tx.id
                    )
                )
            }
        }
    }

    return events.distinctBy { it.id }.sortedBy { it.dueDate }
}

private fun isSameDay(d1: Date, d2: Date): Boolean {
    val c1 = Calendar.getInstance().apply { time = d1 }
    val c2 = Calendar.getInstance().apply { time = d2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

// Helper: Toggle event ready status (Used for unchecking / desmarcar completed items)
private fun toggleEventReady(
    viewModel: FinanceViewModel,
    event: PlannedEvent,
    accounts: List<Account>
) {
    if (!event.isPaid) return

    // 1. If explicit transaction ID exists, delete it
    if (event.transactionId != null) {
        val txToDelete = viewModel.transactions.value.firstOrNull { it.id == event.transactionId }
        if (txToDelete != null) {
            viewModel.deleteTransaction(txToDelete)
            return
        }
    }

    // 2. If it's an installment of a commitment, mark it unpaid
    if (event.commitment != null && event.installmentNumber != null) {
        viewModel.toggleInstallment(
            commitment = event.commitment,
            installmentNumber = event.installmentNumber,
            markAsPaid = false,
            payFromAccount = null
        )
        return
    }

    // 3. If it's a periodic income, delete the matching income transaction
    if (event.isIncome) {
        val txToDelete = viewModel.transactions.value.firstOrNull { tx ->
            tx.type == TransactionType.INCOME.name &&
            (event.transactionId == tx.id ||
             (tx.amount == event.amount && isSameDay(tx.date.toDate(), event.dueDate)))
        }
        if (txToDelete != null) {
            viewModel.deleteTransaction(txToDelete)
        }
        return
    }

    // 4. If it's a fixed expense, delete the matching expense transaction
    if (!event.isIncome && event.commitment != null) {
        val txToDelete = viewModel.transactions.value.firstOrNull { tx ->
            tx.commitmentId == event.commitment.id &&
            (event.transactionId == tx.id || isSameDay(tx.date.toDate(), event.dueDate))
        }
        if (txToDelete != null) {
            viewModel.deleteTransaction(txToDelete)
        }
        return
    }
}

// ------------------------------------------------------------------------------------------------
// CARD: CÁLCULO DE MONTO PARA INICIO DEL PLAN (Hasta el 1er Ingreso)
// ------------------------------------------------------------------------------------------------
@Composable
fun PlanInitialCapitalCard(
    startDate: Date,
    firstIncomeEvent: PlannedEvent?,
    preIncomeEvents: List<PlannedEvent>,
    pendingPreIncomeExpenses: Double,
    totalPreIncomeExpenses: Double,
    amountNeededForZero: Double,
    amountNeededForCushion: Double,
    initialAvailable: Double,
    cushionAmount: Double,
    deficitForZero: Double,
    deficitForCushion: Double,
    balanceAtFirstIncome: Double,
    displayCurrency: AppCurrency,
    rates: ExchangeRates,
    dateFormat: SimpleDateFormat,
    showDetails: Boolean,
    onToggleDetails: () -> Unit,
    planStartAccount: Account? = null,
    onInjectInitialCapital: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val daysToFirstIncome = remember(startDate, firstIncomeEvent) {
        if (firstIncomeEvent != null) {
            val diff = firstIncomeEvent.dueDate.time - startDate.time
            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
        } else 0L
    }

    val targetInjectAmount = kotlin.math.round((if (amountNeededForCushion > 0) amountNeededForCushion else amountNeededForZero) * 100.0) / 100.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("plan_initial_capital_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInjectInitialCapital?.invoke(targetInjectAmount) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldDark.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Capital para Inicio del Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (firstIncomeEvent != null) {
                                "Del ${dateFormat.format(startDate)} al 1er cobro (${dateFormat.format(firstIncomeEvent.dueDate)})"
                            } else {
                                "Desde ${dateFormat.format(startDate)} (sin ingresos periódicos en rango)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                if (firstIncomeEvent != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (daysToFirstIncome == 0L) "¡Cobro Hoy!" else "En $daysToFirstIncome días",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                text = "Toca para inyectar este dinero en la cuenta oculta 'Inicio del Plan', lista para cubrir egresos hasta tu primer cobro:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark,
                fontSize = 12.sp
            )

            // Two Core Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Monto para Saldo en 0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateCardLight,
                    border = BorderStroke(1.dp, SlateBackground),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onInjectInitialCapital?.invoke(amountNeededForZero) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = AccentAmber.copy(alpha = 0.2f),
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Para Saldo en 0",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = formatCurrency(amountNeededForZero, displayCurrency.symbol),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Toca para inyectar este mínimo a 'Inicio del Plan'",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }

                // Metric 2: Monto para Conservar Colchón
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateCardLight,
                    border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onInjectInitialCapital?.invoke(amountNeededForCushion) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldLight,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Con Colchón",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = formatCurrency(amountNeededForCushion, displayCurrency.symbol),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Toca para inyectar este capital optimizado",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            // Interactive Inject Button
            if (onInjectInitialCapital != null) {
                Button(
                    onClick = { onInjectInitialCapital(targetInjectAmount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inject_initial_capital_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (planStartAccount != null && planStartAccount.currentBalance > 0)
                            "Actualizar 'Inicio del Plan' (${formatCurrency(targetInjectAmount, displayCurrency.symbol)})"
                        else
                            "Inyectar Capital a 'Inicio del Plan' (${formatCurrency(targetInjectAmount, displayCurrency.symbol)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (planStartAccount != null && planStartAccount.currentBalance > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldDark.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Fondo activo en 'Inicio del Plan': ${formatCurrency(planStartAccount.currentBalance, displayCurrency.symbol)} (disponible para pagar egresos)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Status Banner based on current available funds
            val statusColor = when {
                initialAvailable >= amountNeededForCushion -> EmeraldLight
                initialAvailable >= amountNeededForZero -> AccentAmber
                else -> AccentRed
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    initialAvailable >= amountNeededForCushion -> EmeraldDark.copy(alpha = 0.35f)
                    initialAvailable >= amountNeededForZero -> AccentAmber.copy(alpha = 0.15f)
                    else -> AccentRed.copy(alpha = 0.18f)
                },
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            initialAvailable >= amountNeededForCushion -> Icons.Default.CheckCircle
                            initialAvailable >= amountNeededForZero -> Icons.Default.Info
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = when {
                                initialAvailable >= amountNeededForCushion -> "¡Fondos iniciales cubiertos con colchón!"
                                initialAvailable >= amountNeededForZero -> "Egresos cubiertos (Colchón parcial)"
                                else -> "Déficit de inicio para cubrir el lapso"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when {
                                initialAvailable >= amountNeededForCushion ->
                                    "Dispones de ${formatCurrency(initialAvailable, displayCurrency.symbol)}. Al llegar el 1er ingreso te quedarán ${formatCurrency(balanceAtFirstIncome, displayCurrency.symbol)} (tu colchón de ${formatCurrency(cushionAmount, displayCurrency.symbol)} queda 100% resguardado)."
                                initialAvailable >= amountNeededForZero ->
                                    "Dispones de ${formatCurrency(initialAvailable, displayCurrency.symbol)}. Cubres los egresos para llegar en ${formatCurrency(balanceAtFirstIncome, displayCurrency.symbol)}, pero faltan ${formatCurrency(deficitForCushion, displayCurrency.symbol)} para mantener completo tu colchón de seguridad (${formatCurrency(cushionAmount, displayCurrency.symbol)})."
                                else ->
                                    "Dispones de ${formatCurrency(initialAvailable, displayCurrency.symbol)} y necesitas ${formatCurrency(amountNeededForZero, displayCurrency.symbol)}. Faltan ${formatCurrency(deficitForZero, displayCurrency.symbol)} para no quedar en negativo antes del 1er ingreso."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Info row for the first income event
            if (firstIncomeEvent != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SlateBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Paid, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "1er Ingreso: ${firstIncomeEvent.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = dateFormat.format(firstIncomeEvent.dueDate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = "+${formatCurrency(rates.convert(firstIncomeEvent.amount, firstIncomeEvent.currency, displayCurrency), displayCurrency.symbol)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                }
            }

            // Toggle breakdown button
            if (preIncomeEvents.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SlateCardLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDetails() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${preIncomeEvents.size} compromiso(s) previos (${formatCurrency(pendingPreIncomeExpenses, displayCurrency.symbol)} pendientes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (showDetails) "Ocultar ▲" else "Ver ▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (showDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        preIncomeEvents.forEach { event ->
                            val eventAmt = rates.convert(event.amount, event.currency, displayCurrency)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SlateBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimaryDark,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${dateFormat.format(event.dueDate)} · ${event.installmentInfo ?: if (event.isFixed) "Gasto Fijo" else "Compromiso"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryDark,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "-${formatCurrency(eventAmt, displayCurrency.symbol)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (event.isPaid) TextSecondaryDark else AccentRed,
                                            fontSize = 11.sp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (event.isPaid) EmeraldDark.copy(alpha = 0.3f) else AccentAmber.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (event.isPaid) "Pagado" else "Pendiente",
                                                color = if (event.isPaid) EmeraldLight else AccentAmber,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
