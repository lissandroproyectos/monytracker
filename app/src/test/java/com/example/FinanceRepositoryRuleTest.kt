package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Account
import com.example.data.model.ExpenseCommitment
import com.example.data.model.PaymentMode
import com.example.data.repository.FinanceRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUser_canCreateAndReadAccount() = runBlocking {
        val uid = signInTestUser("user1@example.com")
        val repository = FinanceRepository(firestore)

        val account = Account(
            name = "Ahorros Principales",
            type = "SAVINGS",
            currentBalance = 1200.0,
            initialBalance = 1200.0,
            currency = "$",
            createdAt = Timestamp.now()
        )

        val accountId = repository.createAccount(uid, account)
        assertNotNull(accountId)

        val accounts = repository.observeAccounts(uid).first()
        assertTrue(accounts.any { it.name == "Ahorros Principales" })
    }

    @Test
    fun authenticatedUser_canCreatePlannedCommitment() = runBlocking {
        val uid = signInTestUser("user2@example.com")
        val repository = FinanceRepository(firestore)

        val commitment = ExpenseCommitment(
            title = "Curso de Kotlin",
            totalAmount = 600.0,
            remainingAmount = 600.0,
            paymentMode = PaymentMode.INSTALLMENTS.name,
            category = "Educación",
            totalInstallments = 6,
            installmentAmount = 100.0,
            createdAt = Timestamp.now()
        )

        val commitmentId = repository.createCommitment(uid, commitment)
        assertNotNull(commitmentId)

        val commitments = repository.observeCommitments(uid).first()
        assertTrue(commitments.any { it.title == "Curso de Kotlin" })
    }

    @Test
    fun unauthenticatedUser_cannotReadAccounts() = runBlocking {
        auth.signOut()
        val repository = FinanceRepository(firestore)

        try {
            repository.observeAccounts("some_user_id").first()
            fail("Expected FirebaseFirestoreException but succeeded")
        } catch (e: Throwable) {
            val fse = e.unwrapFirestoreException()
            assertNotNull("Expected Firestore exception", fse)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fse?.code)
        }
    }
}
