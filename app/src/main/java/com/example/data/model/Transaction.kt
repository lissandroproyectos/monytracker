package com.example.data.model

import com.example.data.currency.AppCurrency
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType(val displayName: String) {
    INCOME("Ingreso"),
    EXPENSE("Egreso"),
    TRANSFER("Transferencia")
}

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val targetAccountId: String? = null,
    val type: String = TransactionType.EXPENSE.name,
    val amount: Double = 0.0,
    val currency: String = AppCurrency.USD_BCV.name,
    val amountInBs: Double = 0.0,
    val isFixedExpense: Boolean = false,
    val category: String = "General",
    val description: String = "",
    val date: Timestamp = Timestamp.now(),
    val notes: String? = null,
    val commitmentId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "accountId" to accountId,
            "targetAccountId" to targetAccountId,
            "type" to type,
            "amount" to amount,
            "currency" to currency,
            "amountInBs" to amountInBs,
            "isFixedExpense" to isFixedExpense,
            "category" to category,
            "description" to description,
            "date" to date,
            "notes" to notes,
            "commitmentId" to commitmentId,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return map.filterValues { it != null }.mapValues { it.value!! }
    }
}
