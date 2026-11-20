package com.example.ui.screens

import com.example.data.currency.AppCurrency
import com.example.data.currency.ExchangeRates
import java.util.Date

data class PoolSimulationResult(
    val events: List<PlannedEvent>,
    val startingPool: Double,
    val endingPool: Double,
    val minPoolBalance: Double,
    val minPoolEvent: PlannedEvent?,
    val firstDeficitEvent: PlannedEvent?,
    val totalPendingIncomes: Double,
    val totalPendingExpenses: Double,
    val totalPeriodIncomes: Double,
    val totalPeriodExpenses: Double
) {
    val hasDeficit: Boolean get() = firstDeficitEvent != null || minPoolBalance < -0.001
}

private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

/**
 * Simulates the general available pool ("Pote General Disponible") chronologically across all
 * planned events.
 *
 * - `startingPool` is the current live sum of all active operating (non-savings, non-closed) accounts.
 * - Events with `isPaid == true` are already reflected in `startingPool`, so they do NOT add/subtract again.
 * - Pending (`!isPaid`) expenses deduct from the running pool; pending incomes add to it.
 * - Detects the exact first movement where the pool drops below zero (`firstDeficitEvent`).
 */
fun simulateGeneralPool(
    rawEvents: List<PlannedEvent>,
    startingPool: Double,
    cushionAmount: Double,
    enableSavingsSweep: Boolean,
    endDateForFinalSweep: Date? = null,
    displayCurrency: AppCurrency,
    rates: ExchangeRates
): PoolSimulationResult {
    val results = mutableListOf<PlannedEvent>()
    var runningAvailable = round2(startingPool)
    var minBalance = runningAvailable
    var minEvent: PlannedEvent? = null
    var firstDeficit: PlannedEvent? = null

    var pendingIncomes = 0.0
    var pendingExpenses = 0.0
    var periodIncomes = 0.0
    var periodExpenses = 0.0

    val sorted = rawEvents.sortedWith(
        compareBy<PlannedEvent> { it.dueDate.time }
            .thenBy { if (it.isIncome) 1 else 0 } // Process same-timestamp expenses before income to catch intraday shortfalls
    )

    sorted.forEach { event ->
        val amountInDisp = round2(rates.convert(event.amount, event.currency, displayCurrency))
        if (event.isIncome) {
            periodIncomes = round2(periodIncomes + amountInDisp)
        } else {
            periodExpenses = round2(periodExpenses + amountInDisp)
        }

        if (event.isPaid) {
            // Already paid/collected: already reflected in live account balances (startingPool)
            results.add(
                event.copy(
                    runningAvailableBefore = runningAvailable,
                    availableBalanceAfter = runningAvailable
                )
            )
        } else if (event.isIncome) {
            pendingIncomes = round2(pendingIncomes + amountInDisp)
            var excessSaved = 0.0
            if (enableSavingsSweep && runningAvailable > cushionAmount) {
                val excess = round2((runningAvailable - cushionAmount).coerceAtLeast(0.0))
                if (excess >= 1.0) {
                    val beforeSweep = runningAvailable
                    runningAvailable = round2(runningAvailable - excess)
                    excessSaved = excess
                    results.add(
                        PlannedEvent(
                            id = "sweep_before_${event.id}",
                            title = "Cuota de Ahorro Sugerida (Antes de ${event.title})",
                            dueDate = event.dueDate,
                            amount = excess,
                            currency = displayCurrency,
                            isFixed = false,
                            isIncome = false,
                            isSavingsSweep = true,
                            isPaid = false,
                            installmentInfo = "Excedente a resguardar",
                            runningAvailableBefore = beforeSweep,
                            availableBalanceAfter = runningAvailable,
                            sweepExcessAmount = excess
                        )
                    )
                }
            }

            val prevRunning = runningAvailable
            runningAvailable = round2(runningAvailable + amountInDisp)
            results.add(
                event.copy(
                    runningAvailableBefore = prevRunning,
                    availableBalanceAfter = runningAvailable,
                    surplusBeforeIncome = excessSaved
                )
            )
        } else {
            pendingExpenses = round2(pendingExpenses + amountInDisp)
            val prevRunning = runningAvailable
            runningAvailable = round2(runningAvailable - amountInDisp)
            val updatedEvent = event.copy(
                runningAvailableBefore = prevRunning,
                availableBalanceAfter = runningAvailable
            )
            results.add(updatedEvent)

            if (runningAvailable < minBalance) {
                minBalance = runningAvailable
                minEvent = updatedEvent
            }
            if (runningAvailable < -0.001 && firstDeficit == null) {
                firstDeficit = updatedEvent
            }
        }
    }

    if (endDateForFinalSweep != null && enableSavingsSweep && runningAvailable > cushionAmount) {
        val finalExcess = round2((runningAvailable - cushionAmount).coerceAtLeast(0.0))
        if (finalExcess >= 1.0) {
            val beforeFinalSweep = runningAvailable
            runningAvailable = round2(runningAvailable - finalExcess)
            results.add(
                PlannedEvent(
                    id = "sweep_final_period",
                    title = "Cuota de Ahorro Final (Remanente del Plan)",
                    dueDate = endDateForFinalSweep,
                    amount = finalExcess,
                    currency = displayCurrency,
                    isFixed = false,
                    isIncome = false,
                    isSavingsSweep = true,
                    isPaid = false,
                    installmentInfo = "Excedente final a Ahorros",
                    runningAvailableBefore = beforeFinalSweep,
                    availableBalanceAfter = runningAvailable,
                    sweepExcessAmount = finalExcess
                )
            )
        }
    }

    return PoolSimulationResult(
        events = results,
        startingPool = round2(startingPool),
        endingPool = runningAvailable,
        minPoolBalance = minBalance,
        minPoolEvent = minEvent,
        firstDeficitEvent = firstDeficit,
        totalPendingIncomes = pendingIncomes,
        totalPendingExpenses = pendingExpenses,
        totalPeriodIncomes = periodIncomes,
        totalPeriodExpenses = periodExpenses
    )
}
