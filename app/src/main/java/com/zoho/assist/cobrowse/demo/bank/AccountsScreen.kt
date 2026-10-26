package com.zoho.assist.cobrowse.demo.bank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zoho.assist.cobrowse.demo.common.BankTheme

/**
 * Accounts: every account the customer holds. The list is 100 rows long and
 * uses a `LazyColumn` (not a `Column`) so rows recycle as they leave the
 * viewport under a fast scroll. Tapping a row opens the account-details sheet
 * hosted by the app shell.
 */
@Composable
fun AccountsScreen(onOpenAccount: (Account) -> Unit, modifier: Modifier = Modifier) {
    // Every masked row is a region the SDK tracks, and it holds frames while
    // regions report their bounds, so a long masked list scrolls less smoothly.
    var redactValues by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "accounts-title") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeader("Accounts")
                Text(
                    "${BankData.allAccounts.size} accounts · tap one for details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Hide balances from the agent",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            if (redactValues) {
                                "Balances and account numbers are barred on their side."
                            } else {
                                "The agent sees every value on this list."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = redactValues, onCheckedChange = { redactValues = it })
                }
            }
        }
        items(BankData.allAccounts, key = { it.accountNumberMasked + it.name }) { acct ->
            AccountCard(acct, sensitive = redactValues, onClick = { onOpenAccount(acct) })
        }
    }
}

@Composable
private fun AccountCard(acct: Account, sensitive: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    if (acct.isCredit) Color(0xFFE17A4F) else Color(0xFF3E8E7E),
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (acct.isCredit) Icons.Filled.AccountBalanceWallet else Icons.Filled.Savings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(acct.name, style = MaterialTheme.typography.bodyLarge)
                val number = @Composable {
                    Text(
                        acct.accountNumberMasked,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (sensitive) Redactable { number() } else number()
            }
            val amount = @Composable {
                Text(
                    if (BankSettings.hideBalances) "••••" else formatAmount(acct.balance),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (acct.balance < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            if (sensitive) Redactable { amount() } else amount()
        }
    }
}

/**
 * Account details, as a sheet that slides up from the bottom of the window.
 *
 * This is NOT a `ModalBottomSheet`: it is a scrim plus a `Surface` in the
 * activity's own tree (see [OverlayScrim]), so the whole thing stays inside
 * this one window.
 */
@Composable
fun BoxScope.AccountDetailsSheet(account: Account?, onDismiss: () -> Unit) {
    if (account != null) OverlayScrim(alpha = 0.4f, onDismiss = onDismiss)

    AnimatedVisibility(
        visible = account != null,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        // Kept non-null for the exit animation by falling back to the primary
        // account: `account` is already null while the sheet slides away.
        val shown = account ?: BankData.primaryAccounts.first()
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    // The sheet is the bottom-most thing in the window and the app
                    // draws edge to edge, so it insets itself past the gesture bar.
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                CircleShape,
                            ),
                    )
                }
                Text(
                    shown.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.size(8.dp))
                ValueRow("Balance", formatAmount(shown.balance), emphasis = true)
                // Account identifiers are withheld: together they are enough to
                // set up a payment. Balance and type stay visible so the agent can help.
                ValueRow("Account #", shown.accountNumberMasked, sensitive = true)
                ValueRow("Sort code", shown.sortCode, sensitive = true)
                ValueRow("IBAN", shown.iban, sensitive = true)
                ValueRow("Type", if (shown.isCredit) "Credit" else "Deposit")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Copy details")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/** Shared scaffold so each preview differs only by the content it renders. */
@Composable
private fun AccountsPreview(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    BankTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}

@Preview(name = "1 Accounts list", showBackground = true, heightDp = 720)
@Composable
private fun PreviewAccountsScreen() = AccountsPreview {
    AccountsScreen(onOpenAccount = {})
}

@Preview(name = "2 Accounts list — dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewAccountsScreenDark() = AccountsPreview(darkTheme = true) {
    AccountsScreen(onOpenAccount = {})
}

@Preview(name = "3 Card — deposit", showBackground = true, widthDp = 400)
@Composable
private fun PreviewAccountCardDeposit() = AccountsPreview {
    Column(Modifier.padding(16.dp)) {
        AccountCard(BankData.primaryAccounts.first(), onClick = {})
    }
}

@Preview(name = "4 Card — credit, overdrawn", showBackground = true, widthDp = 400)
@Composable
private fun PreviewAccountCardCredit() = AccountsPreview {
    Column(Modifier.padding(16.dp)) {
        AccountCard(BankData.primaryAccounts.first { it.isCredit }, onClick = {})
    }
}

@Preview(name = "5 Card — long name", showBackground = true, widthDp = 400)
@Composable
private fun PreviewAccountCardLongName() = AccountsPreview {
    Column(Modifier.padding(16.dp)) {
        AccountCard(
            BankData.primaryAccounts.first().copy(
                name = "Joint Everyday Checking and Household Bills Account",
                balance = 1_234_567.89,
            ),
            onClick = {},
        )
    }
}

// AccountDetailsSheet is BoxScope-scoped and aligns itself to the bottom, so a
// preview has to supply the Box it expects to live in.
@Preview(name = "6 Account details sheet", showBackground = true, heightDp = 720)
@Composable
private fun PreviewAccountDetailsSheet() = AccountsPreview {
    Box(Modifier.fillMaxSize()) {
        AccountDetailsSheet(account = BankData.primaryAccounts.first(), onDismiss = {})
    }
}

@Preview(name = "7 Account details — credit, dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewAccountDetailsSheetDark() = AccountsPreview(darkTheme = true) {
    Box(Modifier.fillMaxSize()) {
        AccountDetailsSheet(
            account = BankData.primaryAccounts.first { it.isCredit },
            onDismiss = {},
        )
    }
}
