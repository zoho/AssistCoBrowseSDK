package com.zoho.assist.cobrowse.demo.bank

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The preferences from Security & privacy that change how the app behaves.
 *
 * App-scoped because the screen that sets them is not the screen that obeys
 * them (balances are read on Home, the keypad on Pay). Deliberately not
 * persisted, so every launch starts from a known demo state.
 */
object BankSettings {

    /**
     * Balances are replaced by dots until the customer taps to reveal them.
     * Revealing one shows it to the customer while the agent still sees a bar,
     * demonstrating that redaction is viewer-side.
     */
    var hideBalances by mutableStateOf(false)

    /**
     * Force PIN entry through the in-app scrambled keypad, removing the choice.
     * The alternative, the system keyboard, is a separate window whose bounds
     * the SDK maps through screen coordinates.
     */
    var alwaysSecureKeypad by mutableStateOf(true)

    /**
     * Which colour scheme to render. Defaults to [ThemeOption.LIGHT] rather than
     * the system setting because the near-black redaction bars are far easier to
     * see against a light screen.
     */
    var theme by mutableStateOf(ThemeOption.LIGHT)
}

/** The three ways the demo can pick a colour scheme. */
enum class ThemeOption(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow system"),
}
