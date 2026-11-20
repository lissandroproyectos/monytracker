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

    @Test
    fun multiDayDeficitSummary_capturesAllDaysAndExpensesBelowZero() {
        val day1 = dayOffset(1)
        val day3 = dayOffset(3)
        val day5 = dayOffset(5)
        val day7 = dayOffset(7)

        val events = listOf(
            PlannedEvent(
                id = "d1_ok",
                title = "Café",
                dueDate = day1,
                amount = 40.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "d1_drop",
                title = "Alquiler",
                dueDate = day1,
                amount = 100.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "d3_deep",
                title = "Luz",
                dueDate = day3,
                amount = 30.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "d5_inc",
                title = "Quincena",
                dueDate = day5,
                amount = 200.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = true,
                isPaid = false
            ),
            PlannedEvent(
                id = "d7_drop2",
                title = "Cuota Moto",
                dueDate = day7,
                amount = 180.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = false,
                isPaid = false
            )
        )

        // Starting pool = 100
        // Day 1: 100 - 40 = +60 (OK), 60 - 100 = -40 (Deficit Day 1: Alquiler)
        // Day 3: -40 - 30 = -70 (Deficit Day 2: Luz)
        // Day 5: -70 + 200 = +130 (Recovered)
        // Day 7: 130 - 180 = -50 (Deficit Day 3: Cuota Moto)
        val result = simulateGeneralPool(
            rawEvents = events,
            startingPool = 100.0,
            cushionAmount = 20.0,
            enableSavingsSweep = false,
            displayCurrency = AppCurrency.USD_BCV,
            rates = ExchangeRates()
        )

        assertEquals(3, result.deficitEvents.size)
        assertEquals(listOf("d1_drop", "d3_deep", "d7_drop2"), result.deficitEvents.map { it.id })
        assertEquals(3, result.deficitDays.size)
        assertEquals(-40.0, result.deficitDays[0].lowestBalance, 0.01)
        assertEquals(-70.0, result.deficitDays[1].lowestBalance, 0.01)
        assertEquals(-50.0, result.deficitDays[2].lowestBalance, 0.01)
    }

    @Test
    fun savingsCoverage_transfersExactCumulativeShortfallBeforeNextIncome() {
        val events = listOf(
            PlannedEvent(
                id = "e1_ok",
                title = "Luz",
                dueDate = dayOffset(2),
                amount = 30.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "e2_drop",
                title = "Alquiler",
                dueDate = dayOffset(4),
                amount = 100.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "e3_drop2",
                title = "Internet",
                dueDate = dayOffset(6),
                amount = 40.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "inc_1",
                title = "Quincena",
                dueDate = dayOffset(10),
                amount = 500.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = true,
                isPaid = false
            )
        )

        // Cuenta General starts at 50, Ahorros has 200:
        // Day 2: Luz (-30) -> 20
        // Day 4: Alquiler (-100) would drop to -80; Day 6: Internet (-40) would drop to -120.
        // Savings covers total shortfall (120) right before Alquiler:
        // 20 + 120 = 140 -> 140 - 100 = 40 -> 40 - 40 = 0 -> +500 = 500.
        val result = simulateGeneralPool(
            rawEvents = events,
            startingPool = 50.0,
            cushionAmount = 20.0,
            enableSavingsSweep = false,
            displayCurrency = AppCurrency.USD_BCV,
            rates = ExchangeRates(),
            initialSavingsAvailable = 200.0
        )

        assertFalse(result.hasDeficit)
        assertTrue(result.deficitEvents.isEmpty())
        assertEquals(1, result.savingsDraws.size)

        val draw = result.savingsDraws.first()
        assertEquals(120.0, draw.amount, 0.01)
        assertFalse(draw.isPaid)
        assertEquals(listOf("Alquiler", "Internet"), draw.coveredExpenseTitles)
        assertEquals(80.0, draw.savingsBalanceAfter!!, 0.01)

        assertEquals(80.0, result.endingSavings, 0.01)
        assertEquals(500.0, result.endingPool, 0.01)
        assertEquals(0.0, result.minPoolBalance, 0.01)
    }

    @Test
    fun insufficientSavings_keepsDeficitAndEmitsInsufficientSavingsAlert() {
        val events = listOf(
            PlannedEvent(
                id = "e1",
                title = "Alquiler",
                dueDate = dayOffset(2),
                amount = 150.0,
                currency = AppCurrency.USD_BCV,
                isFixed = true,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "inc_1",
                title = "Quincena",
                dueDate = dayOffset(8),
                amount = 400.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = true,
                isPaid = false
            )
        )

        // Pool = 30, Alquiler = 150 -> needs 120, but savings only has 50 -> cannot cover all
        val result = simulateGeneralPool(
            rawEvents = events,
            startingPool = 30.0,
            cushionAmount = 10.0,
            enableSavingsSweep = false,
            displayCurrency = AppCurrency.USD_BCV,
            rates = ExchangeRates(),
            initialSavingsAvailable = 50.0
        )

        assertTrue(result.hasDeficit)
        assertTrue(result.savingsDraws.isEmpty())
        assertEquals(1, result.deficitEvents.size)
        assertEquals(1, result.insufficientSavingsAlerts.size)
        assertEquals(120.0, result.insufficientSavingsAlerts.first().neededAmount, 0.01)
        assertEquals(50.0, result.insufficientSavingsAlerts.first().availableSavings, 0.01)
        assertEquals(50.0, result.endingSavings, 0.01)
    }

    @Test
    fun sweepBeforeIncome_growsSavingsAndCoversSubsequentDeficit() {
        val events = listOf(
            PlannedEvent(
                id = "inc_1",
                title = "Bono",
                dueDate = dayOffset(2),
                amount = 100.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = true,
                isPaid = false
            ),
            PlannedEvent(
                id = "e_big",
                title = "Cuota Vehículo",
                dueDate = dayOffset(5),
                amount = 220.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = false,
                isPaid = false
            ),
            PlannedEvent(
                id = "inc_2",
                title = "Salario",
                dueDate = dayOffset(8),
                amount = 300.0,
                currency = AppCurrency.USD_BCV,
                isFixed = false,
                isIncome = true,
                isPaid = false
            )
        )

        // Start pool = 300, cushion = 50, initialSavings = 0.
        // Before inc_1: excess 300 - 50 = 250 swept to savings (pool=50, savings=250).
        // inc_1 (+100): pool = 150.
        // Before e_big (-220): needs 70 -> covered from accumulated savings (250 >= 70)!
        // Draw +70 (savings=180, pool=220), then e_big (-220) -> pool=0.
        // inc_2 (+300): pool=300 (<= no sweep if final not set? no sweep at inc_2 because 0 <= 50).
        val result = simulateGeneralPool(
            rawEvents = events,
            startingPool = 300.0,
            cushionAmount = 50.0,
            enableSavingsSweep = true,
            displayCurrency = AppCurrency.USD_BCV,
            rates = ExchangeRates(),
            initialSavingsAvailable = 0.0
        )

        assertFalse(result.hasDeficit)
        assertEquals(1, result.savingsSweeps.size)
        assertEquals(250.0, result.savingsSweeps.first().amount, 0.01)
        assertEquals(1, result.savingsDraws.size)
        assertEquals(70.0, result.savingsDraws.first().amount, 0.01)
        assertEquals(180.0, result.endingSavings, 0.01)
        assertEquals(300.0, result.endingPool, 0.01)
    }

    @Test
    fun completedSweepAndDrawTransfers_showPaidWithoutDoubleCounting() {
        val inc1 = PlannedEvent(
            id = "inc_1",
            title = "Quincena",
            dueDate = dayOffset(5),
            amount = 400.0,
            currency = AppCurrency.USD_BCV,
            isFixed = false,
            isIncome = true,
            isPaid = false
        )
        val completedSweepTx = com.example.data.model.Transaction(
            id = "tx_sweep_done",
            type = "TRANSFER",
            amount = 150.0,
            currency = AppCurrency.USD_BCV.name,
            commitmentId = "sweep_before_inc_1",
            date = com.google.firebase.Timestamp(dayOffset(5))
        )

        // Because sweep was already executed, startingPool is already 50 (down from 200) and savings is 150.
        val result = simulateGeneralPool(
            rawEvents = listOf(inc1),
            startingPool = 50.0,
            cushionAmount = 50.0,
            enableSavingsSweep = true,
            displayCurrency = AppCurrency.USD_BCV,
            rates = ExchangeRates(),
            initialSavingsAvailable = 150.0,
            completedPlanTransfers = listOf(completedSweepTx)
        )

        assertEquals(1, result.savingsSweeps.size)
        val sweep = result.savingsSweeps.first()
        assertTrue(sweep.isPaid)
        assertEquals("tx_sweep_done", sweep.transactionId)
        assertEquals(150.0, sweep.amount, 0.01)
        assertEquals(450.0, result.endingPool, 0.01)
        assertEquals(150.0, result.endingSavings, 0.01)
    }
}
