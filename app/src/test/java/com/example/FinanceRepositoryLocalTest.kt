package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.currency.AppCurrency
import com.example.data.currency.ExchangeRates
import com.example.data.model.Account
import com.example.data.model.ExpenseCommitment
import com.example.data.model.PaymentMode
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceRepositoryLocalTest {

    @Test
    fun guestMode_canCreateAccountAndPersistWithoutFirebase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Purely local repository with null db
        val repository = FinanceRepository(null, context)
        val guestUserId = "local_guest"

        val account = Account(
            name = "Efectivo Local",
            type = "CASH",
            currentBalance = 50.0,
            initialBalance = 50.0,
            currency = "$"
        )

        val accountId = repository.createAccount(guestUserId, account)
        assertNotNull(accountId)

        val accounts = repository.observeAccounts(guestUserId).first()
        assertTrue("Account should be present in local storage", accounts.any { it.name == "Efectivo Local" })

        // Test transaction creation
        val tx = Transaction(
            accountId = accountId,
            type = TransactionType.INCOME.name,
            amount = 100.0,
            category = "Ingreso",
            description = "Pago en efectivo"
        )
        repository.addTransaction(guestUserId, tx)
        val updatedAccounts = repository.observeAccounts(guestUserId).first()
        val updatedAcc = updatedAccounts.first { it.id == accountId }
        assertEquals(150.0, updatedAcc.currentBalance, 0.001)

        // Test commitment creation
        val commitment = ExpenseCommitment(
            title = "Servicio Local",
            totalAmount = 30.0,
            remainingAmount = 30.0,
            paymentMode = PaymentMode.FULL.name,
            category = "Hogar"
        )
        val commId = repository.createCommitment(guestUserId, commitment)
        assertNotNull(commId)
        val commitments = repository.observeCommitments(guestUserId).first()
        assertTrue(commitments.any { it.title == "Servicio Local" })
    }

    @Test
    fun fixedExpense_singleInstallmentPayment_staysActiveAndTracksCycles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = FinanceRepository(null, context)
        val guestUserId = "local_guest"

        val fixed = ExpenseCommitment(
            title = "Internet Mensual",
            totalAmount = 50.0,
            remainingAmount = 300.0,
            installmentAmount = 50.0,
            totalInstallments = 6L,
            isFixedExpense = true,
            paymentMode = PaymentMode.INSTALLMENTS.name,
            category = "Servicios"
        )
        val commId = repository.createCommitment(guestUserId, fixed)
        val created = repository.observeCommitments(guestUserId).first().first { it.id == commId }

        repository.toggleInstallmentPaid(
            userId = guestUserId,
            commitment = created,
            installmentNumber = 1L,
            markAsPaid = true,
            payFromAccount = null
        )

        val afterOnePayment = repository.observeCommitments(guestUserId).first().first { it.id == commId }
        assertEquals(com.example.data.model.CommitmentStatus.ACTIVE.name, afterOnePayment.status)
        assertEquals(1L, afterOnePayment.paidInstallments)
        assertEquals(250.0, afterOnePayment.remainingAmount, 0.001)
    }

    @Test
    fun corruptedPaidOffFixedExpense_isAutoHealedOnObserve() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = FinanceRepository(null, context)
        val guestUserId = "local_guest"

        val corrupted = ExpenseCommitment(
            title = "Alquiler",
            totalAmount = 200.0,
            remainingAmount = 0.0,
            installmentAmount = 200.0,
            totalInstallments = 6L,
            paidInstallments = 1L,
            isFixedExpense = true,
            status = com.example.data.model.CommitmentStatus.PAID_OFF.name,
            paymentMode = PaymentMode.INSTALLMENTS.name
        )
        val commId = repository.createCommitment(guestUserId, corrupted)
        repository.repairCorruptedFixedCommitments(guestUserId)

        val healed = repository.observeCommitments(guestUserId).first().first { it.id == commId }
        assertEquals(com.example.data.model.CommitmentStatus.ACTIVE.name, healed.status)
        assertTrue(healed.remainingAmount > 0.0)
    }

    @Test
    fun crossCurrencyExpenseAndDelete_preservesExactOriginCurrencyEvenIfRatesChange() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = FinanceRepository(null, context)
        val guestUserId = "local_guest"
        val initialRates = ExchangeRates(dolarBcv = 40.0, euroBcv = 44.0, usdtParalelo = 45.0)

        val vesAccountId = repository.createAccount(
            guestUserId,
            Account(
                name = "Banco Nacional VES",
                type = "BANK",
                currentBalance = 5000.0,
                initialBalance = 5000.0,
                currency = AppCurrency.VES.name
            )
        )

        val usdExpense = Transaction(
            accountId = vesAccountId,
            type = TransactionType.EXPENSE.name,
            amount = 50.0,
            currency = AppCurrency.USD_BCV.name,
            category = "Servicios",
            description = "Pago USD desde cuenta VES"
        )
        repository.addTransaction(guestUserId, usdExpense, initialRates)

        val afterDebitAcc = repository.observeAccounts(guestUserId).first().first { it.id == vesAccountId }
        // 50 USD_BCV * 40 = 2000 VES deducted -> 5000 - 2000 = 3000 VES
        assertEquals(3000.0, afterDebitAcc.currentBalance, 0.001)

        val savedTx = repository.observeTransactions(guestUserId).first().first { it.accountId == vesAccountId }
        assertEquals(2000.0, savedTx.accountDelta ?: 0.0, 0.001)
        assertEquals(AppCurrency.VES.name, savedTx.accountCurrency)

        // Rate fluctuates later, but refunding/deleting MUST restore the exact 2000 VES
        val newerRates = ExchangeRates(dolarBcv = 55.0, euroBcv = 60.0, usdtParalelo = 62.0)
        repository.deleteTransaction(guestUserId, savedTx, newerRates)

        val restoredAcc = repository.observeAccounts(guestUserId).first().first { it.id == vesAccountId }
        assertEquals(5000.0, restoredAcc.currentBalance, 0.001)
    }

    @Test
    fun crossCurrencyTransferAndRevert_restoresOriginAndTargetCurrenciesExactly() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = FinanceRepository(null, context)
        val guestUserId = "local_guest"
        val initialRates = ExchangeRates(dolarBcv = 40.0, euroBcv = 44.0, usdtParalelo = 45.0)

        val usdAccId = repository.createAccount(
            guestUserId,
            Account(
                name = "Cuenta Dólares",
                type = "BANK",
                currentBalance = 300.0,
                initialBalance = 300.0,
                currency = AppCurrency.USD_BCV.name
            )
        )
        val vesSavingsId = repository.createAccount(
            guestUserId,
            Account(
                name = "Ahorro Bolívares",
                type = "BANK",
                currentBalance = 1000.0,
                initialBalance = 1000.0,
                currency = AppCurrency.VES.name,
                isSavings = true
            )
        )

        val transferTx = Transaction(
            accountId = usdAccId,
            targetAccountId = vesSavingsId,
            type = TransactionType.TRANSFER.name,
            amount = 100.0,
            currency = AppCurrency.USD_BCV.name,
            category = "Ahorro",
            description = "Pase USD -> VES"
        )
        repository.addTransaction(guestUserId, transferTx, initialRates)

        val accountsAfterTransfer = repository.observeAccounts(guestUserId).first()
        assertEquals(200.0, accountsAfterTransfer.first { it.id == usdAccId }.currentBalance, 0.001)
        // 100 USD_BCV * 40 = 4000 VES added -> 1000 + 4000 = 5000 VES
        assertEquals(5000.0, accountsAfterTransfer.first { it.id == vesSavingsId }.currentBalance, 0.001)

        val savedTransfer = repository.observeTransactions(guestUserId).first().first { it.accountId == usdAccId }
        assertEquals(100.0, savedTransfer.accountDelta ?: 0.0, 0.001)
        assertEquals(AppCurrency.USD_BCV.name, savedTransfer.accountCurrency)
        assertEquals(4000.0, savedTransfer.targetAccountDelta ?: 0.0, 0.001)
        assertEquals(AppCurrency.VES.name, savedTransfer.targetAccountCurrency)

        val laterRates = ExchangeRates(dolarBcv = 60.0, euroBcv = 65.0, usdtParalelo = 68.0)
        repository.deleteTransaction(guestUserId, savedTransfer, laterRates)

        val accountsRestored = repository.observeAccounts(guestUserId).first()
        assertEquals(300.0, accountsRestored.first { it.id == usdAccId }.currentBalance, 0.001)
        assertEquals(1000.0, accountsRestored.first { it.id == vesSavingsId }.currentBalance, 0.001)
    }

    @Test
    fun unmarkPaidInstallment_refundsOriginVesAccountAndRemovesLinkedTransaction() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = FinanceRepository(null, context)
        val guestUserId = "local_guest"
        val rates = ExchangeRates(dolarBcv = 50.0, euroBcv = 55.0, usdtParalelo = 56.0)

        val vesAccId = repository.createAccount(
            guestUserId,
            Account(
                name = "Pago Móvil",
                type = "BANK",
                currentBalance = 10000.0,
                initialBalance = 10000.0,
                currency = AppCurrency.VES.name
            )
        )
        val vesAccount = repository.observeAccounts(guestUserId).first().first { it.id == vesAccId }

        val debt = ExpenseCommitment(
            title = "Equipo",
            totalAmount = 120.0,
            remainingAmount = 120.0,
            installmentAmount = 40.0,
            totalInstallments = 3L,
            currency = AppCurrency.USD_BCV.name,
            paymentMode = PaymentMode.INSTALLMENTS.name,
            category = "Compras"
        )
        val commId = repository.createCommitment(guestUserId, debt)
        val createdDebt = repository.observeCommitments(guestUserId).first().first { it.id == commId }

        repository.toggleInstallmentPaid(
            userId = guestUserId,
            commitment = createdDebt,
            installmentNumber = 1L,
            markAsPaid = true,
            payFromAccount = vesAccount,
            rates = rates
        )

        // 40 USD * 50 = 2000 VES deducted -> 8000 VES left
        assertEquals(8000.0, repository.observeAccounts(guestUserId).first().first { it.id == vesAccId }.currentBalance, 0.001)
        val updatedDebt = repository.observeCommitments(guestUserId).first().first { it.id == commId }
        assertEquals(1L, updatedDebt.paidInstallments)

        // Unmark installment with different exchange rates -> still refunds exact 2000 VES
        val changedRates = ExchangeRates(dolarBcv = 70.0, euroBcv = 75.0, usdtParalelo = 78.0)
        repository.toggleInstallmentPaid(
            userId = guestUserId,
            commitment = updatedDebt,
            installmentNumber = 1L,
            markAsPaid = false,
            payFromAccount = null,
            rates = changedRates
        )

        val restoredAcc = repository.observeAccounts(guestUserId).first().first { it.id == vesAccId }
        assertEquals(10000.0, restoredAcc.currentBalance, 0.001)
        val revertedDebt = repository.observeCommitments(guestUserId).first().first { it.id == commId }
        assertEquals(0L, revertedDebt.paidInstallments)
        assertTrue(repository.observeTransactions(guestUserId).first().none { it.commitmentId == commId })
    }
}

