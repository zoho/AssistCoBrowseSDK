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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import com.zoho.assist.cobrowse.demo.common.BankTheme

/**
 * Small pieces shared by more than one banking screen. Anything used by a
 * single screen stays private to that screen's file.
 */

/** "label ………… value" row, the shape used by every detail panel and sheet. */
@Composable
fun ValueRow(
    label: String,
    value: String,
    emphasis: Boolean = false,
    sensitive: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Only the value is redacted, never the label, so the agent still sees
        // which field is being withheld.
        val text = @Composable {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (emphasis) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        if (sensitive) Redactable { text() } else text()
    }
}

/** Bold section heading above a list or card group. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
}

/**
 * A tonal note attached to a screen — the bank's own guidance to the customer
 * ("we never ask for your PIN", "statements are generated overnight"), held in
 * a container so it reads as guidance rather than as body copy competing with
 * the content below it.
 */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Labeled single-line input — every form field in the app is one of these. */
@Composable
fun BankTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    // Masks the value for the agent while the field holds one.
    sensitive: Boolean = false,
    // With [sensitive]: also pause sharing while the customer types into it, so
    // the agent sees the privacy placeholder instead of the screen. Off, the
    // agent keeps the live screen with only the field's value masked.
    pauseWhileTyping: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    if (sensitive && pauseWhileTyping) PauseSharingWhileTyping(focused, readOnly)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Outside the redaction on purpose, same rule as ValueRow: the agent
        // sees the field's label, just not its value.
        Text(label, style = MaterialTheme.typography.labelMedium)
        val field = @Composable {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            trailingIcon = trailing,
            supportingText = supporting?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
        }
        if (sensitive) Redactable(masked = value.isNotEmpty()) { field() } else field()
    }
}

/** Round tonal badge holding an icon — used by list rows and quick actions. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    size: androidx.compose.ui.unit.Dp = 40.dp,
) {
    Box(
        modifier = Modifier.size(size).background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * The dimmed backdrop behind the in-window overlays (transfer confirmation,
 * one-time passcode, account-details sheet). They use this scrim plus a
 * `Surface` rather than `Popup()`, `AlertDialog` or `ModalBottomSheet`, each of
 * which would add a separate window.
 */
@Composable
fun BoxScope.OverlayScrim(alpha: Float = 0.45f, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = alpha))
            // No ripple: the scrim is a dismiss target, not a pressable control.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDismiss() },
    )
}

/** Shared scaffold so each preview differs only by the content it renders. */
@Composable
private fun UiPreview(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    BankTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Preview(name = "1 ValueRow", showBackground = true, widthDp = 400)
@Composable
private fun PreviewValueRow() = UiPreview {
    ValueRow("Balance", "$2,786.57", emphasis = true)
    ValueRow("Account #", "•••• 4421")
    ValueRow("IBAN", "GB29 ASBK 2045 1900 0000 00")
}

@Preview(name = "2 SectionHeader", showBackground = true, widthDp = 400)
@Composable
private fun PreviewSectionHeader() = UiPreview { SectionHeader("Accounts") }

@Preview(name = "3 InfoNote", showBackground = true, widthDp = 400)
@Composable
private fun PreviewInfoNote() = UiPreview {
    InfoNote("Card numbers, CVV and PINs stay masked while you share your screen.")
}

@Preview(name = "4 BankTextField — states", showBackground = true, widthDp = 400)
@Composable
private fun PreviewBankTextField() = UiPreview {
    BankTextField("Payee name", "Ada Lovelace", onValueChange = {})
    Spacer(Modifier.size(12.dp))
    BankTextField(
        "Amount",
        "125.00",
        onValueChange = {},
        keyboardType = KeyboardType.Decimal,
        supporting = "Available: $2,786.57",
    )
    Spacer(Modifier.size(12.dp))
    BankTextField(
        "PIN",
        "4821",
        onValueChange = {},
        visualTransformation = PasswordVisualTransformation(),
    )
    Spacer(Modifier.size(12.dp))
    BankTextField("Sort code", "20-45-19", onValueChange = {}, readOnly = true)
}

@Preview(name = "5 BankTextField — dark", showBackground = true, widthDp = 400)
@Composable
private fun PreviewBankTextFieldDark() = UiPreview(darkTheme = true) {
    BankTextField("Payee name", "Ada Lovelace", onValueChange = {})
    Spacer(Modifier.size(12.dp))
    BankTextField(
        "PIN",
        "4821",
        onValueChange = {},
        visualTransformation = PasswordVisualTransformation(),
    )
}

@Preview(name = "6 IconBadge — sizes and colours", showBackground = true, widthDp = 400)
@Composable
private fun PreviewIconBadge() = UiPreview {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Icons.Rounded.Savings)
        IconBadge(Icons.Rounded.Lock, size = 56.dp)
        IconBadge(
            Icons.Rounded.Info,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            container = MaterialTheme.colorScheme.errorContainer,
            size = 28.dp,
        )
    }
}

// OverlayScrim has a BoxScope receiver, so its preview has to supply the Box.
@Preview(name = "7 OverlayScrim", showBackground = true, heightDp = 300, widthDp = 400)
@Composable
private fun PreviewOverlayScrim() = BankTheme(darkTheme = false) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                SectionHeader("Behind the scrim")
                ValueRow("Balance", "$2,786.57", emphasis = true)
            }
            OverlayScrim(onDismiss = {})
        }
    }
}
