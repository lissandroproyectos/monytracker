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
                    _localCommitments.value = parsed.commitments
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

    suspend fun addTransaction(
        userId: String,
        transaction: Transaction,
        rates: ExchangeRates? = null
    ) {
        val txCurrency = AppCurrency.fromCode(transaction.currency)
        val calculatedBs = rates?.convert(transaction.amount, txCurrency, AppCurrency.VES) ?: 0.0

        if (isGuest(userId)) {
            val genId = if (transaction.id.isNotBlank()) transaction.id else UUID.randomUUID().toString()
            val newTx = transaction.copy(id = genId, userId = "local_guest", amountInBs = calculatedBs)
            _localTransactions.value = listOf(newTx) + _localTransactions.value

            // Update originating account balance
            val delta = when (transaction.type) {
                TransactionType.INCOME.name -> transaction.amount
                TransactionType.EXPENSE.name -> -transaction.amount
                TransactionType.TRANSFER.name -> -transaction.amount
                else -> 0.0
            }
            if (delta != 0.0) {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    if (acc.id == transaction.accountId) acc.copy(currentBalance = acc.currentBalance + delta) else acc
                }
            }
            if (transaction.type == TransactionType.TRANSFER.name && !transaction.targetAccountId.isNullOrBlank()) {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    if (acc.id == transaction.targetAccountId) acc.copy(currentBalance = acc.currentBalance + transaction.amount) else acc
                }
            }
            persistLocalGuestData()
            return
        }

        val docRef = firestore.collection("users").document(userId).collection("transactions").document()
        val transactionWithId = transaction.copy(
            id = docRef.id,
            userId = userId,
            amountInBs = calculatedBs
        )
        docRef.set(transactionWithId.toMap()).await()

        val accountDoc = firestore.collection("users").document(userId).collection("accounts").document(transaction.accountId)
        val delta = when (transaction.type) {
            TransactionType.INCOME.name -> transaction.amount
            TransactionType.EXPENSE.name -> -transaction.amount
            TransactionType.TRANSFER.name -> -transaction.amount
            else -> 0.0
        }
        if (delta != 0.0) {
            accountDoc.update("currentBalance", FieldValue.increment(delta)).await()
        }

        if (transaction.type == TransactionType.TRANSFER.name && !transaction.targetAccountId.isNullOrBlank()) {
            val targetDoc = firestore.collection("users").document(userId).collection("accounts").document(transaction.targetAccountId)
            targetDoc.update("currentBalance", FieldValue.increment(transaction.amount)).await()
        }
    }

    suspend fun deleteTransaction(userId: String, transaction: Transaction) {
        if (isGuest(userId)) {
            _localTransactions.value = _localTransactions.value.filter { it.id != transaction.id }
            val revertDelta = when (transaction.type) {
                TransactionType.INCOME.name -> -transaction.amount
                TransactionType.EXPENSE.name -> transaction.amount
                TransactionType.TRANSFER.name -> transaction.amount
                else -> 0.0
            }
            if (revertDelta != 0.0) {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    if (acc.id == transaction.accountId) acc.copy(currentBalance = acc.currentBalance + revertDelta) else acc
                }
            }
            if (transaction.type == TransactionType.TRANSFER.name && !transaction.targetAccountId.isNullOrBlank()) {
                _localAccounts.value = _localAccounts.value.map { acc ->
                    if (acc.id == transaction.targetAccountId) acc.copy(currentBalance = acc.currentBalance - transaction.amount) else acc
                }
            }
            persistLocalGuestData()
            return
        }

        firestore.collection("users").document(userId).collection("transactions").document(transaction.id).delete().await()
        val accountDoc = firestore.collection("users").document(userId).collection("accounts").document(transaction.accountId)
        val revertDelta = when (transaction.type) {
            TransactionType.INCOME.name -> -transaction.amount
            TransactionType.EXPENSE.name -> transaction.amount
            TransactionType.TRANSFER.name -> transaction.amount
            else -> 0.0
        }
        if (revertDelta != 0.0) {
            accountDoc.update("currentBalance", FieldValue.increment(revertDelta)).await()
        }
        if (transaction.type == TransactionType.TRANSFER.name && !transaction.targetAccountId.isNullOrBlank()) {
            val targetDoc = firestore.collection("users").document(userId).collection("accounts").document(transaction.targetAccountId)
            targetDoc.update("currentBalance", FieldValue.increment(-transaction.amount)).await()
        }
    }

    // Expense Commitments
    fun observeCommitments(userId: String): Flow<List<ExpenseCommitment>> {
        if (isGuest(userId)) {
            return _localCommitments.map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
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
                        )
                    } else null
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
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
        rates: ExchangeRates? = null
    ) {
        val currentInstallments = commitment.getProjectedInstallments().toMutableList()
        val index = currentInstallments.indexOfFirst { it.number == installmentNumber }
        if (index == -1) return

        val target = currentInstallments[index]
        if (target.isPaid == markAsPaid) return

        val updatedInstallment = target.copy(
            isPaid = markAsPaid,
            paidDate = if (markAsPaid) Timestamp.now() else null
        )
        currentInstallments[index] = updatedInstallment

        val paidCount = currentInstallments.count { it.isPaid }.toLong()
        val paidTotal = currentInstallments.filter { it.isPaid }.sumOf { it.amount }
        val newRemaining = (commitment.totalAmount - paidTotal).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.001) CommitmentStatus.PAID_OFF.name else CommitmentStatus.ACTIVE.name

        val updatedCommitment = commitment.copy(
            installments = currentInstallments.map { it.toMap() },
            paidInstallments = paidCount,
            remainingAmount = newRemaining,
            status = newStatus
        )
        updateCommitment(userId, updatedCommitment)

        if (markAsPaid && payFromAccount != null) {
            val tx = Transaction(
                userId = userId,
                accountId = payFromAccount.id,
                type = TransactionType.EXPENSE.name,
                amount = target.amount,
                currency = commitment.currency,
                category = commitment.category,
                description = "Cuota $installmentNumber/${commitment.totalInstallments}: ${commitment.title}",
                date = Timestamp.now(),
                commitmentId = commitment.id,
                isFixedExpense = commitment.isFixedExpense
            )
            addTransaction(userId, tx, rates)
        }
    }

    suspend fun recordPayment(
        userId: String,
        payment: InstallmentPayment,
        commitment: ExpenseCommitment,
        account: Account,
        rates: ExchangeRates? = null
    ) {
        require(payment.amount > 0)

        val newRemaining = (commitment.remainingAmount - payment.amount).coerceAtLeast(0.0)
        val newPaidCount = if (commitment.paymentMode == PaymentMode.INSTALLMENTS.name) {
            commitment.paidInstallments + 1L
        } else commitment.paidInstallments

        val newStatus = if (newRemaining <= 0.001) CommitmentStatus.PAID_OFF.name else commitment.status

        val currentInstallments = commitment.getProjectedInstallments().toMutableList()
        val unpaidFirst = currentInstallments.indexOfFirst { !it.isPaid }
        if (unpaidFirst != -1) {
            currentInstallments[unpaidFirst] = currentInstallments[unpaidFirst].copy(
                isPaid = true,
                paidDate = Timestamp.now()
            )
        }

        val updatedCommitment = commitment.copy(
            remainingAmount = newRemaining,
            paidInstallments = newPaidCount,
            status = newStatus,
            installments = currentInstallments.map { it.toMap() }
        )

        updateCommitment(userId, updatedCommitment)

        val paymentDesc = when (commitment.paymentMode) {
            PaymentMode.INSTALLMENTS.name -> "Cuota $newPaidCount/${commitment.totalInstallments}: ${commitment.title}"
            PaymentMode.PARTIAL.name -> "Abono a deuda: ${commitment.title}"
            else -> "Pago: ${commitment.title}"
        }

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
