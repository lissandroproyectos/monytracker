package com.example.data.model

import com.example.data.currency.AppCurrency
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserSettings(
    val displayCurrency: String = AppCurrency.USD_BCV.name,
    val paymentCurrency: String = AppCurrency.USD_BCV.name,
    val safetyCushionAmount: Double = 150.0,
    val savingsBalance: Double = 0.0,
    val customExpenseCategories: List<String> = emptyList(),
    val customIncomeCategories: List<String> = emptyList(),
    val githubRepo: String = "lissandroproyectos/monytracker"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "displayCurrency" to displayCurrency,
            "paymentCurrency" to paymentCurrency,
            "safetyCushionAmount" to safetyCushionAmount,
            "savingsBalance" to savingsBalance,
            "customExpenseCategories" to customExpenseCategories,
            "customIncomeCategories" to customIncomeCategories,
            "githubRepo" to githubRepo
        )
    }
}
