# Changelog

Changes to the Assist Bank CoBrowse Android demo.

## 1.0.1

Built on the CoBrowse SDK `com.zoho.assist:cobrowse:1.0.1`, a security and stability update.

- **SDK 1.0.1.** Redaction set before a session starts is now kept, and stays in force
  across sessions. A statement page already open when the session starts is masked, and a
  page's masks are never carried over from the page before it. A loaded statement no longer
  stays fully black.
- **WebView selectors set once.** The e-Statement's CSS selector is registered once at app
  start instead of on every session start, which `1.0.0` needed because the SDK cleared it
  when a session closed. The selector mode chosen on the e-Statement now carries over into
  the next session instead of being reset.
- **No mask rebuild on session start.** Masked fields no longer rebuild when capture starts or
  resumes, which `1.0.0` needed because it dropped masks registered before `start()`. A field
  the customer is typing into keeps its state when sharing resumes.
- **Scaled embedded page on the e-Statement.** The statement now embeds a *Rewards card* page
  (an iframe) shown at 1.5×, with its card number masked by the same `redact` selector. It
  shows that the agent's mask follows a resized embedded page, which needs SDK `1.0.1`.
- **No flicker when changing the e-Statement mode.** Switching between *Nothing hidden*,
  *Sensitive fields* and *Whole page* no longer rebuilds and reloads the statement, which
  flashed it white. The SDK re-masks the open page when the selectors change.

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
