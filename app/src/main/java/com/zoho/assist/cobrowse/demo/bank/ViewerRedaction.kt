package com.zoho.assist.cobrowse.demo.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zoho.assist.cobrowse.demo.common.BankTheme
import android.webkit.WebViewClient
import com.zoho.assist.cobrowse.CobrowseSession
import com.zoho.assist.cobrowse.masking.redactable
import com.zoho.assist.cobrowse.demo.CobrowseController

/**
 * Viewer-side redaction.
 *
 *  - `Modifier.redactable()` (applied by [Redactable]) is the real masking: the
 *    SDK blacks that rectangle out of the captured bitmap before encode, so the
 *    agent's copy never contains the value.
 *  - [AgentPreview] is only a local, on-device illustration of what the agent sees.
 *
 * Redaction never changes what the customer sees, and every sensitive region goes
 * through [Redactable] so the SDK is wired in one place. The statement's WebView
 * uses the document-level path instead: see [redactableWebViewClient].
 */

/**
 * Wraps [delegate] so the SDK injects its DOM-masking JavaScript on every page
 * load, blacking out nodes matching the selectors registered via
 * `CobrowseController` (here, the statement's `redact` class).
 *
 * Both halves are required: the selector does nothing without a wrapped client,
 * and vice versa. [delegate]'s own callbacks still run.
 */
fun redactableWebViewClient(delegate: WebViewClient): WebViewClient =
    CobrowseSession.INSTANCE.wrapWebViewClient(delegate)

/**
 * Pauses sharing while the customer types into a sensitive field: the viewer sees
 * the SDK's privacy placeholder, so neither the Show toggle nor the keyboard's
 * last-character preview reaches the agent. Sharing resumes when the field loses
 * focus or the keyboard closes; the field's value is masked by [Redactable] from
 * then on.
 *
 * "Typing" is focused with the soft keyboard up. The IME inset is reported on
 * API 30+ even with adjustNothing (that suppresses resizing, not insets); below
 * it there is no reliable inset, so an editable field pauses on focus alone. A
 * read-only field opens no keyboard and never pauses.
 */
@Composable
fun PauseSharingWhileTyping(focused: Boolean, readOnly: Boolean) {
    var pausedByThis by remember { mutableStateOf(false) }
    val imeUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val keyboardKnown = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val shouldPause = focused && !readOnly && (imeUp || !keyboardKnown)

    LaunchedEffect(shouldPause) {
        if (shouldPause && !pausedByThis) {
            pausedByThis = true
            CobrowseController.pauseForSensitiveInput()
        } else if (!shouldPause && pausedByThis) {
            pausedByThis = false
            CobrowseController.resumeAfterSensitiveInput()
        }
    }
    // Leaving the screen with the field focused must not leave sharing paused.
    DisposableEffect(Unit) {
        onDispose {
            if (pausedByThis) {
                pausedByThis = false
                CobrowseController.resumeAfterSensitiveInput()
            }
        }
    }
}

/** True only while composing the agent's copy of a screen (inside [AgentPreview]). */
val LocalAgentView = compositionLocalOf { false }

// Deliberately not a scheme colour: it means "withheld" in light and dark alike,
// and matches the statement stylesheet's #111 bars.
internal val MaskSurface = Color(0xFF101010)

/**
 * Marks its content as sensitive. In the customer's own view this is just its
 * content; inside an [AgentPreview] it is covered by a bar of exactly the same
 * size, so the agent still sees the layout — a field is there, it has a value,
 * the value is withheld.
 *
 * [masked] false leaves the content visible to the agent; sensitive fields pass
 * "has a value", so an empty field shows no bar and clearing it removes the bar.
 */
@Composable
fun Redactable(
    modifier: Modifier = Modifier,
    masked: Boolean = true,
    content: @Composable () -> Unit,
) {
    // redactable() is not gated on session state: it is a no-op with no session,
    // and gating it would let an already-composed screen stream unmasked.
    // [masked] toggles only the modifier, never the tree: a different tree would
    // recreate [content], and a text field recreated on focus loses that focus,
    // so the customer could never type into it.
    // Keyed on the epoch so a session starting rebuilds this subtree, re-running
    // onAttach + onGloballyPositioned; otherwise a capture that began after
    // layout never receives the region's bounds on a static screen.
    key(CobrowseController.redactionEpoch) {
    Box(modifier.then(if (masked) Modifier.redactable() else Modifier)) {
        content()
        if (masked && LocalAgentView.current) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaskSurface, RoundedCornerShape(4.dp)),
            )
        }
    }
    }
}

/**
 * A framed second copy of [content] as a support agent would see it, with every
 * [Redactable] region barred. A local illustration only.
 */
@Composable
fun AgentPreview(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp),
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
        Text(
            "A local illustration. During a real session the SDK masks these " +
                "regions in the captured frame itself.",
            modifier = Modifier.padding(start = 36.dp, end = 12.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.fillMaxWidth().heightIn(min = 96.dp)) {
            CompositionLocalProvider(LocalAgentView provides true) {
                Box(Modifier.fillMaxWidth().padding(12.dp)) { content() }
            }
        }
    }
}

// Previews: `Modifier.redactable()` is safe here because its node tolerates no
// live session.

/** A card-detail block, the app's canonical mix of sensitive and plain fields. */
@Composable
private fun SampleCardFields() {
    Column(Modifier.padding(16.dp)) {
        Text("Travel Rewards Card", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.size(12.dp))

        Text("CARD NUMBER", style = MaterialTheme.typography.labelSmall)
        Redactable {
            Text(
                "4111 1111 1111 1170",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.size(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column {
                Text("EXPIRY", style = MaterialTheme.typography.labelSmall)
                Text("08/27", style = MaterialTheme.typography.bodyLarge)
            }
            Column {
                Text("CVV", style = MaterialTheme.typography.labelSmall)
                Redactable { Text("829", style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

@Composable
private fun RedactionPreview(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    BankTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}

@Preview(name = "1 Customer view — nothing hidden", showBackground = true, widthDp = 400)
@Composable
private fun PreviewCustomerView() = RedactionPreview { SampleCardFields() }

@Preview(name = "2 Agent view — sensitive fields barred", showBackground = true, widthDp = 400)
@Composable
private fun PreviewAgentView() = RedactionPreview {
    AgentPreview(Modifier.padding(12.dp)) { SampleCardFields() }
}

// A value still readable in the lower panel is missing its Redactable.
@Preview(name = "3 Both views — the comparison", showBackground = true, widthDp = 400, heightDp = 620)
@Composable
private fun PreviewBothViews() = RedactionPreview {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("On the customer's device", style = MaterialTheme.typography.labelMedium)
        SampleCardFields()
        AgentPreview { SampleCardFields() }
    }
}

@Preview(name = "4 Both views — dark", showBackground = true, widthDp = 400, heightDp = 620)
@Composable
private fun PreviewBothViewsDark() = RedactionPreview(darkTheme = true) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("On the customer's device", style = MaterialTheme.typography.labelMedium)
        SampleCardFields()
        AgentPreview { SampleCardFields() }
    }
}

@Preview(name = "5 Mask bar — single field", showBackground = true, widthDp = 400)
@Composable
private fun PreviewSingleMaskedField() = RedactionPreview {
    AgentPreview(Modifier.padding(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("PIN", style = MaterialTheme.typography.labelSmall)
            Redactable { Text("4821", style = MaterialTheme.typography.headlineSmall) }
        }
    }
}
