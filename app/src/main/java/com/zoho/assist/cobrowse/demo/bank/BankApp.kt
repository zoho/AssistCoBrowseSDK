package com.zoho.assist.cobrowse.demo.bank

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.zoho.assist.cobrowse.demo.CobrowseController

/** The five destinations on the bottom bar. */
enum class BankTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    ACCOUNTS("Accounts", Icons.Filled.AccountBalance),
    CARDS("Cards", Icons.Filled.CreditCard),
    PAY("Pay", Icons.Rounded.SwapHoriz),
    MORE("More", Icons.Rounded.MoreHoriz),
}

/**
 * A screen pushed OVER the tabs. A route hides the bottom bar and takes a back
 * arrow in the app bar, the way a payment or a statement does in a real
 * banking app.
 */
sealed interface BankRoute {
    val title: String

    data object Statement : BankRoute {
        override val title = "e-Statement"
    }

    data object History : BankRoute {
        override val title = "Transaction history"
    }

    data object Markets : BankRoute {
        override val title = "Live markets"
    }

    data object Security : BankRoute {
        override val title = "Security & privacy"
    }

    data object ReportProblem : BankRoute {
        override val title = "Report a problem"
    }

    data class TxDetail(val transaction: Transaction) : BankRoute {
        override val title = "Transaction"
    }
}

/** Where the transfer journey has got to, once it leaves the form. */
private sealed interface TransferStage {
    val draft: TransferDraft

    data class Review(override val draft: TransferDraft) : TransferStage
    data class Otp(override val draft: TransferDraft) : TransferStage
}

/**
 * The banking app: an app bar, five tabs, the routes pushed over them, and the
 * window-level overlays.
 *
 * Overlays (account details, transfer confirmation, one-time passcode) are
 * siblings of the [Scaffold] inside one root [Box], so their scrim covers the
 * app bar and bottom bar without leaving this window. See [OverlayScrim].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankApp(modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(BankTab.HOME) }
    var route by remember { mutableStateOf<BankRoute?>(null) }
    var accountSheet by remember { mutableStateOf<Account?>(null) }
    var transfer by remember { mutableStateOf<TransferStage?>(null) }
    var sentTransfer by remember { mutableStateOf<TransferDraft?>(null) }
    var shareSheet by remember { mutableStateOf(false) }

    // Back unwinds the innermost thing first: overlay, then sheet, then the
    // pushed route, then back to Home. Only from Home does a press leave the app.
    BackHandler(
        enabled = transfer != null || accountSheet != null || route != null || tab != BankTab.HOME,
    ) {
        when {
            transfer != null -> transfer = null
            accountSheet != null -> accountSheet = null
            route != null -> route = null
            else -> tab = BankTab.HOME
        }
    }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            route?.title
                                ?: if (tab == BankTab.HOME) BankData.BANK_NAME else tab.title,
                        )
                    },
                    navigationIcon = {
                        if (route != null) {
                            IconButton(onClick = { route = null }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        }
                    },
                    actions = {
                        // The only way into a session; tinted while one is live so the
                        // state is visible from every screen.
                        val live = CobrowseController.state is CobrowseController.State.Active
                        IconButton(onClick = { shareSheet = true }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ScreenShare,
                                contentDescription = "Share your screen with support",
                                tint = if (live) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            },
            bottomBar = {
                // NavigationBar, not ShortNavigationBar: on material3 1.4.0 the short
                // bar renders only the first item.
                if (route == null) {
                    NavigationBar(modifier = Modifier.fillMaxWidth()) {
                        for (t in BankTab.entries) {
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = t.title) },
                                label = { Text(t.title) },
                            )
                        }
                    }
                }
            },
        ) { contentPadding ->
            // No imePadding: with windowSoftInputMode=adjustNothing the keyboard
            // overlays the window, so a form doesn't jump when a field takes focus.
            Box(Modifier.fillMaxSize().padding(contentPadding)) {
                when (val r = route) {
                    null -> TabContent(
                        tab = tab,
                        sentTransfer = sentTransfer,
                        onOpenRoute = { route = it },
                        onSelectTab = { tab = it },
                        onOpenAccount = { accountSheet = it },
                        onReviewTransfer = { transfer = TransferStage.Review(it) },
                        onTransferDone = { sentTransfer = null },
                    )
                    is BankRoute.TxDetail -> TransactionDetail(r.transaction, onBack = { route = null })
                    BankRoute.Statement -> StatementScreen()
                    BankRoute.History -> HistoryScreen()
                    BankRoute.Markets -> MarketsScreen()
                    BankRoute.Security -> SecurityScreen()
                    BankRoute.ReportProblem -> ReportProblemScreen()
                }
            }
        }

        AccountDetailsSheet(account = accountSheet, onDismiss = { accountSheet = null })

        when (val stage = transfer) {
            null -> Unit
            is TransferStage.Review -> TransferReviewDialog(
                draft = stage.draft,
                onCancel = { transfer = null },
                onConfirm = { transfer = TransferStage.Otp(stage.draft) },
            )
            is TransferStage.Otp -> OtpOverlay(
                onCancel = { transfer = null },
                onVerify = {
                    sentTransfer = stage.draft
                    transfer = null
                },
            )
        }

        if (shareSheet) {
            ModalBottomSheet(onDismissRequest = { shareSheet = false }) {
                CobrowseSheetContent(onDone = { shareSheet = false })
            }
        }
    }
}

@Composable
private fun TabContent(
    tab: BankTab,
    sentTransfer: TransferDraft?,
    onOpenRoute: (BankRoute) -> Unit,
    onSelectTab: (BankTab) -> Unit,
    onOpenAccount: (Account) -> Unit,
    onReviewTransfer: (TransferDraft) -> Unit,
    onTransferDone: () -> Unit,
) {
    when (tab) {
        BankTab.HOME -> HomeScreen(
            onOpenTransaction = { onOpenRoute(BankRoute.TxDetail(it)) },
            onTransfer = { onSelectTab(BankTab.PAY) },
            onCards = { onSelectTab(BankTab.CARDS) },
            onStatement = { onOpenRoute(BankRoute.Statement) },
            onHistory = { onOpenRoute(BankRoute.History) },
        )
        BankTab.ACCOUNTS -> AccountsScreen(onOpenAccount = onOpenAccount)
        BankTab.CARDS -> CardsScreen()
        BankTab.PAY -> PayScreen(
            completed = sentTransfer,
            onReview = onReviewTransfer,
            onDone = onTransferDone,
        )
        BankTab.MORE -> MoreScreen(onOpen = onOpenRoute)
    }
}
