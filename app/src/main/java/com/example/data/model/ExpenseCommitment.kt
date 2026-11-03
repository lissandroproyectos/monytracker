package com.example.data.model

import com.example.data.currency.AppCurrency
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar
import java.util.Date

enum class PaymentMode(val displayName: String, val description: String) {
    FULL("Pago Completo", "1 pago único en fecha prevista"),
    PARTIAL("Pagos Parciales", "Abonos flexibles según disponibilidad"),
    INSTALLMENTS("Cuotas Planificadas", "Cuotas fijas periódicas (mensual/quincenal/semanal)")
}

enum class CommitmentStatus(val displayName: String) {
    ACTIVE("Activo"),
    PAID_OFF("Completado / Pagado"),
    OVERDUE("Vencido")
}

enum class Frequency(val displayName: String, val daysInterval: Int) {
    WEEKLY("Semanal (cada 7 días)", 7),
    BIWEEKLY("Quincenal (cada 15 días)", 15),
    MONTHLY("Mensual (cada mes)", 30),
    BIMONTHLY("Bimensual (cada 2 meses)", 60),
    QUARTERLY("Trimestral (cada 3 meses)", 90),
    FOUR_MONTHLY("Cuatrimestral (cada 4 meses)", 120),
    SEMIANNUAL("Semestral (cada 6 meses)", 180),
    ANNUAL("Anual (cada año)", 365),
    CUSTOM("Personalizada (cada X días)", 30)
}

data class ProjectedInstallment(
    val number: Long = 1L,
    val dueDate: Timestamp = Timestamp.now(),
    val amount: Double = 0.0,
    val isPaid: Boolean = false,
    val paidDate: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "number" to number,
        "dueDate" to dueDate,
        "amount" to amount,
        "isPaid" to isPaid,
        "paidDate" to paidDate
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ProjectedInstallment {
            return ProjectedInstallment(
                number = (map["number"] as? Number)?.toLong() ?: 1L,
                dueDate = map["dueDate"] as? Timestamp ?: Timestamp.now(),
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                isPaid = map["isPaid"] as? Boolean ?: false,
                paidDate = map["paidDate"] as? Timestamp
            )
        }
    }
}

@IgnoreExtraProperties
data class ExpenseCommitment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val totalAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paymentMode: String = PaymentMode.INSTALLMENTS.name,
    val category: String = "General",
    val isFixedExpense: Boolean = false, // true = Gasto Fijo recurrente, false = Deuda temporal / cuotas
    val currency: String = AppCurrency.USD_BCV.name,
    val totalInstallments: Long = 1L,
    val paidInstallments: Long = 0L,
    val installmentAmount: Double = 0.0,
    val downPayment: Double = 0.0,
    val interestRate: Double = 0.0,
    val isAnnualInterest: Boolean = false,
    val customDays: Int = 30,
    val frequency: String = Frequency.MONTHLY.name,
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp? = null,
    val dueDate: Timestamp = Timestamp.now(),
    val status: String = CommitmentStatus.ACTIVE.name,
    val accountId: String? = null,
    val notes: String? = null,
    val installments: List<Map<String, Any?>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val progressPercentage: Float
        get() {
            if (totalAmount <= 0.0) return 1f
            val paid = (totalAmount - remainingAmount).coerceAtLeast(0.0)
            return (paid / totalAmount).toFloat().coerceIn(0f, 1f)
        }

    fun getProjectedInstallments(): List<ProjectedInstallment> {
        if (installments.isNotEmpty()) {
            val list = installments.map { ProjectedInstallment.fromMap(it) }
            return if (endDate != null) {
                val endLimitMs = endDate.toDate().time
                list.filter { it.dueDate.toDate().time <= endLimitMs }
            } else {
                list
            }
        }
        // Auto-generate projections if not stored
        val list = mutableListOf<ProjectedInstallment>()
        val freq = Frequency.entries.find { it.name == frequency } ?: Frequency.MONTHLY
        val startCal = Calendar.getInstance().apply { time = startDate.toDate() }
        val startDayOfMonth = startCal.get(Calendar.DAY_OF_MONTH)
        val count = totalInstallments.toInt().coerceAtLeast(1)
        val financedAmount = (totalAmount - downPayment).coerceAtLeast(0.0)
        val singleAmount = if (installmentAmount > 0) installmentAmount else (if (count > 0) financedAmount / count else financedAmount)

        for (i in 1..count) {
            val dueCal = startCal.clone() as Calendar
            when (freq) {
                Frequency.WEEKLY -> dueCal.add(Calendar.WEEK_OF_YEAR, i)
                Frequency.BIWEEKLY -> dueCal.add(Calendar.DAY_OF_YEAR, i * 15)
                Frequency.MONTHLY -> {
                    dueCal.add(Calendar.MONTH, i)
                    val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    dueCal.set(Calendar.DAY_OF_MONTH, startDayOfMonth.coerceAtMost(maxDay))
                }
                Frequency.BIMONTHLY -> {
                    dueCal.add(Calendar.MONTH, i * 2)
                    val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    dueCal.set(Calendar.DAY_OF_MONTH, startDayOfMonth.coerceAtMost(maxDay))
                }
                Frequency.QUARTERLY -> {
                    dueCal.add(Calendar.MONTH, i * 3)
                    val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    dueCal.set(Calendar.DAY_OF_MONTH, startDayOfMonth.coerceAtMost(maxDay))
                }
                Frequency.FOUR_MONTHLY -> {
                    dueCal.add(Calendar.MONTH, i * 4)
                    val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    dueCal.set(Calendar.DAY_OF_MONTH, startDayOfMonth.coerceAtMost(maxDay))
                }
                Frequency.SEMIANNUAL -> {
                    dueCal.add(Calendar.MONTH, i * 6)
                    val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    dueCal.set(Calendar.DAY_OF_MONTH, startDayOfMonth.coerceAtMost(maxDay))
                }
                Frequency.ANNUAL -> {
                    dueCal.add(Calendar.YEAR, i)
                    val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    dueCal.set(Calendar.DAY_OF_MONTH, startDayOfMonth.coerceAtMost(maxDay))
                }
                Frequency.CUSTOM -> dueCal.add(Calendar.DAY_OF_YEAR, i * (if (customDays > 0) customDays else 30))
            }
            if (endDate != null && dueCal.time.after(endDate.toDate())) {
                break
            }
            val isAlreadyPaid = i <= paidInstallments
            list.add(
                ProjectedInstallment(
                    number = i.toLong(),
                    dueDate = Timestamp(dueCal.time),
                    amount = singleAmount,
                    isPaid = isAlreadyPaid,
                    paidDate = if (isAlreadyPaid) Timestamp(dueCal.time) else null
                )
            )
        }
        return list
    }

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "title" to title,
            "totalAmount" to totalAmount,
            "remainingAmount" to remainingAmount,
            "paymentMode" to paymentMode,
            "category" to category,
            "isFixedExpense" to isFixedExpense,
            "currency" to currency,
            "totalInstallments" to totalInstallments,
            "paidInstallments" to paidInstallments,
            "installmentAmount" to installmentAmount,
            "downPayment" to downPayment,
            "interestRate" to interestRate,
            "isAnnualInterest" to isAnnualInterest,
            "customDays" to customDays,
            "frequency" to frequency,
            "startDate" to startDate,
            "endDate" to endDate,
            "dueDate" to dueDate,
            "status" to status,
            "accountId" to accountId,
            "notes" to notes,
            "installments" to installments,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return map.filterValues { it != null }.mapValues { it.value!! }
    }
}
