package com.zoho.assist.cobrowse.demo.bank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zoho.assist.cobrowse.demo.CobrowseController
import com.zoho.assist.cobrowse.demo.MainActivity
import com.zoho.assist.cobrowse.demo.common.BankTheme

/**
 * Where the customer enters the session code and starts sharing, reached from
 * the screen-share action in the app bar.
 *
 * Holds no session state of its own: everything comes from
 * [CobrowseController.state], so a session that fails or ends on its own is
 * reflected here without polling.
 */
@Composable
fun CobrowseSheetContent(onDone: () -> Unit) {
    CobrowseSheetContent(
        state = CobrowseController.state,
        onStart = { CobrowseController.start(it, MainActivity::class.java) },
        onPause = CobrowseController::pauseSharing,
        onResume = CobrowseController::resumeSharing,
        onEnd = {
            CobrowseController.end()
            onDone()
        },
    )
}

/**
 * The sheet's body, with no dependency on [CobrowseController], so every state
 * can be rendered from an `@Preview` ([CobrowseController.state] has a private setter).
 *
 * @param initialCode seeds the text field; previews use it to show the enabled
 *   Start button.
 */
@Composable
private fun CobrowseSheetContent(
    state: CobrowseController.State,
    onStart: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    initialCode: String = "",
) {
    var code by remember { mutableStateOf(initialCode) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.ScreenShare,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(12.dp))
            Text("Share your screen", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Enter the code the support agent gives you. They will see this app " +
                "while you share — card numbers, CVV and PINs stay masked on " +
                "their side.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        when (state) {
            is CobrowseController.State.Idle -> {
                OutlinedTextField(
                    value = code,
                    // Whitespace is dropped and input capped at SESSION_CODE_LENGTH
                    // so a pasted code with stray characters still joins. Case is
                    // passed through untouched: the SDK never changes it, so
                    // upper-casing here could break a case-sensitive code.
                    onValueChange = {
                        code = it.filter { c -> !c.isWhitespace() }
                            .take(SESSION_CODE_LENGTH)
                    },
                    label = { Text("Session code") },
                    placeholder = { Text("the code the agent reads out") },
                    supportingText = { Text("$SESSION_CODE_LENGTH characters") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // The widget code is not asked for here: it authenticates the app,
                // not the customer. See CobrowseController.widgetCode.
                state.message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onStart(code) },
                    enabled = code.length == SESSION_CODE_LENGTH,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start sharing") }
            }

            CobrowseController.State.Connecting -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(12.dp))
                    Text("Connecting…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            is CobrowseController.State.Active -> {
                Text(
                    if (state.sharing) "Sharing your screen" else "Connected to the agent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { if (state.sharing) onPause() else onResume() },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (state.sharing) "Pause" else "Resume") }
                    Button(
                        onClick = onEnd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.weight(1f),
                    ) { Text("End") }
                }
            }
        }
    }
}

/**
 * Length of an Assist portal session code. The field caps input at this and
 * Start stays disabled until exactly this many characters are entered. Only the
 * length is enforced, not the character set, so an alphanumeric code still works.
 */
private const val SESSION_CODE_LENGTH = 6

// Previews cover every branch of the `when (state)` above. They use
/** Shared scaffold so each preview differs only by the state it renders. */
@Composable
private fun SheetPreview(
    state: CobrowseController.State,
    initialCode: String = "",
    darkTheme: Boolean = false,
) {
    BankTheme(darkTheme = darkTheme) {
        // Surface supplies the sheet's background, which dark mode needs.
        Surface {
            CobrowseSheetContent(
                state = state,
                onStart = {},
                onPause = {},
                onResume = {},
                onEnd = {},
                initialCode = initialCode,
            )
        }
    }
}

@Preview(name = "1 Idle — empty", showBackground = true)
@Composable
private fun PreviewIdleEmpty() = SheetPreview(CobrowseController.State.Idle())

@Preview(name = "2 Idle — code entered", showBackground = true)
@Composable
private fun PreviewIdleCodeEntered() =
    SheetPreview(CobrowseController.State.Idle(), initialCode = "AB1234")

@Preview(name = "3 Idle — join rejected", showBackground = true)
@Composable
private fun PreviewIdleError() = SheetPreview(
    CobrowseController.State.Idle(
        "Couldn't start: You have entered an incorrect key. Please try again.",
    ),
    initialCode = "AB1234",
)

@Preview(name = "4 Idle — no widget code", showBackground = true)
@Composable
private fun PreviewIdleNoWidgetCode() = SheetPreview(
    CobrowseController.State.Idle(
        "Widget code not set. Add yours in BankApplication.kt.",
    ),
    initialCode = "AB1234",
)

@Preview(name = "5 Connecting", showBackground = true)
@Composable
private fun PreviewConnecting() = SheetPreview(CobrowseController.State.Connecting)

@Preview(name = "6 Active — sharing", showBackground = true)
@Composable
private fun PreviewActiveSharing() =
    SheetPreview(CobrowseController.State.Active(sharing = true))

@Preview(name = "7 Active — paused", showBackground = true)
@Composable
private fun PreviewActivePaused() =
    SheetPreview(CobrowseController.State.Active(sharing = false))

@Preview(name = "8 Idle — dark", showBackground = true)
@Composable
private fun PreviewIdleDark() =
    SheetPreview(CobrowseController.State.Idle(), initialCode = "AB1234", darkTheme = true)

@Preview(name = "9 Active — dark", showBackground = true)
@Composable
private fun PreviewActiveDark() =
    SheetPreview(CobrowseController.State.Active(sharing = true), darkTheme = true)
