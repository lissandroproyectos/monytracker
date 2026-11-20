package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.currency.AppCurrency
import com.example.data.currency.ExchangeRates
import com.example.data.model.Account
import com.example.data.model.BackupManager
import com.example.data.model.CloudBackupItem
import com.example.data.model.CommitmentStatus
import com.example.data.model.ExpenseCommitment
import com.example.data.model.InstallmentPayment
import com.example.data.model.MarketTemplate
import com.example.data.model.PaymentMode
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UserSettings
import com.google.firebase.Timestamp
import java.util.Date
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "FinanceRepository"
private const val PREFS_NAME = "local_guest_finance_prefs"
private const val KEY_GUEST_DATA = "guest_finance_json"

class FinanceRepository(
    private val db: FirebaseFirestore?,
    private val context: Context? = null
) {
    private val firestore: FirebaseFirestore
        get() = db ?: throw IllegalStateException("Firebase App is not initialized.")

    constructor(context: Context) : this(
        try {
            val databaseIdRes = context.applicationContext.resources.getIdentifier("firestore_database_id", "string", context.applicationContext.packageName)
            val databaseId = if (databaseIdRes != 0) context.applicationContext.getString(databaseIdRes) else "ai-studio-android-finanzas-cc0cfb4f-7207-4923-ae50-256f4126d7eb"
            FirebaseFirestore.getInstance(databaseId)
        } catch (e: Exception) {
            try { FirebaseFirestore.getInstance() } catch (ex: Exception) { null }
        },
        context.applicationContext
    )

    // Local guest state flows for offline / guest mode
    private val _localSettings = MutableStateFlow(UserSettings())
    private val _localAccounts = MutableStateFlow<List<Account>>(emptyList())
    private val _localTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _localCommitments = MutableStateFlow<List<ExpenseCommitment>>(emptyList())
    private val _localTemplates = MutableStateFlow<List<MarketTemplate>>(emptyList())

    init {
        loadLocalGuestData()
    }

    private fun loadLocalGuestData() {
        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = prefs.getString(KEY_GUEST_DATA, null)
                if (!json.isNullOrBlank()) {
                    val parsed = BackupManager.parseJson(json, "local_guest")
                    parsed.settings?.let { _localSettings.value = it }
                    _localAccounts.value = parsed.accounts
                    _localCommitments.value = parsed.commitments.map { it.normalizedIfCorruptedFixed() }
                    _localTransactions.value = parsed.transactions
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading local guest storage", e)
            }
        }
    }

    private fun persistLocalGuestData() {
        context?.let { ctx ->
            try {
                val json = BackupManager.exportToJson(
                    accounts = _localAccounts.value,
                    commitments = _localCommitments.value,
                    transactions = _localTransactions.value,
                    settings = _localSettings.value
                )
                ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_GUEST_DATA, json)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting local guest storage", e)
            }
        }
    }

    private fun isGuest(userId: String) = db == null || userId.isBlank() || userId == "local_guest"

    // User Settings
    fun observeSettings(userId: String): Flow<UserSettings> {
        if (isGuest(userId) || db == null) return _localSettings.asStateFlow()
        return firestore.collection("users").document(userId).collection("settings").document("preferences")
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserSettings::class.java) ?: UserSettings()
            }
    }

    suspend fun updateSettings(userId: String, settings: UserSettings) {
        if (isGuest(userId) || db == null) {
            _localSettings.value = settings
            persistLocalGuestData()
            return
        }
        firestore.collection("users").document(userId).collection("settings").document("preferences")
            .set(settings.toMap()).await()
    }

    // Accounts
    fun observeAccounts(userId: String): Flow<List<Account>> {
        if (isGuest(userId)) return _localAccounts.asStateFlow()
        return firestore.collection("users").document(userId).collection("accounts")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val acc = doc.toObject(Account::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (acc != null) {
                        val rawSavings = doc.getBoolean("isSavings") ?: doc.getBoolean("savings")
                        val rawPeriodic = doc.getBoolean("hasPeriodicIncome") ?: doc.getBoolean("periodicIncome")
                        acc.copy(
                            isSavings = rawSavings ?: acc.isSavings,
                            hasPeriodicIncome = rawPeriodic ?: acc.hasPeriodicIncome
                        )
                    } else null
                }
            }
    }

    suspend fun createAccount(userId: String, account: Account): String {
        if (isGuest(userId)) {
            val generatedId = if (account.id.isNotBlank()) account.id else UUID.randomUUID().toString()
            val newAcc = account.copy(id = generatedId, userId = "local_guest")
            _localAccounts.value = _localAccounts.value + newAcc
            persistLocalGuestData()
            return generatedId
        }
        val docRef = firestore.collection("users").document(userId).collection("accounts").document()
        val accountWithId = account.copy(id = docRef.id, userId = userId)
        docRef.set(accountWithId.toMap()).await()
        return docRef.id
    }

    suspend fun updateAccount(userId: String, account: Account) {
        if (isGuest(userId)) {
            _localAccounts.value = _localAccounts.value.map { if (it.id == account.id) account else it }
            persistLocalGuestData()
            return
        }
        if (account.id.isBlank()) return
        val docRef = firestore.collection("users").document(userId).collection("accounts").document(account.id)
        docRef.set(account.toMap(), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun deleteAccount(userId: String, accountId: String) {
        if (isGuest(userId)) {
            _localAccounts.value = _localAccounts.value.filter { it.id != accountId }
            persistLocalGuestData()
            return
        }
        firestore.collection("users").document(userId).collection("accounts").document(accountId).delete().await()
    }

    // Transactions
    fun observeTransactions(userId: String): Flow<List<Transaction>> {
        if (isGuest(userId)) {
            return _localTransactions.map { list -> list.sortedByDescending { it.date.seconds } }
        }
        return firestore.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val tx = doc.toObject(Transaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (tx != null) {
                        val rawFixed = doc.getBoolean("isFixedExpense") ?: doc.getBoolean("fixedExpense")
                        if (rawFixed != null) tx.copy(isFixedExpense = rawFixed) else tx
                    } else null
                }.sortedByDescending { it.date.seconds }
            }
    }

    private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

    private suspend fun resolveAccountCurrency(userId: String, accountId: String?): AppCurrency? {
        if (accountId.isNullOrBlank() || accountId == "EXTERNAL") return null
        if (isGuest(userId)) {
            return _localAccounts.value.firstOrNull { it.id == accountId }?.let { AppCurrency.fromCode(it.currency) }
        }
        return try {
            val snap = firestore.collection("users").document(userId).collection("accounts").document(accountId).get().await()
            val code = snap.getString("currency")
            code?.let { AppCurrency.fromCode(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun convertToTargetCurrency(
        amount: Double,
        from: AppCurrency,
        to: AppCurrency,
        rates: ExchangeRates
    ): Double {
        if (from == to) return round2(amount)
        return rates.convert(amount, from, to)
    }

    suspend fun addTransaction(
        userId: String,
        transaction: Transaction,
        rates: ExchangeRates? = null
    ) {
        val effectiveRates = rates ?: ExchangeRates()
        val txCurrency = AppCurrency.fromCode(transaction.currency)
        val calculatedBs = effectiveRates.convert(transaction.amount, txCurrency, AppCurrency.VES)

        val originAccCurrency = resolveAccountCurrency(userId, transaction.accountId) ?: txCurrency
        val originMagnitude = if (
            transaction.accountDelta != null &&
            transaction.accountDelta > 0.0 &&
            transaction.accountCurrency == originAccCurrency.name
        ) {
            round2(transaction.accountDelta)
        } else {
            convertToTargetCurrency(transaction.amount, txCurrency, originAccCurrency, effectiveRates)
        }

        val isTransferWithTarget = transaction.type == TransactionType.TRANSFER.name &&
            !transaction.targetAccountId.isNullOrBlank() &&
            transaction.targetAccountId != "EXTERNAL"
        val targetAccCurrency = if (isTransferWithTarget) {
            resolveAccountCurrency(userId, transaction.targetAccountId) ?: txCurrency
        } else null
        val targetMagnitude = if (isTransferWithTarget && targetAccCurrency != null) {
            if (
                transaction.targetAccountDelta != null &&
                transaction.targetAccountDelta > 0.0 &&
                transaction.targetAccountCurrency == targetAccCurrency.name
            ) {
                round2(transaction.targetAccountDelta)
            } else {
                convertToTargetCurrency(transaction.amount, txCurrency, targetAccCurrency, effectiveRates)
            }
        } else null

        val enrichedTx = transaction.copy(
            amountInBs = calculatedBs,
            accountDelta = if (transaction.accountId.isNotBlank() && transaction.accountId != "EXTERNAL") originMagnitude else null,
            accountCurrency = if (transaction.accountId.isNotBlank() && transaction.accountId != "EXTERNAL") originAccCurrency.name else null,
            targetAccountDelta = targetMagnitude,
            targetAccountCurrency = targetAccCurrency?.name
        )

        if (isGuest(userId)) {
            val genId = if (enrichedTx.id.isNotBlank()) enrichedTx.id else UUID.randomUUID().toString()
            val newTx = enrichedTx.copy(id = genId, userId = "local_guest")
            _localTransactions.value = listOf(newTx) + _localTransactions.value

            val delta = when (newTx.type) {
                TransactionType.INCOME.name -> originMagnitude
                TransactionType.EXPENSE.name -> -originMagnitude
                TransactionType.TRANSFER.name -> -originMagnitude
                else -> 0.0
            }
            if (delta != 0.0 && newTx.accountId.isNotBlank() && newTx.accountId != "EXTERNAL") {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    if (acc.id == newTx.accountId) acc.copy(currentBalance = round2(acc.currentBalance + delta)) else acc
                }
            }
            if (isTransferWithTarget && targetMagnitude != null) {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    if (acc.id == newTx.targetAccountId) acc.copy(currentBalance = round2(acc.currentBalance + targetMagnitude)) else acc
                }
            }
            persistLocalGuestData()
            return
        }

        val docRef = firestore.collection("users").document(userId).collection("transactions").document()
        val transactionWithId = enrichedTx.copy(
            id = docRef.id,
            userId = userId
        )
        docRef.set(transactionWithId.toMap()).await()

        val delta = when (transactionWithId.type) {
            TransactionType.INCOME.name -> originMagnitude
            TransactionType.EXPENSE.name -> -originMagnitude
            TransactionType.TRANSFER.name -> -originMagnitude
            else -> 0.0
        }
        if (delta != 0.0 && transactionWithId.accountId.isNotBlank() && transactionWithId.accountId != "EXTERNAL") {
            val accountDoc = firestore.collection("users").document(userId).collection("accounts").document(transactionWithId.accountId)
            accountDoc.update("currentBalance", FieldValue.increment(delta)).await()
        }

        if (isTransferWithTarget && targetMagnitude != null && !transactionWithId.targetAccountId.isNullOrBlank()) {
            val targetDoc = firestore.collection("users").document(userId).collection("accounts").document(transactionWithId.targetAccountId)
            targetDoc.update("currentBalance", FieldValue.increment(targetMagnitude)).await()
        }
    }

    suspend fun deleteTransaction(
        userId: String,
        transaction: Transaction,
        rates: ExchangeRates? = null
    ) {
        val effectiveRates = rates ?: ExchangeRates()
        val txCurrency = AppCurrency.fromCode(transaction.currency)

        // Respect origin currency: restore exact stored accountDelta in its origin currency if available
        val originAccCurrency = resolveAccountCurrency(userId, transaction.accountId)
        val storedOriginMagnitude = transaction.accountDelta?.takeIf { it > 0.0 } ?: transaction.amount
        val storedOriginCurrency = AppCurrency.fromCode(transaction.accountCurrency ?: transaction.currency)
        val originRevertMagnitude = if (originAccCurrency != null) {
            convertToTargetCurrency(storedOriginMagnitude, storedOriginCurrency, originAccCurrency, effectiveRates)
        } else {
            round2(storedOriginMagnitude)
        }

        val revertDelta = when (transaction.type) {
            TransactionType.INCOME.name -> -originRevertMagnitude
            TransactionType.EXPENSE.name -> originRevertMagnitude
            TransactionType.TRANSFER.name -> originRevertMagnitude
            else -> 0.0
        }

        val isTransferWithTarget = transaction.type == TransactionType.TRANSFER.name &&
            !transaction.targetAccountId.isNullOrBlank() &&
            transaction.targetAccountId != "EXTERNAL"
        val targetRevertMagnitude = if (isTransferWithTarget) {
            val targetAccCurrency = resolveAccountCurrency(userId, transaction.targetAccountId)
            val storedTargetMagnitude = transaction.targetAccountDelta?.takeIf { it > 0.0 } ?: transaction.amount
            val storedTargetCurrency = AppCurrency.fromCode(transaction.targetAccountCurrency ?: txCurrency.name)
            if (targetAccCurrency != null) {
                convertToTargetCurrency(storedTargetMagnitude, storedTargetCurrency, targetAccCurrency, effectiveRates)
            } else {
                round2(storedTargetMagnitude)
            }
        } else 0.0

        if (isGuest(userId)) {
            _localTransactions.value = _localTransactions.value.filter { it.id != transaction.id }
            if (revertDelta != 0.0 && transaction.accountId.isNotBlank() && transaction.accountId != "EXTERNAL") {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    if (acc.id == transaction.accountId) acc.copy(currentBalance = round2(acc.currentBalance + revertDelta)) else acc
                }
            }
            if (isTransferWithTarget && targetRevertMagnitude != 0.0) {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    if (acc.id == transaction.targetAccountId) acc.copy(currentBalance = round2(acc.currentBalance - targetRevertMagnitude)) else acc
                }
            }
            persistLocalGuestData()
            return
        }

        firestore.collection("users").document(userId).collection("transactions").document(transaction.id).delete().await()
        if (revertDelta != 0.0 && transaction.accountId.isNotBlank() && transaction.accountId != "EXTERNAL") {
            val accountDoc = firestore.collection("users").document(userId).collection("accounts").document(transaction.accountId)
            accountDoc.update("currentBalance", FieldValue.increment(revertDelta)).await()
        }
        if (isTransferWithTarget && targetRevertMagnitude != 0.0 && !transaction.targetAccountId.isNullOrBlank()) {
            val targetDoc = firestore.collection("users").document(userId).collection("accounts").document(transaction.targetAccountId)
            targetDoc.update("currentBalance", FieldValue.increment(-targetRevertMagnitude)).await()
        }
    }

    suspend fun updateTransaction(
        userId: String,
        oldTransaction: Transaction,
        newTransaction: Transaction,
        rates: ExchangeRates? = null
    ) {
        val effectiveRates = rates ?: ExchangeRates()
        val newTxCurrency = AppCurrency.fromCode(newTransaction.currency)
        val calculatedBs = effectiveRates.convert(newTransaction.amount, newTxCurrency, AppCurrency.VES)

        // Compute exact new account-native deltas
        val newOriginCurrency = resolveAccountCurrency(userId, newTransaction.accountId) ?: newTxCurrency
        val newOriginMagnitude = convertToTargetCurrency(newTransaction.amount, newTxCurrency, newOriginCurrency, effectiveRates)

        val newIsTransferWithTarget = newTransaction.type == TransactionType.TRANSFER.name &&
            !newTransaction.targetAccountId.isNullOrBlank() &&
            newTransaction.targetAccountId != "EXTERNAL"
        val newTargetCurrency = if (newIsTransferWithTarget) {
            resolveAccountCurrency(userId, newTransaction.targetAccountId) ?: newTxCurrency
        } else null
        val newTargetMagnitude = if (newIsTransferWithTarget && newTargetCurrency != null) {
            convertToTargetCurrency(newTransaction.amount, newTxCurrency, newTargetCurrency, effectiveRates)
        } else null

        val updatedTx = newTransaction.copy(
            id = oldTransaction.id,
            userId = if (isGuest(userId)) "local_guest" else userId,
            amountInBs = calculatedBs,
            commitmentId = newTransaction.commitmentId ?: oldTransaction.commitmentId,
            accountDelta = if (newTransaction.accountId.isNotBlank() && newTransaction.accountId != "EXTERNAL") newOriginMagnitude else null,
            accountCurrency = if (newTransaction.accountId.isNotBlank() && newTransaction.accountId != "EXTERNAL") newOriginCurrency.name else null,
            targetAccountDelta = newTargetMagnitude,
            targetAccountCurrency = newTargetCurrency?.name,
            createdAt = oldTransaction.createdAt ?: newTransaction.createdAt
        )

        // Compute net balance changes per accountId (revert old in its origin currency + apply new in its account currency)
        val netAccountDeltas = mutableMapOf<String, Double>()
        fun addDelta(accountId: String?, delta: Double) {
            if (!accountId.isNullOrBlank() && accountId != "EXTERNAL" && delta != 0.0) {
                netAccountDeltas[accountId] = round2((netAccountDeltas[accountId] ?: 0.0) + delta)
            }
        }

        // Revert old transaction impact in old account's origin currency
        val oldOriginAccCurrency = resolveAccountCurrency(userId, oldTransaction.accountId)
        val oldOriginStoredMag = oldTransaction.accountDelta?.takeIf { it > 0.0 } ?: oldTransaction.amount
        val oldOriginStoredCurr = AppCurrency.fromCode(oldTransaction.accountCurrency ?: oldTransaction.currency)
        val oldOriginRevertMag = if (oldOriginAccCurrency != null) {
            convertToTargetCurrency(oldOriginStoredMag, oldOriginStoredCurr, oldOriginAccCurrency, effectiveRates)
        } else round2(oldOriginStoredMag)

        when (oldTransaction.type) {
            TransactionType.INCOME.name -> addDelta(oldTransaction.accountId, -oldOriginRevertMag)
            TransactionType.EXPENSE.name -> addDelta(oldTransaction.accountId, oldOriginRevertMag)
            TransactionType.TRANSFER.name -> {
                addDelta(oldTransaction.accountId, oldOriginRevertMag)
                val oldTargetAccCurr = resolveAccountCurrency(userId, oldTransaction.targetAccountId)
                val oldTargetStoredMag = oldTransaction.targetAccountDelta?.takeIf { it > 0.0 } ?: oldTransaction.amount
                val oldTargetStoredCurr = AppCurrency.fromCode(oldTransaction.targetAccountCurrency ?: oldTransaction.currency)
                val oldTargetRevertMag = if (oldTargetAccCurr != null) {
                    convertToTargetCurrency(oldTargetStoredMag, oldTargetStoredCurr, oldTargetAccCurr, effectiveRates)
                } else round2(oldTargetStoredMag)
                addDelta(oldTransaction.targetAccountId, -oldTargetRevertMag)
            }
        }

        // Apply new transaction impact
        when (updatedTx.type) {
            TransactionType.INCOME.name -> addDelta(updatedTx.accountId, newOriginMagnitude)
            TransactionType.EXPENSE.name -> addDelta(updatedTx.accountId, -newOriginMagnitude)
            TransactionType.TRANSFER.name -> {
                addDelta(updatedTx.accountId, -newOriginMagnitude)
                if (newTargetMagnitude != null) {
                    addDelta(updatedTx.targetAccountId, newTargetMagnitude)
                }
            }
        }

        if (isGuest(userId)) {
            _localTransactions.value = _localTransactions.value.map {
                if (it.id == oldTransaction.id) updatedTx else it
            }.sortedByDescending { it.date.seconds }

            if (netAccountDeltas.isNotEmpty()) {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    val delta = netAccountDeltas[acc.id] ?: 0.0
                    if (kotlin.math.abs(delta) > 0.0001) {
                        acc.copy(currentBalance = round2(acc.currentBalance + delta))
                    } else acc
                }
            }
            persistLocalGuestData()
            return
        }

        firestore.collection("users").document(userId)
            .collection("transactions").document(oldTransaction.id)
            .set(updatedTx.toMap())
            .await()

        netAccountDeltas.forEach { (accId, delta) ->
            if (kotlin.math.abs(delta) > 0.0001) {
                val accountDoc = firestore.collection("users").document(userId)
                    .collection("accounts").document(accId)
                accountDoc.update("currentBalance", FieldValue.increment(delta)).await()
            }
        }
    }

    // Expense Commitments
    fun observeCommitments(userId: String): Flow<List<ExpenseCommitment>> {
        if (isGuest(userId)) {
            return _localCommitments.map { list ->
                list.map { it.normalizedIfCorruptedFixed() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        }
        return firestore.collection("users").document(userId).collection("commitments")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val com = doc.toObject(ExpenseCommitment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (com != null) {
                        val rawFixed = doc.getBoolean("isFixedExpense") ?: doc.getBoolean("fixedExpense")
                        val rawAnnual = doc.getBoolean("isAnnualInterest") ?: doc.getBoolean("annualInterest")
                        com.copy(
                            isFixedExpense = rawFixed ?: com.isFixedExpense,
                            isAnnualInterest = rawAnnual ?: com.isAnnualInterest
                        ).normalizedIfCorruptedFixed()
                    } else null
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    suspend fun repairCorruptedFixedCommitments(userId: String) {
        try {
            if (isGuest(userId)) {
                var changed = false
                val updated = _localCommitments.value.map { com ->
                    val healed = com.normalizedIfCorruptedFixed()
                    if (healed.status != com.status || healed.remainingAmount != com.remainingAmount) {
                        changed = true
                    }
                    healed
                }
                if (changed) {
                    _localCommitments.value = updated
                    persistLocalGuestData()
                }
                return
            }

            val snapshot = firestore.collection("users").document(userId)
                .collection("commitments")
                .get()
                .await()

            for (doc in snapshot.documents) {
                val com = doc.toObject(ExpenseCommitment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: continue
                val rawFixed = doc.getBoolean("isFixedExpense") ?: doc.getBoolean("fixedExpense")
                val rawAnnual = doc.getBoolean("isAnnualInterest") ?: doc.getBoolean("annualInterest")
                val resolved = com.copy(
                    isFixedExpense = rawFixed ?: com.isFixedExpense,
                    isAnnualInterest = rawAnnual ?: com.isAnnualInterest
                )
                val healed = resolved.normalizedIfCorruptedFixed()
                if (healed.status != resolved.status || kotlin.math.abs(healed.remainingAmount - resolved.remainingAmount) > 0.01) {
                    updateCommitment(userId, healed)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed repairing fixed commitments: ${e.message}")
        }
    }

    suspend fun createCommitment(userId: String, commitment: ExpenseCommitment): String {
        val projections = commitment.getProjectedInstallments().map { it.toMap() }
        val remAmount = if (commitment.remainingAmount <= 0.0) commitment.totalAmount else commitment.remainingAmount

        if (isGuest(userId)) {
            val genId = if (commitment.id.isNotBlank()) commitment.id else UUID.randomUUID().toString()
            val comWithId = commitment.copy(
                id = genId,
                userId = "local_guest",
                remainingAmount = remAmount,
                installments = projections
            )
            _localCommitments.value = listOf(comWithId) + _localCommitments.value
            persistLocalGuestData()
            return genId
        }

        val docRef = firestore.collection("users").document(userId).collection("commitments").document()
        val commitmentWithId = commitment.copy(
            id = docRef.id,
            userId = userId,
            remainingAmount = remAmount,
            installments = projections
        )
        docRef.set(commitmentWithId.toMap()).await()
        return docRef.id
    }

    suspend fun updateCommitment(userId: String, commitment: ExpenseCommitment) {
        if (isGuest(userId)) {
            _localCommitments.value = _localCommitments.value.map { if (it.id == commitment.id) commitment else it }
            persistLocalGuestData()
            return
        }
        if (commitment.id.isBlank()) return
        val docRef = firestore.collection("users").document(userId).collection("commitments").document(commitment.id)
        docRef.set(commitment.toMap(), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun deleteCommitment(userId: String, commitmentId: String) {
        if (isGuest(userId)) {
            _localCommitments.value = _localCommitments.value.filter { it.id != commitmentId }
            persistLocalGuestData()
            return
        }
        firestore.collection("users").document(userId).collection("commitments").document(commitmentId).delete().await()
    }

    suspend fun toggleInstallmentPaid(
        userId: String,
        commitment: ExpenseCommitment,
        installmentNumber: Long,
        markAsPaid: Boolean,
        payFromAccount: Account? = null,
        rates: ExchangeRates? = null,
        paidDate: Date? = null,
        paidAmount: Double? = null
    ) {
        val currentInstallments = commitment.getProjectedInstallments().toMutableList()
        val index = currentInstallments.indexOfFirst { it.number == installmentNumber }
        if (index == -1) return

        val target = currentInstallments[index]
        if (target.isPaid == markAsPaid) return

        val effectivePaidDate = paidDate ?: Date()
        val effectiveAmount = if (markAsPaid && paidAmount != null && paidAmount > 0.0) round2(paidAmount) else target.amount
        val updatedInstallment = target.copy(
            isPaid = markAsPaid,
            paidDate = if (markAsPaid) Timestamp(effectivePaidDate) else null,
            amount = effectiveAmount
        )
        currentInstallments[index] = updatedInstallment

        val (paidCount, newRemaining, newStatus) = commitment.computeUpdatedState(currentInstallments)
        val nextDueDate = currentInstallments.firstOrNull { !it.isPaid }?.dueDate ?: commitment.dueDate

        val updatedCommitment = commitment.copy(
            installments = currentInstallments.map { it.toMap() },
            paidInstallments = paidCount,
            remainingAmount = newRemaining,
            status = newStatus,
            dueDate = nextDueDate
        )
        updateCommitment(userId, updatedCommitment)

        if (markAsPaid && payFromAccount != null && payFromAccount.id.isNotBlank() && payFromAccount.id != "EXTERNAL") {
            val tx = Transaction(
                userId = userId,
                accountId = payFromAccount.id,
                type = TransactionType.EXPENSE.name,
                amount = effectiveAmount,
                currency = commitment.currency,
                category = commitment.category,
                description = "Cuota $installmentNumber/${commitment.totalInstallments}: ${commitment.title}",
                date = Timestamp(effectivePaidDate),
                commitmentId = commitment.id,
                isFixedExpense = commitment.isFixedExpense
            )
            addTransaction(userId, tx, rates)
        } else if (!markAsPaid) {
            // Return money to its original paying account in its origin currency when unmarking as paid
            val linkedTx = if (isGuest(userId)) {
                _localTransactions.value.firstOrNull { tx ->
                    tx.commitmentId == commitment.id && tx.description.contains("Cuota $installmentNumber/")
                }
            } else {
                try {
                    val snap = firestore.collection("users").document(userId)
                        .collection("transactions")
                        .whereEqualTo("commitmentId", commitment.id)
                        .get()
                        .await()
                    snap.documents
                        .mapNotNull { it.toObject(Transaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                        .firstOrNull { it.description.contains("Cuota $installmentNumber/") }
                } catch (e: Exception) {
                    null
                }
            }
            if (linkedTx != null) {
                deleteTransaction(userId, linkedTx, rates)
            }
        }
    }

    suspend fun recordPayment(
        userId: String,
        payment: InstallmentPayment,
        commitment: ExpenseCommitment,
        account: Account?,
        rates: ExchangeRates? = null
    ) {
        require(payment.amount > 0)

        val currentInstallments = commitment.getProjectedInstallments().toMutableList()
        val unpaidFirst = currentInstallments.indexOfFirst { !it.isPaid }
        if (unpaidFirst != -1) {
            currentInstallments[unpaidFirst] = currentInstallments[unpaidFirst].copy(
                isPaid = true,
                paidDate = payment.paymentDate
            )
        }

        val (newPaidCount, newRemaining, newStatus) = if (commitment.isFixedExpense) {
            val (paidC, rem, st) = commitment.computeUpdatedState(currentInstallments)
            val safeRemaining = if (currentInstallments.isEmpty()) {
                kotlin.math.round((commitment.remainingAmount - payment.amount).coerceAtLeast(0.0) * 100.0) / 100.0
            } else rem
            Triple(paidC, safeRemaining, st)
        } else {
            val rem = kotlin.math.round((commitment.remainingAmount - payment.amount).coerceAtLeast(0.0) * 100.0) / 100.0
            val paidC = if (commitment.paymentMode == PaymentMode.INSTALLMENTS.name) {
                commitment.paidInstallments + 1L
            } else commitment.paidInstallments
            val st = if (rem <= 0.001) CommitmentStatus.PAID_OFF.name else commitment.status
            Triple(paidC, rem, st)
        }

        val nextDueDate = currentInstallments.firstOrNull { !it.isPaid }?.dueDate ?: commitment.dueDate

        val updatedCommitment = commitment.copy(
            remainingAmount = newRemaining,
            paidInstallments = newPaidCount,
            status = newStatus,
            dueDate = nextDueDate,
            installments = currentInstallments.map { it.toMap() }
        )

        updateCommitment(userId, updatedCommitment)

        val paymentDesc = when (commitment.paymentMode) {
            PaymentMode.INSTALLMENTS.name -> "Cuota $newPaidCount/${commitment.totalInstallments}: ${commitment.title}"
            PaymentMode.PARTIAL.name -> "Abono a deuda: ${commitment.title}"
            else -> "Pago: ${commitment.title}"
        }

        // Only create an expense transaction if an account was selected and not external money
        if (account != null && payment.accountId.isNotBlank() && payment.accountId != "EXTERNAL") {
            val transaction = Transaction(
                userId = userId,
                accountId = payment.accountId,
                type = TransactionType.EXPENSE.name,
                amount = payment.amount,
                currency = commitment.currency,
                category = commitment.category,
                description = paymentDesc,
                date = payment.paymentDate,
                commitmentId = commitment.id,
                isFixedExpense = commitment.isFixedExpense,
                notes = payment.notes
            )
            addTransaction(userId, transaction, rates)
        }
    }

    // Marketplace Templates
    fun observeMarketplaceTemplates(): Flow<List<MarketTemplate>> {
        return _localTemplates.asStateFlow()
    }

    suspend fun publishTemplate(userId: String, template: MarketTemplate): String {
        val genId = if (template.id.isNotBlank()) template.id else UUID.randomUUID().toString()
        val item = template.copy(id = genId, userId = userId)
        _localTemplates.value = listOf(item) + _localTemplates.value.filter { it.id != genId }
        if (!isGuest(userId)) {
            try {
                val docRef = firestore.collection("market_templates").document(genId)
                docRef.set(item.toMap()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed publishing template to cloud", e)
            }
        }
        return genId
    }

    suspend fun upvoteTemplate(templateId: String) {
        _localTemplates.value = _localTemplates.value.map {
            if (it.id == templateId) it.copy(upvotes = it.upvotes + 1) else it
        }
        if (db != null) {
            try {
                firestore.collection("market_templates").document(templateId)
                    .update("upvotes", FieldValue.increment(1)).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed upvoting in cloud", e)
            }
        }
    }

    // Backups & Restore
    suspend fun saveBackupSnapshot(
        userId: String,
        note: String,
        accounts: List<Account>,
        commitments: List<ExpenseCommitment>,
        transactions: List<Transaction>,
        settings: UserSettings?
    ): String {
        val jsonPayload = BackupManager.exportToJson(accounts, commitments, transactions, settings)
        if (isGuest(userId)) {
            persistLocalGuestData()
            return "local_snapshot_${System.currentTimeMillis()}"
        }
        val docRef = firestore.collection("users").document(userId).collection("backups").document()
        val data = mapOf(
            "id" to docRef.id,
            "userId" to userId,
            "note" to note.ifBlank { "Respaldo automático" },
            "timestamp" to FieldValue.serverTimestamp(),
            "accountsCount" to accounts.size,
            "commitmentsCount" to commitments.size,
            "transactionsCount" to transactions.size,
            "payload" to jsonPayload
        )
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun importAndRestoreData(
        userId: String,
        jsonString: String
    ): BackupManager.ParsedBackup {
        val parsed = BackupManager.parseJson(jsonString, userId)

        if (isGuest(userId)) {
            parsed.settings?.let { _localSettings.value = it }
            _localAccounts.value = parsed.accounts
            _localCommitments.value = parsed.commitments
            _localTransactions.value = parsed.transactions
            persistLocalGuestData()
            return parsed
        }

        parsed.settings?.let { s -> updateSettings(userId, s) }

        for (acc in parsed.accounts) {
            val docRef = if (acc.id.isNotBlank()) {
                firestore.collection("users").document(userId).collection("accounts").document(acc.id)
            } else {
                firestore.collection("users").document(userId).collection("accounts").document()
            }
            docRef.set(acc.copy(id = docRef.id, userId = userId).toMap()).await()
        }

        for (com in parsed.commitments) {
            val docRef = if (com.id.isNotBlank()) {
                firestore.collection("users").document(userId).collection("commitments").document(com.id)
            } else {
                firestore.collection("users").document(userId).collection("commitments").document()
            }
            docRef.set(com.copy(id = docRef.id, userId = userId).toMap()).await()
        }

        for (tx in parsed.transactions) {
            val docRef = if (tx.id.isNotBlank()) {
                firestore.collection("users").document(userId).collection("transactions").document(tx.id)
            } else {
                firestore.collection("users").document(userId).collection("transactions").document()
            }
            docRef.set(tx.copy(id = docRef.id, userId = userId).toMap()).await()
        }

        return parsed
    }

    fun observeCloudBackups(userId: String): Flow<List<CloudBackupItem>> {
        if (isGuest(userId)) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return firestore.collection("users").document(userId).collection("backups")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val note = doc.getString("note") ?: "Respaldo"
                        val accountsCount = doc.getLong("accountsCount")?.toInt() ?: 0
                        val commitmentsCount = doc.getLong("commitmentsCount")?.toInt() ?: 0
                        val transactionsCount = doc.getLong("transactionsCount")?.toInt() ?: 0
                        val payload = doc.getString("payload") ?: ""
                        
                        val tsObj = doc.get("timestamp")
                        val tsLong = when (tsObj) {
                            is Timestamp -> tsObj.toDate().time
                            is Long -> tsObj
                            is Number -> tsObj.toLong()
                            else -> System.currentTimeMillis()
                        }

                        CloudBackupItem(
                            id = id,
                            userId = userId,
                            note = note,
                            timestamp = tsLong,
                            accountsCount = accountsCount,
                            commitmentsCount = commitmentsCount,
                            transactionsCount = transactionsCount,
                            payload = payload
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed parsing cloud backup doc ${doc.id}", e)
                        null
                    }
                }.sortedByDescending { it.timestamp }
            }
    }

    suspend fun deleteCloudBackup(userId: String, backupId: String) {
        if (!isGuest(userId) && db != null) {
            try {
                firestore.collection("users").document(userId).collection("backups").document(backupId).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting cloud backup", e)
            }
        }
    }
}
