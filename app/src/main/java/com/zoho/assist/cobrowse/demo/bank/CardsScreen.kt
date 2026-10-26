package com.zoho.assist.cobrowse.demo.bank

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoho.assist.cobrowse.demo.common.BankTheme

/**
 * Cards: one tile per card, with a control to reveal the full number (hidden by
 * default). The card number and CVV are [Redactable], so they are barred in the
 * agent's copy while staying legible to the customer.
 */
@Composable
fun CardsScreen(modifier: Modifier = Modifier) {
    // Per-card UI state, keyed by nickname so it survives a scroll that
    // recycles the tile.
    val revealed = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "cards-title") { SectionHeader("Cards") }
        items(BankData.cards, key = { it.nickname }) { card ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CardTile(card, revealed = revealed[card.nickname] == true)
                CardControls(
                    revealed = revealed[card.nickname] == true,
                    onToggleReveal = { revealed[card.nickname] = revealed[card.nickname] != true },
                )
            }
        }
        item(key = "cards-note") {
            InfoNote(
                "Card numbers stay hidden until you reveal them. Revealing one shows it " +
                    "on this device only — during a screen share the agent still sees a bar " +
                    "over the number and the CVV. ${BankData.BANK_NAME} will never ask you " +
                    "to read a full card number aloud.",
            )
        }
    }
}

/** `4929 8821 6440 1170` → `•••• •••• •••• 1170`. */
private fun maskNumber(number: String): String {
    val groups = number.split(" ")
    if (groups.size < 2) return number
    return groups.dropLast(1).joinToString(" ") { "•".repeat(it.length) } + " " + groups.last()
}

@Composable
private fun CardTile(card: PaymentCard, revealed: Boolean) {
    // heightIn(min=), not a fixed height, so the tile grows with a larger font scale.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(card.gradient))
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    card.nickname,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    card.network,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            Spacer(Modifier.height(36.dp))
            Redactable {
                Text(
                    if (revealed) card.number else maskNumber(card.number),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                    ),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LabeledField("EXPIRY", card.expiry)
                Redactable {
                    LabeledField("CVV", if (revealed) card.cvv else "•".repeat(card.cvv.length))
                }
            }
        }
    }
}

@Composable
private fun CardControls(revealed: Boolean, onToggleReveal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (revealed) {
                    "Visible to you only — the agent still sees a bar"
                } else {
                    "Tap to reveal the full number on this device"
                },
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onToggleReveal) {
                Text(if (revealed) "Hide number" else "Show number")
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String) {
    Column {
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            value,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

/** Shared scaffold so each preview differs only by the content it renders. */
@Composable
private fun CardsPreview(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    BankTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}

@Preview(name = "1 Cards list", showBackground = true, heightDp = 720)
@Composable
private fun PreviewCardsScreen() = CardsPreview {
    CardsScreen()
}

@Preview(name = "2 Cards list — dark", showBackground = true, heightDp = 720)
@Composable
private fun PreviewCardsScreenDark() = CardsPreview(darkTheme = true) {
    CardsScreen()
}

@Preview(name = "3 Card tile — masked", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCardTileMasked() = CardsPreview {
    Column(Modifier.padding(16.dp)) {
        CardTile(BankData.cards.first(), revealed = false)
    }
}

@Preview(name = "4 Card tile — revealed", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCardTileRevealed() = CardsPreview {
    Column(Modifier.padding(16.dp)) {
        CardTile(BankData.cards.first(), revealed = true)
    }
}

@Preview(name = "5 Card tile — Mastercard", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCardTileMastercard() = CardsPreview {
    Column(Modifier.padding(16.dp)) {
        CardTile(BankData.cards.first { it.network == "MASTERCARD" }, revealed = true)
    }
}

@Preview(name = "6 Card tile — Amex, 4-digit CVV", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCardTileAmex() = CardsPreview {
    Column(Modifier.padding(16.dp)) {
        CardTile(BankData.cards.first { it.network == "AMEX" }, revealed = false)
    }
}

@Preview(name = "7 Card controls — default", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCardControls() = CardsPreview {
    Column(Modifier.padding(16.dp)) {
        CardControls(
            revealed = false,
            onToggleReveal = {},
        )
    }
}

@Preview(name = "8 Card controls — revealed", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCardControlsRevealed() = CardsPreview {
    Column(Modifier.padding(16.dp)) {
        CardControls(
            revealed = true,
            onToggleReveal = {},
        )
    }
}

// LabeledField draws white text, so the preview supplies the card gradient behind it.
@Preview(name = "9 Labeled field", showBackground = true, widthDp = 400)
@Composable
private fun PreviewLabeledField() = CardsPreview {
    val card = BankData.cards.first()
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(card.gradient))
            .padding(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LabeledField("EXPIRY", card.expiry)
            LabeledField("CVV", card.cvv)
        }
    }
}
