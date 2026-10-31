package com.example.data.model

import com.example.data.currency.AppCurrency
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class InstallmentPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val commitmentId: String = "",
    val accountId: String = "",
    val amount: Double = 0.0,
    val currency: String = AppCurrency.USD_BCV.name,
    val installmentNumber: Long = 0L,
    val paymentDate: Timestamp = Timestamp.now(),
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "commitmentId" to commitmentId,
            "accountId" to accountId,
            "amount" to amount,
            "currency" to currency,
            "installmentNumber" to installmentNumber,
            "paymentDate" to paymentDate,
            "notes" to notes,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return map.filterValues { it != null }.mapValues { it.value!! }
    }
}
