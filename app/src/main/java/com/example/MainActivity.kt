package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.FinanceRepository
import com.example.ui.FinanceViewModel
import com.example.ui.MainApp
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.FinanzasTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanzasTheme {
                AppRoot()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppRoot(
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val context = LocalContext.current
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isGuestMode by rememberSaveable { mutableStateOf(false) }

    val user = currentUser

    if (user == null && !isGuestMode) {
        AuthScreen(
            onSignInSuccess = {
                isGuestMode = false
            },
            onContinueAsGuest = {
                isGuestMode = true
            }
        )
    } else {
        val activeUserId = if (user != null) user.uid else "local_guest"
        val databaseId = stringResource(R.string.firestore_database_id)

        val viewModel: FinanceViewModel = viewModel(
            key = activeUserId,
            factory = viewModelFactory {
                initializer {
                    val db = FirebaseFirestore.getInstance(databaseId)
                    val repository = FinanceRepository(db, context.applicationContext)
                    FinanceViewModel(repository, activeUserId)
                }
            }
        )

        MainApp(
            viewModel = viewModel,
            onSignOut = {
                isGuestMode = false
                auth.signOut()
            }
        )
    }
}
