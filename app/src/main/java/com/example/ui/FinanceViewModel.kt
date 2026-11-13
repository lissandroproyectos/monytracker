package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.currency.AppCurrency
import com.example.data.currency.CurrencyService
import com.example.data.currency.ExchangeRates
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.CloudBackupItem
import com.example.data.model.CommitmentStatus
import com.example.data.model.ExpenseCommitment
import com.example.data.model.Frequency
import com.example.data.model.InstallmentPayment
import com.example.data.model.MarketTemplate
import com.example.data.model.PaymentMode
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UserSettings
import com.example.data.repository.FinanceRepository
import com.example.ui.components.CelebrationData
import com.example.ui.components.formatCurrency
import com.google.firebase.Timestamp
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import com.example.ui.screens.PlannedEvent

data class FinancialHealth(
    val totalAvailableInDisplay: Double = 0.0,
    val totalSavingsInDisplay: Double = 0.0,
    val totalFixedExpensesInDisplay: Double = 0.0,
    val totalDebtsInDisplay: Double = 0.0,
    val safetyCushionAmount: Double = 150.0,
    val isSafetyCushionBreached: Boolean = false,
    val surplusAvailableForSavings: Double = 0.0,
    val displayCurrency: AppCurrency = AppCurrency.USD_BCV,
    val paymentCurrency: AppCurrency = AppCurrency.USD_BCV
)

data class CashflowPoint(
    val date: Date,
    val label: String,
    val projectedBalance: Double,
    val change: Double,
    val description: String
)

sealed interface UiMessage {
    data class Success(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

class FinanceViewModel(
    private val repository: FinanceRepository,
    val currentUserId: String
) : ViewModel() {

    private val _exchangeRates = MutableStateFlow(ExchangeRates())
    val exchangeRates = _exchangeRates.asStateFlow()

    val settings: StateFlow<UserSettings> = repository.observeSettings(currentUserId)
        .catch { emit(UserSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val accounts: StateFlow<List<Account>> = repository.observeAccounts(currentUserId)
        .catch { e ->
            _errorMessage.value = "Error al cargar cuentas: ${e.localizedMessage}"
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.observeTransactions(currentUserId)
        .catch { e ->
            _errorMessage.value = "Error al cargar movimientos: ${e.localizedMessage}"
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commitments: StateFlow<List<ExpenseCommitment>> = repository.observeCommitments(currentUserId)
        .catch { e ->
            _errorMessage.value = "Error al cargar compromisos: ${e.localizedMessage}"
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketplaceTemplates: StateFlow<List<MarketTemplate>> = repository.observeMarketplaceTemplates()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudBackups: StateFlow<List<CloudBackupItem>> = repository.observeCloudBackups(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Projection period filter (months)
    private val _projectionMonths = MutableStateFlow(3)
    val projectionMonths = _projectionMonths.asStateFlow()

    fun setProjectionPeriodMonths(months: Int) {
        _projectionMonths.value = months
    }

    // Comprehensive financial metrics in display currency
    val financialHealth: StateFlow<FinancialHealth> = combine(
        accounts,
        commitments,
        settings,
        exchangeRates
    ) { accs, comms, userSettings, rates ->
        val dispCurr = AppCurrency.fromCode(userSettings.displayCurrency)
        val payCurr = AppCurrency.fromCode(userSettings.paymentCurrency)

        // Operating liquid cash vs Savings
        val liquidAccounts = accs.filter { !it.isSavings }
        val savingsAccounts = accs.filter { it.isSavings }

        val totalAvailable = liquidAccounts.sumOf { acc ->
            rates.convert(acc.currentBalance, AppCurrency.fromCode(acc.currency), dispCurr)
        }

        val totalSavings = savingsAccounts.sumOf { acc ->
            rates.convert(acc.currentBalance, AppCurrency.fromCode(acc.currency), dispCurr)
        }

        val activeComms = comms.filter { it.status == CommitmentStatus.ACTIVE.name }

        // Separate fixed expenses vs temporary debts
        val fixedExpenses = activeComms.filter { it.isFixedExpense }.sumOf { c ->
            val recurringAmt = if (c.installmentAmount > 0) c.installmentAmount else (if (c.totalInstallments > 0) c.totalAmount / c.totalInstallments else c.totalAmount)
            rates.convert(recurringAmt, AppCurrency.fromCode(c.currency), dispCurr)
        }

        val temporaryDebts = activeComms.filter { !it.isFixedExpense }.sumOf { c ->
            rates.convert(c.remainingAmount, AppCurrency.fromCode(c.currency), dispCurr)
        }

        val cushion = userSettings.safetyCushionAmount
        val isBreached = totalAvailable < cushion
        val surplus = (totalAvailable - (cushion + temporaryDebts + fixedExpenses)).coerceAtLeast(0.0)

        FinancialHealth(
            totalAvailableInDisplay = totalAvailable,
            totalSavingsInDisplay = totalSavings,
            totalFixedExpensesInDisplay = fixedExpenses,
            totalDebtsInDisplay = temporaryDebts,
            safetyCushionAmount = cushion,
            isSafetyCushionBreached = isBreached,
            surplusAvailableForSavings = surplus,
            displayCurrency = dispCurr,
            paymentCurrency = payCurr
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialHealth())

    // Projected cashflow points for graph across chosen range
    val projectedCashflow: StateFlow<List<CashflowPoint>> = combine(
        financialHealth,
        commitments,
        accounts,
        projectionMonths,
        exchangeRates
    ) { health, comms, accs, months, rates ->
        val points = mutableListOf<CashflowPoint>()
        var runningBalance = health.totalAvailableInDisplay
        val cal = Calendar.getInstance()
        val now = cal.time

        // Point 0: today
        points.add(
            CashflowPoint(
                date = now,
                label = "Hoy",
                projectedBalance = runningBalance,
                change = 0.0,
                description = "Saldo actual disponible"
            )
        )

        // Project upcoming installments across the target months
        val endCal = Calendar.getInstance().apply { add(Calendar.MONTH, months) }
        val allEvents = mutableListOf<Pair<Date, Triple<Double, String, String>>>()

        // 1. Debt and Expense commitments
        comms.filter { it.status == CommitmentStatus.ACTIVE.name }.forEach { c ->
            val commCurrency = AppCurrency.fromCode(c.currency)
            c.getProjectedInstallments().filter { !it.isPaid }.forEach { inst ->
                val instDate = inst.dueDate.toDate()
                if (instDate.after(now) && instDate.before(endCal.time)) {
                    val amountInDisp = rates.convert(inst.amount, commCurrency, health.displayCurrency)
                    allEvents.add(
                        instDate to Triple(
                            amountInDisp,
                            "${c.title} (Cuota ${inst.number}/${c.totalInstallments})",
                            if (c.isFixedExpense) "Gasto Fijo" else "Deuda"
                        )
                    )
                }
            }
        }

        // 2. Periodic Incomes configured on Accounts
        accs.filter { it.hasPeriodicIncome && it.periodicIncomeAmount > 0 }.forEach { acc ->
            val incCurrency = AppCurrency.fromCode(acc.periodicIncomeCurrency)
            val incAmountDisp = rates.convert(acc.periodicIncomeAmount, incCurrency, health.displayCurrency)

            val stepCal = Calendar.getInstance()
            while (stepCal.before(endCal)) {
                when (acc.periodicIncomeFrequency) {
                    "QUINCENAL" -> {
                        val d15 = (stepCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 15) }
                        if (d15.time.after(now) && d15.before(endCal)) {
                            allEvents.add(d15.time to Triple(incAmountDisp, "${acc.periodicIncomeDescription} (${acc.name})", "Ingreso Periódico"))
                        }
                        val dEnd = (stepCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
                        if (dEnd.time.after(now) && dEnd.before(endCal)) {
                            allEvents.add(dEnd.time to Triple(incAmountDisp, "${acc.periodicIncomeDescription} (${acc.name})", "Ingreso Periódico"))
                        }
                        stepCal.add(Calendar.MONTH, 1)
                    }
                    "SEMANAL" -> {
                        stepCal.add(Calendar.DAY_OF_YEAR, 7)
                        if (stepCal.time.after(now) && stepCal.before(endCal)) {
                            allEvents.add(stepCal.time to Triple(incAmountDisp, "${acc.periodicIncomeDescription} (${acc.name})", "Ingreso Periódico"))
                        }
                    }
                    else -> { // MENSUAL
                        val targetDay = acc.periodicIncomeDayOfMonth.coerceIn(1, stepCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        val dMonth = (stepCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, targetDay) }
                        if (dMonth.time.after(now) && dMonth.before(endCal)) {
                            allEvents.add(dMonth.time to Triple(incAmountDisp, "${acc.periodicIncomeDescription} (${acc.name})", "Ingreso Periódico"))
                        }
                        stepCal.add(Calendar.MONTH, 1)
                    }
                }
            }
        }

        allEvents.sortBy { it.first }

        allEvents.forEach { (date, info) ->
            val (amount, title, type) = info
            val isIncome = type == "Ingreso Periódico"
            val delta = if (isIncome) amount else -amount
            runningBalance += delta
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.forLanguageTag("es-ES"))
            points.add(
                CashflowPoint(
                    date = date,
                    label = sdf.format(date),
                    projectedBalance = runningBalance,
                    change = delta,
                    description = "$type: $title"
                )
            )
        }

        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userMessage = MutableStateFlow<UiMessage?>(null)
    val userMessage = _userMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _celebrationEvent = MutableStateFlow<CelebrationData?>(null)
    val celebrationEvent = _celebrationEvent.asStateFlow()

    private val _isRefreshingRates = MutableStateFlow(false)
    val isRefreshingRates = _isRefreshingRates.asStateFlow()

    init {
        refreshRates()
    }

    fun refreshRates() {
        viewModelScope.launch {
            _isRefreshingRates.value = true
            try {
                val newRates = CurrencyService.fetchRates()
                _exchangeRates.value = newRates
            } catch (e: Exception) {
                Log.w("FinanceVM", "Failed to refresh rates: ${e.message}")
            } finally {
                _isRefreshingRates.value = false
            }
        }
    }

    fun clearMessage() {
        _userMessage.value = null
        _errorMessage.value = null
    }

    fun clearCelebration() {
        _celebrationEvent.value = null
    }

    fun triggerCelebration(data: CelebrationData) {
        _celebrationEvent.value = data
    }

    fun updateSafetyCushion(amount: Double) {
        viewModelScope.launch {
            val curr = settings.value
            repository.updateSettings(currentUserId, curr.copy(safetyCushionAmount = amount))
            _userMessage.value = UiMessage.Success("Colchón de seguridad actualizado a $amount")
        }
    }

    fun updateCurrencyAndCushionSettings(display: AppCurrency, payment: AppCurrency, cushion: Double) {
        viewModelScope.launch {
            val curr = settings.value
            repository.updateSettings(
                currentUserId,
                curr.copy(
                    displayCurrency = display.name,
                    paymentCurrency = payment.name,
                    safetyCushionAmount = cushion
                )
            )
            _userMessage.value = UiMessage.Success("Configuración monetaria guardada: Ver en ${display.displayName}")
        }
    }

    fun updateCurrencies(display: AppCurrency, payment: AppCurrency) {
        viewModelScope.launch {
            val curr = settings.value
            repository.updateSettings(
                currentUserId,
                curr.copy(displayCurrency = display.name, paymentCurrency = payment.name)
            )
            _userMessage.value = UiMessage.Success("Monedas configuradas: Ver en ${display.displayName}, Pagar en ${payment.displayName}")
        }
    }

    fun createInitialAccountsIfEmpty() {
        viewModelScope.launch {
            if (accounts.value.isEmpty()) {
                try {
                    // Create practical Venezuelan multi-currency accounts
                    repository.createAccount(
                        currentUserId,
                        Account(
                            name = "Banesco / Pago Móvil (Bs)",
                            type = AccountType.BANK.name,
                            currentBalance = 150000.0,
                            initialBalance = 150000.0,
                            currency = AppCurrency.VES.name,
                            colorHex = "#10B981",
                            iconName = "bank"
                        )
                    )
                    repository.createAccount(
                        currentUserId,
                        Account(
                            name = "Efectivo USD (BCV)",
                            type = AccountType.CASH.name,
                            currentBalance = 200.0,
                            initialBalance = 200.0,
                            currency = AppCurrency.USD_BCV.name,
                            colorHex = "#3B82F6",
                            iconName = "cash"
                        )
                    )
                    repository.createAccount(
                        currentUserId,
                        Account(
                            name = "Binance USDT",
                            type = AccountType.DIGITAL_WALLET.name,
                            currentBalance = 350.0,
                            initialBalance = 350.0,
                            currency = AppCurrency.USDT.name,
                            colorHex = "#F59E0B",
                            iconName = "wallet"
                        )
                    )
                    repository.createAccount(
                        currentUserId,
                        Account(
                            name = "Fondo de Ahorro / Colchón",
                            type = AccountType.SAVINGS.name,
                            currentBalance = 250.0,
                            initialBalance = 250.0,
                            currency = AppCurrency.USD_BCV.name,
                            colorHex = "#8B5CF6",
                            iconName = "savings",
                            isSavings = true
                        )
                    )
                    _userMessage.value = UiMessage.Success("Cuentas multi-moneda configuradas")
                } catch (e: Exception) {
                    _errorMessage.value = "Error creando cuentas: ${e.localizedMessage}"
                }
            }
        }
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.createAccount(currentUserId, account)
                _userMessage.value = UiMessage.Success("Cuenta añadida exitosamente")
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear cuenta: ${e.localizedMessage}"
            }
        }
    }

    fun injectPlanStartCapital(amount: Double, currency: String = AppCurrency.USD_BCV.name) {
        viewModelScope.launch {
            try {
                val roundedAmount = kotlin.math.round(amount * 100.0) / 100.0
                val existing = accounts.value.firstOrNull { it.isPlanStartAccount }
                if (existing != null) {
                    val updated = existing.copy(
                        currentBalance = roundedAmount,
                        initialBalance = roundedAmount,
                        currency = currency,
                        isSystemAccount = true
                    )
                    repository.updateAccount(currentUserId, updated)
                } else {
                    val newAcc = Account(
                        id = "inicio_plan",
                        userId = currentUserId,
                        name = "Inicio del Plan",
                        type = AccountType.CASH.name,
                        currentBalance = roundedAmount,
                        initialBalance = roundedAmount,
                        currency = currency,
                        colorHex = "#6366F1",
                        isSavings = false,
                        isSystemAccount = true
                    )
                    repository.createAccount(currentUserId, newAcc)
                }
                val symbol = AppCurrency.fromCode(currency).symbol
                val formatted = String.format(Locale.US, "%.2f", roundedAmount)
                _userMessage.value = UiMessage.Success("Fondo de Inicio del Plan establecido en $symbol $formatted")
            } catch (e: Exception) {
                _errorMessage.value = "Error al inyectar capital inicial: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAccount(accountId: String) {
        val acc = accounts.value.firstOrNull { it.id == accountId }
        if (acc != null && acc.isPlanStartAccount) {
            _errorMessage.value = "La cuenta 'Inicio del Plan' es del sistema y no puede ser eliminada."
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteAccount(currentUserId, accountId)
                _userMessage.value = UiMessage.Success("Cuenta eliminada")
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar cuenta: ${e.localizedMessage}"
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.addTransaction(currentUserId, transaction, exchangeRates.value)
                _userMessage.value = UiMessage.Success("Movimiento registrado")
            } catch (e: Exception) {
                _errorMessage.value = "Error al registrar movimiento: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(currentUserId, transaction)
                _userMessage.value = UiMessage.Success("Movimiento eliminado y saldo restaurado")
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar movimiento: ${e.localizedMessage}"
            }
        }
    }

    fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(currentUserId, oldTransaction, newTransaction, exchangeRates.value)
                _userMessage.value = UiMessage.Success("Movimiento actualizado y saldo ajustado")
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar movimiento: ${e.localizedMessage}"
            }
        }
    }

    fun addCommitment(commitment: ExpenseCommitment) {
        viewModelScope.launch {
            try {
                repository.createCommitment(currentUserId, commitment)
                val typeLabel = if (commitment.isFixedExpense) "Gasto fijo" else "Deuda/Plan de cuotas"
                _userMessage.value = UiMessage.Success("$typeLabel creado con éxito")
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear compromiso: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCommitment(commitmentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCommitment(currentUserId, commitmentId)
                _userMessage.value = UiMessage.Success("Compromiso eliminado")
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar compromiso: ${e.localizedMessage}"
            }
        }
    }

    fun payCommitment(payment: InstallmentPayment, commitment: ExpenseCommitment, account: Account?) {
        viewModelScope.launch {
            try {
                val newRemaining = (commitment.remainingAmount - payment.amount).coerceAtLeast(0.0)
                val isPaidOff = newRemaining <= 0.001
                val commCurrency = AppCurrency.fromCode(commitment.currency)

                repository.recordPayment(currentUserId, payment, commitment, account, exchangeRates.value)
                val accMsg = if (account != null) "debitado de ${account.name}" else "registrado con dinero externo (sin debitar cuentas)"
                _userMessage.value = UiMessage.Success("Pago $accMsg")

                val title = if (isPaidOff) "¡Felicidades! 🎉 ¡Deuda Saldada!" else "¡Pago Registrado! 🎊"
                val message = if (isPaidOff) {
                    if (commitment.isFixedExpense) {
                        "¡Has completado el ciclo del gasto fijo \"${commitment.title}\"!"
                    } else {
                        "¡Has terminado y salido de la deuda \"${commitment.title}\"! Disfruta tu logro y libertad financiera."
                    }
                } else {
                    "Se debitó ${formatCurrency(payment.amount, commCurrency.symbol)} para \"${commitment.title}\". ¡Un paso más cerca de tu meta!"
                }

                _celebrationEvent.value = CelebrationData(
                    title = title,
                    message = message,
                    commitmentTitle = commitment.title,
                    amountPaid = payment.amount,
                    remainingAmount = newRemaining,
                    currencySymbol = commCurrency.symbol,
                    isPaidOff = isPaidOff
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error registrando pago: ${e.localizedMessage}"
            }
        }
    }

    fun toggleInstallment(
        commitment: ExpenseCommitment,
        installmentNumber: Long,
        markAsPaid: Boolean,
        payFromAccount: Account?,
        paidDate: Date? = null
    ) {
        viewModelScope.launch {
            try {
                val currentInstallments = commitment.getProjectedInstallments()
                val willBeAllPaid = markAsPaid && currentInstallments.all {
                    if (it.number == installmentNumber) true else it.isPaid
                }

                repository.toggleInstallmentPaid(
                    currentUserId,
                    commitment,
                    installmentNumber,
                    markAsPaid,
                    payFromAccount,
                    exchangeRates.value,
                    paidDate
                )
                val statusText = if (markAsPaid) "marcada como pagada" else "marcada como pendiente"
                val accText = payFromAccount?.let { " (${it.name})" } ?: " (dinero externo)"
                _userMessage.value = UiMessage.Success("Cuota #$installmentNumber $statusText$accText")

                if (willBeAllPaid) {
                    val commCurrency = AppCurrency.fromCode(commitment.currency)
                    _celebrationEvent.value = CelebrationData(
                        title = "¡Felicidades! 🎉 ¡Deuda Saldada!",
                        message = "¡Has completado todas las cuotas de \"${commitment.title}\"! ¡Eres libre de esta deuda!",
                        commitmentTitle = commitment.title,
                        amountPaid = commitment.installmentAmount,
                        remainingAmount = 0.0,
                        currencySymbol = commCurrency.symbol,
                        isPaidOff = true
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error actualizando cuota: ${e.localizedMessage}"
            }
        }
    }

    fun editEventPayment(
        event: PlannedEvent,
        newAccount: Account?,
        newPaidDate: Date,
        newAmount: Double,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                // 1. Find and delete previous transaction if one exists (reverting old account balance)
                var oldTx = if (event.transactionId != null) {
                    transactions.value.firstOrNull { it.id == event.transactionId }
                } else null

                if (oldTx == null && event.commitment != null && event.installmentNumber != null) {
                    oldTx = transactions.value.firstOrNull {
                        it.commitmentId == event.commitment.id &&
                        it.description.contains("Cuota ${event.installmentNumber}/")
                    }
                }

                if (oldTx == null && !event.isIncome && event.commitment != null) {
                    oldTx = transactions.value.firstOrNull {
                        it.commitmentId == event.commitment.id && isSameDay(it.date.toDate(), event.dueDate)
                    }
                }

                if (oldTx == null && event.isIncome) {
                    oldTx = transactions.value.firstOrNull {
                        it.type == "INCOME" &&
                        (event.transactionId == it.id || (it.amount == event.amount && isSameDay(it.date.toDate(), event.dueDate)))
                    }
                }

                if (oldTx != null) {
                    repository.deleteTransaction(currentUserId, oldTx)
                }

                // 2. Register updated payment/income
                if (event.commitment != null && event.installmentNumber != null) {
                    // Update installment in commitment
                    val currentInstallments = event.commitment.getProjectedInstallments().toMutableList()
                    val idx = currentInstallments.indexOfFirst { it.number == event.installmentNumber }
                    if (idx != -1) {
                        currentInstallments[idx] = currentInstallments[idx].copy(
                            isPaid = true,
                            paidDate = Timestamp(newPaidDate),
                            amount = if (newAmount > 0) newAmount else currentInstallments[idx].amount
                        )
                        val updatedCommitment = event.commitment.copy(
                            installments = currentInstallments.map { it.toMap() }
                        )
                        repository.updateCommitment(currentUserId, updatedCommitment)
                    }

                    if (newAccount != null && newAccount.id.isNotBlank() && newAccount.id != "EXTERNAL") {
                        val newTx = Transaction(
                            userId = currentUserId,
                            accountId = newAccount.id,
                            type = "EXPENSE",
                            amount = if (newAmount > 0) newAmount else event.amount,
                            currency = event.commitment.currency,
                            category = event.commitment.category,
                            description = "Cuota ${event.installmentNumber}/${event.commitment.totalInstallments}: ${event.commitment.title}" + if (notes.isNotBlank()) " ($notes)" else "",
                            date = Timestamp(newPaidDate),
                            commitmentId = event.commitment.id,
                            isFixedExpense = event.commitment.isFixedExpense,
                            notes = notes.trim().ifBlank { null }
                        )
                        repository.addTransaction(currentUserId, newTx, exchangeRates.value)
                    }
                } else if (!event.isIncome && event.commitment != null) {
                    if (newAccount != null && newAccount.id.isNotBlank() && newAccount.id != "EXTERNAL") {
                        val newTx = Transaction(
                            userId = currentUserId,
                            accountId = newAccount.id,
                            type = "EXPENSE",
                            amount = newAmount,
                            currency = event.currency.name,
                            category = event.commitment.category,
                            description = "Pago de ${event.title}" + if (notes.isNotBlank()) " ($notes)" else "",
                            date = Timestamp(newPaidDate),
                            commitmentId = event.commitment.id,
                            isFixedExpense = true,
                            notes = notes.trim().ifBlank { null }
                        )
                        repository.addTransaction(currentUserId, newTx, exchangeRates.value)
                    }
                } else if (event.isIncome) {
                    if (newAccount != null && newAccount.id.isNotBlank() && newAccount.id != "EXTERNAL") {
                        val newTx = Transaction(
                            userId = currentUserId,
                            accountId = newAccount.id,
                            type = "INCOME",
                            amount = newAmount,
                            currency = event.currency.name,
                            description = event.title + if (notes.isNotBlank()) " ($notes)" else "",
                            date = Timestamp(newPaidDate),
                            notes = notes.trim().ifBlank { null }
                        )
                        repository.addTransaction(currentUserId, newTx, exchangeRates.value)
                    }
                }

                val accName = newAccount?.name ?: "Dinero Externo"
                _userMessage.value = UiMessage.Success("Pago actualizado a $accName")
            } catch (e: Exception) {
                _errorMessage.value = "Error al editar pago: ${e.localizedMessage}"
            }
        }
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    // Transfer surplus or emergency funds between operating cash and savings
    fun transferSavings(
        fromAccount: Account,
        toAccount: Account,
        amount: Double,
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                var actualToAccount = toAccount
                if (actualToAccount.id.isBlank()) {
                    val newId = repository.createAccount(currentUserId, actualToAccount)
                    actualToAccount = actualToAccount.copy(id = newId)
                }

                val roundedAmount = kotlin.math.round(amount * 100.0) / 100.0

                val tx = Transaction(
                    userId = currentUserId,
                    accountId = fromAccount.id,
                    targetAccountId = actualToAccount.id,
                    type = "TRANSFER",
                    amount = roundedAmount,
                    currency = fromAccount.currency,
                    description = notes?.ifBlank { null }
                        ?: if (actualToAccount.isSavings) "Reserva a Fondo de Ahorro / Colchón" else "Retiro de emergencia desde Ahorros",
                    date = Timestamp.now()
                )
                repository.addTransaction(currentUserId, tx, exchangeRates.value)
                val curr = AppCurrency.fromCode(fromAccount.currency)
                _userMessage.value = UiMessage.Success("Transferidos ${formatCurrency(roundedAmount, curr.symbol)} de ${fromAccount.name} a ${actualToAccount.name}")
            } catch (e: Exception) {
                _errorMessage.value = "Error en transferencia: ${e.localizedMessage}"
            }
        }
    }

    // Share template in Marketplace
    fun publishMarketplaceTemplate(template: MarketTemplate) {
        viewModelScope.launch {
            try {
                repository.publishTemplate(currentUserId, template)
                _userMessage.value = UiMessage.Success("Plantilla compartida en el Marketplace")
            } catch (e: Exception) {
                _errorMessage.value = "Error publicando plantilla: ${e.localizedMessage}"
            }
        }
    }

    fun upvoteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                repository.upvoteTemplate(templateId)
            } catch (e: Exception) {
                Log.w("FinanceVM", "Failed upvoting: ${e.message}")
            }
        }
    }

    // CRUD Updates
    fun updateAccount(account: Account) {
        viewModelScope.launch {
            try {
                repository.updateAccount(currentUserId, account)
                _userMessage.value = UiMessage.Success("Cuenta ${account.name} actualizada")
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar cuenta: ${e.localizedMessage}"
            }
        }
    }

    fun updateCommitment(commitment: ExpenseCommitment) {
        viewModelScope.launch {
            try {
                repository.updateCommitment(currentUserId, commitment)
                val typeLabel = if (commitment.isFixedExpense) "Gasto fijo" else "Deuda/Plan de cuotas"
                _userMessage.value = UiMessage.Success("$typeLabel actualizado")
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar compromiso: ${e.localizedMessage}"
            }
        }
    }

    // Backups & JSON Export / Import
    fun exportBackupJson(): String {
        return com.example.data.model.BackupManager.exportToJson(
            accounts = accounts.value,
            commitments = commitments.value,
            transactions = transactions.value,
            settings = settings.value
        )
    }

    fun saveBackupSnapshot(note: String) {
        viewModelScope.launch {
            try {
                repository.saveBackupSnapshot(
                    userId = currentUserId,
                    note = note,
                    accounts = accounts.value,
                    commitments = commitments.value,
                    transactions = transactions.value,
                    settings = settings.value
                )
                _userMessage.value = UiMessage.Success("Respaldo guardado exitosamente")
            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar respaldo: ${e.localizedMessage}"
            }
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                val parsed = repository.importAndRestoreData(currentUserId, jsonString)
                _userMessage.value = UiMessage.Success("Respaldo restaurado: ${parsed.accounts.size} cuentas, ${parsed.commitments.size} compromisos")
            } catch (e: Exception) {
                _errorMessage.value = "Error al importar respaldo: ${e.localizedMessage}"
            }
        }
    }

    fun restoreCloudBackup(item: CloudBackupItem) {
        if (item.payload.isNotBlank()) {
            importBackup(item.payload)
        } else {
            _errorMessage.value = "Este punto de respaldo no contiene datos válidos."
        }
    }

    fun deleteCloudBackup(item: CloudBackupItem) {
        viewModelScope.launch {
            try {
                repository.deleteCloudBackup(currentUserId, item.id)
                _userMessage.value = UiMessage.Success("Respaldo eliminado de la nube")
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar respaldo: ${e.localizedMessage}"
            }
        }
    }

    fun addCustomCategory(categoryName: String, isIncome: Boolean) {
        val trimmed = categoryName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            try {
                val current = settings.value
                val updated = if (isIncome) {
                    if (current.customIncomeCategories.contains(trimmed)) return@launch
                    current.copy(customIncomeCategories = current.customIncomeCategories + trimmed)
                } else {
                    if (current.customExpenseCategories.contains(trimmed)) return@launch
                    current.copy(customExpenseCategories = current.customExpenseCategories + trimmed)
                }
                repository.updateSettings(currentUserId, updated)
                _userMessage.value = UiMessage.Success("Nueva categoría '$trimmed' agregada")
            } catch (e: Exception) {
                _errorMessage.value = "Error al agregar categoría: ${e.localizedMessage}"
            }
        }
    }

    fun updateGithubRepo(repo: String) {
        viewModelScope.launch {
            try {
                val current = settings.value
                repository.updateSettings(currentUserId, current.copy(githubRepo = repo.trim()))
            } catch (e: Exception) {
                Log.w("FinanceVM", "Error saving githubRepo: ${e.message}")
            }
        }
    }

    fun applySavingsSweep(
        amount: Double,
        currency: AppCurrency,
        note: String,
        fromAccountId: String? = null,
        toAccountId: String? = null
    ) {
        viewModelScope.launch {
            try {
                var savingsAcc = if (!toAccountId.isNullOrBlank()) {
                    accounts.value.firstOrNull { it.id == toAccountId }
                } else {
                    accounts.value.firstOrNull { it.isSavings }
                        ?: accounts.value.firstOrNull { it.name.contains("Ahorro", ignoreCase = true) }
                }

                if (savingsAcc == null) {
                    val newSavings = Account(
                        userId = currentUserId,
                        name = "Fondo de Ahorro / Colchón",
                        type = AccountType.SAVINGS.name,
                        currency = currency.name,
                        isSavings = true,
                        colorHex = "#8B5CF6",
                        iconName = "savings"
                    )
                    val newId = repository.createAccount(currentUserId, newSavings)
                    savingsAcc = newSavings.copy(id = newId)
                }

                val sourceAcc = if (!fromAccountId.isNullOrBlank()) {
                    accounts.value.firstOrNull { it.id == fromAccountId }
                } else {
                    accounts.value.filter { !it.isSavings && !it.isPlanStartAccount }
                        .maxByOrNull { it.currentBalance }
                        ?: accounts.value.firstOrNull { !it.isSavings }
                }

                if (sourceAcc != null) {
                    val amountInSource = exchangeRates.value.convert(amount, currency, AppCurrency.fromCode(sourceAcc.currency))
                    val roundedAmount = kotlin.math.round(amountInSource * 100.0) / 100.0
                    val tx = Transaction(
                        userId = currentUserId,
                        accountId = sourceAcc.id,
                        targetAccountId = savingsAcc.id,
                        type = TransactionType.TRANSFER.name,
                        amount = roundedAmount,
                        currency = sourceAcc.currency,
                        description = "💰 Cuota de Ahorro (Excedente): $note",
                        category = "Ahorro",
                        date = Timestamp.now()
                    )
                    repository.addTransaction(currentUserId, tx, exchangeRates.value)
                    val srcCurr = AppCurrency.fromCode(sourceAcc.currency)
                    _userMessage.value = UiMessage.Success("Guardados ${formatCurrency(roundedAmount, srcCurr.symbol)} en ${savingsAcc.name} desde ${sourceAcc.name}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error aplicando barrido de ahorro: ${e.localizedMessage}"
            }
        }
    }
}
