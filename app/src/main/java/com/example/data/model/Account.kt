package com.example.data.model

import com.example.data.currency.AppCurrency
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AccountType(val displayName: String, val iconRes: String) {
    CASH("Efectivo", "cash"),
    BANK("Cuenta Bancaria", "bank"),
    CREDIT_CARD("Tarjeta de Crédito", "credit_card"),
    DIGITAL_WALLET("Billetera Digital / Cripto", "wallet"),
    SAVINGS("Cuenta de Ahorros / Colchón", "savings")
}

@IgnoreExtraProperties
data class Account(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = AccountType.CASH.name,
    val currentBalance: Double = 0.0,
    val initialBalance: Double = 0.0,
    val currency: String = AppCurrency.USD_BCV.name,
    val colorHex: String = "#10B981",
    val iconName: String = "wallet",
    val isSavings: Boolean = false,
    val hasPeriodicIncome: Boolean = false,
    val periodicIncomeAmount: Double = 0.0,
    val periodicIncomeCurrency: String = AppCurrency.USD_BCV.name,
    val periodicIncomeFrequency: String = "QUINCENAL", // QUINCENAL, MENSUAL, SEMANAL
    val periodicIncomeDayOfMonth: Int = 15,
    val periodicIncomeDescription: String = "Sueldo / Honorarios",
    val isSystemAccount: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isPlanStartAccount: Boolean
        get() = isSystemAccount || name == "Inicio del Plan" || id == "inicio_plan"

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "name" to name,
            "type" to type,
            "currentBalance" to currentBalance,
            "initialBalance" to initialBalance,
            "currency" to currency,
            "colorHex" to colorHex,
            "iconName" to iconName,
            "isSavings" to isSavings,
            "hasPeriodicIncome" to hasPeriodicIncome,
            "periodicIncomeAmount" to periodicIncomeAmount,
            "periodicIncomeCurrency" to periodicIncomeCurrency,
            "periodicIncomeFrequency" to periodicIncomeFrequency,
            "periodicIncomeDayOfMonth" to periodicIncomeDayOfMonth,
            "periodicIncomeDescription" to periodicIncomeDescription,
            "isSystemAccount" to isSystemAccount,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return map.filterValues { it != null }.mapValues { it.value!! }
    }
}
