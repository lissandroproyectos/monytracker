package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        mutableStateOf(if (accountToEdit != null) accountToEdit.currentBalance.toString() else "")
    }
    var isSavings by remember { mutableStateOf(accountToEdit?.isSavings ?: false) }
    var selectedColor by remember { mutableStateOf(accountToEdit?.colorHex ?: COLOR_OPTIONS[0]) }

    // Periodic Income fields
    var hasPeriodicIncome by remember { mutableStateOf(accountToEdit?.hasPeriodicIncome ?: false) }
    var periodicIncomeAmountText by remember {
        mutableStateOf(if (accountToEdit != null && accountToEdit.hasPeriodicIncome) accountToEdit.periodicIncomeAmount.toString() else "")
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .testTag("add_account_dialog"),
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
                        text = if (accountToEdit != null) "Editar Cuenta" else "Nueva Cuenta / Billetera",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

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
                        val color = Color(android.graphics.Color.parseColor(hex))
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }

                    if (onDelete != null) {
                        Button(
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("delete_account_dialog_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) {
                            Text("Eliminar")
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
                        modifier = Modifier.weight(1f).testTag("save_account_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(if (accountToEdit != null) "Actualizar" else "Guardar")
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

                // Origin Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val currentAccName = accounts.find { it.id == selectedAccountId }?.name ?: "Seleccionar Cuenta"
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
                        accounts.forEach { acc ->
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
                            accounts.filter { it.id != selectedAccountId }.forEach { acc ->
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

    var title by remember {
        mutableStateOf(commitmentToEdit?.title ?: prefilledTemplate?.title ?: "")
    }
    var isFixedExpense by remember {
        mutableStateOf(commitmentToEdit?.isFixedExpense ?: initialIsFixed)
    }
    var totalAmountText by remember {
        mutableStateOf(if (commitmentToEdit != null) commitmentToEdit.totalAmount.toString() else "")
    }
    var hasDownPayment by remember {
        mutableStateOf((commitmentToEdit?.downPayment ?: 0.0) > 0.0)
    }
    var downPaymentText by remember {
        mutableStateOf(if ((commitmentToEdit?.downPayment ?: 0.0) > 0.0) commitmentToEdit!!.downPayment.toString() else "")
    }
    var alreadyPaidCountText by remember {
        mutableStateOf((commitmentToEdit?.paidInstallments ?: 0L).toString())
    }
    var fixedExpenseDueDate by remember {
        mutableStateOf(commitmentToEdit?.dueDate?.toDate() ?: Date())
    }
    var selectedCurrency by remember {
        mutableStateOf(
            if (commitmentToEdit != null) AppCurrency.fromCode(commitmentToEdit.currency)
            else AppCurrency.USD_BCV
        )
    }
    var paymentMode by remember {
        mutableStateOf(
            if (commitmentToEdit != null) PaymentMode.entries.find { it.name == commitmentToEdit.paymentMode } ?: PaymentMode.INSTALLMENTS
            else if (prefilledTemplate != null) PaymentMode.entries.find { it.name == prefilledTemplate.paymentMode } ?: PaymentMode.INSTALLMENTS
            else PaymentMode.INSTALLMENTS
        )
    }
    var installmentsCountText by remember {
        mutableStateOf((commitmentToEdit?.totalInstallments ?: 3L).toString())
    }
    var applyInterest by remember {
        mutableStateOf((commitmentToEdit?.interestRate ?: prefilledTemplate?.interestRate ?: 0.0) > 0.0)
    }
    var isAnnualInterest by remember {
        mutableStateOf(commitmentToEdit?.isAnnualInterest ?: prefilledTemplate?.isAnnualInterest ?: false)
    }
    var interestRateText by remember {
        mutableStateOf((commitmentToEdit?.interestRate ?: prefilledTemplate?.interestRate ?: 0.0).let { if (it > 0) it.toString() else "5.0" })
    }
    var customDaysText by remember {
        mutableStateOf((commitmentToEdit?.customDays ?: prefilledTemplate?.customDays ?: 30).toString())
    }
    var frequency by remember {
        mutableStateOf(
            if (commitmentToEdit != null) Frequency.entries.find { it.name == commitmentToEdit.frequency } ?: Frequency.MONTHLY
            else if (prefilledTemplate != null) Frequency.entries.find { it.name == prefilledTemplate.frequency } ?: Frequency.BIWEEKLY
            else Frequency.MONTHLY
        )
    }
    var selectedCategory by remember {
        mutableStateOf(commitmentToEdit?.category ?: prefilledTemplate?.category ?: CATEGORY_EXPENSES[0])
    }
    var defaultAccountId by remember {
        mutableStateOf(commitmentToEdit?.accountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var notes by remember {
        mutableStateOf(commitmentToEdit?.notes ?: prefilledTemplate?.description ?: "")
    }

    var showTemplatePicker by remember { mutableStateOf(false) }
    var showCreateCustomTemplateForm by remember { mutableStateOf(false) }
    var customTemplateTitle by remember { mutableStateOf("") }
    var customTemplateCategory by remember { mutableStateOf(CATEGORY_EXPENSES[0]) }
    var customTemplateFreq by remember { mutableStateOf(Frequency.BIWEEKLY) }
    var customTemplatePaymentMode by remember { mutableStateOf(PaymentMode.INSTALLMENTS) }
    var customTemplateApplyInterest by remember { mutableStateOf(false) }
    var customTemplateIsAnnual by remember { mutableStateOf(false) }
    var customTemplateInterestText by remember { mutableStateOf("5.0") }
    var customTemplateDaysText by remember { mutableStateOf("30") }
    var customTemplateDesc by remember { mutableStateOf("") }

    var freqExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var currExpanded by remember { mutableStateOf(false) }
    var customFreqExpanded by remember { mutableStateOf(false) }
    var customCatExpanded by remember { mutableStateOf(false) }

    val totalAmount = totalAmountText.toDoubleOrNull() ?: 0.0
    val downPayment = if (hasDownPayment) (downPaymentText.toDoubleOrNull() ?: 0.0) else 0.0
    val financedBase = (totalAmount - downPayment).coerceAtLeast(0.0)
    val rawInterestRate = if (applyInterest) (interestRateText.toDoubleOrNull() ?: 0.0) else 0.0
    val totalInstallments = (installmentsCountText.toLongOrNull() ?: 1L).coerceAtLeast(1L)
    val alreadyPaidCount = (alreadyPaidCountText.toLongOrNull() ?: 0L).coerceIn(0L, totalInstallments)
    val customDays = customDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 30

    val termInMonths = when (frequency) {
        Frequency.WEEKLY -> (totalInstallments * 7.0) / 30.0
        Frequency.BIWEEKLY -> (totalInstallments * 15.0) / 30.0
        Frequency.MONTHLY -> totalInstallments.toDouble()
        Frequency.CUSTOM -> (totalInstallments * customDays.toDouble()) / 30.0
    }

    val effectiveInterestRate = if (rawInterestRate > 0.0) {
        if (isAnnualInterest) {
            (rawInterestRate / 12.0) * termInMonths
        } else {
            rawInterestRate * termInMonths
        }
    } else 0.0

    val totalFinancedWithInterest = if (effectiveInterestRate > 0) financedBase * (1.0 + effectiveInterestRate / 100.0) else financedBase
    val totalWithInterest = downPayment + totalFinancedWithInterest
    val calculatedInstallmentAmount = if (totalInstallments > 0 && totalFinancedWithInterest > 0) totalFinancedWithInterest / totalInstallments else 0.0

    // Planned installments: initialized from existing or generated
    var customInstallments by remember {
        mutableStateOf<List<ProjectedInstallment>>(
            if (commitmentToEdit != null && commitmentToEdit.installments.isNotEmpty()) {
                commitmentToEdit.getProjectedInstallments()
            } else emptyList()
        )
    }

    // Debt Type Template selector sub-dialog
    if (showTemplatePicker) {
        Dialog(
            onDismissRequest = { showTemplatePicker = false; showCreateCustomTemplateForm = false },
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
                            text = if (showCreateCustomTemplateForm) "Configurar Tipo de Deuda" else "Tipos de Deudas & Plantillas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        IconButton(onClick = { showTemplatePicker = false; showCreateCustomTemplateForm = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                        }
                    }

                    if (showCreateCustomTemplateForm) {
                        Text(
                            text = "Personaliza el modo de pago, frecuencia e intereses. La cantidad de cuotas la indicarás al crear la deuda:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )

                        OutlinedTextField(
                            value = customTemplateTitle,
                            onValueChange = { customTemplateTitle = it },
                            label = { Text("Nombre del Tipo (ej. Cashea Quincenal, Préstamo 5%, Crédito Bancario)") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_template_title_input"),
                            singleLine = true
                        )

                        // Payment mode selection in template
                        Text("Modalidad de pago predeterminada:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PaymentMode.entries.forEach { mode ->
                                val isSel = customTemplatePaymentMode == mode
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) EmeraldPrimary else SlateBackground,
                                    modifier = Modifier.weight(1f).clickable { customTemplatePaymentMode = mode }
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
                            // Frecuencia
                            ExposedDropdownMenuBox(
                                expanded = customFreqExpanded,
                                onExpandedChange = { customFreqExpanded = it },
                                modifier = Modifier.weight(1.3f)
                            ) {
                                OutlinedTextField(
                                    value = customTemplateFreq.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Frecuencia") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customFreqExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = customFreqExpanded,
                                    onDismissRequest = { customFreqExpanded = false }
                                ) {
                                    Frequency.entries.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.displayName) },
                                            onClick = {
                                                customTemplateFreq = f
                                                customFreqExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Intereses %
                            OutlinedTextField(
                                value = customTemplateInterestText,
                                onValueChange = { customTemplateInterestText = it },
                                label = { Text("Interés (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        if (customTemplateFreq == Frequency.CUSTOM) {
                            OutlinedTextField(
                                value = customTemplateDaysText,
                                onValueChange = { customTemplateDaysText = it },
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
                                .clickable { customTemplateApplyInterest = !customTemplateApplyInterest }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = customTemplateApplyInterest,
                                onCheckedChange = { customTemplateApplyInterest = it }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "¿Aplica intereses?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        if (customTemplateApplyInterest) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (!customTemplateIsAnnual) EmeraldPrimary else SlateBackground,
                                    modifier = Modifier.weight(1f).clickable { customTemplateIsAnnual = false }
                                ) {
                                    Text(
                                        text = "Mensual",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!customTemplateIsAnnual) NavySurface else TextSecondaryDark,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (customTemplateIsAnnual) EmeraldPrimary else SlateBackground,
                                    modifier = Modifier.weight(1f).clickable { customTemplateIsAnnual = true }
                                ) {
                                    Text(
                                        text = "Anual",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (customTemplateIsAnnual) NavySurface else TextSecondaryDark,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customTemplateDesc,
                            onValueChange = { customTemplateDesc = it },
                            label = { Text("Descripción o condiciones especiales (opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCreateCustomTemplateForm = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Volver")
                            }

                            Button(
                                onClick = {
                                    if (customTemplateTitle.isNotBlank()) {
                                        val newTmpl = MarketTemplate(
                                            title = customTemplateTitle.trim(),
                                            description = customTemplateDesc.trim(),
                                            category = customTemplateCategory,
                                            paymentMode = customTemplatePaymentMode.name,
                                            frequency = customTemplateFreq.name,
                                            installmentsCount = 3L,
                                            interestRate = if (customTemplateApplyInterest) (customTemplateInterestText.toDoubleOrNull() ?: 0.0) else 0.0,
                                            isAnnualInterest = customTemplateIsAnnual,
                                            customDays = customTemplateDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                                            creatorName = "Personalizado",
                                            upvotes = 1L
                                        )
                                        onSaveCustomTemplate?.invoke(newTmpl)

                                        // Apply parameters to dialog
                                        title = newTmpl.title
                                        paymentMode = customTemplatePaymentMode
                                        frequency = customTemplateFreq
                                        applyInterest = newTmpl.interestRate > 0.0
                                        isAnnualInterest = newTmpl.isAnnualInterest
                                        interestRateText = newTmpl.interestRate.toString()
                                        customDaysText = newTmpl.customDays.toString()
                                        selectedCategory = newTmpl.category
                                        notes = newTmpl.description
                                        showTemplatePicker = false
                                        showCreateCustomTemplateForm = false
                                    }
                                },
                                enabled = customTemplateTitle.isNotBlank(),
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
                            onClick = { showCreateCustomTemplateForm = true },
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
                                        title = tmpl.title
                                        paymentMode = PaymentMode.entries.find { it.name == tmpl.paymentMode } ?: PaymentMode.INSTALLMENTS
                                        frequency = Frequency.entries.find { it.name == tmpl.frequency } ?: Frequency.BIWEEKLY
                                        applyInterest = tmpl.interestRate > 0.0
                                        isAnnualInterest = tmpl.isAnnualInterest
                                        interestRateText = tmpl.interestRate.toString()
                                        customDaysText = tmpl.customDays.toString()
                                        selectedCategory = tmpl.category
                                        notes = tmpl.description
                                        showTemplatePicker = false
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .testTag("add_commitment_dialog"),
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
                        text = if (commitmentToEdit != null) {
                            if (isFixedExpense) "Editar Gasto Fijo" else "Editar Deuda"
                        } else {
                            if (isFixedExpense) "Nuevo Gasto Fijo" else "Nueva Deuda / Plan de Cuotas"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondaryDark)
                    }
                }

                // Selector: Gasto Fijo vs Deuda Temporal
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isFixedExpense) EmeraldPrimary else SlateBackground,
                        modifier = Modifier.weight(1f).clickable { isFixedExpense = false }
                    ) {
                        Text(
                            text = "Deuda / Cuotas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!isFixedExpense) NavySurface else TextSecondaryDark,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFixedExpense) AccentAmber else SlateBackground,
                        modifier = Modifier.weight(1f).clickable { isFixedExpense = true }
                    ) {
                        Text(
                            text = "Gasto Fijo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFixedExpense) NavySurface else TextSecondaryDark,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Button to choose or customize debt type
                if (!isFixedExpense && commitmentToEdit == null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, EmeraldLight.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { showTemplatePicker = true }
                            .testTag("open_marketplace_picker_btn"),
                        color = SlateCardLight
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = EmeraldLight)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "⚙️ Elegir / Personalizar Tipo de Deuda",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Configura frecuencia, interés % y reglas de pago",
                                    color = TextSecondaryDark,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isFixedExpense) "Nombre del Gasto Fijo (ej. Alquiler, Internet)" else "Título (ej. Cashea Zapatería, Crédito Banco)") },
                    modifier = Modifier.fillMaxWidth().testTag("commitment_title_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalAmountText,
                        onValueChange = { totalAmountText = it },
                        label = { Text("Monto Base") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.3f).testTag("commitment_total_input"),
                        singleLine = true
                    )

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

                if (!isFixedExpense) {
                    // 1. Initial Payment (Inicial)
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
                                    .clickable { hasDownPayment = !hasDownPayment }
                            ) {
                                Checkbox(
                                    checked = hasDownPayment,
                                    onCheckedChange = { hasDownPayment = it },
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
                                        text = if (hasDownPayment) "El saldo restante se dividirá en las cuotas" else "Sin inicial (100% financiado)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasDownPayment) EmeraldLight else TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (hasDownPayment) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = downPaymentText,
                                    onValueChange = { downPaymentText = it },
                                    label = { Text("Monto de Cuota Inicial (${selectedCurrency.symbol})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth().testTag("down_payment_input"),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // 2. Cuotas y Frecuencia
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = installmentsCountText,
                            onValueChange = { installmentsCountText = it },
                            label = { Text("Cantidad de Cuotas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("commitment_installments_input"),
                            singleLine = true
                        )

                        ExposedDropdownMenuBox(
                            expanded = freqExpanded,
                            onExpandedChange = { freqExpanded = it },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            OutlinedTextField(
                                value = frequency.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Frecuencia") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = freqExpanded,
                                onDismissRequest = { freqExpanded = false }
                            ) {
                                Frequency.entries.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f.displayName) },
                                        onClick = {
                                            frequency = f
                                            freqExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // If Frequency is CUSTOM, ask explicitly for the days
                    if (frequency == Frequency.CUSTOM) {
                        OutlinedTextField(
                            value = customDaysText,
                            onValueChange = { customDaysText = it },
                            label = { Text("Frecuencia personalizada: ¿Cada cuántos días se paga cada cuota?") },
                            placeholder = { Text("Ej. 45") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("commitment_custom_days_input"),
                            singleLine = true
                        )
                    }

                    // 3. Cuotas ya pagadas anteriormente
                    OutlinedTextField(
                        value = alreadyPaidCountText,
                        onValueChange = { alreadyPaidCountText = it },
                        label = { Text("Cuotas ya pagadas previamente (ej. 0 o 1)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("already_paid_installments_input"),
                        singleLine = true
                    )

                    // 4. Intereses (Único y no duplicado!)
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
                                    .clickable { applyInterest = !applyInterest }
                            ) {
                                Checkbox(
                                    checked = applyInterest,
                                    onCheckedChange = { applyInterest = it },
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
                                        text = if (applyInterest) "Interés configurable (mensual o anual)" else "0% de interés (sin recargo adicional)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (applyInterest) EmeraldLight else TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (applyInterest) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = interestRateText,
                                        onValueChange = { interestRateText = it },
                                        label = { Text("Tasa de Interés (%)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.2f).testTag("commitment_interest_input"),
                                        singleLine = true
                                    )

                                    // Period toggle: Mensual vs Anual
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (!isAnnualInterest) EmeraldPrimary else SlateBackground,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { isAnnualInterest = false }
                                    ) {
                                        Text(
                                            text = "Mensual",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isAnnualInterest) NavySurface else TextSecondaryDark,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAnnualInterest) EmeraldPrimary else SlateBackground,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { isAnnualInterest = true }
                                    ) {
                                        Text(
                                            text = "Anual",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAnnualInterest) NavySurface else TextSecondaryDark,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Resumen de cálculo
                    if (calculatedInstallmentAmount > 0) {
                        val freqDesc = if (frequency == Frequency.CUSTOM) "cada $customDays días" else "cada ${frequency.displayName.lowercase()}"
                        Surface(
                            color = EmeraldDark.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Monto por cuota: ${selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", calculatedInstallmentAmount)} $freqDesc",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldLight,
                                    fontWeight = FontWeight.Bold
                                )
                                if (hasDownPayment && downPayment > 0) {
                                    Text(
                                        text = "Inicial pagada: ${selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", downPayment)} • Restante financiado: ${selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", totalFinancedWithInterest)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                                if (applyInterest && rawInterestRate > 0) {
                                    Text(
                                        text = "Total a pagar (${totalInstallments} cuotas + ${rawInterestRate}% ${if (isAnnualInterest) "anual" else "mensual"}): ${selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", totalWithInterest)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AccentAmber,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Text(
                                        text = "0% interés • Total: ${selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", totalAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // 5. CRONOGRAMA DE CUOTAS PLANIFICADAS Y FECHAS EDITABLES!
                    val activeSchedule = remember(customInstallments, totalInstallments, calculatedInstallmentAmount, frequency, customDays, alreadyPaidCount) {
                        if (customInstallments.isNotEmpty() && customInstallments.size == totalInstallments.toInt()) {
                            customInstallments
                        } else {
                            val list = mutableListOf<ProjectedInstallment>()
                            val startCal = Calendar.getInstance().apply {
                                time = commitmentToEdit?.startDate?.toDate() ?: Date()
                            }
                            for (i in 1..totalInstallments.toInt()) {
                                val dueCal = startCal.clone() as Calendar
                                when (frequency) {
                                    Frequency.WEEKLY -> dueCal.add(Calendar.WEEK_OF_YEAR, i)
                                    Frequency.BIWEEKLY -> dueCal.add(Calendar.DAY_OF_YEAR, i * 15)
                                    Frequency.MONTHLY -> dueCal.add(Calendar.MONTH, i)
                                    Frequency.CUSTOM -> dueCal.add(Calendar.DAY_OF_YEAR, i * customDays)
                                }
                                val isPaid = i <= alreadyPaidCount
                                list.add(
                                    ProjectedInstallment(
                                        number = i.toLong(),
                                        dueDate = Timestamp(dueCal.time),
                                        amount = calculatedInstallmentAmount,
                                        isPaid = isPaid,
                                        paidDate = if (isPaid) Timestamp(dueCal.time) else null
                                    )
                                )
                            }
                            list
                        }
                    }

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
                                    text = "Toca la fecha para editar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }

                            activeSchedule.forEachIndexed { idx, inst ->
                                val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(inst.dueDate.toDate())
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

                                        // Clickable date chip to edit date!
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldDark.copy(alpha = 0.35f),
                                            modifier = Modifier.clickable {
                                                showPlatformDatePicker(context, inst.dueDate.toDate()) { newDate ->
                                                    val updated = activeSchedule.toMutableList()
                                                    updated[idx] = inst.copy(dueDate = Timestamp(newDate))
                                                    customInstallments = updated
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
                                            text = "${selectedCurrency.symbol} ${String.format(Locale.US, "%.2f", inst.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimaryDark
                                        )

                                        // Status toggle chip
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (inst.isPaid) EmeraldLight.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f),
                                            modifier = Modifier.clickable {
                                                val updated = activeSchedule.toMutableList()
                                                val newPaid = !inst.isPaid
                                                updated[idx] = inst.copy(isPaid = newPaid, paidDate = if (newPaid) Timestamp.now() else null)
                                                customInstallments = updated
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
                } else {
                    // GASTO FIJO RECURRENTE (Con periodicidad y próximo vencimiento)
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
                                expanded = freqExpanded,
                                onExpandedChange = { freqExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = frequency.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Frecuencia con que se paga") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = freqExpanded,
                                    onDismissRequest = { freqExpanded = false }
                                ) {
                                    Frequency.entries.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.displayName) },
                                            onClick = {
                                                frequency = f
                                                freqExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (frequency == Frequency.CUSTOM) {
                                OutlinedTextField(
                                    value = customDaysText,
                                    onValueChange = { customDaysText = it },
                                    label = { Text("¿Cada cuántos días se repite?") },
                                    placeholder = { Text("Ej. 30") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            val fixedDueDateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(fixedExpenseDueDate)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Próximo cobro o vencimiento:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldDark.copy(alpha = 0.35f),
                                    modifier = Modifier.clickable {
                                        showPlatformDatePicker(context, fixedExpenseDueDate) { newDate ->
                                            fixedExpenseDueDate = newDate
                                        }
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(fixedDueDateFmt, color = EmeraldLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // Category
                val combinedCategories = (CATEGORY_EXPENSES + customCategories).distinct()
                CategorySelectorDropdown(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    categoriesList = combinedCategories,
                    isIncome = false,
                    onCreateCategory = onCreateCategory
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas u observaciones (opcional)") },
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

                    if (onDelete != null) {
                        Button(
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("delete_commitment_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) {
                            Text("Eliminar")
                        }
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && totalAmount > 0) {
                                val effectiveSchedule = if (!isFixedExpense) {
                                    if (customInstallments.isNotEmpty() && customInstallments.size == totalInstallments.toInt()) {
                                        customInstallments
                                    } else {
                                        val list = mutableListOf<ProjectedInstallment>()
                                        val startCal = Calendar.getInstance().apply { time = commitmentToEdit?.startDate?.toDate() ?: Date() }
                                        for (i in 1..totalInstallments.toInt()) {
                                            val dueCal = startCal.clone() as Calendar
                                            when (frequency) {
                                                Frequency.WEEKLY -> dueCal.add(Calendar.WEEK_OF_YEAR, i)
                                                Frequency.BIWEEKLY -> dueCal.add(Calendar.DAY_OF_YEAR, i * 15)
                                                Frequency.MONTHLY -> dueCal.add(Calendar.MONTH, i)
                                                Frequency.CUSTOM -> dueCal.add(Calendar.DAY_OF_YEAR, i * customDays)
                                            }
                                            val isPaid = i <= alreadyPaidCount
                                            list.add(
                                                ProjectedInstallment(
                                                    number = i.toLong(),
                                                    dueDate = Timestamp(dueCal.time),
                                                    amount = calculatedInstallmentAmount,
                                                    isPaid = isPaid,
                                                    paidDate = if (isPaid) Timestamp(dueCal.time) else null
                                                )
                                            )
                                        }
                                        list
                                    }
                                } else emptyList()

                                val effectiveTotal = if (!isFixedExpense) totalWithInterest else totalAmount
                                val remaining = if (!isFixedExpense) {
                                    effectiveSchedule.filter { !it.isPaid }.sumOf { it.amount }
                                } else totalAmount

                                onSave(
                                    ExpenseCommitment(
                                        id = commitmentToEdit?.id ?: "",
                                        userId = commitmentToEdit?.userId ?: "",
                                        title = title.trim(),
                                        totalAmount = effectiveTotal,
                                        remainingAmount = remaining,
                                        paymentMode = if (isFixedExpense) PaymentMode.FULL.name else PaymentMode.INSTALLMENTS.name,
                                        category = selectedCategory,
                                        isFixedExpense = isFixedExpense,
                                        currency = selectedCurrency.name,
                                        totalInstallments = if (isFixedExpense) 1L else totalInstallments,
                                        paidInstallments = if (isFixedExpense) 0L else effectiveSchedule.count { it.isPaid }.toLong(),
                                        installmentAmount = if (isFixedExpense) totalAmount else calculatedInstallmentAmount,
                                        downPayment = downPayment,
                                        interestRate = if (applyInterest && !isFixedExpense) rawInterestRate else 0.0,
                                        isAnnualInterest = isAnnualInterest,
                                        customDays = customDays,
                                        frequency = frequency.name,
                                        dueDate = if (isFixedExpense) Timestamp(fixedExpenseDueDate) else (effectiveSchedule.firstOrNull()?.dueDate ?: Timestamp.now()),
                                        accountId = defaultAccountId.ifBlank { null },
                                        status = if (remaining <= 0.001) CommitmentStatus.PAID_OFF.name else CommitmentStatus.ACTIVE.name,
                                        notes = notes.trim().ifBlank { null },
                                        installments = effectiveSchedule.map { it.toMap() },
                                        createdAt = commitmentToEdit?.createdAt
                                    )
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank() && totalAmount > 0,
                        modifier = Modifier.weight(1f).testTag("save_commitment_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(if (commitmentToEdit != null) "Actualizar" else "Crear")
                    }
                }
            }
        }
    }
}

// Dialog to Pay / Record Installment
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayCommitmentDialog(
    commitment: ExpenseCommitment,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (InstallmentPayment, Account) -> Unit
) {
    val suggestedAmount = if (commitment.paymentMode == PaymentMode.INSTALLMENTS.name) {
        commitment.installmentAmount.coerceAtMost(commitment.remainingAmount)
    } else commitment.remainingAmount

    val commCurrency = AppCurrency.fromCode(commitment.currency)
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", suggestedAmount)) }
    var selectedAccountId by remember {
        mutableStateOf(commitment.accountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var accountExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
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

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto en ${commCurrency.symbol}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                    singleLine = true
                )

                // Select Debit Account
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val accName = selectedAccount?.name ?: "Selecciona cuenta a debitar"
                    OutlinedTextField(
                        value = "$accName (${selectedAccount?.currency ?: ""})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Debitar de Cuenta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.currency}) • Saldo: ${acc.currentBalance}") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

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
                            if (amount > 0 && selectedAccount != null) {
                                onConfirm(
                                    InstallmentPayment(
                                        commitmentId = commitment.id,
                                        accountId = selectedAccount.id,
                                        amount = amount,
                                        currency = commitment.currency,
                                        installmentNumber = commitment.paidInstallments + 1L,
                                        paymentDate = Timestamp.now(),
                                        notes = notes.trim().ifBlank { null }
                                    ),
                                    selectedAccount
                                )
                                onDismiss()
                            }
                        },
                        enabled = amount > 0 && selectedAccount != null,
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
