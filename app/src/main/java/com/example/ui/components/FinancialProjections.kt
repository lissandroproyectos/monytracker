package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Frequency
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.currency.AppCurrency
import com.example.data.currency.ExchangeRates
import com.example.data.model.Account
import com.example.data.model.ExpenseCommitment
import com.example.data.model.MarketTemplate
import com.example.data.model.ProjectedInstallment
import com.example.data.model.UserSettings
import com.example.ui.CashflowPoint
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
import java.util.Date
import java.util.Locale

@Composable
fun CashflowProjectionCard(
    points: List<CashflowPoint>,
    selectedPeriodMonths: Int,
    onPeriodSelected: (Int) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cashflow_projection_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(EmeraldLight.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Comportamiento del Dinero",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Proyección en el tiempo según pagos y cuotas",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Range Selector (1M, 3M, 6M)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1 to "1M", 3 to "3M", 6 to "6M").forEach { (months, label) ->
                        val isSelected = selectedPeriodMonths == months
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) EmeraldPrimary else SlateBackground,
                            modifier = Modifier.clickable { onPeriodSelected(months) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NavySurface else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart
            if (points.isNotEmpty()) {
                val minBalance = points.minOf { it.projectedBalance }.coerceAtMost(0.0)
                val maxBalance = points.maxOf { it.projectedBalance }.coerceAtLeast(100.0)
                val range = (maxBalance - minBalance).coerceAtLeast(1.0)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(SlateBackground, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                        val width = size.width
                        val height = size.height

                        val stepX = if (points.size > 1) width / (points.size - 1) else width
                        val path = Path()
                        val fillPath = Path()

                        points.forEachIndexed { i, p ->
                            val normalizedY = ((p.projectedBalance - minBalance) / range).toFloat()
                            val x = i * stepX
                            val y = height - (normalizedY * height).coerceIn(4f, height - 4f)

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }

                        if (points.isNotEmpty()) {
                            val lastX = (points.size - 1) * stepX
                            fillPath.lineTo(lastX, height)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(EmeraldLight.copy(alpha = 0.35f), Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                )
                            )

                            drawPath(
                                path = path,
                                color = EmeraldLight,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Draw point circles
                            points.forEachIndexed { i, p ->
                                val normalizedY = ((p.projectedBalance - minBalance) / range).toFloat()
                                val x = i * stepX
                                val y = height - (normalizedY * height).coerceIn(4f, height - 4f)
                                drawCircle(
                                    color = if (p.projectedBalance >= 0) EmeraldLight else AccentRed,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Trajectory points preview
                val endPoint = points.lastOrNull()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saldo Proyectado al final del plan (${endPoint?.label ?: ""}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = formatCurrency(endPoint?.projectedBalance ?: 0.0, currencySymbol),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if ((endPoint?.projectedBalance ?: 0.0) >= 0) EmeraldLight else AccentRed
                    )
                }
            } else {
                Text(
                    text = "Registra gastos o cuotas para proyectar el flujo de caja.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

// Dialog to inspect projected cuotas and mark them as paid / unpaid
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScheduleDialog(
    commitment: ExpenseCommitment,
    accounts: List<Account>,
    rates: ExchangeRates,
    displayCurrency: AppCurrency,
    onDismiss: () -> Unit,
    onToggleInstallment: (Long, Boolean, Account?) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onUpdateCommitment: ((ExpenseCommitment) -> Unit)? = null
) {
    val context = LocalContext.current
    val installments = commitment.getProjectedInstallments()
    val commCurrency = AppCurrency.fromCode(commitment.currency)
    val freqEnum = remember(commitment.frequency) {
        Frequency.entries.find { it.name == commitment.frequency } ?: Frequency.MONTHLY
    }
    var selectedDebitAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    val selectedAccount = accounts.find { it.id == selectedDebitAccountId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .testTag("installments_schedule_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detalles y Cronograma",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = commitment.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldLight
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                // Action buttons: Edit and Delete inside modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onEdit != null) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEdit()
                            },
                            modifier = Modifier.weight(1f).testTag("dialog_edit_commitment_${commitment.id}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Editar")
                        }
                    }

                    if (onDelete != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onDelete()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            modifier = Modifier.weight(1f).testTag("dialog_delete_commitment_${commitment.id}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Eliminar")
                        }
                    }
                }

                // Account selector for auto-debiting when marking as paid
                if (accounts.isNotEmpty()) {
                    var accExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = accExpanded,
                        onExpandedChange = { accExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: "Seleccionar cuenta para debitar",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cuenta para debitar al pagar") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = accExpanded,
                            onDismissRequest = { accExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.currency})") },
                                    onClick = {
                                        selectedDebitAccountId = acc.id
                                        accExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cuotas proyectadas (${installments.size}):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Toca fecha para editar en cascada",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight,
                        fontSize = 10.sp
                    )
                }

                // List of installments
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    installments.forEachIndexed { idx, inst ->
                        val amountInDisp = rates.convert(inst.amount, commCurrency, displayCurrency)
                        Surface(
                            color = if (inst.isPaid) EmeraldDark.copy(alpha = 0.2f) else SlateBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = inst.isPaid,
                                        onCheckedChange = { isChecked ->
                                            onToggleInstallment(inst.number, isChecked, if (isChecked) selectedAccount else null)
                                        },
                                        modifier = Modifier.testTag("toggle_inst_${inst.number}")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Cuota #${inst.number}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (inst.isPaid) EmeraldLight else TextPrimaryDark,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldDark.copy(alpha = 0.35f),
                                            modifier = Modifier.clickable {
                                                showPlatformDatePicker(context, inst.dueDate.toDate()) { newDate ->
                                                    val cascaded = cascadeRecalculateInstallments(
                                                        schedule = installments,
                                                        changedIndex = idx,
                                                        newDate = newDate,
                                                        frequency = freqEnum,
                                                        customDays = commitment.customDays
                                                    )
                                                    val updatedCommitment = commitment.copy(
                                                        installments = cascaded.map { it.toMap() }
                                                    )
                                                    onUpdateCommitment?.invoke(updatedCommitment)
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.EditCalendar,
                                                    contentDescription = "Editar fecha",
                                                    tint = EmeraldLight,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = formatDate(inst.dueDate.toDate()),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldLight,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(inst.amount, commCurrency.symbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (inst.isPaid) EmeraldLight else TextPrimaryDark,
                                        fontSize = 13.sp
                                    )
                                    if (commCurrency != displayCurrency) {
                                        Text(
                                            text = "≈ ${formatCurrency(amountInDisp, displayCurrency.symbol)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryDark,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Listo")
                }
            }
        }
    }
}

// Dialog for Currency & Safety Cushion settings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsDialog(
    currentDisplay: AppCurrency,
    currentPayment: AppCurrency,
    currentCushion: Double,
    onDismiss: () -> Unit,
    onSave: (AppCurrency, AppCurrency, Double) -> Unit
) {
    var displayCurrency by remember { mutableStateOf(currentDisplay) }
    var paymentCurrency by remember { mutableStateOf(currentPayment) }
    var cushionText by remember { mutableStateOf(currentCushion.toString()) }
    var dispExpanded by remember { mutableStateOf(false) }
    var payExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .testTag("currency_settings_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configuración Monetaria",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                // Moneda para ver
                Text("Moneda para VER (Consolidado de la App):", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                ExposedDropdownMenuBox(
                    expanded = dispExpanded,
                    onExpandedChange = { dispExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${displayCurrency.flagEmoji} ${displayCurrency.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dispExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dispExpanded,
                        onDismissRequest = { dispExpanded = false }
                    ) {
                        AppCurrency.entries.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text("${curr.flagEmoji} ${curr.displayName}") },
                                onClick = {
                                    displayCurrency = curr
                                    dispExpanded = false
                                }
                            )
                        }
                    }
                }

                // Moneda para pagar
                Text("Moneda preferida para PAGAR:", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                ExposedDropdownMenuBox(
                    expanded = payExpanded,
                    onExpandedChange = { payExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${paymentCurrency.flagEmoji} ${paymentCurrency.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = payExpanded,
                        onDismissRequest = { payExpanded = false }
                    ) {
                        AppCurrency.entries.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text("${curr.flagEmoji} ${curr.displayName}") },
                                onClick = {
                                    paymentCurrency = curr
                                    payExpanded = false
                                }
                            )
                        }
                    }
                }

                // Colchón de seguridad
                Text("Monto del Colchón de Seguridad Recomendado:", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                OutlinedTextField(
                    value = cushionText,
                    onValueChange = { cushionText = it },
                    label = { Text("Monto mínimo en ${displayCurrency.symbol}") },
                    modifier = Modifier.fillMaxWidth().testTag("cushion_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val parsed = cushionText.toDoubleOrNull() ?: 150.0
                            onSave(displayCurrency, paymentCurrency, parsed)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).testTag("save_currency_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

// Dialog to transfer to or from savings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSavingsDialog(
    accounts: List<Account>,
    suggestedAmount: Double,
    displayCurrency: AppCurrency,
    onDismiss: () -> Unit,
    onConfirm: (Account, Account, Double) -> Unit
) {
    val liquidAccounts = accounts.filter { !it.isSavings }
    val savingsAccounts = accounts.filter { it.isSavings }

    var isAddingToSavings by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf(if (suggestedAmount > 0) String.format(Locale.US, "%.2f", suggestedAmount) else "50.0") }

    var selectedFromAccountId by remember { mutableStateOf(liquidAccounts.firstOrNull()?.id ?: "") }
    var selectedToAccountId by remember { mutableStateOf(savingsAccounts.firstOrNull()?.id ?: "") }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .testTag("transfer_savings_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fondo de Ahorro y Colchón",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                // Mode Tabs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAddingToSavings) EmeraldPrimary else SlateBackground,
                        modifier = Modifier.weight(1f).clickable { isAddingToSavings = true }
                    ) {
                        Text(
                            text = "Apartar a Ahorros",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAddingToSavings) NavySurface else TextSecondaryDark,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isAddingToSavings) AccentAmber else SlateBackground,
                        modifier = Modifier.weight(1f).clickable { isAddingToSavings = false }
                    ) {
                        Text(
                            text = "Disponer de Ahorros",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!isAddingToSavings) NavySurface else TextSecondaryDark,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto a transferir") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val fromSource = if (isAddingToSavings) liquidAccounts else savingsAccounts
                val toDest = if (isAddingToSavings) savingsAccounts else liquidAccounts

                val fromAcc = fromSource.find { it.id == selectedFromAccountId } ?: fromSource.firstOrNull()
                val toAcc = toDest.find { it.id == selectedToAccountId } ?: toDest.firstOrNull()

                Text("Desde:", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Text(
                    text = "${fromAcc?.name ?: "Sin cuenta"} (${fromAcc?.currency ?: ""})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text("Hacia:", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                Text(
                    text = "${toAcc?.name ?: "Sin cuenta de destino"} (${toAcc?.currency ?: ""})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val parsed = amountText.toDoubleOrNull() ?: 0.0
                            if (parsed > 0 && fromAcc != null && toAcc != null) {
                                onConfirm(fromAcc, toAcc, parsed)
                                onDismiss()
                            }
                        },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && fromAcc != null && toAcc != null,
                        modifier = Modifier.weight(1f).testTag("confirm_savings_transfer_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}
