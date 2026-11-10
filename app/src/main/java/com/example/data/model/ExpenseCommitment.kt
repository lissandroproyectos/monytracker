package com.example.data.model

import com.example.data.currency.AppCurrency
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
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
    CASHEA("Cashea (cada 14 días)", 14),
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
        private fun parseTimestamp(value: Any?): Timestamp {
            return when (value) {
                is Timestamp -> value
                is Date -> Timestamp(value)
                is Long -> Timestamp(Date(value))
                is Double -> Timestamp(Date(value.toLong()))
                is Map<*, *> -> {
                    val sec = (value["seconds"] as? Number)?.toLong() ?: (value["_seconds"] as? Number)?.toLong() ?: 0L
                    val nano = (value["nanoseconds"] as? Number)?.toInt() ?: (value["_nanoseconds"] as? Number)?.toInt() ?: 0
                    Timestamp(sec, nano)
                }
                else -> Timestamp.now()
            }
        }

        fun fromMap(map: Map<String, Any?>): ProjectedInstallment {
            return ProjectedInstallment(
                number = (map["number"] as? Number)?.toLong() ?: 1L,
                dueDate = parseTimestamp(map["dueDate"]),
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                isPaid = map["isPaid"] as? Boolean ?: false,
                paidDate = map["paidDate"]?.let { parseTimestamp(it) }
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
    @get:PropertyName("isFixedExpense") @set:PropertyName("isFixedExpense")
    var isFixedExpense: Boolean = false, // true = Gasto Fijo recurrente, false = Deuda temporal / cuotas
    val currency: String = AppCurrency.USD_BCV.name,
    val totalInstallments: Long = 1L,
    val paidInstallments: Long = 0L,
    val installmentAmount: Double = 0.0,
    val downPayment: Double = 0.0,
    val interestRate: Double = 0.0,
    @get:PropertyName("isAnnualInterest") @set:PropertyName("isAnnualInterest")
    var isAnnualInterest: Boolean = false,
    val customDays: Int = 30,
    val frequency: String = Frequency.MONTHLY.name,
    val paymentDayOfMonth: Int = 0,
    val secondPaymentDay: Int = 0,
    val paymentDayOfWeek: Int = 0,
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

    fun getScheduleSummary(): String {
        val freq = Frequency.entries.find { it.name == frequency } ?: Frequency.MONTHLY
        val startCal = Calendar.getInstance().apply { time = startDate.toDate() }
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)
        val dayMonth = if (paymentDayOfMonth in 1..31) paymentDayOfMonth else startDay
        val secondDay = if (secondPaymentDay in 1..31) secondPaymentDay else (if (dayMonth <= 15) dayMonth + 15 else (dayMonth - 15).coerceAtLeast(1))
        val dayWeek = if (paymentDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) paymentDayOfWeek else startCal.get(Calendar.DAY_OF_WEEK)

        val dayWeekName = when (dayWeek) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> "semana"
        }

        val d1 = minOf(dayMonth, secondDay)
        val d2 = maxOf(dayMonth, secondDay)

        return when (freq) {
            Frequency.WEEKLY -> "Semanal (cada $dayWeekName)"
            Frequency.CASHEA -> "Cashea (cada 14 días)"
            Frequency.BIWEEKLY -> "Quincenal (días $d1 y $d2)"
            Frequency.MONTHLY -> "Mensual (día $dayMonth)"
            Frequency.BIMONTHLY -> "Bimensual (día $dayMonth)"
            Frequency.QUARTERLY -> "Trimestral (día $dayMonth)"
            Frequency.FOUR_MONTHLY -> "Cuatrimestral (día $dayMonth)"
            Frequency.SEMIANNUAL -> "Semestral (día $dayMonth)"
            Frequency.ANNUAL -> "Anual (día $dayMonth)"
            Frequency.CUSTOM -> "Cada $customDays días"
        }
    }

    fun getNextPaymentDateText(): String {
        val unpaid = getProjectedInstallments().firstOrNull { !it.isPaid }
        return if (unpaid != null) {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.forLanguageTag("es-ES"))
            "Próx: " + sdf.format(unpaid.dueDate.toDate())
        } else if (endDate != null) {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            "Fin: " + sdf.format(endDate.toDate())
        } else {
            "Saldado"
        }
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
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)
        val dayMonth = if (paymentDayOfMonth in 1..31) paymentDayOfMonth else startDay
        val secondDay = if (secondPaymentDay in 1..31) secondPaymentDay else (if (dayMonth <= 15) dayMonth + 15 else (dayMonth - 15).coerceAtLeast(1))
        val dayWeek = if (paymentDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) paymentDayOfWeek else startCal.get(Calendar.DAY_OF_WEEK)

        val count = totalInstallments.toInt().coerceAtLeast(1)
        val financedAmount = (totalAmount - downPayment).coerceAtLeast(0.0)
        val rawSingle = if (installmentAmount > 0) installmentAmount else (if (count > 0) financedAmount / count else financedAmount)
        val singleAmount = kotlin.math.round(rawSingle * 100.0) / 100.0

        when (freq) {
            Frequency.WEEKLY -> {
                val cal = startCal.clone() as Calendar
                val curDow = cal.get(Calendar.DAY_OF_WEEK)
                val diff = (dayWeek - curDow + 7) % 7
                val initialOffset = if (diff == 0) 7 else diff
                cal.add(Calendar.DAY_OF_YEAR, initialOffset)
                for (i in 1..count) {
                    val dueCal = cal.clone() as Calendar
                    dueCal.add(Calendar.WEEK_OF_YEAR, i - 1)
                    if (endDate != null && dueCal.time.after(endDate.toDate())) break
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
            }
            Frequency.CASHEA -> {
                for (i in 1..count) {
                    val dueCal = startCal.clone() as Calendar
                    dueCal.add(Calendar.DAY_OF_YEAR, i * 14)
                    if (endDate != null && dueCal.time.after(endDate.toDate())) break
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
            }
            Frequency.BIWEEKLY -> {
                val d1 = minOf(dayMonth, secondDay)
                val d2 = maxOf(dayMonth, secondDay)
                val cal = startCal.clone() as Calendar
                var curMonth = cal.get(Calendar.MONTH)
                var curYear = cal.get(Calendar.YEAR)
                val curDay = cal.get(Calendar.DAY_OF_MONTH)
                var useSecond: Boolean
                if (curDay < d1) {
                    useSecond = false
                } else if (curDay < d2) {
                    useSecond = true
                } else {
                    useSecond = false
                    curMonth++
                    if (curMonth > 11) { curMonth = 0; curYear++ }
                }

                for (i in 1..count) {
                    val dueCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, curYear)
                        set(Calendar.MONTH, curMonth)
                        val targetD = if (useSecond) d2 else d1
                        val maxD = getActualMaximum(Calendar.DAY_OF_MONTH)
                        set(Calendar.DAY_OF_MONTH, targetD.coerceAtMost(maxD))
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    if (useSecond) {
                        useSecond = false
                        curMonth++
                        if (curMonth > 11) { curMonth = 0; curYear++ }
                    } else {
                        useSecond = true
                    }
                    if (endDate != null && dueCal.time.after(endDate.toDate())) break
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
            }
            Frequency.MONTHLY -> {
                val cal = startCal.clone() as Calendar
                var curMonth = cal.get(Calendar.MONTH)
                var curYear = cal.get(Calendar.YEAR)
                val curDay = cal.get(Calendar.DAY_OF_MONTH)
                if (curDay >= dayMonth) {
                    curMonth++
                    if (curMonth > 11) { curMonth = 0; curYear++ }
                }
                for (i in 1..count) {
                    val dueCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, curYear)
                        set(Calendar.MONTH, curMonth)
                        val maxD = getActualMaximum(Calendar.DAY_OF_MONTH)
                        set(Calendar.DAY_OF_MONTH, dayMonth.coerceAtMost(maxD))
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    curMonth++
                    if (curMonth > 11) { curMonth = 0; curYear++ }
                    if (endDate != null && dueCal.time.after(endDate.toDate())) break
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
            }
            else -> {
                for (i in 1..count) {
                    val dueCal = startCal.clone() as Calendar
                    val offset = i
                    when (freq) {
                        Frequency.BIMONTHLY -> {
                            dueCal.add(Calendar.MONTH, offset * 2)
                            val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            dueCal.set(Calendar.DAY_OF_MONTH, dayMonth.coerceAtMost(maxDay))
                        }
                        Frequency.QUARTERLY -> {
                            dueCal.add(Calendar.MONTH, offset * 3)
                            val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            dueCal.set(Calendar.DAY_OF_MONTH, dayMonth.coerceAtMost(maxDay))
                        }
                        Frequency.FOUR_MONTHLY -> {
                            dueCal.add(Calendar.MONTH, offset * 4)
                            val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            dueCal.set(Calendar.DAY_OF_MONTH, dayMonth.coerceAtMost(maxDay))
                        }
                        Frequency.SEMIANNUAL -> {
                            dueCal.add(Calendar.MONTH, offset * 6)
                            val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            dueCal.set(Calendar.DAY_OF_MONTH, dayMonth.coerceAtMost(maxDay))
                        }
                        Frequency.ANNUAL -> {
                            dueCal.add(Calendar.YEAR, offset)
                            val maxDay = dueCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            dueCal.set(Calendar.DAY_OF_MONTH, dayMonth.coerceAtMost(maxDay))
                        }
                        Frequency.CUSTOM -> dueCal.add(Calendar.DAY_OF_YEAR, offset * (if (customDays > 0) customDays else 30))
                        else -> dueCal.add(Calendar.MONTH, offset)
                    }
                    if (endDate != null && dueCal.time.after(endDate.toDate())) break
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
            }
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
            "fixedExpense" to isFixedExpense,
            "currency" to currency,
            "totalInstallments" to totalInstallments,
            "paidInstallments" to paidInstallments,
            "installmentAmount" to installmentAmount,
            "downPayment" to downPayment,
            "interestRate" to interestRate,
            "isAnnualInterest" to isAnnualInterest,
            "annualInterest" to isAnnualInterest,
            "customDays" to customDays,
            "frequency" to frequency,
            "paymentDayOfMonth" to paymentDayOfMonth,
            "secondPaymentDay" to secondPaymentDay,
            "paymentDayOfWeek" to paymentDayOfWeek,
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
