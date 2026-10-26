package com.zoho.assist.cobrowse.demo.bank

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zoho.assist.cobrowse.demo.common.BankTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Home: the customer's landing screen — balance, quick actions, a spend
 * breakdown for the month and the recent-transaction feed. Tapping a
 * transaction opens [TransactionDetail].
 */
@Composable
fun HomeScreen(
    onOpenTransaction: (Transaction) -> Unit,
    onTransfer: () -> Unit,
    onStatement: () -> Unit,
    onHistory: () -> Unit,
    onCards: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "balance") { BalanceCard() }
        item(key = "quick-actions") {
            QuickActions(
                onTransfer = onTransfer,
                onStatement = onStatement,
                onHistory = onHistory,
                onCards = onCards,
            )
        }
        item(key = "donut") { SpendDonut(spent = formatAmount(BankData.spentThisMonth)) }
        item(key = "section-title") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader("Transactions")
                TextButton(onClick = onHistory) { Text("See all") }
            }
        }
        items(BankData.transactions, key = { it.id }) { tx ->
            Box(modifier = Modifier.fillMaxWidth().clickable { onOpenTransaction(tx) }) {
                TransactionRow(tx)
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun BalanceCard() {
    // Reset whenever the setting is turned back on, so each demo starts hidden.
    var revealed by remember(BankSettings.hideBalances) { mutableStateOf(false) }
    val hidden = BankSettings.hideBalances && !revealed

    Card(
        onClick = { if (BankSettings.hideBalances) revealed = !revealed },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // The label stays readable to the agent; the figure and account do not.
            Text(
                if (BankSettings.hideBalances) {
                    if (revealed) "Total balance · tap to hide" else "Total balance · tap to reveal"
                } else {
                    "Total balance"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            // Redactable either way: hiding is for the person holding the phone,
            // the bar is what withholds the value from the agent.
            Redactable {
                Text(
                    if (hidden) "••••••" else formatAmount(BankData.balance),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            Redactable {
                Text(
                    "Everyday Checking •••• 4421",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** The four things a customer does most, one tap from the landing screen. */
@Composable
private fun QuickActions(
    onTransfer: () -> Unit,
    onStatement: () -> Unit,
    onHistory: () -> Unit,
    onCards: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickAction("Transfer", Icons.Rounded.SwapHoriz, Modifier.weight(1f), onTransfer)
        QuickAction("Cards", Icons.Rounded.CreditCard, Modifier.weight(1f), onCards)
        QuickAction("Statement", Icons.AutoMirrored.Rounded.ReceiptLong, Modifier.weight(1f), onStatement)
        QuickAction("History", Icons.Rounded.History, Modifier.weight(1f), onHistory)
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconBadge(icon, size = 36.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Spend-by-category ring for the current month, drawn on a [Canvas]. */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun SpendDonut(spent: String, modifier: Modifier = Modifier) {
    val shares = BankData.categoryShares
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().aspectRatio(1f).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val sideDp = maxWidth
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringStroke = size.minDimension * 0.22f
            val inset = ringStroke / 2f
            val topLeft = Offset(inset, inset)
            val arcSize = Size(size.width - ringStroke, size.height - ringStroke)
            var sweepStart = -90f
            shares.forEach { (cat, share) ->
                val sweep = 360f * share
                drawArc(
                    color = cat.color,
                    startAngle = sweepStart,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = ringStroke),
                )
                sweepStart += sweep
            }
        }
        var sweepStart = -90f
        shares.forEach { (cat, share) ->
            val mid = sweepStart + (360f * share) / 2f
            val rad = Math.toRadians(mid.toDouble())
            val rRatio = 0.36f
            Icon(
                imageVector = cat.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = (cos(rad).toFloat() * sideDp.value * rRatio).dp,
                        y = (sin(rad).toFloat() * sideDp.value * rRatio).dp,
                    )
                    .size(28.dp),
            )
            sweepStart += 360f * share
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Spent",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                spent,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "this month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One posted transaction, shared by the Home feed and the full statement history. */
@Composable
fun TransactionRow(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    sensitive: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(transaction.category.color.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                transaction.category.icon,
                contentDescription = null,
                tint = transaction.category.color,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val payee = @Composable {
                Text(
                    transaction.payee,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (sensitive) Redactable { payee() } else payee()
            Text(
                transaction.dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val amount = @Composable {
            Text(
                formatAmount(transaction.amount),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (sensitive) Redactable { amount() } else amount()
    }
}

/** The receipt behind a tapped transaction. */
@Composable
fun TransactionDetail(transaction: Transaction, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text("Transaction", style = MaterialTheme.typography.titleMedium)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(72.dp)
                        .background(transaction.category.color, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        transaction.category.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    transaction.payee,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(4.dp))
                Redactable {
                    Text(
                        formatAmount(transaction.amount),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    transaction.dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
        Column(Modifier.padding(horizontal = 20.dp)) {
            ValueRow("Category", transaction.category.label)
            ValueRow("Posted to", "Everyday Checking")
            ValueRow(
                "Reference",
                "TXN-${transaction.id.toString().padStart(8, '0')}",
                sensitive = true,
            )
            ValueRow("Merchant ID", "MID-9928-${transaction.id}")
            ValueRow("Status", "Settled")
        }
        HorizontalDivider()
    }
}

/** Shared scaffold so each preview differs only by the content it renders. */
@Composable
private fun HomePreview(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    BankTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}

@Preview(name = "1 Home", showBackground = true, heightDp = 720)
@Composable
private fun PreviewHomeScreen() = HomePreview {
    HomeScreen(
        onOpenTransaction = {},
        onTransfer = {},
        onStatement = {},
        onHistory = {},
        onCards = {},
    )
}

@Preview(name = "2 Home — dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewHomeScreenDark() = HomePreview(darkTheme = true) {
    HomeScreen(
        onOpenTransaction = {},
        onTransfer = {},
        onStatement = {},
        onHistory = {},
        onCards = {},
    )
}

@Preview(name = "3 Balance card", showBackground = true, widthDp = 400)
@Composable
private fun PreviewBalanceCard() = HomePreview {
    BalanceCard()
}

@Preview(name = "4 Quick actions", showBackground = true, widthDp = 400)
@Composable
private fun PreviewQuickActions() = HomePreview {
    Column(Modifier.padding(vertical = 12.dp)) {
        QuickActions(onTransfer = {}, onStatement = {}, onHistory = {}, onCards = {})
    }
}

// SpendDonut sizes its ring from its width, so widthDp gives the Canvas a non-zero size.
@Preview(name = "5 Spend donut", showBackground = true, widthDp = 400, heightDp = 400)
@Composable
private fun PreviewSpendDonut() = HomePreview {
    SpendDonut(spent = formatAmount(BankData.spentThisMonth))
}

@Preview(name = "6 Transaction row", showBackground = true, widthDp = 400)
@Composable
private fun PreviewTransactionRow() = HomePreview {
    TransactionRow(BankData.transactions.first())
}

@Preview(name = "7 Transaction row — negative amount", showBackground = true, widthDp = 400)
@Composable
private fun PreviewTransactionRowRefund() = HomePreview {
    TransactionRow(
        BankData.transactions.first().copy(payee = "Tutor Time — refund", amount = -187.40),
    )
}

@Preview(name = "8 Transaction row — long payee", showBackground = true, widthDp = 400)
@Composable
private fun PreviewTransactionRowLongPayee() = HomePreview {
    TransactionRow(
        BankData.transactions.first().copy(
            payee = "Tutor Time Early Learning & Childcare Centre, Oakland",
            amount = 1_234.56,
        ),
    )
}

@Preview(name = "9 Transaction detail", showBackground = true, heightDp = 720)
@Composable
private fun PreviewTransactionDetail() = HomePreview {
    TransactionDetail(transaction = BankData.transactions.first(), onBack = {})
}

@Preview(name = "10 Transaction detail — dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewTransactionDetailDark() = HomePreview(darkTheme = true) {
    TransactionDetail(
        transaction = BankData.transactions.first { it.category == Category.DINING },
        onBack = {},
    )
}
