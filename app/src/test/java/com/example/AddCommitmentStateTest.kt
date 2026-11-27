package com.example

import com.example.data.model.ExpenseCommitment
import com.example.data.model.Frequency
import com.example.data.model.PaymentMode
import com.example.data.model.ProjectedInstallment
import com.example.ui.components.AddCommitmentState
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class AddCommitmentStateTest {

    private fun dateAt(year: Int, month: Int, day: Int): Timestamp {
        val cal = Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(cal.time)
    }

    @Test
    fun changingBaseAmountOnEditedDebt_updatesPlannedInstallmentsAndPreservesDates() {
        val d1 = dateAt(2026, Calendar.NOVEMBER, 10)
        val d2 = dateAt(2026, Calendar.NOVEMBER, 25)
        val d3 = dateAt(2026, Calendar.DECEMBER, 10)
        val original = ExpenseCommitment(
            id = "c1",
            title = "Compra Laptop",
            totalAmount = 300.0,
            remainingAmount = 300.0,
            paymentMode = PaymentMode.INSTALLMENTS.name,
            isFixedExpense = false,
            totalInstallments = 3L,
            paidInstallments = 0L,
            installmentAmount = 100.0,
            frequency = Frequency.BIWEEKLY.name,
            startDate = dateAt(2026, Calendar.NOVEMBER, 1),
            installments = listOf(
                ProjectedInstallment(number = 1L, dueDate = d1, amount = 100.0, isPaid = false).toMap(),
                ProjectedInstallment(number = 2L, dueDate = d2, amount = 100.0, isPaid = false).toMap(),
                ProjectedInstallment(number = 3L, dueDate = d3, amount = 100.0, isPaid = false).toMap()
            )
        )

        val state = AddCommitmentState(
            commitmentToEdit = original,
            prefilledTemplate = null,
            initialIsFixed = false,
            defaultAccountIdInitial = "acc1"
        )

        // Change Monto Base from 300 -> 600
        state.totalAmountText = "600"
        state.syncCustomScheduleAmounts()

        val schedule = state.computeActiveSchedule()
        assertEquals(3, schedule.size)
        assertEquals(200.0, schedule[0].amount, 0.01)
        assertEquals(200.0, schedule[1].amount, 0.01)
        assertEquals(200.0, schedule[2].amount, 0.01)
        // Custom dates preserved
        assertEquals(d1.toDate().time, schedule[0].dueDate.toDate().time)
        assertEquals(d2.toDate().time, schedule[1].dueDate.toDate().time)
        assertEquals(d3.toDate().time, schedule[2].dueDate.toDate().time)

        val saved = state.buildCommitmentToSave()
        assertNotNull(saved)
        assertEquals(600.0, saved!!.totalAmount, 0.01)
        assertEquals(600.0, saved.remainingAmount, 0.01)
        assertEquals(200.0, saved.installmentAmount, 0.01)
        val savedProj = saved.getProjectedInstallments()
        assertEquals(listOf(200.0, 200.0, 200.0), savedProj.map { it.amount })
    }

    @Test
    fun downPaymentIntegrated_recalculatesFinancedInstallmentsAndHandlesInterestEditRoundtrip() {
        val original = ExpenseCommitment(
            id = "c2",
            title = "Teléfono",
            // Base 220, downPayment 20 => financedBase 200, 10% monthly over 2 months (20%) => 240 financed + 20 initial = 260
            totalAmount = 260.0,
            remainingAmount = 240.0,
            paymentMode = PaymentMode.INSTALLMENTS.name,
            isFixedExpense = false,
            totalInstallments = 2L,
            installmentAmount = 120.0,
            downPayment = 20.0,
            interestRate = 10.0,
            isAnnualInterest = false,
            frequency = Frequency.MONTHLY.name,
            startDate = dateAt(2026, Calendar.NOVEMBER, 1),
            installments = listOf(
                ProjectedInstallment(number = 1L, dueDate = dateAt(2026, Calendar.DECEMBER, 1), amount = 120.0).toMap(),
                ProjectedInstallment(number = 2L, dueDate = dateAt(2027, Calendar.JANUARY, 1), amount = 120.0).toMap()
            )
        )

        val state = AddCommitmentState(
            commitmentToEdit = original,
            prefilledTemplate = null,
            initialIsFixed = false,
            defaultAccountIdInitial = "acc1"
        )

        // Recovered Monto Base without double-counting interest
        assertEquals(220.0, state.totalAmount, 0.01)
        assertTrue(state.hasDownPayment)
        assertEquals(120.0, state.calculatedInstallmentAmount, 0.01)

        // Change Cuota Inicial to 120 => financedBase 100, +20% = 120 => 60 per installment
        state.downPaymentText = "120"
        state.syncCustomScheduleAmounts()
        assertEquals(60.0, state.calculatedInstallmentAmount, 0.01)
        assertEquals(listOf(60.0, 60.0), state.computeActiveSchedule().map { it.amount })

        // Clearing Cuota Inicial sets hasDownPayment=false and financedBase=220
        state.downPaymentText = ""
        state.syncCustomScheduleAmounts()
        assertFalse(state.hasDownPayment)
        assertEquals(132.0, state.calculatedInstallmentAmount, 0.01)
        assertEquals(listOf(132.0, 132.0), state.computeActiveSchedule().map { it.amount })
    }

    @Test
    fun changingBaseAmountOnEditedFixedExpense_updatesUnpaidInstallmentsOnly() {
        val d1 = dateAt(2026, Calendar.NOVEMBER, 5)
        val d2 = dateAt(2026, Calendar.DECEMBER, 5)
        val d3 = dateAt(2027, Calendar.JANUARY, 5)
        val original = ExpenseCommitment(
            id = "f1",
            title = "Internet",
            totalAmount = 40.0,
            remainingAmount = 80.0,
            paymentMode = PaymentMode.INSTALLMENTS.name,
            isFixedExpense = true,
            totalInstallments = 3L,
            paidInstallments = 1L,
            installmentAmount = 40.0,
            frequency = Frequency.MONTHLY.name,
            paymentDayOfMonth = 5,
            startDate = d1,
            installments = listOf(
                ProjectedInstallment(number = 1L, dueDate = d1, amount = 40.0, isPaid = true, paidDate = d1).toMap(),
                ProjectedInstallment(number = 2L, dueDate = d2, amount = 40.0, isPaid = false).toMap(),
                ProjectedInstallment(number = 3L, dueDate = d3, amount = 40.0, isPaid = false).toMap()
            )
        )

        val state = AddCommitmentState(
            commitmentToEdit = original,
            prefilledTemplate = null,
            initialIsFixed = true,
            defaultAccountIdInitial = "acc1"
        )
        state.fixedCyclesCountText = "3"

        // Update Monto recurrente from 40 -> 55
        state.totalAmountText = "55"
        val schedule = state.computeActiveFixedSchedule()
        assertEquals(3, schedule.size)
        // Historically paid installment retains 40.0, unpaid cycles update to 55.0
        assertEquals(40.0, schedule[0].amount, 0.01)
        assertTrue(schedule[0].isPaid)
        assertEquals(55.0, schedule[1].amount, 0.01)
        assertEquals(55.0, schedule[2].amount, 0.01)

        val saved = state.buildCommitmentToSave()
        assertNotNull(saved)
        assertEquals(55.0, saved!!.totalAmount, 0.01)
        assertEquals(110.0, saved.remainingAmount, 0.01)
    }
}
