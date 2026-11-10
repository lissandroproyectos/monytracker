package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.currency.AppCurrency
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.CommitmentStatus
import com.example.data.model.ExpenseCommitment
import com.example.data.model.Frequency
import com.example.data.model.InstallmentPayment
import com.example.data.model.MarketTemplate
import com.example.data.model.PaymentMode
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.CheckCircle
import com.example.data.model.ProjectedInstallment
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

val COLOR_OPTIONS = listOf(
    "#10B981", "#3B82F6", "#8B5CF6", "#F59E0B", "#EF4444", "#06B6D4", "#EC4899"
)

val CATEGORY_EXPENSES = listOf(
    "Alimentación", "Vivienda", "Transporte", "Servicios", "Salud",
    "Educación", "Entretenimiento", "Compras", "Tecnología", "Otros"
)

val CATEGORY_INCOMES = listOf(
    "Salario", "Ventas", "Honorarios", "Inversiones", "Regalo", "Otros"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectorDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categoriesList: List<String>,
    isIncome: Boolean = false,
    onCreateCategory: ((String, Boolean) -> Unit)? = null,
    label: String = "Categoría"
) {
    var expanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("category_selector_field")
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categoriesList.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat) },
                    onClick = {
                        onCategorySelected(cat)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("➕ Crear nueva categoría...", fontWeight = FontWeight.Bold, color = EmeraldLight) },
                onClick = {
                    expanded = false
                    showCreateDialog = true
                },
                modifier = Modifier.testTag("create_new_category_option")
            )
        }
    }

    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva Categoría", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Ingresa el nombre de la nueva categoría:", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Nombre (ej. Mascotas, Gimnasio, Cripto)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_category_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newCatName.trim()
                        if (name.isNotBlank()) {
                            onCreateCategory?.invoke(name, isIncome)
                            onCategorySelected(name)
                            showCreateDialog = false
                            newCatName = ""
                        }
                    },
                    enabled = newCatName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("confirm_create_category_btn")
                ) {
                    Text("Guardar Categoría")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = SlateCard
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    accountToEdit: Account? = null,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var selectedType by remember {
        mutableStateOf(
            if (accountToEdit != null) AccountType.entries.find { it.name == accountToEdit.type } ?: AccountType.CASH
            else AccountType.CASH
        )
    }
    var selectedCurrency by remember {
        mutableStateOf(
            if (accountToEdit != null) AppCurrency.fromCode(accountToEdit.currency)
            else AppCurrency.USD_BCV
        )
    }
    var balanceText by remember {
        mutableStateOf(if (accountToEdit != null) String.format(Locale.US, "%.2f", accountToEdit.currentBalance) else "")
    }
    var isSavings by remember { mutableStateOf(accountToEdit?.isSavings ?: false) }
    var selectedColor by remember { mutableStateOf(accountToEdit?.colorHex ?: COLOR_OPTIONS[0]) }

    // Periodic Income fields
    var hasPeriodicIncome by remember { mutableStateOf(accountToEdit?.hasPeriodicIncome ?: false) }
    var periodicIncomeAmountText by remember {
        mutableStateOf(if (accountToEdit != null && accountToEdit.hasPeriodicIncome) String.format(Locale.US, "%.2f", accountToEdit.periodicIncomeAmount) else "")
    }
    var periodicFreq by remember { mutableStateOf(accountToEdit?.periodicIncomeFrequency ?: "QUINCENAL") }
    var periodicDayOfMonthText by remember {
        mutableStateOf((accountToEdit?.periodicIncomeDayOfMonth ?: 15).toString())
    }
    var periodicIncomeDesc by remember {
        mutableStateOf(accountToEdit?.periodicIncomeDescription ?: "Sueldo / Nómina")
    }

    var typeExpanded by remember { mutableStateOf(false) }
    var currExpanded by remember { mutableStateOf(false) }
    var periodicFreqExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .testTag("add_account_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Pinned Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (accountToEdit != null) "Editar Cuenta" else "Nueva Cuenta / Billetera",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                        }
                    }

                    // Scrollable Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre (ej. Banesco Bs, Efectivo USD, Binance)") },
                            modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
                            singleLine = true
                        )

                        // Currency Dropdown
                        ExposedDropdownMenuBox(
                            expanded = currExpanded,
                            onExpandedChange = { currExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = "${selectedCurrency.flagEmoji} ${selectedCurrency.displayName}",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Moneda de la Cuenta") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = currExpanded,
                                onDismissRequest = { currExpanded = false }
                            ) {
                                AppCurrency.entries.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text("${curr.flagEmoji} ${curr.displayName}") },
                                        onClick = {
                                            selectedCurrency = curr
                                            currExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Account Type Dropdown
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedType.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de Cuenta") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                AccountType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.displayName) },
                                        onClick = {
                                            selectedType = type
                                            if (type == AccountType.SAVINGS) isSavings = true
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = balanceText,
                            onValueChange = { balanceText = it },
                            label = { Text(if (accountToEdit != null) "Saldo Actual (${selectedCurrency.symbol})" else "Saldo Inicial (${selectedCurrency.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("account_balance_input"),
                            singleLine = true
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSavings,
                                onCheckedChange = { isSavings = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Destinar como Fondo de Ahorro / Colchón",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimaryDark
                            )
                        }

                        // Periodic Income Section
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCardLight),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = hasPeriodicIncome,
                                        onCheckedChange = { hasPeriodicIncome = it },
                                        modifier = Modifier.testTag("toggle_periodic_income")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "¿Recibe dinero periódico en esta cuenta?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "Sueldo, honorarios, remesas periódicas",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (hasPeriodicIncome) {
                                    OutlinedTextField(
                                        value = periodicIncomeDesc,
                                        onValueChange = { periodicIncomeDesc = it },
                                        label = { Text("Concepto (ej. Nómina Quincenal, Remesa)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = periodicIncomeAmountText,
                                        onValueChange = { periodicIncomeAmountText = it },
                                        label = { Text("Monto periódico (${selectedCurrency.displayName})") },
                                        prefix = { Text("${selectedCurrency.symbol} ") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth().testTag("periodic_income_amount_input"),
                                        singleLine = true
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = periodicFreqExpanded,
                                        onExpandedChange = { periodicFreqExpanded = it }
                                    ) {
                                        val freqLabel = when (periodicFreq) {
                                            "QUINCENAL" -> "Quincenal (Día 15 y fin de mes)"
                                            "SEMANAL" -> "Semanal (Cada 7 días)"
                                            else -> "Mensual (Día fijo de cada mes)"
                                        }
                                        OutlinedTextField(
                                            value = freqLabel,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Frecuencia de Cobro") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodicFreqExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = periodicFreqExpanded,
                                            onDismissRequest = { periodicFreqExpanded = false }
                                        ) {
                                            listOf(
                                                "QUINCENAL" to "Quincenal (Día 15 y fin de mes)",
                                                "MENSUAL" to "Mensual (Día fijo)",
                                                "SEMANAL" to "Semanal (Cada 7 días)"
                                            ).forEach { (code, label) ->
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        periodicFreq = code
                                                        periodicFreqExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (periodicFreq == "MENSUAL") {
                                        OutlinedTextField(
                                            value = periodicDayOfMonthText,
                                            onValueChange = { periodicDayOfMonthText = it },
                                            label = { Text("Día del mes (1 - 31)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Color distintivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            COLOR_OPTIONS.forEach { hex ->
                                val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { EmeraldLight }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColor = hex }
                                        .then(
                                            if (selectedColor == hex) Modifier.border(2.5.dp, Color.White, CircleShape)
                                            else Modifier
                                        )
                                )
                            }
                        }
                    }

                    // Pinned Footer Actions
                    Surface(
                        color = SlateCard,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text("Cancelar", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                            }

                            if (onDelete != null) {
                                Button(
                                    onClick = {
                                        onDelete()
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).testTag("delete_account_dialog_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                ) {
                                    Text("Eliminar", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    val parsed = balanceText.toDoubleOrNull() ?: 0.0
                                    val parsedPeriodicAmount = periodicIncomeAmountText.toDoubleOrNull() ?: 0.0
                                    val parsedDay = periodicDayOfMonthText.toIntOrNull() ?: 15
                                    if (name.isNotBlank()) {
                                        onSave(
                                            Account(
                                                id = accountToEdit?.id ?: "",
                                                userId = accountToEdit?.userId ?: "",
                                                name = name.trim(),
                                                type = selectedType.name,
                                                currentBalance = parsed,
                                                initialBalance = if (accountToEdit != null) accountToEdit.initialBalance else parsed,
                                                currency = selectedCurrency.name,
                                                colorHex = selectedColor,
                                                iconName = selectedType.iconRes,
                                                isSavings = isSavings,
                                                hasPeriodicIncome = hasPeriodicIncome,
                                                periodicIncomeAmount = parsedPeriodicAmount,
                                                periodicIncomeCurrency = selectedCurrency.name,
                                                periodicIncomeFrequency = periodicFreq,
                                                periodicIncomeDayOfMonth = parsedDay.coerceIn(1, 31),
                                                periodicIncomeDescription = periodicIncomeDesc.trim().ifBlank { "Sueldo" },
                                                createdAt = accountToEdit?.createdAt
                                            )
                                        )
                                        onDismiss()
                                    }
                                },
                                enabled = name.isNotBlank(),
                                modifier = Modifier.weight(1.1f).testTag("save_account_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(if (accountToEdit != null) "Actualizar" else "Guardar", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    initialType: String = TransactionType.EXPENSE.name,
    accounts: List<Account>,
    customExpenseCategories: List<String> = emptyList(),
    customIncomeCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onCreateCategory: ((String, Boolean) -> Unit)? = null
) {
    var type by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isFixedExpense by remember { mutableStateOf(false) }

    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var targetAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }

    val currentAcc = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
    var selectedCurrency by remember {
        mutableStateOf(AppCurrency.fromCode(currentAcc?.currency))
    }

    var selectedCategory by remember {
        mutableStateOf(if (initialType == TransactionType.INCOME.name) CATEGORY_INCOMES[0] else CATEGORY_EXPENSES[0])
    }

    var accountExpanded by remember { mutableStateOf(false) }
    var targetAccountExpanded by remember { mutableStateOf(false) }
    var currExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .testTag("add_tx_dialog"),
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
                        text = "Registrar Movimiento",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                // Type Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        TransactionType.EXPENSE.name to "Egreso",
                        TransactionType.INCOME.name to "Ingreso",
                        TransactionType.TRANSFER.name to "Transferir"
                    ).forEach { (tKey, label) ->
                        val isSelected = type == tKey
                        val activeColor = when (tKey) {
                            TransactionType.INCOME.name -> EmeraldLight
                            TransactionType.TRANSFER.name -> AccentAmber
                            else -> AccentRed
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) activeColor else SlateBackground,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    type = tKey
                                    selectedCategory = if (tKey == TransactionType.INCOME.name) CATEGORY_INCOMES[0] else CATEGORY_EXPENSES[0]
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NavySurface else TextSecondaryDark,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.3f).testTag("tx_amount_input"),
                        singleLine = true
                    )

                    // Currency picker
                    ExposedDropdownMenuBox(
                        expanded = currExpanded,
                        onExpandedChange = { currExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency.symbolWithCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Moneda") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = currExpanded,
                            onDismissRequest = { currExpanded = false }
                        ) {
                            AppCurrency.entries.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.flagEmoji} ${c.symbolWithCode} - ${c.displayName}") },
                                    onClick = {
                                        selectedCurrency = c
                                        currExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (ej. Mercado, Alquiler, Salario)") },
                    modifier = Modifier.fillMaxWidth().testTag("tx_desc_input"),
                    singleLine = true
                )

                if (type == TransactionType.EXPENSE.name) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isFixedExpense,
                            onCheckedChange = { isFixedExpense = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Marcar como Gasto Fijo Recurrente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryDark
                        )
                    }
                }

                if (type != TransactionType.TRANSFER.name) {
                    val isInc = type == TransactionType.INCOME.name
                    val baseList = if (isInc) CATEGORY_INCOMES else CATEGORY_EXPENSES
                    val customList = if (isInc) customIncomeCategories else customExpenseCategories
                    val combinedList = (baseList + customList).distinct()

                    CategorySelectorDropdown(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        categoriesList = combinedList,
                        isIncome = isInc,
                        onCreateCategory = onCreateCategory
                    )
                }

                // Filter accounts: Income cannot target plan start account; Expense can use plan start account if balance > 0
                val selectableAccounts = remember(accounts, type) {
                    if (type == TransactionType.INCOME.name) {
                        accounts.filter { !it.isPlanStartAccount }
                    } else {
                        accounts.filter { !it.isPlanStartAccount || it.currentBalance > 0 }
                    }
                }

                // Origin Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val currentAccName = selectableAccounts.find { it.id == selectedAccountId }?.name ?: "Seleccionar Cuenta"
                    OutlinedTextField(
                        value = currentAccName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (type == TransactionType.TRANSFER.name) "Cuenta Origen" else "Cuenta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        selectableAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.currency})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    selectedCurrency = AppCurrency.fromCode(acc.currency)
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                if (type == TransactionType.TRANSFER.name) {
                    ExposedDropdownMenuBox(
                        expanded = targetAccountExpanded,
                        onExpandedChange = { targetAccountExpanded = it }
                    ) {
                        val targetAccName = accounts.find { it.id == targetAccountId }?.name ?: "Seleccionar Destino"
                        OutlinedTextField(
                            value = targetAccName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cuenta Destino") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetAccountExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = targetAccountExpanded,
                            onDismissRequest = { targetAccountExpanded = false }
                        ) {
                            accounts.filter { it.id != selectedAccountId && !it.isPlanStartAccount }.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.currency})") },
                                    onClick = {
                                        targetAccountId = acc.id
                                        targetAccountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            if (parsed > 0 && selectedAccountId.isNotBlank()) {
                                onSave(
                                    Transaction(
                                        accountId = selectedAccountId,
                                        targetAccountId = if (type == TransactionType.TRANSFER.name) targetAccountId else null,
                                        type = type,
                                        amount = parsed,
                                        currency = selectedCurrency.name,
                                        isFixedExpense = isFixedExpense,
                                        category = if (type == TransactionType.TRANSFER.name) "Transferencia" else selectedCategory,
                                        description = description.trim().ifBlank { selectedCategory },
                                        date = Timestamp.now()
                                    )
                                )
                                onDismiss()
                            }
                        },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && selectedAccountId.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("save_tx_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

fun showPlatformDatePicker(context: android.content.Context, initialDate: Date, onDateSelected: (Date) -> Unit) {
    val cal = Calendar.getInstance().apply { time = initialDate }
    android.app.DatePickerDialog(
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
            onDateSelected(sel)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// Cascade recalculates all subsequent installments when any installment's date is adjusted
fun cascadeRecalculateInstallments(
    schedule: List<ProjectedInstallment>,
    changedIndex: Int,
    newDate: Date,
    frequency: Frequency,
    customDays: Int
): List<ProjectedInstallment> {
    val updated = schedule.toMutableList()
    // 1. Update the chosen installment
    updated[changedIndex] = updated[changedIndex].copy(dueDate = Timestamp(newDate))

    val baseCal = Calendar.getInstance().apply { time = newDate }
    val baseDayOfMonth = baseCal.get(Calendar.DAY_OF_MONTH)

    // 2. Cascade recalculate all subsequent installments based on the newly adjusted date
    var step = 1
    for (i in (changedIndex + 1) until updated.size) {
        val nextCal = baseCal.clone() as Calendar
        when (frequency) {
            Frequency.WEEKLY -> nextCal.add(Calendar.WEEK_OF_YEAR, step)
            Frequency.CASHEA -> nextCal.add(Calendar.DAY_OF_YEAR, step * 14)
            Frequency.BIWEEKLY -> nextCal.add(Calendar.DAY_OF_YEAR, step * 15)
            Frequency.MONTHLY -> {
                nextCal.add(Calendar.MONTH, step)
                val maxDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                nextCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
            }
            Frequency.BIMONTHLY -> {
                nextCal.add(Calendar.MONTH, step * 2)
                val maxDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                nextCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
            }
            Frequency.QUARTERLY -> {
                nextCal.add(Calendar.MONTH, step * 3)
                val maxDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                nextCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
            }
            Frequency.FOUR_MONTHLY -> {
                nextCal.add(Calendar.MONTH, step * 4)
                val maxDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                nextCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
            }
            Frequency.SEMIANNUAL -> {
                nextCal.add(Calendar.MONTH, step * 6)
                val maxDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                nextCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
            }
            Frequency.ANNUAL -> {
                nextCal.add(Calendar.YEAR, step)
                val maxDay = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                nextCal.set(Calendar.DAY_OF_MONTH, baseDayOfMonth.coerceAtMost(maxDay))
            }
            Frequency.CUSTOM -> nextCal.add(Calendar.DAY_OF_YEAR, step * customDays)
        }
        updated[i] = updated[i].copy(dueDate = Timestamp(nextCal.time))
        step++
    }
    return updated
}

// State holder for AddCommitmentDialog to prevent Dalvik/ART register overflow (VerifyError)
class AddCommitmentState(
    val commitmentToEdit: ExpenseCommitment?,
    val prefilledTemplate: MarketTemplate?,
    val initialIsFixed: Boolean,
    defaultAccountIdInitial: String
) {
    var title by mutableStateOf(commitmentToEdit?.title ?: prefilledTemplate?.title ?: "")
    var isFixedExpense by mutableStateOf(commitmentToEdit?.isFixedExpense ?: initialIsFixed)
    var commitmentStartDate by mutableStateOf(try { commitmentToEdit?.startDate?.toDate() } catch (e: Exception) { null } ?: Date())
    var totalAmountText by mutableStateOf(if (commitmentToEdit != null) commitmentToEdit.totalAmount.toString() else "")
    var hasDownPayment by mutableStateOf((commitmentToEdit?.downPayment ?: 0.0) > 0.0)
    var downPaymentText by mutableStateOf(if ((commitmentToEdit?.downPayment ?: 0.0) > 0.0) commitmentToEdit?.downPayment?.toString() ?: "" else "")
    var alreadyPaidCountText by mutableStateOf((commitmentToEdit?.paidInstallments ?: 0L).toString())
    var fixedExpenseDueDate by mutableStateOf(try { commitmentToEdit?.dueDate?.toDate() } catch (e: Exception) { null } ?: Date())
    var hasEndDate by mutableStateOf(commitmentToEdit?.endDate != null)
    var fixedExpenseEndDate by mutableStateOf(try { commitmentToEdit?.endDate?.toDate() } catch (e: Exception) { null } ?: Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time)
    var fixedCyclesCountText by mutableStateOf((if (commitmentToEdit != null && commitmentToEdit.installments.isNotEmpty()) commitmentToEdit.installments.size.toLong() else 6L).toString())
    var selectedCurrency by mutableStateOf(
        if (commitmentToEdit != null) AppCurrency.fromCode(commitmentToEdit.currency)
        else AppCurrency.USD_BCV
    )
    var paymentMode by mutableStateOf(
        if (commitmentToEdit != null) PaymentMode.entries.find { it.name == commitmentToEdit.paymentMode } ?: PaymentMode.INSTALLMENTS
        else if (prefilledTemplate != null) PaymentMode.entries.find { it.name == prefilledTemplate.paymentMode } ?: PaymentMode.INSTALLMENTS
        else PaymentMode.INSTALLMENTS
    )
    var installmentsCountText by mutableStateOf((commitmentToEdit?.totalInstallments ?: 3L).toString())
    var hasInstallments by mutableStateOf(
        if (commitmentToEdit != null) {
            commitmentToEdit.totalInstallments > 1L || commitmentToEdit.paymentMode == PaymentMode.INSTALLMENTS.name
        } else false
    )
    var applyInterest by mutableStateOf((commitmentToEdit?.interestRate ?: prefilledTemplate?.interestRate ?: 0.0) > 0.0)
    var isAnnualInterest by mutableStateOf(commitmentToEdit?.isAnnualInterest ?: prefilledTemplate?.isAnnualInterest ?: false)
    var interestRateText by mutableStateOf((commitmentToEdit?.interestRate ?: prefilledTemplate?.interestRate ?: 0.0).let { if (it > 0) it.toString() else "5.0" })
    var customDaysText by mutableStateOf((commitmentToEdit?.customDays ?: prefilledTemplate?.customDays ?: 30).toString())
    var frequency by mutableStateOf(
        if (commitmentToEdit != null) Frequency.entries.find { it.name == commitmentToEdit.frequency } ?: Frequency.MONTHLY
        else if (prefilledTemplate != null) Frequency.entries.find { it.name == prefilledTemplate.frequency } ?: Frequency.BIWEEKLY
        else Frequency.MONTHLY
    )
    var selectedCategory by mutableStateOf(commitmentToEdit?.category ?: prefilledTemplate?.category ?: CATEGORY_EXPENSES[0])
    var defaultAccountId by mutableStateOf(commitmentToEdit?.accountId ?: defaultAccountIdInitial)
    var notes by mutableStateOf(commitmentToEdit?.notes ?: prefilledTemplate?.description ?: "")

    private val initialCal = Calendar.getInstance().apply {
        time = (try { commitmentToEdit?.startDate?.toDate() } catch (e: Exception) { null } ?: Date())
    }

    var paymentDayOfMonth by mutableStateOf(
        if (commitmentToEdit != null && commitmentToEdit.paymentDayOfMonth in 1..31) {
            commitmentToEdit.paymentDayOfMonth
        } else {
            initialCal.get(Calendar.DAY_OF_MONTH)
        }
    )

    var secondPaymentDay by mutableStateOf(
        if (commitmentToEdit != null && commitmentToEdit.secondPaymentDay in 1..31) {
            commitmentToEdit.secondPaymentDay
        } else {
            val d = if (commitmentToEdit != null && commitmentToEdit.paymentDayOfMonth in 1..31) commitmentToEdit.paymentDayOfMonth else initialCal.get(Calendar.DAY_OF_MONTH)
            if (d <= 15) d + 15 else (d - 15).coerceAtLeast(1)
        }
    )

    var paymentDayOfWeek by mutableStateOf(
        if (commitmentToEdit != null && commitmentToEdit.paymentDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) {
            commitmentToEdit.paymentDayOfWeek
        } else {
            initialCal.get(Calendar.DAY_OF_WEEK)
        }
    )

    fun onUpdatePaymentDayOfMonth(day: Int) {
        val clamped = day.coerceIn(1, 31)
        paymentDayOfMonth = clamped
        if (frequency == Frequency.BIWEEKLY) {
            secondPaymentDay = if (clamped <= 15) clamped + 15 else (clamped - 15).coerceAtLeast(1)
        }
        customInstallments = emptyList()
        customFixedInstallments = emptyList()
    }

    fun onUpdateSecondPaymentDay(day: Int) {
        secondPaymentDay = day.coerceIn(1, 31)
        customInstallments = emptyList()
        customFixedInstallments = emptyList()
    }

    fun onUpdatePaymentDayOfWeek(dow: Int) {
        paymentDayOfWeek = dow
        customInstallments = emptyList()
        customFixedInstallments = emptyList()
    }

    var showTemplatePicker by mutableStateOf(false)
    var showCreateCustomTemplateForm by mutableStateOf(false)
    var customTemplateTitle by mutableStateOf("")
    var customTemplateCategory by mutableStateOf(CATEGORY_EXPENSES[0])
    var customTemplateFreq by mutableStateOf(Frequency.BIWEEKLY)
    var customTemplatePaymentMode by mutableStateOf(PaymentMode.INSTALLMENTS)
    var customTemplateApplyInterest by mutableStateOf(false)
    var customTemplateIsAnnual by mutableStateOf(false)
    var customTemplateInterestText by mutableStateOf("5.0")
    var customTemplateDaysText by mutableStateOf("30")
    var customTemplateDesc by mutableStateOf("")

    var freqExpanded by mutableStateOf(false)
    var categoryExpanded by mutableStateOf(false)
    var currExpanded by mutableStateOf(false)
    var customFreqExpanded by mutableStateOf(false)
    var customCatExpanded by mutableStateOf(false)

    var customInstallments by mutableStateOf<List<ProjectedInstallment>>(
        if (commitmentToEdit != null && !commitmentToEdit.isFixedExpense && commitmentToEdit.installments.isNotEmpty()) {
            commitmentToEdit.getProjectedInstallments()
        } else emptyList()
    )

    var customFixedInstallments by mutableStateOf<List<ProjectedInstallment>>(
        if (commitmentToEdit != null && commitmentToEdit.isFixedExpense && commitmentToEdit.installments.isNotEmpty()) {
            commitmentToEdit.getProjectedInstallments()
        } else emptyList()
    )

    val totalAmount: Double get() = totalAmountText.toDoubleOrNull() ?: 0.0
    val downPayment: Double get() = if (hasDownPayment) (downPaymentText.toDoubleOrNull() ?: 0.0) else 0.0
    val financedBase: Double get() = (totalAmount - downPayment).coerceAtLeast(0.0)
    val rawInterestRate: Double get() = if (applyInterest) (interestRateText.toDoubleOrNull() ?: 0.0) else 0.0
    val totalInstallments: Long get() = if (hasInstallments) (installmentsCountText.toLongOrNull() ?: 1L).coerceAtLeast(1L) else 1L
    val alreadyPaidCount: Long get() = (alreadyPaidCountText.toLongOrNull() ?: 0L).coerceIn(0L, totalInstallments)
    val customDays: Int get() = customDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 30

    val termInMonths: Double get() = when (frequency) {
        Frequency.WEEKLY -> (totalInstallments * 7.0) / 30.0
        Frequency.CASHEA -> (totalInstallments * 14.0) / 30.0
        Frequency.BIWEEKLY -> (totalInstallments * 15.0) / 30.0
        Frequency.MONTHLY -> totalInstallments.toDouble()
        Frequency.BIMONTHLY -> totalInstallments.toDouble() * 2.0
        Frequency.QUARTERLY -> totalInstallments.toDouble() * 3.0
        Frequency.FOUR_MONTHLY -> totalInstallments.toDouble() * 4.0
        Frequency.SEMIANNUAL -> totalInstallments.toDouble() * 6.0
        Frequency.ANNUAL -> totalInstallments.toDouble() * 12.0
        Frequency.CUSTOM -> (totalInstallments * customDays.toDouble()) / 30.0
    }

    val effectiveInterestRate: Double get() = if (rawInterestRate > 0.0) {
        if (isAnnualInterest) {
            (rawInterestRate / 12.0) * termInMonths
        } else {
            rawInterestRate * termInMonths
        }
    } else 0.0

    val totalFinancedWithInterest: Double get() = if (effectiveInterestRate > 0) financedBase * (1.0 + effectiveInterestRate / 100.0) else financedBase
    val totalWithInterest: Double get() = downPayment + totalFinancedWithInterest
    val calculatedInstallmentAmount: Double get() = if (totalInstallments > 0 && totalFinancedWithInterest > 0) kotlin.math.round((totalFinancedWithInterest / totalInstallments) * 100.0) / 100.0 else 0.0

    fun computeActiveSchedule(): List<ProjectedInstallment> {
        return if (customInstallments.isNotEmpty() && customInstallments.size == totalInstallments.toInt()) {
            customInstallments
        } else {
            val temp = ExpenseCommitment(
                startDate = Timestamp(commitmentStartDate),
                totalInstallments = totalInstallments,
                paidInstallments = alreadyPaidCount,
                installmentAmount = calculatedInstallmentAmount,
                frequency = frequency.name,
                paymentDayOfMonth = paymentDayOfMonth,
                secondPaymentDay = secondPaymentDay,
                paymentDayOfWeek = paymentDayOfWeek,
                customDays = customDays
            )
            temp.getProjectedInstallments()
        }
    }

    fun computeActiveFixedSchedule(): List<ProjectedInstallment> {
        val maxLimit = if (hasEndDate) 120 else (fixedCyclesCountText.toIntOrNull() ?: 6).coerceIn(1, 48)
        val temp = ExpenseCommitment(
            startDate = Timestamp(fixedExpenseDueDate),
            endDate = if (hasEndDate) {
                val endCal = Calendar.getInstance().apply {
                    time = fixedExpenseEndDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Timestamp(endCal.time)
            } else null,
            totalInstallments = maxLimit.toLong(),
            installmentAmount = totalAmount,
            frequency = frequency.name,
            paymentDayOfMonth = paymentDayOfMonth,
            secondPaymentDay = secondPaymentDay,
            paymentDayOfWeek = paymentDayOfWeek,
            customDays = customDays
        )
        val generated = temp.getProjectedInstallments()
        return if (customFixedInstallments.isNotEmpty()) {
            generated.mapIndexed { idx, p ->
                val custom = customFixedInstallments.getOrNull(idx)
                if (custom != null) {
                    custom.copy(isPaid = custom.isPaid)
                } else p
            }
        } else {
            generated
        }
    }


    fun buildCommitmentToSave(): ExpenseCommitment? {
        if (title.isBlank() || totalAmount <= 0) return null
        return if (isFixedExpense) {
            val effectiveFixedSchedule = computeActiveFixedSchedule()
            val remaining = effectiveFixedSchedule.filter { !it.isPaid }.sumOf { it.amount }.let { if (it > 0) it else totalAmount }
            ExpenseCommitment(
                id = commitmentToEdit?.id ?: "",
                userId = commitmentToEdit?.userId ?: "",
                title = title.trim(),
                totalAmount = totalAmount,
                remainingAmount = remaining,
                paymentMode = PaymentMode.INSTALLMENTS.name,
                category = selectedCategory,
                isFixedExpense = true,
                currency = selectedCurrency.name,
                totalInstallments = effectiveFixedSchedule.size.toLong().coerceAtLeast(1L),
                paidInstallments = effectiveFixedSchedule.count { it.isPaid }.toLong(),
                installmentAmount = totalAmount,
                downPayment = 0.0,
                interestRate = 0.0,
                isAnnualInterest = false,
                customDays = customDays,
                frequency = frequency.name,
                paymentDayOfMonth = paymentDayOfMonth,
                secondPaymentDay = secondPaymentDay,
                paymentDayOfWeek = paymentDayOfWeek,
                startDate = effectiveFixedSchedule.firstOrNull()?.dueDate ?: Timestamp(fixedExpenseDueDate),
                endDate = if (hasEndDate) {
                    val endCal = Calendar.getInstance().apply {
                        time = fixedExpenseEndDate
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    Timestamp(endCal.time)
                } else null,
                dueDate = effectiveFixedSchedule.firstOrNull { !it.isPaid }?.dueDate ?: effectiveFixedSchedule.firstOrNull()?.dueDate ?: Timestamp(fixedExpenseDueDate),
                accountId = defaultAccountId.ifBlank { null },
                status = if (effectiveFixedSchedule.isNotEmpty() && effectiveFixedSchedule.all { it.isPaid }) CommitmentStatus.PAID_OFF.name else CommitmentStatus.ACTIVE.name,
                notes = notes.trim().ifBlank { null },
                installments = effectiveFixedSchedule.map { it.toMap() },
                createdAt = commitmentToEdit?.createdAt
            )
        } else {
            val effectiveSchedule = computeActiveSchedule()
            val effectiveTotal = totalWithInterest
            val remaining = effectiveSchedule.filter { !it.isPaid }.sumOf { it.amount }
            ExpenseCommitment(
                id = commitmentToEdit?.id ?: "",
                userId = commitmentToEdit?.userId ?: "",
                title = title.trim(),
                totalAmount = effectiveTotal,
                remainingAmount = remaining,
                paymentMode = if (hasInstallments) PaymentMode.INSTALLMENTS.name else PaymentMode.FULL.name,
                category = selectedCategory,
                isFixedExpense = false,
                currency = selectedCurrency.name,
                totalInstallments = totalInstallments,
                paidInstallments = effectiveSchedule.count { it.isPaid }.toLong(),
                installmentAmount = calculatedInstallmentAmount,
                downPayment = downPayment,
                interestRate = if (applyInterest) rawInterestRate else 0.0,
                isAnnualInterest = isAnnualInterest,
                customDays = customDays,
                frequency = frequency.name,
                paymentDayOfMonth = paymentDayOfMonth,
                secondPaymentDay = secondPaymentDay,
                paymentDayOfWeek = paymentDayOfWeek,
                startDate = Timestamp(commitmentStartDate),
                dueDate = effectiveSchedule.firstOrNull()?.dueDate ?: Timestamp.now(),
                accountId = defaultAccountId.ifBlank { null },
                status = if (remaining <= 0.001) CommitmentStatus.PAID_OFF.name else CommitmentStatus.ACTIVE.name,
                notes = notes.trim().ifBlank { null },
                installments = effectiveSchedule.map { it.toMap() },
                createdAt = commitmentToEdit?.createdAt
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtTemplatePickerDialog(
    state: AddCommitmentState,
    templates: List<MarketTemplate>,
    onSaveCustomTemplate: ((MarketTemplate) -> Unit)?
) {
    Dialog(
        onDismissRequest = { state.showTemplatePicker = false; state.showCreateCustomTemplateForm = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard)
        ) {
            Column(
                modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.showCreateCustomTemplateForm) "Configurar Tipo de Deuda" else "Tipos de Deudas & Plantillas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = { state.showTemplatePicker = false; state.showCreateCustomTemplateForm = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                if (state.showCreateCustomTemplateForm) {
                    Text(
                        text = "Personaliza el modo de pago, frecuencia e intereses. La cantidad de cuotas la indicarás al crear la deuda:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )

                    OutlinedTextField(
                        value = state.customTemplateTitle,
                        onValueChange = { state.customTemplateTitle = it },
                        label = { Text("Nombre del Tipo (ej. Cashea Quincenal, Préstamo 5%, Crédito Bancario)") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_template_title_input"),
                        singleLine = true
                    )

                    Text("Modalidad de pago predeterminada:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PaymentMode.entries.forEach { mode ->
                            val isSel = state.customTemplatePaymentMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) EmeraldPrimary else SlateBackground,
                                modifier = Modifier.weight(1f).clickable { state.customTemplatePaymentMode = mode }
                            ) {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) NavySurface else TextSecondaryDark,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = state.customFreqExpanded,
                            onExpandedChange = { state.customFreqExpanded = it },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            OutlinedTextField(
                                value = state.customTemplateFreq.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Frecuencia") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.customFreqExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = state.customFreqExpanded,
                                onDismissRequest = { state.customFreqExpanded = false }
                            ) {
                                Frequency.entries.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f.displayName) },
                                        onClick = {
                                            state.customTemplateFreq = f
                                            state.customFreqExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.customTemplateInterestText,
                            onValueChange = { state.customTemplateInterestText = it },
                            label = { Text("Interés (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    if (state.customTemplateFreq == Frequency.CUSTOM) {
                        OutlinedTextField(
                            value = state.customTemplateDaysText,
                            onValueChange = { state.customTemplateDaysText = it },
                            label = { Text("Días entre cada cuota (ej. 45)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { state.customTemplateApplyInterest = !state.customTemplateApplyInterest }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = state.customTemplateApplyInterest,
                            onCheckedChange = { state.customTemplateApplyInterest = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "¿Aplica intereses?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    if (state.customTemplateApplyInterest) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!state.customTemplateIsAnnual) EmeraldPrimary else SlateBackground,
                                modifier = Modifier.weight(1f).clickable { state.customTemplateIsAnnual = false }
                            ) {
                                Text(
                                    text = "Mensual",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!state.customTemplateIsAnnual) NavySurface else TextSecondaryDark,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (state.customTemplateIsAnnual) EmeraldPrimary else SlateBackground,
                                modifier = Modifier.weight(1f).clickable { state.customTemplateIsAnnual = true }
                            ) {
                                Text(
                                    text = "Anual",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.customTemplateIsAnnual) NavySurface else TextSecondaryDark,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.customTemplateDesc,
                        onValueChange = { state.customTemplateDesc = it },
                        label = { Text("Descripción o condiciones especiales (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { state.showCreateCustomTemplateForm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Volver")
                        }

                        Button(
                            onClick = {
                                if (state.customTemplateTitle.isNotBlank()) {
                                    val newTmpl = MarketTemplate(
                                        title = state.customTemplateTitle.trim(),
                                        description = state.customTemplateDesc.trim(),
                                        category = state.customTemplateCategory,
                                        paymentMode = state.customTemplatePaymentMode.name,
                                        frequency = state.customTemplateFreq.name,
                                        installmentsCount = 3L,
                                        interestRate = if (state.customTemplateApplyInterest) (state.customTemplateInterestText.toDoubleOrNull() ?: 0.0) else 0.0,
                                        isAnnualInterest = state.customTemplateIsAnnual,
                                        customDays = state.customTemplateDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                                        creatorName = "Personalizado",
                                        upvotes = 1L
                                    )
                                    onSaveCustomTemplate?.invoke(newTmpl)

                                    state.title = newTmpl.title
                                    state.paymentMode = state.customTemplatePaymentMode
                                    state.frequency = state.customTemplateFreq
                                    state.applyInterest = newTmpl.interestRate > 0.0
                                    state.isAnnualInterest = newTmpl.isAnnualInterest
                                    state.interestRateText = newTmpl.interestRate.toString()
                                    state.customDaysText = newTmpl.customDays.toString()
                                    state.selectedCategory = newTmpl.category
                                    state.notes = newTmpl.description
                                    state.showTemplatePicker = false
                                    state.showCreateCustomTemplateForm = false
                                }
                            },
                            enabled = state.customTemplateTitle.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("save_custom_template_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Guardar y Usar")
                        }
                    }
                } else {
                    Text(
                        text = "Selecciona un tipo de deuda o crea una personalizada con tus propias reglas de pago e intereses:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )

                    Button(
                        onClick = { state.showCreateCustomTemplateForm = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_custom_debt_type_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        Text("➕ Personalizar Tipo de Deuda", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(4.dp))

                    val defaultTemplates = if (templates.isNotEmpty()) templates else listOf(
                        MarketTemplate(
                            id = "cashea-classic",
                            title = "Cashea Quincenal (Sin Intereses)",
                            category = "Compras",
                            paymentMode = PaymentMode.INSTALLMENTS.name,
                            frequency = Frequency.BIWEEKLY.name,
                            interestRate = 0.0,
                            description = "Financiamiento Cashea: Inicial en tienda + cuotas quincenales exactas sin intereses.",
                            creatorName = "Sistema"
                        ),
                        MarketTemplate(
                            id = "credito-tarjeta",
                            title = "Crédito Tarjeta / Banco",
                            category = "Finanzas",
                            paymentMode = PaymentMode.INSTALLMENTS.name,
                            frequency = Frequency.MONTHLY.name,
                            interestRate = 12.0,
                            description = "Crédito financiero o consumo amortizable en cuotas mensuales con tasa de interés.",
                            creatorName = "Sistema"
                        ),
                        MarketTemplate(
                            id = "vehicular",
                            title = "Financiamiento Vehicular / Moto",
                            category = "Transporte",
                            paymentMode = PaymentMode.INSTALLMENTS.name,
                            frequency = Frequency.MONTHLY.name,
                            interestRate = 8.0,
                            description = "Plan amortizable mensual para compra o reparación vehicular.",
                            creatorName = "Sistema"
                        ),
                        MarketTemplate(
                            id = "prestamo-personal",
                            title = "Préstamo Personal / USDT",
                            category = "Deuda",
                            paymentMode = PaymentMode.INSTALLMENTS.name,
                            frequency = Frequency.BIWEEKLY.name,
                            interestRate = 5.0,
                            description = "Préstamo flexible entre conocidos o comercial en cripto/dólares.",
                            creatorName = "Sistema"
                        )
                    )

                    defaultTemplates.forEach { tmpl ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, SlateCardLight, RoundedCornerShape(12.dp))
                                .clickable {
                                    state.title = tmpl.title
                                    state.paymentMode = PaymentMode.entries.find { it.name == tmpl.paymentMode } ?: PaymentMode.INSTALLMENTS
                                    state.frequency = Frequency.entries.find { it.name == tmpl.frequency } ?: Frequency.BIWEEKLY
                                    state.applyInterest = tmpl.interestRate > 0.0
                                    state.isAnnualInterest = tmpl.isAnnualInterest
                                    state.interestRateText = tmpl.interestRate.toString()
                                    state.customDaysText = tmpl.customDays.toString()
                                    state.selectedCategory = tmpl.category
                                    state.notes = tmpl.description
                                    state.showTemplatePicker = false
                                },
                            color = SlateCardLight
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(tmpl.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${tmpl.frequency} • ${tmpl.category}" + (if (tmpl.interestRate > 0) " • Interés ${tmpl.interestRate}%" else " • 0% Interés"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldLight
                                )
                                if (tmpl.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(tmpl.description, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebtScheduleCard(
    state: AddCommitmentState,
    context: android.content.Context
) {
    val activeSchedule = state.computeActiveSchedule()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCardLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Cronograma de Cuotas Planificadas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = "Toca para editar (cascada)",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            activeSchedule.forEachIndexed { idx, inst ->
                val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES")).format(inst.dueDate.toDate())
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cuota #${inst.number}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldDark.copy(alpha = 0.35f),
                            modifier = Modifier.clickable {
                                showPlatformDatePicker(context, inst.dueDate.toDate()) { newDate ->
                                    val cascaded = cascadeRecalculateInstallments(
                                        schedule = activeSchedule,
                                        changedIndex = idx,
                                        newDate = newDate,
                                        frequency = state.frequency,
                                        customDays = state.customDays
                                    )
                                    state.customInstallments = cascaded
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(dateFmt, style = MaterialTheme.typography.labelSmall, color = EmeraldLight, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "${state.selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", inst.amount)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (inst.isPaid) EmeraldLight.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                            modifier = Modifier.clickable {
                                val updated = activeSchedule.toMutableList()
                                val newPaid = !inst.isPaid
                                updated[idx] = inst.copy(isPaid = newPaid, paidDate = if (newPaid) Timestamp.now() else null)
                                state.customInstallments = updated
                            }
                        ) {
                            Text(
                                text = if (inst.isPaid) "✓ Pagada" else "Pendiente",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (inst.isPaid) EmeraldLight else AccentAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FixedExpenseScheduleCard(
    state: AddCommitmentState,
    context: android.content.Context
) {
    val activeFixedSchedule = state.computeActiveFixedSchedule()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCardLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Cuotas Planificadas del Gasto Fijo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = "Toca para ajustar (cascada)",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            activeFixedSchedule.forEachIndexed { idx, inst ->
                val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES")).format(inst.dueDate.toDate())
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pago #${inst.number}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldDark.copy(alpha = 0.35f),
                            modifier = Modifier.clickable {
                                showPlatformDatePicker(context, inst.dueDate.toDate()) { newDate ->
                                    val cascaded = cascadeRecalculateInstallments(
                                        schedule = activeFixedSchedule,
                                        changedIndex = idx,
                                        newDate = newDate,
                                        frequency = state.frequency,
                                        customDays = state.customDays
                                    )
                                    state.customFixedInstallments = cascaded
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(dateFmt, style = MaterialTheme.typography.labelSmall, color = EmeraldLight, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "${state.selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", inst.amount)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (inst.isPaid) EmeraldLight.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                            modifier = Modifier.clickable {
                                val updated = activeFixedSchedule.toMutableList()
                                val newPaid = !inst.isPaid
                                updated[idx] = inst.copy(isPaid = newPaid, paidDate = if (newPaid) Timestamp.now() else null)
                                state.customFixedInstallments = updated
                            }
                        ) {
                            Text(
                                text = if (inst.isPaid) "✓ Pagado" else "Pendiente",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (inst.isPaid) EmeraldLight else AccentAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDayConfigSection(state: AddCommitmentState) {
    when (state.frequency) {
        Frequency.WEEKLY -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = SlateBackground.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "¿Qué día de la semana se paga?",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }

                    val daysOfWeek = listOf(
                        Calendar.MONDAY to "Lun",
                        Calendar.TUESDAY to "Mar",
                        Calendar.WEDNESDAY to "Mié",
                        Calendar.THURSDAY to "Jue",
                        Calendar.FRIDAY to "Vie",
                        Calendar.SATURDAY to "Sáb",
                        Calendar.SUNDAY to "Dom"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        daysOfWeek.forEach { (calDay, label) ->
                            val isSelected = state.paymentDayOfWeek == calDay
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { state.onUpdatePaymentDayOfWeek(calDay) }
                                    .testTag("weekly_day_chip_$label"),
                                color = if (isSelected) EmeraldPrimary else SlateCardLight,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }

                    val dayName = when (state.paymentDayOfWeek) {
                        Calendar.MONDAY -> "Lunes"
                        Calendar.TUESDAY -> "Martes"
                        Calendar.WEDNESDAY -> "Miércoles"
                        Calendar.THURSDAY -> "Jueves"
                        Calendar.FRIDAY -> "Viernes"
                        Calendar.SATURDAY -> "Sábado"
                        Calendar.SUNDAY -> "Domingo"
                        else -> "el día seleccionado"
                    }
                    Text(
                        text = "📅 Se cobrará cada semana los $dayName",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight
                    )
                }
            }
        }

        Frequency.CASHEA -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = SlateBackground.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Modalidad Cashea (cada 14 días)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }
                    Text(
                        text = "📅 Cuotas programadas automáticamente cada 14 días a partir de la fecha de inicio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight
                    )
                }
            }
        }

        Frequency.BIWEEKLY -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = SlateBackground.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Días de pago quincenal (cada 15 días)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (state.paymentDayOfMonth > 0) state.paymentDayOfMonth.toString() else "",
                            onValueChange = {
                                val d = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 1
                                state.onUpdatePaymentDayOfMonth(d)
                            },
                            label = { Text("1er Pago (Día)", fontSize = 11.sp) },
                            placeholder = { Text("12") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("biweekly_day1_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = if (state.secondPaymentDay > 0) state.secondPaymentDay.toString() else "",
                            onValueChange = {
                                val d = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 15
                                state.onUpdateSecondPaymentDay(d)
                            },
                            label = { Text("2do Pago (Día)", fontSize = 11.sp) },
                            placeholder = { Text("27") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("biweekly_day2_input"),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1 to 16, 12 to 27, 15 to 30).forEach { (d1, d2) ->
                            val isSelected = (state.paymentDayOfMonth == d1 && state.secondPaymentDay == d2) ||
                                (state.paymentDayOfMonth == d2 && state.secondPaymentDay == d1)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        state.onUpdatePaymentDayOfMonth(d1)
                                        state.onUpdateSecondPaymentDay(d2)
                                    },
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.25f) else SlateCardLight,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$d1 y $d2",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EmeraldLight else TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }

                    val d1 = minOf(state.paymentDayOfMonth, state.secondPaymentDay)
                    val d2 = maxOf(state.paymentDayOfMonth, state.secondPaymentDay)
                    Text(
                        text = "📅 Pagos automáticos: días $d1 y $d2 de cada mes (+15 días)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight
                    )
                }
            }
        }

        Frequency.MONTHLY, Frequency.BIMONTHLY, Frequency.QUARTERLY, Frequency.FOUR_MONTHLY, Frequency.SEMIANNUAL, Frequency.ANNUAL -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = SlateBackground.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "¿Qué día del mes se realiza el pago?",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (state.paymentDayOfMonth > 0) state.paymentDayOfMonth.toString() else "",
                            onValueChange = {
                                val d = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 1
                                state.onUpdatePaymentDayOfMonth(d)
                            },
                            label = { Text("Día (1..31)", fontSize = 11.sp) },
                            placeholder = { Text("12") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f).testTag("monthly_payment_day_input"),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.weight(2.1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(1, 5, 12, 15, 20, 25, 30).forEach { d ->
                                val isSelected = state.paymentDayOfMonth == d
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { state.onUpdatePaymentDayOfMonth(d) }
                                        .testTag("day_chip_$d"),
                                    color = if (isSelected) EmeraldPrimary else SlateCardLight,
                                    shape = RoundedCornerShape(6.dp),
                                    border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$d",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "📅 Se cobrará el día ${state.paymentDayOfMonth} de cada período",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight
                    )
                }
            }
        }

        Frequency.CUSTOM -> {
            // Handled via customDays input
        }
    }
}

@Composable
fun CalculatedInstallmentsPreviewCard(state: AddCommitmentState) {
    if (state.totalAmount <= 0) return
    val schedule = if (state.isFixedExpense) state.computeActiveFixedSchedule() else state.computeActiveSchedule()
    if (schedule.isEmpty()) return

    val context = LocalContext.current
    val currSymbol = state.selectedCurrency.symbol
    val isInstallments = state.hasInstallments || state.isFixedExpense

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("installments_preview_card"),
        colors = CardDefaults.cardColors(containerColor = SlateCardLight),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isInstallments) "Cronograma de Cuotas Planificadas" else "Resumen de Pago Único",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Toca la fecha para editarla (recalcula en cascada)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLight,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isInstallments) "${schedule.size} cuotas" else "1 pago",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            schedule.forEachIndexed { idx, inst ->
                val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES")).format(inst.dueDate.toDate())
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cuota #${inst.number}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldDark.copy(alpha = 0.35f),
                            modifier = Modifier.clickable {
                                showPlatformDatePicker(context, inst.dueDate.toDate()) { newDate ->
                                    val cascaded = cascadeRecalculateInstallments(
                                        schedule = schedule,
                                        changedIndex = idx,
                                        newDate = newDate,
                                        frequency = state.frequency,
                                        customDays = state.customDays
                                    )
                                    if (state.isFixedExpense) {
                                        state.customFixedInstallments = cascaded
                                    } else {
                                        state.customInstallments = cascaded
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(dateFmt, style = MaterialTheme.typography.labelSmall, color = EmeraldLight, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = formatCurrency(inst.amount, currSymbol),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (inst.isPaid) EmeraldLight.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                            modifier = Modifier.clickable {
                                val updated = schedule.toMutableList()
                                val newPaid = !inst.isPaid
                                updated[idx] = inst.copy(isPaid = newPaid, paidDate = if (newPaid) Timestamp.now() else null)
                                if (state.isFixedExpense) {
                                    state.customFixedInstallments = updated
                                } else {
                                    state.customInstallments = updated
                                }
                            }
                        ) {
                            Text(
                                text = if (inst.isPaid) "✓ Pagada" else "Pendiente",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (inst.isPaid) EmeraldLight else AccentAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Dialog to add Fixed Expense or Temporary Debt (Cuotas / Créditos)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCommitmentDialog(
    accounts: List<Account>,
    templates: List<MarketTemplate> = emptyList(),
    customCategories: List<String> = emptyList(),
    prefilledTemplate: MarketTemplate? = null,
    commitmentToEdit: ExpenseCommitment? = null,
    initialIsFixed: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ExpenseCommitment) -> Unit,
    onSaveCustomTemplate: ((MarketTemplate) -> Unit)? = null,
    onCreateCategory: ((String, Boolean) -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val state = remember(commitmentToEdit, prefilledTemplate, initialIsFixed, accounts) {
        AddCommitmentState(
            commitmentToEdit = commitmentToEdit,
            prefilledTemplate = prefilledTemplate,
            initialIsFixed = initialIsFixed,
            defaultAccountIdInitial = accounts.firstOrNull()?.id ?: ""
        )
    }

    if (state.showTemplatePicker) {
        DebtTemplatePickerDialog(
            state = state,
            templates = templates,
            onSaveCustomTemplate = onSaveCustomTemplate
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .testTag("add_commitment_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Pinned Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (commitmentToEdit != null) {
                                if (state.isFixedExpense) "Editar Gasto Fijo" else "Editar Deuda"
                            } else {
                                if (state.isFixedExpense) "Nuevo Gasto Fijo" else "Nueva Deuda / Plan de Cuotas"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                        }
                    }

                    HorizontalDivider(color = SlateCardLight.copy(alpha = 0.5f))

                    // Scrollable form body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                // Selector: Gasto Fijo vs Deuda Temporal
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tipo de registro:", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!state.isFixedExpense) EmeraldPrimary else SlateBackground,
                            border = if (!state.isFixedExpense) null else BorderStroke(1.dp, SlateCardLight),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    state.isFixedExpense = false
                                    state.customInstallments = emptyList()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = if (!state.isFixedExpense) NavySurface else EmeraldLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Deuda / Cuotas",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!state.isFixedExpense) NavySurface else TextPrimaryDark
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Monto total a financiar",
                                    fontSize = 9.sp,
                                    color = if (!state.isFixedExpense) NavySurface.copy(alpha = 0.8f) else TextSecondaryDark
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (state.isFixedExpense) AccentAmber else SlateBackground,
                            border = if (state.isFixedExpense) null else BorderStroke(1.dp, SlateCardLight),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    state.isFixedExpense = true
                                    state.customFixedInstallments = emptyList()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Store,
                                        contentDescription = null,
                                        tint = if (state.isFixedExpense) NavySurface else AccentAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Gasto Fijo",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isFixedExpense) NavySurface else TextPrimaryDark
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Pago recurrente continuo",
                                    fontSize = 9.sp,
                                    color = if (state.isFixedExpense) NavySurface.copy(alpha = 0.8f) else TextSecondaryDark
                                )
                            }
                        }
                    }
                }

                // 1. Name
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { newTitle ->
                        state.title = newTitle
                        if (state.commitmentToEdit == null) {
                            val lower = newTitle.lowercase()
                            val fixedKeywords = listOf("alquiler", "internet", "netflix", "luz", "agua", "gas", "cantv", "condominio", "colegio", "suscripcion", "suscripción", "servicio", "renta", "gym", "gimnasio", "sueldo", "nomina", "nómina")
                            if (fixedKeywords.any { lower.contains(it) }) {
                                state.isFixedExpense = true
                            }
                        }
                    },
                    label = { Text(if (state.isFixedExpense) "Nombre del Gasto Fijo" else "Título (ej. Cashea, Préstamo)") },
                    modifier = Modifier.fillMaxWidth().testTag("commitment_title_input"),
                    singleLine = true
                )

                // 2. Date
                val startDateToUse = if (state.isFixedExpense) state.fixedExpenseDueDate else state.commitmentStartDate
                val startDateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES")).format(startDateToUse)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateCardLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isFixedExpense) "Fecha de inicio / primer cobro:" else "Fecha de inicio de la deuda / compra:",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (state.isFixedExpense) {
                                val cal = Calendar.getInstance().apply { time = startDateToUse }
                                val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                                Text(
                                    text = "Día recurrente: día $dayNum de cada ciclo (${state.frequency.displayName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldLight,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldDark.copy(alpha = 0.35f),
                            modifier = Modifier.clickable {
                                showPlatformDatePicker(context, startDateToUse) { newDate ->
                                    if (state.isFixedExpense) {
                                        state.fixedExpenseDueDate = newDate
                                        state.customFixedInstallments = emptyList()
                                    } else {
                                        state.commitmentStartDate = newDate
                                        state.customInstallments = emptyList()
                                    }
                                }
                            }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(startDateFmt, color = EmeraldLight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. Amount and Currency
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.totalAmountText,
                        onValueChange = { state.totalAmountText = it },
                        label = { Text(if (state.isFixedExpense) "Monto recurrente" else "Monto Base") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.3f).testTag("commitment_total_input"),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = state.currExpanded,
                        onExpandedChange = { state.currExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = state.selectedCurrency.symbolWithCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Moneda") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.currExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = state.currExpanded,
                            onDismissRequest = { state.currExpanded = false }
                        ) {
                            AppCurrency.entries.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.flagEmoji} ${c.symbolWithCode}") },
                                    onClick = { state.selectedCurrency = c; state.currExpanded = false }
                                )
                            }
                        }
                    }
                }

                // 4. Conditional Fields
                if (!state.isFixedExpense) {
                    // DEUDA TEMPORAL / CRÉDITO

                    // Cuotas (opcional via Checkbox)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateCardLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    state.hasInstallments = !state.hasInstallments
                                    state.customInstallments = emptyList()
                                }
                            ) {
                                Checkbox(
                                    checked = state.hasInstallments,
                                    onCheckedChange = {
                                        state.hasInstallments = it
                                        state.customInstallments = emptyList()
                                    },
                                    modifier = Modifier.testTag("has_installments_checkbox")
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "¿Esta deuda se paga en cuotas?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = if (state.hasInstallments) "Plan financiado en cuotas periódicas" else "1 pago único en fecha prevista (sin cuotas)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (state.hasInstallments) EmeraldLight else TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (state.hasInstallments) {
                                Spacer(Modifier.height(10.dp))
                                // Cuotas, Frecuencia y Cuotas adelantadas en la misma línea
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.installmentsCountText,
                                        onValueChange = {
                                            state.installmentsCountText = it
                                            state.customInstallments = emptyList()
                                        },
                                        label = { Text("Cuotas", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.9f).testTag("installments_count_input"),
                                        singleLine = true
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = state.freqExpanded,
                                        onExpandedChange = { state.freqExpanded = it },
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        OutlinedTextField(
                                            value = when(state.frequency) {
                                                Frequency.MONTHLY -> "Mensual"
                                                Frequency.CASHEA -> "Cashea (14d)"
                                                Frequency.BIWEEKLY -> "Quincenal"
                                                Frequency.WEEKLY -> "Semanal"
                                                Frequency.BIMONTHLY -> "2 Meses"
                                                Frequency.QUARTERLY -> "3 Meses"
                                                Frequency.FOUR_MONTHLY -> "4 Meses"
                                                Frequency.SEMIANNUAL -> "6 Meses"
                                                Frequency.ANNUAL -> "Anual"
                                                Frequency.CUSTOM -> "Personal."
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Frecuencia", fontSize = 11.sp) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.freqExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            singleLine = true
                                        )
                                        ExposedDropdownMenu(
                                            expanded = state.freqExpanded,
                                            onDismissRequest = { state.freqExpanded = false }
                                        ) {
                                            Frequency.entries.forEach { f ->
                                                DropdownMenuItem(
                                                    text = { Text(f.displayName) },
                                                    onClick = {
                                                        state.frequency = f
                                                        state.freqExpanded = false
                                                        state.customInstallments = emptyList()
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = state.alreadyPaidCountText,
                                        onValueChange = {
                                            state.alreadyPaidCountText = it
                                            state.customInstallments = emptyList()
                                        },
                                        label = { Text("Adelantadas", fontSize = 11.sp) },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("already_paid_installments_input"),
                                        singleLine = true
                                    )
                                }

                                if (state.frequency == Frequency.CUSTOM) {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = state.customDaysText,
                                        onValueChange = { state.customDaysText = it; state.customInstallments = emptyList() },
                                        label = { Text("Frecuencia personalizada: ¿Cada cuántos días se paga cada cuota?") },
                                        placeholder = { Text("Ej. 21") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().testTag("commitment_custom_days_input"),
                                        singleLine = true
                                    )
                                } else {
                                    Spacer(Modifier.height(8.dp))
                                    PaymentDayConfigSection(state = state)
                                }

                                Spacer(Modifier.height(10.dp))
                                // Cuota Inicial opcional dentro de la sección de cuotas
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SlateBackground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().clickable { state.hasDownPayment = !state.hasDownPayment }
                                        ) {
                                            Checkbox(
                                                checked = state.hasDownPayment,
                                                onCheckedChange = { state.hasDownPayment = it },
                                                modifier = Modifier.testTag("has_down_payment_checkbox")
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "¿Pagaste o pagarás una cuota inicial?",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimaryDark
                                                )
                                                Text(
                                                    text = if (state.hasDownPayment) "Se descuenta antes de financiar cuotas" else "Sin inicial (100% financiado)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (state.hasDownPayment) EmeraldLight else TextSecondaryDark,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        if (state.hasDownPayment) {
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = state.downPaymentText,
                                                onValueChange = { state.downPaymentText = it },
                                                label = { Text("Monto de la Cuota Inicial") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.fillMaxWidth().testTag("down_payment_input"),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Intereses (opcional via Checkbox)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateCardLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { state.applyInterest = !state.applyInterest }
                            ) {
                                Checkbox(
                                    checked = state.applyInterest,
                                    onCheckedChange = { state.applyInterest = it },
                                    modifier = Modifier.testTag("apply_interest_checkbox")
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "¿Aplica tasa de interés a esta deuda?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = if (state.applyInterest) "Interés configurable (mensual o anual)" else "0% de interés (sin recargo adicional)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (state.applyInterest) EmeraldLight else TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (state.applyInterest) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = state.interestRateText,
                                        onValueChange = { state.interestRateText = it },
                                        label = { Text("Tasa de Interés (%)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.2f).testTag("commitment_interest_input"),
                                        singleLine = true
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (!state.isAnnualInterest) EmeraldPrimary else SlateBackground,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { state.isAnnualInterest = false }
                                    ) {
                                        Text(
                                            text = "Mensual",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!state.isAnnualInterest) NavySurface else TextSecondaryDark,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (state.isAnnualInterest) EmeraldPrimary else SlateBackground,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { state.isAnnualInterest = true }
                                    ) {
                                        Text(
                                            text = "Anual",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.isAnnualInterest) NavySurface else TextSecondaryDark,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Resumen de cálculo
                    if (state.calculatedInstallmentAmount > 0) {
                        val freqDesc = if (state.frequency == Frequency.CUSTOM) "cada ${state.customDays} días" else "cada ${state.frequency.displayName.lowercase()}"
                        Surface(
                            color = EmeraldDark.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (state.hasInstallments) {
                                        "Monto por cuota: ${state.selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", state.calculatedInstallmentAmount)} $freqDesc"
                                    } else {
                                        "Monto total a pagar: ${state.selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", state.totalWithInterest)}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldLight,
                                    fontWeight = FontWeight.Bold
                                )
                                if (state.hasDownPayment && state.downPayment > 0) {
                                    Text(
                                        text = "Inicial pagada: ${state.selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", state.downPayment)} • Restante financiado: ${state.selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", state.totalFinancedWithInterest)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                                if (state.applyInterest && state.rawInterestRate > 0) {
                                    Text(
                                        text = "Total a pagar (${if (state.hasInstallments) "${state.totalInstallments} cuotas" else "1 pago"} + ${state.rawInterestRate}% ${if (state.isAnnualInterest) "anual" else "mensual"}): ${state.selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", state.totalWithInterest)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AccentAmber,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Text(
                                        text = "0% interés • Total: ${state.selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", state.totalAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // GASTO FIJO RECURRENTE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCardLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Periodicidad del Gasto Fijo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            ExposedDropdownMenuBox(
                                expanded = state.freqExpanded,
                                onExpandedChange = { state.freqExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = state.frequency.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Frecuencia con que se paga") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.freqExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = state.freqExpanded,
                                    onDismissRequest = { state.freqExpanded = false }
                                ) {
                                    Frequency.entries.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.displayName) },
                                            onClick = {
                                                state.frequency = f
                                                state.freqExpanded = false
                                                state.customFixedInstallments = emptyList()
                                            }
                                        )
                                    }
                                }
                            }

                            if (state.frequency == Frequency.CUSTOM) {
                                OutlinedTextField(
                                    value = state.customDaysText,
                                    onValueChange = {
                                        state.customDaysText = it
                                        state.customFixedInstallments = emptyList()
                                    },
                                    label = { Text("¿Cada cuántos días se repite?") },
                                    placeholder = { Text("Ej. 30") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            } else {
                                PaymentDayConfigSection(state = state)
                            }

                            // Optional End Date
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SlateBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            state.hasEndDate = !state.hasEndDate
                                            state.customFixedInstallments = emptyList()
                                        }
                                    ) {
                                        Checkbox(
                                            checked = state.hasEndDate,
                                            onCheckedChange = {
                                                state.hasEndDate = it
                                                state.customFixedInstallments = emptyList()
                                            }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "¿Tiene fecha de finalización? (opcional)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                text = if (state.hasEndDate) "La app calcula automáticamente las cuotas hasta la fecha fin" else "Gasto recurrente permanente / indefinido",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (state.hasEndDate) EmeraldLight else TextSecondaryDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (state.hasEndDate) {
                                        Spacer(Modifier.height(8.dp))
                                        val fixedEndDateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES")).format(state.fixedExpenseEndDate)
                                        val calculatedCycles = state.computeActiveFixedSchedule().size
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Fecha de fin / expiración:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondaryDark
                                                )
                                                Text(
                                                    text = "Total calculado: $calculatedCycles pagos proyectados",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldLight,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = EmeraldDark.copy(alpha = 0.35f),
                                                modifier = Modifier.clickable {
                                                    showPlatformDatePicker(context, state.fixedExpenseEndDate) { newDate ->
                                                        state.fixedExpenseEndDate = newDate
                                                        state.customFixedInstallments = emptyList()
                                                    }
                                                }
                                            ) {
                                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(fixedEndDateFmt, color = EmeraldLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!state.hasEndDate) {
                                OutlinedTextField(
                                    value = state.fixedCyclesCountText,
                                    onValueChange = {
                                        state.fixedCyclesCountText = it
                                        state.customFixedInstallments = emptyList()
                                    },
                                    label = { Text("Número de pagos o cuotas a planificar (proyección inicial)") },
                                    placeholder = { Text("6") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                CalculatedInstallmentsPreviewCard(state = state)

                // 5. Category
                val combinedCategories = (CATEGORY_EXPENSES + customCategories).distinct()
                CategorySelectorDropdown(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { state.selectedCategory = it },
                    categoriesList = combinedCategories,
                    isIncome = false,
                    onCreateCategory = onCreateCategory
                )

                // 6. Notes
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { state.notes = it },
                    label = { Text("Notas u observaciones (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                    Spacer(modifier = Modifier.height(6.dp))
                } // End scrollable form Column

                HorizontalDivider(color = SlateCardLight.copy(alpha = 0.5f))

                // Pinned Action Buttons at bottom
                Surface(
                    color = SlateCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", maxLines = 1)
                        }

                        if (onDelete != null) {
                            Button(
                                onClick = {
                                    onDelete()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f).testTag("delete_commitment_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                            ) {
                                Text("Eliminar", maxLines = 1)
                            }
                        }

                        Button(
                            onClick = {
                                val commitment = state.buildCommitmentToSave()
                                if (commitment != null) {
                                    onSave(commitment)
                                    onDismiss()
                                }
                            },
                            enabled = state.title.isNotBlank() && state.totalAmount > 0,
                            modifier = Modifier.weight(1f).testTag("save_commitment_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text(if (commitmentToEdit != null) "Actualizar" else "Crear", maxLines = 1)
                        }
                    }
                }
            } // End Card Column
        } // End Card
    } // End Box
} // End Dialog
} // End AddCommitmentDialog

// Dialog to Pay / Record Installment
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayCommitmentDialog(
    commitment: ExpenseCommitment,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (InstallmentPayment, Account?) -> Unit
) {
    val suggestedAmount = if (commitment.paymentMode == PaymentMode.INSTALLMENTS.name) {
        commitment.installmentAmount.coerceAtMost(commitment.remainingAmount)
    } else commitment.remainingAmount

    val commCurrency = AppCurrency.fromCode(commitment.currency)
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", suggestedAmount)) }
    val payAccounts = remember(accounts) {
        accounts.filter { !it.isPlanStartAccount || it.currentBalance > 0 }
    }

    var selectedAccountId by remember {
        mutableStateOf(
            commitment.accountId?.takeIf { id -> payAccounts.any { it.id == id } }
                ?: payAccounts.firstOrNull { it.isPlanStartAccount }?.id
                ?: payAccounts.firstOrNull()?.id
                ?: "EXTERNAL"
        )
    }
    var selectedDate by remember { mutableStateOf(Date()) }
    var accountExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val selectedAccount = payAccounts.find { it.id == selectedAccountId }

    if (showDatePicker) {
        val cal = Calendar.getInstance().apply { time = selectedDate }
        val context = LocalContext.current
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDate = newCal.time
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showDatePicker = false }
            show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pay_dialog"),
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
                            text = "Registrar Pago / Abono",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                        }
                    }

                    Surface(
                        color = SlateBackground,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = commitment.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saldo pendiente: ${formatCurrency(commitment.remainingAmount, commCurrency.symbol)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AccentAmber
                            )
                            if (commitment.paymentMode == PaymentMode.INSTALLMENTS.name) {
                                Text(
                                    text = "Cuota planificada: ${commitment.paidInstallments + 1} de ${commitment.totalInstallments}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }

                    // Select Debit Account
                    ExposedDropdownMenuBox(
                        expanded = accountExpanded,
                        onExpandedChange = { accountExpanded = it }
                    ) {
                        val accountLabel = if (selectedAccountId == "EXTERNAL") {
                            "🌐 Dinero Externo (Sin afectar mis cuentas)"
                        } else if (selectedAccount != null) {
                            val accCurr = AppCurrency.fromCode(selectedAccount.currency)
                            "${selectedAccount.name} (${selectedAccount.currency}) • Saldo: ${formatCurrency(selectedAccount.currentBalance, accCurr.symbol)}"
                        } else {
                            "Seleccionar cuenta a debitar"
                        }

                        OutlinedTextField(
                            value = accountLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("¿De qué cuenta salió el pago?") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("🌐 Dinero Externo", fontWeight = FontWeight.Bold)
                                        Text("No descuenta saldo de ninguna cuenta registrada", fontSize = 11.sp, color = TextSecondaryDark)
                                    }
                                },
                                onClick = {
                                    selectedAccountId = "EXTERNAL"
                                    accountExpanded = false
                                }
                            )
                            HorizontalDivider(color = SlateBackground)
                            payAccounts.forEach { acc ->
                                val accCurr = AppCurrency.fromCode(acc.currency)
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.currency}) • Saldo: ${formatCurrency(acc.currentBalance, accCurr.symbol)}") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Selection Field
                    OutlinedTextField(
                        value = dateFormat.format(selectedDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha de Pago") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CreditCard, contentDescription = "Seleccionar fecha", tint = EmeraldLight)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monto en ${commCurrency.symbol}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Nota de comprobante / memo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                if (amount > 0 && (selectedAccountId == "EXTERNAL" || selectedAccount != null)) {
                                    onConfirm(
                                        InstallmentPayment(
                                            commitmentId = commitment.id,
                                            accountId = selectedAccount?.id ?: "",
                                            amount = amount,
                                            currency = commitment.currency,
                                            installmentNumber = commitment.paidInstallments + 1L,
                                            paymentDate = Timestamp(selectedDate),
                                            notes = notes.trim().ifBlank { null }
                                        ),
                                        selectedAccount
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = amount > 0 && (selectedAccountId == "EXTERNAL" || selectedAccount != null),
                            modifier = Modifier.weight(1f).testTag("confirm_payment_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Confirmar Pago")
                        }
                    }
                }
            }
        }
    }
}

// Dialog to share a plan as a template to the Marketplace
@Composable
fun ShareTemplateDialog(
    commitment: ExpenseCommitment,
    onDismiss: () -> Unit,
    onPublish: (MarketTemplate) -> Unit
) {
    var title by remember { mutableStateOf(commitment.title) }
    var description by remember { mutableStateOf("Plan de ${commitment.totalInstallments} cuotas ${commitment.frequency.lowercase()}") }
    var creatorName by remember { mutableStateOf("Usuario") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .testTag("share_template_dialog"),
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
                        text = "Compartir en Marketplace",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                Text(
                    text = "Publica este esquema de cuotas como plantilla para que otros usuarios de la comunidad puedan usarlo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la plantilla") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (ej. Cashea 3 cuotas, financiamiento tienda)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = creatorName,
                    onValueChange = { creatorName = it },
                    label = { Text("Nombre del creador") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onPublish(
                                    MarketTemplate(
                                        title = title.trim(),
                                        description = description.trim(),
                                        category = commitment.category,
                                        paymentMode = commitment.paymentMode,
                                        frequency = commitment.frequency,
                                        installmentsCount = commitment.totalInstallments,
                                        creatorName = creatorName.trim().ifBlank { "Comunidad" },
                                        upvotes = 1L
                                    )
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f).testTag("publish_template_btn")
                    ) {
                        Text("Publicar")
                    }
                }
            }
        }
    }
}

// Dialog to confirm account, date, and amount when marking an event as paid / collected
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmPaymentDialog(
    title: String,
    suggestedAmount: Double,
    currencyCode: String,
    dueDate: Date,
    accounts: List<Account>,
    isIncome: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (account: Account?, paidDate: Date, amount: Double, notes: String) -> Unit
) {
    val currency = AppCurrency.fromCode(currencyCode)
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", suggestedAmount)) }

    val payAccounts = remember(accounts) {
        accounts.filter { !it.isPlanStartAccount || it.currentBalance > 0 }
    }

    // selectedAccountId == "EXTERNAL" means Dinero Externo / Sin Afectar Cuentas
    var selectedAccountId by remember {
        mutableStateOf(
            payAccounts.firstOrNull { it.isPlanStartAccount }?.id
                ?: payAccounts.firstOrNull()?.id
                ?: "EXTERNAL"
        )
    }

    var selectedDate by remember { mutableStateOf(dueDate) }
    var notes by remember { mutableStateOf("") }
    var accountExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        val cal = Calendar.getInstance().apply { time = selectedDate }
        val context = LocalContext.current
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDate = newCal.time
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showDatePicker = false }
            show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .testTag("confirm_payment_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isIncome) "Confirmar Cobro / Ingreso" else "Registrar Pago / Salida",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                        }
                    }

                    // Event Summary Box
                    Surface(
                        color = SlateBackground,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Monto previsto: ${formatCurrency(suggestedAmount, currency.symbol)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isIncome) EmeraldLight else AccentAmber
                            )
                        }
                    }

                    // Account Selection Dropdown
                    ExposedDropdownMenuBox(
                        expanded = accountExpanded,
                        onExpandedChange = { accountExpanded = it }
                    ) {
                        val selectedAcc = payAccounts.find { it.id == selectedAccountId }
                        val accountLabel = if (selectedAccountId == "EXTERNAL") {
                            "🌐 Dinero Externo (Sin afectar mis cuentas)"
                        } else if (selectedAcc != null) {
                            val accCurr = AppCurrency.fromCode(selectedAcc.currency)
                            "${selectedAcc.name} (${selectedAcc.currency}) • Saldo: ${formatCurrency(selectedAcc.currentBalance, accCurr.symbol)}"
                        } else {
                            if (isIncome) "Seleccionar cuenta de destino" else "Seleccionar cuenta de origen"
                        }

                        OutlinedTextField(
                            value = accountLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isIncome) "¿A qué cuenta ingresó el dinero?" else "¿De qué cuenta salió el pago?") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("🌐 Dinero Externo", fontWeight = FontWeight.Bold)
                                        Text(
                                            if (isIncome) "No se sumará a ninguna cuenta registrada" else "No descuenta saldo de ninguna cuenta registrada",
                                            fontSize = 11.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                },
                                onClick = {
                                    selectedAccountId = "EXTERNAL"
                                    accountExpanded = false
                                }
                            )
                            HorizontalDivider(color = SlateBackground)
                            payAccounts.forEach { acc ->
                                val accCurr = AppCurrency.fromCode(acc.currency)
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.currency}) • Saldo: ${formatCurrency(acc.currentBalance, accCurr.symbol)}") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Selection Field
                    OutlinedTextField(
                        value = dateFormat.format(selectedDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isIncome) "Fecha de Cobro" else "Fecha de Pago") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CreditCard, contentDescription = "Seleccionar fecha", tint = EmeraldLight)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )

                    // Amount Field
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (isIncome) "Monto cobrado (${currency.symbol})" else "Monto pagado (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Optional Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Nota u observación (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: suggestedAmount
                                val account = payAccounts.find { it.id == selectedAccountId }
                                onConfirm(account, selectedDate, amount, notes.trim())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1.2f).testTag("confirm_payment_btn")
                        ) {
                            Text(if (isIncome) "Confirmar Cobro" else "Confirmar Pago", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
