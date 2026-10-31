package com.example.data.model

import com.example.data.currency.AppCurrency
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MarketTemplate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Compras",
    val paymentMode: String = PaymentMode.INSTALLMENTS.name,
    val frequency: String = Frequency.BIWEEKLY.name,
    val installmentsCount: Long = 3L,
    val interestRate: Double = 0.0,
    val currency: String = AppCurrency.USD_BCV.name,
    val creatorName: String = "Comunidad",
    val upvotes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "title" to title,
            "description" to description,
            "category" to category,
            "paymentMode" to paymentMode,
            "frequency" to frequency,
            "installmentsCount" to installmentsCount,
            "interestRate" to interestRate,
            "currency" to currency,
            "creatorName" to creatorName,
            "upvotes" to upvotes,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
        )
        return map.filterValues { it != null }.mapValues { it.value!! }
    }
}

