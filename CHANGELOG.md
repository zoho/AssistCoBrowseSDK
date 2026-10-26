# Changelog

Changes to the Assist Bank CoBrowse Android demo.

## 1.0.0

First release, built on the CoBrowse SDK `com.zoho.assist:cobrowse:1.0.0`.

- **Co-browse session.** The customer taps the screen-share action, enters the session code
  the agent reads out, and the agent sees the app live. Sharing can be paused, resumed and
  ended from the session sheet.
- **Credentials.** The widget code, the domain it was issued for and an optional website
  domain (the site the agent sees the session coming from) are set in one place in the app
  and committed blank, so no credential ships in the repo.
- **Viewer-side masking.** Sensitive values are masked on the agent's side only: card numbers,
  CVV, account details, balances, transaction passwords, the PIN, the secure keypad's digit
  labels and the one-time passcode. Fields are masked while they hold a value.
- **Pause while typing a secret.** Sharing pauses while the customer types into a sensitive
  field, so the agent sees a privacy placeholder instead. Pay shows both approaches: the
  transaction password pauses, and its confirmation keeps sharing with only the value masked.
- **WebView masking.** The e-Statement masks DOM nodes by CSS selector, or the whole page.
- **Agent preview.** An on-device copy of a screen with masked regions barred, for demos and
  QA.
- **Report a problem.** View, search, download, share or email the SDK's on-device session
  logs.
- **Banking sample.** Home, Accounts, Cards, Pay and More tabs on in-memory sample data, with
  light and dark brand themes.
- **Commit hook.** `tools/git-hooks/pre-commit` refuses commits that would publish a widget
  code.
