package com.zoho.assist.cobrowse.demo.bank

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.zoho.assist.cobrowse.demo.CobrowseController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zoho.assist.cobrowse.demo.common.BankTheme

/**
 * More: the profile header plus everything that isn't day-to-day banking —
 * the e-statement, the full transaction history, live markets, the security
 * settings and reporting a problem to support.
 */
@Composable
fun MoreScreen(onOpen: (BankRoute) -> Unit, modifier: Modifier = Modifier) {
    val entries = listOf(
        Triple(BankRoute.Statement, "e-Statement", Icons.AutoMirrored.Rounded.ReceiptLong),
        Triple(BankRoute.History, "Transaction history", Icons.Rounded.History),
        Triple(BankRoute.Markets, "Live markets", Icons.AutoMirrored.Rounded.ShowChart),
        Triple(BankRoute.Security, "Security & privacy", Icons.Rounded.Security),
        Triple(BankRoute.ReportProblem, "Report a problem", Icons.Rounded.BugReport),
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "profile") { ProfileHeader() }
        item(key = "appearance") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOption.entries.forEach { t ->
                        FilterChip(
                            selected = BankSettings.theme == t,
                            onClick = { BankSettings.theme = t },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        }
        items(entries, key = { it.second }) { (route, title, icon) ->
            MoreRow(title = title, icon = icon, onClick = { onOpen(route) })
        }
        item(key = "version") {
            Text(
                "${BankData.BANK_NAME} · version 1.0",
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileHeader() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        androidx.compose.foundation.shape.CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    BankData.CUSTOMER_NAME.split(" ").map { it.first() }.joinToString(""),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(
                    BankData.CUSTOMER_NAME,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    "Customer ID 4421-9928",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MoreRow(title: String, icon: ImageVector, onClick: () -> Unit) {
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
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, size = 44.dp)
            Spacer(Modifier.size(14.dp))
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The statement as in-memory HTML, loaded into a real [WebView] so the screen
 * works with no network.
 *
 * Sensitive values carry `class="redact"`; the stylesheet turns them into black
 * bars when `mask-fields` is set on `<body>`, so the bars scroll and reflow with
 * the content they cover.
 */
private const val STATEMENT_HTML =
    "<!DOCTYPE html><html><head>" +
        "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
        "<style>" +
        "body{font-family:sans-serif;padding:16px;font-size:15px}" +
        ".redact{border-radius:3px;padding:0 3px}" +
        "body.mask-fields .redact{background:#111;color:transparent}" +
        "</style></head>" +
        "<body>" +
        "<h2>Account statement</h2>" +
        "<p><b>Account holder:</b> <span class='redact'>Alex Morgan</span></p>" +
        "<p><b>Account:</b> <span class='redact'>Everyday Checking &bull;&bull;&bull;&bull; 4421</span></p>" +
        "<p><b>Card:</b> <span class='redact'>4111 1111 1111 1111</span></p>" +
        "<p><b>Statement period:</b> 1 &ndash; 30 April</p>" +
        "<hr>" +
        "<table style='width:100%;border-collapse:collapse'>" +
        "<tr><th align='left'>Date</th><th align='left'>Payee</th><th align='right'>Amount</th></tr>" +
        "<tr><td>14 Apr</td><td>Tutor Time</td><td align='right'>$187.40</td></tr>" +
        "<tr><td>05 Apr</td><td>Comcast</td><td align='right'>$110.50</td></tr>" +
        "<tr><td>05 Apr</td><td>Tesco</td><td align='right'>$186.22</td></tr>" +
        "<tr><td>03 Apr</td><td>Starbucks</td><td align='right'>$6.85</td></tr>" +
        "</table>" +
        "<hr>" +
        "<p><b>Closing balance:</b> <span class='redact'>$12,480.55</span></p>" +
        "<p><b>Tax ID on file:</b> <span class='redact'>123-45-6789</span></p>" +
        "</body></html>"

/** Pushes a redaction class onto the document's `<body>`. */
private fun WebView.applyBodyClass(bodyClass: String) {
    evaluateJavascript("document.body.className='$bodyClass';", null)
}

@Composable
fun StatementScreen(modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf(StatementRedaction.FIELDS) }

    // The SDK masks DOM nodes matching these selectors inside a wrapped WebView,
    // on the agent's side only.
    LaunchedEffect(mode) {
        when (mode) {
            StatementRedaction.NONE -> CobrowseController.setWebViewRedactedSelectors()
            StatementRedaction.FIELDS ->
                CobrowseController.setWebViewRedactedSelectors(".redact")
            // Whole page uses a native region over the WebView instead: a CSS
            // selector would let the page paint before the injection runs.
            StatementRedaction.WHOLE_PAGE -> CobrowseController.setWebViewRedactedSelectors()
        }
    }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoNote(
            "Redaction applies to the SESSION VIEWER only — your own statement below is " +
                "never masked, whichever mode is selected.",
        )

        Text("What the agent sees", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatementRedaction.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.label) },
                )
            }
        }
        Text(
            mode.blurb,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The customer's own view, always unmasked locally. In WHOLE_PAGE the
        // native region over this WebView bars only the agent's copy.
        //
        // Keyed on the mode because the SDK freezes the selector list into the
        // script it injects on page load; rebuilding the WebView makes it
        // re-inject with the current selectors.
        key(mode) {
            val statement = @Composable {
                StatementWebView(
                    bodyClass = "",
                    modifier = Modifier.fillMaxWidth().fillMaxSize(),
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (mode == StatementRedaction.WHOLE_PAGE) {
                    Redactable { statement() }
                } else {
                    statement()
                }
            }
        }

        // A local illustration of the same document as the agent would get it.
        Column(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Support agent's screen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.fillMaxSize()) {
                key(mode) {
                    StatementWebView(
                        bodyClass = if (mode == StatementRedaction.FIELDS) "mask-fields" else "",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (mode == StatementRedaction.WHOLE_PAGE) {
                    Box(Modifier.fillMaxSize().background(MaskSurface))
                }
            }
        }
    }
}

/** The three ways this demo can hand the statement to a support agent. */
enum class StatementRedaction(val label: String, val blurb: String) {
    NONE(
        "Nothing hidden",
        "The agent reads the statement exactly as you see it.",
    ),
    FIELDS(
        "Sensitive fields",
        "Name, account, card number, balance and tax ID are barred; the rest is readable.",
    ),
    WHOLE_PAGE(
        "Whole page",
        "The entire statement is covered on the agent's side.",
    ),
}

@Composable
private fun StatementWebView(bodyClass: String, modifier: Modifier = Modifier) {
    // The WebViewClient outlives a recomposition, so it reads the class through
    // a holder rather than capturing the value it was built with.
    val current = rememberUpdatedState(bodyClass)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                @Suppress("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                // Wrapped so the SDK masks the document's `redact` nodes in
                // the captured frame. The client below still runs.
                webViewClient = redactableWebViewClient(
                    object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            // The first `update` can run before the document
                            // exists, so the class is re-applied on load too.
                            view.applyBodyClass(current.value)
                        }
                    },
                )
                loadDataWithBaseURL(null, STATEMENT_HTML, "text/html", "utf-8", null)
            }
        },
        update = { it.applyBodyClass(bodyClass) },
    )
}

private enum class Sort(val label: String) {
    NEWEST("Newest"),
    AMOUNT("Amount"),
    PAYEE("Payee"),
}

/**
 * The full posted history: 1000 keyed rows in a `LazyColumn`.
 *
 * Re-sorting replaces the whole list in one state write; an in-place
 * `SnapshotStateList.sort()` would do ~1000 writes per tap and make rows flicker.
 */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    var sort by remember { mutableStateOf(Sort.NEWEST) }
    var rows by remember { mutableStateOf(BankData.history) }
    // As on Accounts: every masked row is a region the SDK tracks, so turning
    // this off makes the long list scroll more smoothly.
    var redactValues by remember { mutableStateOf(true) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Sort.entries.forEach { s ->
                FilterChip(
                    selected = sort == s,
                    onClick = {
                        sort = s
                        rows = when (s) {
                            Sort.NEWEST -> BankData.history
                            Sort.AMOUNT -> BankData.history.sortedByDescending { it.amount }
                            Sort.PAYEE -> BankData.history.sortedBy { it.payee }
                        }
                    },
                    label = { Text(s.label) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${rows.size} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (redactValues) {
                        "Payee and amount are barred on the agent's side."
                    } else {
                        "The agent sees every payee and amount."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = redactValues, onCheckedChange = { redactValues = it })
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(rows, key = { it.id }) { tx ->
                TransactionRow(tx, sensitive = redactValues)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

/**
 * A continuously animated screen: the live-price ticker keeps running for as
 * long as it is on screen, which makes it the place to watch how the app
 * behaves across a Home/lock transition and back.
 */
@Composable
fun MarketsScreen(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "markets")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "tick",
    )
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoNote("Prices refresh continuously while this screen is open.")
        Column(Modifier.fillMaxWidth()) {
            ValueRow("Live portfolio value", formatAmount(8_317.42 + t * 40), emphasis = true)
        }
        // Moving marker, so the animation is obvious across a lifecycle transition.
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(
                Modifier
                    .offset { IntOffset((t * 600).toInt(), 0) }
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            )
        }
        HorizontalDivider()
        BankData.tickers.forEach { ticker ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        ticker.symbol,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        ticker.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    // Public quotes, so this is a policy choice; symbol and name
                    // stay readable to the agent.
                    Redactable {
                        Text(
                            formatAmount(ticker.price + t * ticker.changePct),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Redactable {
                        Text(
                            "%+.2f%%".format(ticker.changePct),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ticker.changePct < 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityScreen(modifier: Modifier = Modifier) {

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InfoNote(
            "${BankData.BANK_NAME} staff will never ask for your PIN, transaction " +
                "password or a one-time passcode.",
        )
        Spacer(Modifier.height(8.dp))
        SettingRow(
            "Always use the secure keypad",
            "Forces PIN entry through the in-app scrambled keypad. Turn it off to " +
                "authorise with the system keyboard instead.",
            BankSettings.alwaysSecureKeypad,
        ) { BankSettings.alwaysSecureKeypad = it }
        SettingRow(
            "Hide balances",
            "Balances show as dots until you tap them. Revealing one shows it on " +
                "this device only — an agent still sees a bar.",
            BankSettings.hideBalances,
        ) { BankSettings.hideBalances = it }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Shared scaffold so each preview differs only by the content it renders. */
@Composable
private fun MorePreview(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    BankTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}

@Preview(name = "1 More", showBackground = true, heightDp = 720)
@Composable
private fun PreviewMoreScreen() = MorePreview {
    MoreScreen(onOpen = {})
}

@Preview(name = "2 More — dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewMoreScreenDark() = MorePreview(darkTheme = true) {
    MoreScreen(onOpen = {})
}

@Preview(name = "3 Profile header", showBackground = true, widthDp = 400)
@Composable
private fun PreviewProfileHeader() = MorePreview {
    Column(Modifier.padding(16.dp)) {
        ProfileHeader()
    }
}

@Preview(name = "4 More row", showBackground = true, widthDp = 400)
@Composable
private fun PreviewMoreRow() = MorePreview {
    Column(Modifier.padding(16.dp)) {
        MoreRow(
            title = "Transaction history",
            icon = Icons.Rounded.History,
            onClick = {},
        )
    }
}

// The render pane has no WebView, so both statement panes come up blank; only
// the surrounding chrome is previewed.
@Preview(name = "5 e-Statement", showBackground = true, heightDp = 720)
@Composable
private fun PreviewStatementScreen() = MorePreview {
    StatementScreen()
}

@Preview(name = "6 Transaction history", showBackground = true, heightDp = 720)
@Composable
private fun PreviewHistoryScreen() = MorePreview {
    HistoryScreen()
}

@Preview(name = "7 Live markets — gains and losses", showBackground = true, heightDp = 720)
@Composable
private fun PreviewMarketsScreen() = MorePreview {
    MarketsScreen()
}

@Preview(name = "8 Security & privacy", showBackground = true, heightDp = 720)
@Composable
private fun PreviewSecurityScreen() = MorePreview {
    SecurityScreen()
}

@Preview(name = "9 Setting row — on", showBackground = true, widthDp = 400)
@Composable
private fun PreviewSettingRowOn() = MorePreview {
    Column(Modifier.padding(16.dp)) {
        SettingRow(
            title = "Always use the secure keypad",
            subtitle = "Enter your PIN on the in-app keypad instead of the system keyboard.",
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(name = "10 Setting row — off", showBackground = true, widthDp = 400)
@Composable
private fun PreviewSettingRowOff() = MorePreview {
    Column(Modifier.padding(16.dp)) {
        SettingRow(
            title = "Hide balances on launch",
            subtitle = "Balances stay masked until you tap to reveal them.",
            checked = false,
            onCheckedChange = {},
        )
    }
}
