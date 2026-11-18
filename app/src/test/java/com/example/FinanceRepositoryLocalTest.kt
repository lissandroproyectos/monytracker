package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
}

