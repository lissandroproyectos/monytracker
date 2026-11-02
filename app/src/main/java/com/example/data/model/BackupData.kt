package com.example.data.model

import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val accountsCount: Int = 0,
    val commitmentsCount: Int = 0,
    val transactionsCount: Int = 0,
    val note: String = ""
)

object BackupManager {

    fun exportToJson(
        accounts: List<Account>,
        commitments: List<ExpenseCommitment>,
        transactions: List<Transaction>,
        settings: UserSettings?
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        root.put("app", "Finanzas")

        // Settings
        settings?.let {
            val sObj = JSONObject()
            sObj.put("displayCurrency", it.displayCurrency)
            sObj.put("paymentCurrency", it.paymentCurrency)
            sObj.put("safetyCushionAmount", it.safetyCushionAmount)
            sObj.put("savingsBalance", it.savingsBalance)
            root.put("settings", sObj)
        }

        // Accounts
        val accArr = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("type", acc.type)
            obj.put("currentBalance", acc.currentBalance)
            obj.put("initialBalance", acc.initialBalance)
            obj.put("currency", acc.currency)
            obj.put("colorHex", acc.colorHex)
            obj.put("iconName", acc.iconName)
            obj.put("isSavings", acc.isSavings)
            obj.put("hasPeriodicIncome", acc.hasPeriodicIncome)
            obj.put("periodicIncomeAmount", acc.periodicIncomeAmount)
            obj.put("periodicIncomeCurrency", acc.periodicIncomeCurrency)
            obj.put("periodicIncomeFrequency", acc.periodicIncomeFrequency)
            obj.put("periodicIncomeDayOfMonth", acc.periodicIncomeDayOfMonth)
            obj.put("periodicIncomeDescription", acc.periodicIncomeDescription)
            accArr.put(obj)
        }
        root.put("accounts", accArr)

        // Commitments
        val comArr = JSONArray()
        commitments.forEach { com ->
            val obj = JSONObject()
            obj.put("id", com.id)
            obj.put("title", com.title)
            obj.put("totalAmount", com.totalAmount)
            obj.put("remainingAmount", com.remainingAmount)
            obj.put("currency", com.currency)
            obj.put("paymentMode", com.paymentMode)
            obj.put("category", com.category)
            obj.put("isFixedExpense", com.isFixedExpense)
            obj.put("status", com.status)
            obj.put("frequency", com.frequency)
            obj.put("totalInstallments", com.totalInstallments)
            obj.put("paidInstallments", com.paidInstallments)
            obj.put("installmentAmount", com.installmentAmount)
            obj.put("interestRate", com.interestRate)
            obj.put("isAnnualInterest", com.isAnnualInterest)
            obj.put("customDays", com.customDays)
            obj.put("notes", com.notes ?: "")
            comArr.put(obj)
        }
        root.put("commitments", comArr)

        // Transactions
        val txArr = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("accountId", tx.accountId)
            obj.put("type", tx.type)
            obj.put("amount", tx.amount)
            obj.put("currency", tx.currency)
            obj.put("category", tx.category)
            obj.put("description", tx.description)
            obj.put("isFixedExpense", tx.isFixedExpense)
            obj.put("targetAccountId", tx.targetAccountId ?: "")
            obj.put("commitmentId", tx.commitmentId ?: "")
            obj.put("timestampSeconds", tx.date?.seconds ?: (System.currentTimeMillis() / 1000L))
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        return root.toString(2)
    }

    data class ParsedBackup(
        val accounts: List<Account>,
        val commitments: List<ExpenseCommitment>,
        val transactions: List<Transaction>,
        val settings: UserSettings?
    )

    fun parseJson(jsonString: String, userId: String): ParsedBackup {
        val root = JSONObject(jsonString)
        
        var settings: UserSettings? = null
        if (root.has("settings")) {
            val sObj = root.getJSONObject("settings")
            settings = UserSettings(
                displayCurrency = sObj.optString("displayCurrency", "USD_BCV"),
                paymentCurrency = sObj.optString("paymentCurrency", "USD_BCV"),
                safetyCushionAmount = sObj.optDouble("safetyCushionAmount", 150.0),
                savingsBalance = sObj.optDouble("savingsBalance", 0.0)
            )
        }

        val accounts = mutableListOf<Account>()
        if (root.has("accounts")) {
            val arr = root.getJSONArray("accounts")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                accounts.add(
                    Account(
                        id = o.optString("id", ""),
                        userId = userId,
                        name = o.optString("name", "Cuenta"),
                        type = o.optString("type", AccountType.CASH.name),
                        currentBalance = o.optDouble("currentBalance", 0.0),
                        initialBalance = o.optDouble("initialBalance", 0.0),
                        currency = o.optString("currency", "USD_BCV"),
                        colorHex = o.optString("colorHex", "#10B981"),
                        iconName = o.optString("iconName", "wallet"),
                        isSavings = o.optBoolean("isSavings", false),
                        hasPeriodicIncome = o.optBoolean("hasPeriodicIncome", false),
                        periodicIncomeAmount = o.optDouble("periodicIncomeAmount", 0.0),
                        periodicIncomeCurrency = o.optString("periodicIncomeCurrency", "USD_BCV"),
                        periodicIncomeFrequency = o.optString("periodicIncomeFrequency", "QUINCENAL"),
                        periodicIncomeDayOfMonth = o.optInt("periodicIncomeDayOfMonth", 15),
                        periodicIncomeDescription = o.optString("periodicIncomeDescription", "Sueldo"),
                        createdAt = Timestamp.now()
                    )
                )
            }
        }

        val commitments = mutableListOf<ExpenseCommitment>()
        if (root.has("commitments")) {
            val arr = root.getJSONArray("commitments")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                commitments.add(
                    ExpenseCommitment(
                        id = o.optString("id", ""),
                        userId = userId,
                        title = o.optString("title", "Compromiso"),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        remainingAmount = o.optDouble("remainingAmount", 0.0),
                        currency = o.optString("currency", "USD_BCV"),
                        paymentMode = o.optString("paymentMode", PaymentMode.FULL.name),
                        category = o.optString("category", "General"),
                        isFixedExpense = o.optBoolean("isFixedExpense", false),
                        status = o.optString("status", CommitmentStatus.ACTIVE.name),
                        frequency = o.optString("frequency", Frequency.MONTHLY.name),
                        totalInstallments = o.optLong("totalInstallments", 1L),
                        paidInstallments = o.optLong("paidInstallments", 0L),
                        installmentAmount = o.optDouble("installmentAmount", 0.0),
                        interestRate = o.optDouble("interestRate", 0.0),
                        isAnnualInterest = o.optBoolean("isAnnualInterest", false),
                        customDays = o.optInt("customDays", 30),
                        notes = o.optString("notes").ifBlank { null },
                        createdAt = Timestamp.now()
                    )
                )
            }
        }

        val transactions = mutableListOf<Transaction>()
        if (root.has("transactions")) {
            val arr = root.getJSONArray("transactions")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val sec = o.optLong("timestampSeconds", System.currentTimeMillis() / 1000L)
                transactions.add(
                    Transaction(
                        id = o.optString("id", ""),
                        userId = userId,
                        accountId = o.optString("accountId", ""),
                        type = o.optString("type", TransactionType.EXPENSE.name),
                        amount = o.optDouble("amount", 0.0),
                        currency = o.optString("currency", "USD_BCV"),
                        category = o.optString("category", "General"),
                        description = o.optString("description", "Movimiento"),
                        isFixedExpense = o.optBoolean("isFixedExpense", false),
                        targetAccountId = o.optString("targetAccountId").ifBlank { null },
                        commitmentId = o.optString("commitmentId").ifBlank { null },
                        date = Timestamp(sec, 0),
                        createdAt = Timestamp.now()
                    )
                )
            }
        }

        return ParsedBackup(accounts, commitments, transactions, settings)
    }
}
