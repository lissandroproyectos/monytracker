package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SystemUpdate
import com.example.ui.components.AppUpdateDialog
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.currency.AppCurrency
import com.example.data.model.Account
import com.example.data.model.ExpenseCommitment
import com.example.data.model.MarketTemplate
import com.example.data.model.TransactionType
import com.example.ui.components.AddCommitmentDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.CurrencySettingsDialog
import com.example.ui.components.InstallmentsScheduleDialog
import com.example.ui.components.PayCommitmentDialog
import com.example.ui.components.TransferSavingsDialog
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.CalendarTimelineScreen
import com.example.ui.screens.CommitmentsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.*
import com.example.ui.components.ProfileSettingsDialog

enum class ScreenTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Resumen", Icons.Filled.Home, Icons.Outlined.Home),
    TIMELINE("Calendario", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    COMMITMENTS("Egresos", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    ACCOUNTS("Cuentas", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: FinanceViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var showTransactionsScreen by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    val activeFirebaseUser = remember {
        try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
    }

    // Dialog state
    var showCurrencySettingsDialog by remember { mutableStateOf(false) }
    var showTransferSavingsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf<String?>(null) }
    var showAddCommitmentDialog by remember { mutableStateOf(false) }
    var prefilledTemplateForCommitment by remember { mutableStateOf<MarketTemplate?>(null) }
    var commitmentToPay by remember { mutableStateOf<ExpenseCommitment?>(null) }
    var commitmentToInspectInstallments by remember { mutableStateOf<ExpenseCommitment?>(null) }
    var commitmentToEditInDialog by remember { mutableStateOf<ExpenseCommitment?>(null) }
    var commitmentToDeleteInDialog by remember { mutableStateOf<ExpenseCommitment?>(null) }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val health by viewModel.financialHealth.collectAsStateWithLifecycle()
    val rates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val marketplaceTemplates by viewModel.marketplaceTemplates.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            when (it) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(it.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar(it.message)
            }
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // BackHandler
    BackHandler(enabled = showTransactionsScreen || currentTab != ScreenTab.DASHBOARD) {
        if (showTransactionsScreen) {
            showTransactionsScreen = false
        } else {
            currentTab = ScreenTab.DASHBOARD
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SlateBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finanzas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NavySurface
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(EmeraldDark.copy(alpha = 0.35f))
                            .clickable { showProfileSettingsDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Perfil y Ajustes",
                                tint = EmeraldLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeFirebaseUser?.email?.substringBefore("@") ?: "Ajustes",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NavySurface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                ScreenTab.entries.forEach { tab ->
                    val selected = !showTransactionsScreen && currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            showTransactionsScreen = false
                            currentTab = tab
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavySurface,
                            selectedTextColor = EmeraldLight,
                            indicatorColor = EmeraldLight,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showTransactionsScreen) {
                TransactionsScreen(viewModel = viewModel)
            } else {
                when (currentTab) {
                    ScreenTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAccounts = { currentTab = ScreenTab.ACCOUNTS },
                        onNavigateToTransactions = { showTransactionsScreen = true },
                        onNavigateToCommitments = { currentTab = ScreenTab.COMMITMENTS },
                        onNavigateToTimeline = { currentTab = ScreenTab.TIMELINE },
                        onNewIncome = { showAddTxDialog = TransactionType.INCOME.name },
                        onNewExpense = { showAddTxDialog = TransactionType.EXPENSE.name },
                        onNewCommitment = {
                            prefilledTemplateForCommitment = null
                            showAddCommitmentDialog = true
                        },
                        onPayCommitment = { comm -> commitmentToPay = comm },
                        onViewInstallments = { comm -> commitmentToInspectInstallments = comm },
                        onOpenCurrencySettings = { showCurrencySettingsDialog = true },
                        onTransferSavings = { showTransferSavingsDialog = true }
                    )
                    ScreenTab.COMMITMENTS -> CommitmentsScreen(
                        viewModel = viewModel,
                        onViewInstallments = { comm -> commitmentToEditInDialog = comm },
                        onOpenDebtTypes = {
                            prefilledTemplateForCommitment = null
                            showAddCommitmentDialog = true
                        }
                    )
                    ScreenTab.TIMELINE -> CalendarTimelineScreen(viewModel = viewModel)
                    ScreenTab.ACCOUNTS -> AccountsScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showSignOutConfirm) {
        val emailStr = activeFirebaseUser?.email ?: activeFirebaseUser?.displayName ?: ""
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Cerrar Sesión de Google", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (emailStr.isNotBlank()) {
                        Text("Usuario actual: $emailStr", color = EmeraldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "Tus cuentas, movimientos y compromisos están respaldados en la nube de Firebase. ¿Deseas cerrar la sesión?",
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    modifier = Modifier.testTag("confirm_sign_out_btn")
                ) {
                    Text("Cerrar Sesión", color = EmeraldLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancelar", color = TextSecondaryDark)
                }
            },
            containerColor = SlateCard
        )
    }

    // Modal dialogs
    showAddTxDialog?.let { initialType ->
        if (accounts.isNotEmpty()) {
            AddTransactionDialog(
                initialType = initialType,
                accounts = accounts,
                customExpenseCategories = settings.customExpenseCategories,
                customIncomeCategories = settings.customIncomeCategories,
                onDismiss = { showAddTxDialog = null },
                onSave = { tx -> viewModel.addTransaction(tx) },
                onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) }
            )
        }
    }

    if (showAddCommitmentDialog) {
        AddCommitmentDialog(
            accounts = accounts,
            templates = marketplaceTemplates,
            customCategories = settings.customExpenseCategories,
            prefilledTemplate = prefilledTemplateForCommitment,
            onDismiss = {
                showAddCommitmentDialog = false
                prefilledTemplateForCommitment = null
            },
            onSave = { newCommitment -> viewModel.addCommitment(newCommitment) },
            onSaveCustomTemplate = { newTmpl -> viewModel.publishMarketplaceTemplate(newTmpl) },
            onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) }
        )
    }

    commitmentToPay?.let { comm ->
        PayCommitmentDialog(
            commitment = comm,
            accounts = accounts,
            onDismiss = { commitmentToPay = null },
            onConfirm = { payment, account ->
                viewModel.payCommitment(payment, comm, account)
            }
        )
    }

    commitmentToInspectInstallments?.let { comm ->
        InstallmentsScheduleDialog(
            commitment = comm,
            accounts = accounts,
            rates = rates,
            displayCurrency = health.displayCurrency,
            onDismiss = { commitmentToInspectInstallments = null },
            onToggleInstallment = { number, isPaid, account ->
                viewModel.toggleInstallment(comm, number, isPaid, account)
            },
            onEdit = { commitmentToEditInDialog = comm },
            onDelete = { commitmentToDeleteInDialog = comm }
        )
    }

    commitmentToEditInDialog?.let { comm ->
        AddCommitmentDialog(
            accounts = accounts,
            templates = marketplaceTemplates,
            customCategories = settings.customExpenseCategories,
            commitmentToEdit = comm,
            onDismiss = { commitmentToEditInDialog = null },
            onSave = { updated ->
                viewModel.updateCommitment(updated)
                commitmentToEditInDialog = null
            },
            onCreateCategory = { name, isInc -> viewModel.addCustomCategory(name, isInc) },
            onDelete = {
                viewModel.deleteCommitment(comm.id)
                commitmentToEditInDialog = null
            }
        )
    }

    commitmentToDeleteInDialog?.let { comm ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { commitmentToDeleteInDialog = null },
            title = { androidx.compose.material3.Text("¿Eliminar compromiso?", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = { androidx.compose.material3.Text("Se eliminará '${comm.title}' y su cronograma de cuotas.") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.deleteCommitment(comm.id)
                        commitmentToDeleteInDialog = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.AccentRed)
                ) {
                    androidx.compose.material3.Text("Eliminar", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { commitmentToDeleteInDialog = null }) {
                    androidx.compose.material3.Text("Cancelar")
                }
            },
            containerColor = com.example.ui.theme.SlateCard
        )
    }

    if (showCurrencySettingsDialog) {
        CurrencySettingsDialog(
            currentDisplay = health.displayCurrency,
            currentPayment = health.paymentCurrency,
            currentCushion = health.safetyCushionAmount,
            onDismiss = { showCurrencySettingsDialog = false },
            onSave = { disp, pay, cushion ->
                viewModel.updateCurrencies(disp, pay)
                viewModel.updateSafetyCushion(cushion)
            }
        )
    }

    if (showTransferSavingsDialog) {
        TransferSavingsDialog(
            accounts = accounts,
            suggestedAmount = health.surplusAvailableForSavings,
            displayCurrency = health.displayCurrency,
            onDismiss = { showTransferSavingsDialog = false },
            onConfirm = { from, to, amount ->
                viewModel.transferSavings(from, to, amount)
            }
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showUpdateDialog) {
        AppUpdateDialog(
            currentRepo = settings.githubRepo,
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (showProfileSettingsDialog) {
        ProfileSettingsDialog(
            displayCurrency = health.displayCurrency,
            paymentCurrency = health.paymentCurrency,
            onOpenCurrencySettings = { showCurrencySettingsDialog = true },
            onOpenBackup = { showBackupDialog = true },
            onOpenUpdate = { showUpdateDialog = true },
            onSignOutClick = { showSignOutConfirm = true },
            onDismiss = { showProfileSettingsDialog = false }
        )
    }
}
