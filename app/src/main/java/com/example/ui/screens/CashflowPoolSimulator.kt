package com.example.ui.screens

import com.example.data.currency.AppCurrency
import com.example.data.currency.ExchangeRates
import com.example.data.model.Transaction
import java.util.Calendar
import java.util.Date

data class DeficitDaySummary(
    val date: Date,
    val events: List<PlannedEvent>,
    val lowestBalance: Double,
    val totalDeficitExpensesInDisplay: Double
)

data class InsufficientSavingsAlert(
    val date: Date,
    val neededAmount: Double,
    val availableSavings: Double,
    val uncoveredExpenseTitles: List<String>
)

data class PoolSimulationResult(
    val events: List<PlannedEvent>,
    val startingPool: Double,
    val endingPool: Double,
    val startingSavings: Double = 0.0,
    val endingSavings: Double = 0.0,
    val minPoolBalance: Double,
    val minPoolEvent: PlannedEvent?,
    val firstDeficitEvent: PlannedEvent?,
    val deficitEvents: List<PlannedEvent> = emptyList(),
    val deficitDays: List<DeficitDaySummary> = emptyList(),
    val savingsSweeps: List<PlannedEvent> = emptyList(),
    val savingsDraws: List<PlannedEvent> = emptyList(),
    val insufficientSavingsAlerts: List<InsufficientSavingsAlert> = emptyList(),
    val totalPendingIncomes: Double,
    val totalPendingExpenses: Double,
    val totalPeriodIncomes: Double,
    val totalPeriodExpenses: Double
) {
    val hasDeficit: Boolean get() = firstDeficitEvent != null || minPoolBalance < -0.001
}

private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

/**
 * Simulates the "Cuenta General Disponible" chronologically across all planned events.
 *
 * - `startingPool` is the current live sum of all active operating (non-savings, non-closed) accounts.
 * - `initialSavingsAvailable` is the current live sum of all active savings accounts.
 * - Events with `isPaid == true` are already reflected in live balances, so they do NOT add/subtract again.
 * - Right before each pending income, if `enableSavingsSweep` is true and running balance exceeds
 *   `cushionAmount`, suggests moving the surplus into savings (`isSavingsSweep = true`).
 * - Whenever pending expenses before the next pending income drop the general balance below 0,
 *   checks if `runningSavings` can cover ALL expenses that require savings before that next income.
 *   If covered, calculates their total shortfall and emits a `SavingsDraw` event (`isSavingsDraw = true`)
 *   representing an outflow from Savings and an inflow to Cuenta General.
 * - Any remaining uncovered shortfall is reported in `deficitEvents` and `deficitDays`.
 */
fun simulateGeneralPool(
    rawEvents: List<PlannedEvent>,
    startingPool: Double,
    cushionAmount: Double,
    enableSavingsSweep: Boolean,
    endDateForFinalSweep: Date? = null,
    displayCurrency: AppCurrency,
    rates: ExchangeRates,
    initialSavingsAvailable: Double = 0.0,
    completedPlanTransfers: List<Transaction> = emptyList()
): PoolSimulationResult {
    val results = mutableListOf<PlannedEvent>()
    var runningAvailable = round2(startingPool)
    var runningSavings = round2(initialSavingsAvailable.coerceAtLeast(0.0))
    var minBalance = runningAvailable
    var minEvent: PlannedEvent? = null
    val allDeficitEvents = mutableListOf<PlannedEvent>()
    val allSavingsSweeps = mutableListOf<PlannedEvent>()
    val allSavingsDraws = mutableListOf<PlannedEvent>()
    val allInsufficientAlerts = mutableListOf<InsufficientSavingsAlert>()

    var pendingIncomes = 0.0
    var pendingExpenses = 0.0
    var periodIncomes = 0.0
    var periodExpenses = 0.0

    val completedByPlanId = completedPlanTransfers
        .filter { !it.commitmentId.isNullOrBlank() }
        .associateBy { it.commitmentId!! }

    val sorted = rawEvents.sortedWith(
        compareBy<PlannedEvent> { it.dueDate.time }
            .thenBy { if (it.isIncome) 1 else 0 } // Process same-timestamp expenses before income to catch intraday shortfalls
    )

    var idx = 0
    while (idx < sorted.size) {
        val current = sorted[idx]

        if (current.isPaid) {
            val amountInDisp = round2(rates.convert(current.amount, current.currency, displayCurrency))
            if (current.isIncome) {
                periodIncomes = round2(periodIncomes + amountInDisp)
            } else {
                periodExpenses = round2(periodExpenses + amountInDisp)
            }
            results.add(
                current.copy(
                    runningAvailableBefore = runningAvailable,
                    availableBalanceAfter = runningAvailable
                )
            )
            idx++
            continue
        }

        if (current.isIncome) {
            val amountInDisp = round2(rates.convert(current.amount, current.currency, displayCurrency))
            periodIncomes = round2(periodIncomes + amountInDisp)
            pendingIncomes = round2(pendingIncomes + amountInDisp)

            val sweepId = "sweep_before_${current.id}"
            val completedSweep = completedByPlanId[sweepId]
            var excessSaved = 0.0

            if (completedSweep != null) {
                val paidSweepAmt = round2(
                    rates.convert(completedSweep.amount, AppCurrency.fromCode(completedSweep.currency), displayCurrency)
                )
                excessSaved = paidSweepAmt
                val sweepEvent = PlannedEvent(
                    id = sweepId,
                    title = "Cuota de Ahorro (Antes de ${current.title})",
                    dueDate = current.dueDate,
                    amount = paidSweepAmt,
                    currency = displayCurrency,
                    isFixed = false,
                    isIncome = false,
                    isSavingsSweep = true,
                    isPaid = true,
                    installmentInfo = "Transferido a Ahorros",
                    transactionId = completedSweep.id,
                    runningAvailableBefore = runningAvailable,
                    availableBalanceAfter = runningAvailable,
                    sweepExcessAmount = paidSweepAmt,
                    savingsBalanceAfter = runningSavings
                )
                results.add(sweepEvent)
                allSavingsSweeps.add(sweepEvent)
            } else if (enableSavingsSweep && runningAvailable > cushionAmount) {
                val excess = round2((runningAvailable - cushionAmount).coerceAtLeast(0.0))
                if (excess >= 1.0) {
                    val beforeSweep = runningAvailable
                    runningAvailable = round2(runningAvailable - excess)
                    runningSavings = round2(runningSavings + excess)
                    excessSaved = excess
                    val sweepEvent = PlannedEvent(
                        id = sweepId,
                        title = "Cuota de Ahorro Sugerida (Antes de ${current.title})",
                        dueDate = current.dueDate,
                        amount = excess,
                        currency = displayCurrency,
                        isFixed = false,
                        isIncome = false,
                        isSavingsSweep = true,
                        isPaid = false,
                        installmentInfo = "Excedente sobre colchón a resguardar",
                        runningAvailableBefore = beforeSweep,
                        availableBalanceAfter = runningAvailable,
                        sweepExcessAmount = excess,
                        savingsBalanceAfter = runningSavings
                    )
                    results.add(sweepEvent)
                    allSavingsSweeps.add(sweepEvent)
                }
            }

            val prevRunning = runningAvailable
            runningAvailable = round2(runningAvailable + amountInDisp)
            results.add(
                current.copy(
                    runningAvailableBefore = prevRunning,
                    availableBalanceAfter = runningAvailable,
                    surplusBeforeIncome = excessSaved
                )
            )
            idx++
            continue
        }

        // Contiguous block of events before the next pending income (or end of list)
        var nextPendingIncomeIdx = idx
        while (nextPendingIncomeIdx < sorted.size) {
            val candidate = sorted[nextPendingIncomeIdx]
            if (!candidate.isPaid && candidate.isIncome) break
            nextPendingIncomeIdx++
        }

        val segment = sorted.subList(idx, nextPendingIncomeIdx)
        val segmentKey = if (nextPendingIncomeIdx < sorted.size) {
            "before_${sorted[nextPendingIncomeIdx].id}"
        } else {
            "final"
        }
        val primaryDrawId = "draw_$segmentKey"
        val completedPrimaryDraw = completedByPlanId[primaryDrawId]
        val activeDrawId = if (completedPrimaryDraw != null) "draw_extra_$segmentKey" else primaryDrawId
        val completedActiveDraw = completedByPlanId[activeDrawId]

        // Dry-run lookahead on pending expenses in this segment to find cumulative shortfall before next income
        var simBal = runningAvailable
        var minSimBal = simBal
        var firstDeficitIdxInSeg = -1
        val deficitExpensesInSeg = mutableListOf<PlannedEvent>()

        segment.forEachIndexed { segIdx, segEv ->
            if (!segEv.isPaid && !segEv.isIncome) {
                val amt = round2(rates.convert(segEv.amount, segEv.currency, displayCurrency))
                simBal = round2(simBal - amt)
                if (simBal < -0.001) {
                    if (firstDeficitIdxInSeg == -1) {
                        firstDeficitIdxInSeg = segIdx
                    }
                    deficitExpensesInSeg.add(segEv)
                }
                if (simBal < minSimBal) {
                    minSimBal = simBal
                }
            }
        }

        val totalShortfall = if (minSimBal < -0.001) round2(-minSimBal) else 0.0
        val canCoverFromSavings = completedActiveDraw == null &&
            firstDeficitIdxInSeg != -1 &&
            totalShortfall > 0.001 &&
            (runningSavings + 0.001 >= totalShortfall)

        if (completedActiveDraw == null && firstDeficitIdxInSeg != -1 && totalShortfall > 0.001 && !canCoverFromSavings && runningSavings > 0.001) {
            allInsufficientAlerts.add(
                InsufficientSavingsAlert(
                    date = segment[firstDeficitIdxInSeg].dueDate,
                    neededAmount = totalShortfall,
                    availableSavings = runningSavings,
                    uncoveredExpenseTitles = deficitExpensesInSeg.map { it.title }
                )
            )
        }

        if (completedPrimaryDraw != null) {
            val paidDrawAmt = round2(
                rates.convert(completedPrimaryDraw.amount, AppCurrency.fromCode(completedPrimaryDraw.currency), displayCurrency)
            )
            val paidDrawEvent = PlannedEvent(
                id = primaryDrawId,
                title = "Ingreso desde Ahorros a Cuenta General",
                dueDate = completedPrimaryDraw.date.toDate(),
                amount = paidDrawAmt,
                currency = displayCurrency,
                isFixed = false,
                isIncome = true,
                isSavingsDraw = true,
                isPaid = true,
                installmentInfo = "Cubierto desde Ahorros",
                transactionId = completedPrimaryDraw.id,
                runningAvailableBefore = runningAvailable,
                availableBalanceAfter = runningAvailable,
                savingsBalanceAfter = runningSavings
            )
            results.add(paidDrawEvent)
            allSavingsDraws.add(paidDrawEvent)
        }

        segment.forEachIndexed { segIdx, segEv ->
            if (canCoverFromSavings && segIdx == firstDeficitIdxInSeg) {
                val beforeDrawPool = runningAvailable
                runningAvailable = round2(runningAvailable + totalShortfall)
                runningSavings = round2((runningSavings - totalShortfall).coerceAtLeast(0.0))
                val coveredTitles = deficitExpensesInSeg.map { it.title }
                val drawEvent = PlannedEvent(
                    id = activeDrawId,
                    title = "Pasar de Ahorros a Cuenta General",
                    dueDate = segEv.dueDate,
                    amount = totalShortfall,
                    currency = displayCurrency,
                    isFixed = false,
                    isIncome = true,
                    isSavingsDraw = true,
                    isPaid = false,
                    installmentInfo = "Cubre ${coveredTitles.size} egreso${if (coveredTitles.size == 1) "" else "s"} antes del próximo ingreso",
                    coveredExpenseTitles = coveredTitles,
                    runningAvailableBefore = beforeDrawPool,
                    availableBalanceAfter = runningAvailable,
                    savingsBalanceAfter = runningSavings
                )
                results.add(drawEvent)
                allSavingsDraws.add(drawEvent)
            }

            val amtDisp = round2(rates.convert(segEv.amount, segEv.currency, displayCurrency))
            if (segEv.isIncome) {
                periodIncomes = round2(periodIncomes + amtDisp)
            } else {
                periodExpenses = round2(periodExpenses + amtDisp)
            }

            if (segEv.isPaid) {
                results.add(
                    segEv.copy(
                        runningAvailableBefore = runningAvailable,
                        availableBalanceAfter = runningAvailable
                    )
                )
            } else {
                pendingExpenses = round2(pendingExpenses + amtDisp)
                val prevRunning = runningAvailable
                runningAvailable = round2(runningAvailable - amtDisp)
                val updatedEvent = segEv.copy(
                    runningAvailableBefore = prevRunning,
                    availableBalanceAfter = runningAvailable
                )
                results.add(updatedEvent)

                if (runningAvailable < minBalance) {
                    minBalance = runningAvailable
                    minEvent = updatedEvent
                }
                if (runningAvailable < -0.001) {
                    allDeficitEvents.add(updatedEvent)
                }
            }
        }

        idx = nextPendingIncomeIdx
    }

    val completedFinalSweep = completedByPlanId["sweep_final_period"]
    if (completedFinalSweep != null) {
        val paidFinalAmt = round2(
            rates.convert(completedFinalSweep.amount, AppCurrency.fromCode(completedFinalSweep.currency), displayCurrency)
        )
        val sweepEvent = PlannedEvent(
            id = "sweep_final_period",
            title = "Cuota de Ahorro Final (Remanente del Plan)",
            dueDate = endDateForFinalSweep ?: completedFinalSweep.date.toDate(),
            amount = paidFinalAmt,
            currency = displayCurrency,
            isFixed = false,
            isIncome = false,
            isSavingsSweep = true,
            isPaid = true,
            installmentInfo = "Transferido a Ahorros",
            transactionId = completedFinalSweep.id,
            runningAvailableBefore = runningAvailable,
            availableBalanceAfter = runningAvailable,
            sweepExcessAmount = paidFinalAmt,
            savingsBalanceAfter = runningSavings
        )
        results.add(sweepEvent)
        allSavingsSweeps.add(sweepEvent)
    } else if (endDateForFinalSweep != null && enableSavingsSweep && runningAvailable > cushionAmount) {
        val finalExcess = round2((runningAvailable - cushionAmount).coerceAtLeast(0.0))
        if (finalExcess >= 1.0) {
            val beforeFinalSweep = runningAvailable
            runningAvailable = round2(runningAvailable - finalExcess)
            runningSavings = round2(runningSavings + finalExcess)
            val sweepEvent = PlannedEvent(
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
                sweepExcessAmount = finalExcess,
                savingsBalanceAfter = runningSavings
            )
            results.add(sweepEvent)
            allSavingsSweeps.add(sweepEvent)
        }
    }

    val deficitDays = allDeficitEvents
        .groupBy { ev ->
            val cal = Calendar.getInstance().apply { time = ev.dueDate }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }
        .values
        .map { dayEvents ->
            val lowest = dayEvents.minOf { it.availableBalanceAfter }
            val totalDayExpenses = round2(
                dayEvents.sumOf { rates.convert(it.amount, it.currency, displayCurrency) }
            )
            DeficitDaySummary(
                date = dayEvents.first().dueDate,
                events = dayEvents,
                lowestBalance = lowest,
                totalDeficitExpensesInDisplay = totalDayExpenses
            )
        }
        .sortedBy { it.date.time }

    return PoolSimulationResult(
        events = results,
        startingPool = round2(startingPool),
        endingPool = runningAvailable,
        startingSavings = round2(initialSavingsAvailable.coerceAtLeast(0.0)),
        endingSavings = runningSavings,
        minPoolBalance = minBalance,
        minPoolEvent = minEvent,
        firstDeficitEvent = allDeficitEvents.firstOrNull(),
        deficitEvents = allDeficitEvents,
        deficitDays = deficitDays,
        savingsSweeps = allSavingsSweeps,
        savingsDraws = allSavingsDraws,
        insufficientSavingsAlerts = allInsufficientAlerts,
        totalPendingIncomes = pendingIncomes,
        totalPendingExpenses = pendingExpenses,
        totalPeriodIncomes = periodIncomes,
        totalPeriodExpenses = periodExpenses
    )
}
