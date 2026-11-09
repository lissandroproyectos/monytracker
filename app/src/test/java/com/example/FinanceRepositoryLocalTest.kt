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
}
