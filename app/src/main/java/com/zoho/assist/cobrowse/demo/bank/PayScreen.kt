package com.zoho.assist.cobrowse.demo.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zoho.assist.cobrowse.demo.common.BankTheme

/** What the customer is about to send — carried through review, OTP and receipt. */
data class TransferDraft(
    val payeeName: String,
    val accountNumber: String,
    val sortCode: String,
    val amount: Double,
    val reference: String,
)

/** How the customer authorises the payment. */
private enum class AuthMethod(val label: String) {
    SECURE_KEYPAD("Secure keypad"),
    SYSTEM_KEYBOARD("System keyboard"),
}

/**
 * Pay: the transfer journey — beneficiary details, authorisation, the review and
 * one-time-passcode overlays owned by the app shell, and finally a receipt.
 *
 * Deliberately exercises every input shape: number, decimal, password with a
 * show/hide toggle, numeric password via the system IME, and a read-only field
 * fed by the in-app [SecureKeypad].
 */
@Composable
fun PayScreen(
    completed: TransferDraft?,
    onReview: (TransferDraft) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (completed != null) {
        TransferReceipt(completed, onDone, modifier)
        return
    }

    var payeeName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var sortCode by remember { mutableStateOf("20-45-19") }
    var amount by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmVisible by remember { mutableStateOf(false) }
    // Keyed on the setting: on means the keypad with no choice offered; off
    // defaults to the system keyboard, with the chips available to switch.
    val lockedToKeypad = BankSettings.alwaysSecureKeypad
    var method by remember(lockedToKeypad) {
        mutableStateOf(
            if (lockedToKeypad) AuthMethod.SECURE_KEYPAD else AuthMethod.SYSTEM_KEYBOARD,
        )
    }
    val effectiveMethod = if (lockedToKeypad) AuthMethod.SECURE_KEYPAD else method
    var pin by remember { mutableStateOf("") }

    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val ready = payeeName.isNotBlank() &&
        accountNumber.length >= 8 &&
        amountValue > 0.0 &&
        password.length >= 4 &&
        confirmPassword == password &&
        pin.length == 6

    Column(
        modifier
            .fillMaxSize()
            // No imePadding: with windowSoftInputMode=adjustNothing the keyboard
            // overlays the window; the scroll is what reaches a covered field.
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionHeader("Send money")

        Text("Saved payees", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BankData.payees, key = { it.accountNumber }) { p ->
                PayeeChip(p) {
                    payeeName = p.name
                    accountNumber = p.accountNumber.filter(Char::isDigit)
                }
            }
        }

        BankTextField(
            label = "Beneficiary name",
            value = payeeName,
            onValueChange = { payeeName = it },
        )
        BankTextField(
            label = "Account number",
            value = accountNumber,
            onValueChange = { accountNumber = it.filter(Char::isDigit).take(12) },
            keyboardType = KeyboardType.Number,
            supporting = "8–12 digits",
        )
        BankTextField(
            label = "Sort code",
            value = sortCode,
            onValueChange = { sortCode = it.take(8) },
            keyboardType = KeyboardType.Number,
        )
        BankTextField(
            label = "Amount",
            value = amount,
            onValueChange = { input ->
                // One decimal point, two decimal places.
                val cleaned = input.filter { it.isDigit() || it == '.' }
                val parts = cleaned.split('.')
                amount = when {
                    parts.size <= 1 -> cleaned.take(9)
                    else -> parts[0].take(9) + "." + parts[1].take(2)
                }
            },
            keyboardType = KeyboardType.Decimal,
            supporting = if (amountValue > 0) "Sending ${formatAmount(amountValue)}" else null,
        )
        BankTextField(
            label = "Reference",
            value = reference,
            onValueChange = { reference = it.take(24) },
            supporting = "Shown on the beneficiary's statement",
        )
        BankTextField(
            label = "Transaction password",
            value = password,
            onValueChange = { password = it },
            keyboardType = KeyboardType.Password,
            supporting = "Screen sharing pauses while you type",
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailing = {
                androidx.compose.material3.TextButton(
                    onClick = { passwordVisible = !passwordVisible },
                ) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
            sensitive = true,
        )
        // The other approach: sharing carries on while the customer types and
        // only the field's value is masked, from the first character until it
        // is cleared.
        BankTextField(
            label = "Confirm transaction password",
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            keyboardType = KeyboardType.Password,
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            supporting = if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                "Doesn't match"
            } else {
                "Only this field is masked while it has text"
            },
            trailing = {
                androidx.compose.material3.TextButton(
                    onClick = { confirmVisible = !confirmVisible },
                ) {
                    Text(if (confirmVisible) "Hide" else "Show")
                }
            },
            sensitive = true,
            pauseWhileTyping = false,
        )

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        Text(
            "Authorise with your 6-digit PIN",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthMethod.entries.forEach { m ->
                FilterChip(
                    selected = effectiveMethod == m,
                    enabled = !lockedToKeypad,
                    onClick = { method = m; pin = "" },
                    label = { Text(m.label) },
                    leadingIcon = {
                        Icon(
                            if (m == AuthMethod.SECURE_KEYPAD) {
                                Icons.Rounded.Dialpad
                            } else {
                                Icons.Rounded.Keyboard
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        when (effectiveMethod) {
            AuthMethod.SECURE_KEYPAD -> {
                InfoNote(
                    "The secure keypad gives NO press feedback and its digit layout is " +
                        "scrambled every time it opens, so an onlooker can see it in use " +
                        "without being able to read your PIN off the tap positions.",
                )
                // Read-only → tapping it opens no soft keyboard; the keypad below
                // is the only way in.
                PinField(pin, readOnly = true)
                SecureKeypad(
                    onDigit = { d -> if (pin.length < 6) pin += d },
                    onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                    onClear = { pin = "" },
                )
            }
            AuthMethod.SYSTEM_KEYBOARD -> {
                InfoNote("Uses the system number-password keyboard instead of the in-app keypad.")
                PinField(
                    pin,
                    readOnly = false,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                )
            }
        }

        // What the agent sees: PIN and transaction password are barred once they
        // hold a value, amount and payee never are, so the agent can still help
        // with the payment. (While the customer types in them the agent sees the
        // privacy placeholder instead of the whole screen.)
        AgentPreview {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ValueRow("Amount", formatAmount(amountValue), emphasis = true)
                ValueRow("To", payeeName.ifBlank { "—" })
                BankTextField(
                    label = "Transaction password",
                    value = password,
                    onValueChange = {},
                    readOnly = true,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    sensitive = true,
                )
                PinField(pin, readOnly = true)
            }
        }

        Button(
            onClick = {
                onReview(
                    TransferDraft(
                        payeeName = payeeName.trim(),
                        accountNumber = accountNumber,
                        sortCode = sortCode,
                        amount = amountValue,
                        reference = reference.ifBlank { "Transfer" },
                    ),
                )
            },
            enabled = ready,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Review transfer")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PayeeChip(payee: Payee, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    payee.name.take(1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.size(8.dp))
            Column {
                Text(payee.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    payee.bank,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PinField(
    pin: String,
    readOnly: Boolean,
    onValueChange: (String) -> Unit = {},
) {
    // Sharing is not paused while the customer types: the agent keeps the live
    // screen and only the PIN box is masked.
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("PIN", style = MaterialTheme.typography.labelMedium)
        // Masked once it holds a digit, the whole field: the dots and the
        // counter both give away the PIN length. Empty, it shows no bar.
        Redactable(masked = pin.isNotEmpty()) {
            OutlinedTextField(
                value = pin,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                ),
                visualTransformation = PasswordVisualTransformation(),
                supportingText = { Text("${pin.length} / 6") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The in-app PIN keypad, used instead of the system soft keyboard. Keys render
 * no press feedback and the digit order is scrambled each time it appears, so
 * neither a tap nor a remembered position reveals a digit.
 *
 * Only the digit labels are redacted: the agent sees a keypad of blank keys in
 * use, so they can still guide the customer to it, but cannot read the digits.
 * With the order scrambled and unreadable, a tap position maps to no digit.
 * C and ⌫ stay visible; they reveal nothing about the PIN.
 */
@Composable
private fun SecureKeypad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onClear: () -> Unit) {
    val digits = remember { ('0'..'9').toList().shuffled() }
    val rows = listOf(
        listOf(digits[0], digits[1], digits[2]),
        listOf(digits[3], digits[4], digits[5]),
        listOf(digits[6], digits[7], digits[8]),
        listOf('C', digits[9], '⌫'),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    KeypadKey(key, Modifier.weight(1f)) {
                        when (key) {
                            'C' -> onClear()
                            '⌫' -> onDelete()
                            else -> onDigit(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(label: Char, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // no ripple → a tap doesn't reveal which key was pressed
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        val text = @Composable { Text(label.toString(), style = MaterialTheme.typography.titleLarge) }
        if (label.isDigit()) Redactable { text() } else text()
    }
}

/** Shown after the passcode is verified. */
@Composable
private fun TransferReceipt(draft: TransferDraft, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Text(
            "Transfer sent",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            formatAmount(draft.amount),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth()) {
            ValueRow("To", draft.payeeName)
            ValueRow("Account", "•••• ${draft.accountNumber.takeLast(4)}")
            ValueRow("Sort code", draft.sortCode)
            ValueRow("Reference", draft.reference)
            ValueRow("Status", "Completed")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Make another transfer")
        }
    }
}

// The transfer overlays are rendered by the app shell, in-window (scrim plus
// Surface) rather than as an AlertDialog or Popup(), which would add a second window.

/** Step 2 of the journey: confirm what is about to be sent. */
@Composable
fun BoxScope.TransferReviewDialog(
    draft: TransferDraft,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayScrim(alpha = 0.5f, onDismiss = onCancel)
    androidx.compose.material3.Surface(
        modifier = Modifier.align(Alignment.Center).padding(32.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Confirm transfer",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(8.dp))
            ValueRow("Amount", formatAmount(draft.amount), emphasis = true)
            ValueRow("To", draft.payeeName)
            ValueRow("Account", "•••• ${draft.accountNumber.takeLast(4)}")
            ValueRow("Sort code", draft.sortCode)
            ValueRow("Reference", draft.reference)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                androidx.compose.material3.TextButton(onClick = onCancel) { Text("Cancel") }
                androidx.compose.material3.TextButton(onClick = onConfirm) { Text("Send") }
            }
        }
    }
}

/** Step 3: the one-time passcode the bank "sent" to the registered device. */
@Composable
fun BoxScope.OtpOverlay(onCancel: () -> Unit, onVerify: () -> Unit) {
    OverlayScrim(alpha = 0.4f, onDismiss = onCancel)
    androidx.compose.material3.Surface(
        modifier = Modifier.align(Alignment.Center).padding(40.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "One-time passcode",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                "Sent to •••• 8842",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            // The code authorises the transfer, so the agent must never read it.
            Redactable {
                Text(
                    "482 913",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.TextButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onVerify) { Text("Verify") }
            }
        }
    }
}

/** Shared scaffold so each preview differs only by the content it renders. */
@Composable
private fun PayPreview(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    BankTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}

/** A sent transfer, built from a real saved payee rather than an invented one. */
private val SampleDraft = BankData.payees.first().let { payee ->
    TransferDraft(
        payeeName = payee.name,
        accountNumber = payee.accountNumber.filter(Char::isDigit),
        sortCode = "20-45-19",
        amount = 250.00,
        reference = "Rent share",
    )
}

@Preview(name = "1 Pay screen", showBackground = true, heightDp = 720)
@Composable
private fun PreviewPayScreen() = PayPreview {
    PayScreen(completed = null, onReview = {}, onDone = {})
}

@Preview(name = "2 Pay screen — dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewPayScreenDark() = PayPreview(darkTheme = true) {
    PayScreen(completed = null, onReview = {}, onDone = {})
}

@Preview(name = "3 Payee chip", showBackground = true, widthDp = 400)
@Composable
private fun PreviewPayeeChip() = PayPreview {
    Column(Modifier.padding(16.dp)) {
        PayeeChip(BankData.payees.first(), onClick = {})
    }
}

@Preview(name = "4 Payee chip — long name", showBackground = true, widthDp = 400)
@Composable
private fun PreviewPayeeChipLongName() = PayPreview {
    Column(Modifier.padding(16.dp)) {
        PayeeChip(
            BankData.payees.first { it.name.length > 12 }.copy(
                bank = "Civic Union Bank — Cheapside branch",
            ),
            onClick = {},
        )
    }
}

@Preview(name = "5 PIN field — empty", showBackground = true, widthDp = 400)
@Composable
private fun PreviewPinFieldEmpty() = PayPreview {
    Column(Modifier.padding(16.dp)) {
        PinField(pin = "", readOnly = true)
    }
}

@Preview(name = "6 PIN field — partly entered", showBackground = true, widthDp = 400)
@Composable
private fun PreviewPinFieldPartial() = PayPreview {
    Column(Modifier.padding(16.dp)) {
        PinField(pin = "417", readOnly = true)
    }
}

@Preview(name = "7 PIN field — complete, editable", showBackground = true, widthDp = 400)
@Composable
private fun PreviewPinFieldComplete() = PayPreview {
    Column(Modifier.padding(16.dp)) {
        PinField(pin = "417238", readOnly = false, onValueChange = {})
    }
}

// The keypad shuffles its digits on each composition, so the layout changes per rebuild.
@Preview(name = "8 Secure keypad", showBackground = true, widthDp = 400)
@Composable
private fun PreviewSecureKeypad() = PayPreview {
    Column(Modifier.padding(16.dp)) {
        SecureKeypad(onDigit = {}, onDelete = {}, onClear = {})
    }
}

@Preview(name = "9 Keypad keys", showBackground = true, widthDp = 400)
@Composable
private fun PreviewKeypadKey() = PayPreview {
    Row(
        Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeypadKey('7', Modifier.weight(1f)) {}
        KeypadKey('C', Modifier.weight(1f)) {}
        KeypadKey('⌫', Modifier.weight(1f)) {}
    }
}

@Preview(name = "10 Transfer receipt", showBackground = true, heightDp = 720)
@Composable
private fun PreviewTransferReceipt() = PayPreview {
    TransferReceipt(draft = SampleDraft, onDone = {})
}

@Preview(name = "11 Transfer receipt — large amount", showBackground = true, heightDp = 720)
@Composable
private fun PreviewTransferReceiptLargeAmount() = PayPreview {
    TransferReceipt(
        draft = SampleDraft.copy(amount = 12_450.75, reference = "Deposit on 14B"),
        onDone = {},
    )
}

// TransferReviewDialog and OtpOverlay are BoxScope-scoped, so previews supply the Box.
@Preview(name = "12 Transfer review dialog", showBackground = true, heightDp = 720)
@Composable
private fun PreviewTransferReviewDialog() = PayPreview {
    Box(Modifier.fillMaxSize()) {
        TransferReviewDialog(draft = SampleDraft, onCancel = {}, onConfirm = {})
    }
}

@Preview(name = "13 Transfer review dialog — dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewTransferReviewDialogDark() = PayPreview(darkTheme = true) {
    Box(Modifier.fillMaxSize()) {
        TransferReviewDialog(draft = SampleDraft, onCancel = {}, onConfirm = {})
    }
}

@Preview(name = "14 OTP overlay", showBackground = true, heightDp = 720)
@Composable
private fun PreviewOtpOverlay() = PayPreview {
    Box(Modifier.fillMaxSize()) {
        OtpOverlay(onCancel = {}, onVerify = {})
    }
}

@Preview(name = "15 OTP overlay — dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewOtpOverlayDark() = PayPreview(darkTheme = true) {
    Box(Modifier.fillMaxSize()) {
        OtpOverlay(onCancel = {}, onVerify = {})
    }
}
