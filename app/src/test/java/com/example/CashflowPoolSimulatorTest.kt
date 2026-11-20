package com.example

import com.example.data.currency.AppCurrency
import com.example.data.currency.ExchangeRates
import com.example.ui.screens.PlannedEvent
import com.example.ui.screens.simulateGeneralPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class CashflowPoolSimulatorTest {

    private fun dayOffset(days: Int): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 20)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }.time
    }

    @Test
    fun sequentialSubtraction_andExactDeficitDetection() {
        val events = listOf(
            PlannedEvent(
                id = "e1",
                title = "Pago Internet",
                dueDate = dayOffset(1),
                amount = 50.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "e2",
                title = "Mercado",
                dueDate = dayOffset(2),
                amount = 100.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "e3",
                title = "Cuota Tarjeta",
                dueDate = dayOffset(3),
                amount = 80.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = false,
                isPaid = false
            )
        )

        val result = simulateGeneralPool(
            rawEvents = events,
            startingPool = 200.0,
            cushionAmount = 50.0,
            enableSavingsSweep = false,
            displayCurrency = AppCurrency.USD_BCV,
            rates = ExchangeRates()
        )

        assertEquals(200.0, result.startingPool, 0.01)
        assertEquals(3, result.events.size)

        // 200 - 50 = 150
        assertEquals(200.0, result.events[0].runningAvailableBefore!!, 0.01)
        assertEquals(150.0, result.events[0].availableBalanceAfter, 0.01)

        // 150 - 100 = 50
        assertEquals(150.0, result.events[1].runningAvailableBefore!!, 0.01)
        assertEquals(50.0, result.events[1].availableBalanceAfter, 0.01)

        // 50 - 80 = -30 (deficit!)
        assertEquals(50.0, result.events[2].runningAvailableBefore!!, 0.01)
        assertEquals(-30.0, result.events[2].availableBalanceAfter, 0.01)

        assertEquals(-30.0, result.endingPool, 0.01)
        assertEquals(-30.0, result.minPoolBalance, 0.01)
        assertTrue(result.hasDeficit)
        assertNotNull(result.firstDeficitEvent)
        assertEquals("e3", result.firstDeficitEvent?.id)
        assertEquals(230.0, result.totalPendingExpenses, 0.01)
    }

    @Test
    fun paidEvents_doNotDoubleDeductOrDoubleAddToPool() {
        val events = listOf(
            PlannedEvent(
                id = "paid_exp",
                title = "Pago ya realizado",
                dueDate = dayOffset(0),
                amount = 75.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = true
            ),
            PlannedEvent(
                id = "paid_inc",
                title = "Quincena ya cobrada",
                dueDate = dayOffset(0),
                amount = 300.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = true,
                isPaid = true
            ),
            PlannedEvent(
                id = "pending_exp",
                title = "Luz",
                dueDate = dayOffset(2),
                amount = 50.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = false
            )
        )

        val result = simulateGeneralPool(
            rawEvents = events,
            startingPool = 200.0,
            cushionAmount = 100.0,
            enableSavingsSweep = false,
            displayCurrency = AppCurrency.USD_BCV,
            rates = ExchangeRates()
        )

        // Paid events remain 200 -> 200; pending expense deducts 50 -> 150
        assertEquals(200.0, result.events[0].availableBalanceAfter, 0.01)
        assertEquals(200.0, result.events[1].availableBalanceAfter, 0.01)
        assertEquals(150.0, result.events[2].availableBalanceAfter, 0.01)
        assertEquals(150.0, result.endingPool, 0.01)
        assertEquals(50.0, result.totalPendingExpenses, 0.01)
        assertEquals(0.0, result.totalPendingIncomes, 0.01)
        assertFalse(result.hasDeficit)
        assertNull(result.firstDeficitEvent)
    }

    @Test
    fun subsequentIncomeRecovery_stillFlagsFirstDeficitAndTracksMinimum() {
        val sameDay = dayOffset(2)
        val events = listOf(
            PlannedEvent(
                id = "e1",
                title = "Pago Grande",
                dueDate = sameDay,
                amount = 230.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "i1",
                title = "Salario",
                dueDate = sameDay,
                amount = 300.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = true,
                isPaid = false
            )
        )

        val result = simulateGeneralPool(
            rawEvents = events,
            startingPool = 200.0,
            cushionAmount = 50.0,
            enableSavingsSweep = false,
            displayCurrency = AppCurrency.USD_BCV,
            rates = ExchangeRates()
        )

        // Same day expense processed first: 200 - 230 = -30, then +300 = 270
        assertTrue(result.hasDeficit)
        assertEquals("e1", result.firstDeficitEvent?.id)
        assertEquals(-30.0, result.minPoolBalance, 0.01)
        assertEquals(270.0, result.endingPool, 0.01)
    }
}
