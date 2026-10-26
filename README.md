# Assist CoBrowse Android demo

Zoho Assist co-browsing for Android, distributed as a Maven artifact. This repository is a
sample app that integrates it.

`:app` is a single-Activity Compose banking app. A customer taps the screen-share action,
enters the session code a support agent reads out, and the agent sees the app live.
Sensitive values are masked **on the agent's side only**; the customer's own screen is never
changed.

The banking side is all in-memory sample data with no network calls, so the only things a
session needs from outside are the credentials below. The package is
`com.zoho.assist.cobrowse.demo`.

What changed between versions is in [CHANGELOG.md](CHANGELOG.md).

---

## Installation

Gradle. The SDK is served from Zoho's Maven host, not Maven Central. Add the repository in
`settings.gradle`:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = "https://maven.zohodl.com/" }
    }
}
```

and the dependency in your app module's `build.gradle`:

```gradle
dependencies {
    implementation 'com.zoho.assist:cobrowse:1.0.0'
}
```

Minimum Android 6.0 (API 23).

Several AndroidX dependencies in `app/build.gradle` are marked `// SDK floor`: they match what
the SDK declares. Keep yours at or above those versions; lowering one fails at runtime with
`NoSuchMethodError`, not at build time.

### Requirements for this sample

| | |
|---|---|
| minSdk / targetSdk / compileSdk | 23 / 37 / 37 |
| AGP | 9.3.0 (pinned — check that your Android Studio accepts a newer AGP before bumping) |
| Gradle | 9.7.1 (wrapper is committed) |
| Kotlin | 2.4.20 |
| Java | 17 |
| CoBrowse SDK | `com.zoho.assist:cobrowse:1.0.0` from `https://maven.zohodl.com/` |

---

## Credentials: widget code and domain

**Both values come from the Assist co-browse web app.** Sign in to your co-browse account to
get them — they cannot be obtained from the SDK or from this repository.

| Value | Where to get it |
|---|---|
| `widgetCode` | The co-browse widget settings in the web app — the long token identifying your app |
| `domain` | The domain shown with your widget code in the co-browse widget settings, e.g. `assist.zoho.in` |
| `sessionKey` | The session code shown when an agent starts a session; the customer types it into your app |
| `websiteDomain` | *Optional.* The website the agent sees the session coming from, as a bare host (`www.example.com`) or an `https` URL. Blank leaves the SDK's default, `assist.zoho.com` |

`widgetCode` and `domain` are fixed for your account — set them once in your app.
`sessionKey` is per-session and entered by the user at runtime.

`domain` is required: the SDK has no default. Give it as a bare host, exactly as the widget
settings show it, with no scheme or path. A widget code only works with the domain it was
issued for; with any other, the SDK refuses the session.

The widget code authenticates the app, not the customer, so it never appears in the UI. In a
production app, don't ship it in the APK at all, because anything compiled in can be
extracted: fetch it from your backend after login and assign it before the customer taps
Start.

---

## Usage

```kotlin
import com.zoho.assist.cobrowse.AssistCobrowseApplication
import com.zoho.assist.cobrowse.CobrowseSession

// Your Application class must extend AssistCobrowseApplication.
class MyApplication : AssistCobrowseApplication() {
    override fun onCreate() {
        super.onCreate()
        CobrowseSession.INSTANCE.setContext(this)
    }
}

// In the Activity: before it is started, so the SDK can register its permission launchers.
CobrowseSession.INSTANCE.onCreate(activity, callbacks)   // callbacks: SessionCallbacks

// When the customer taps Start.
val config = CobrowseSession.CobrowseSessionConfig(
    sessionKey = code,          // session code entered by the user
    widgetCode = widgetCode,    // from the co-browse web app
    domain     = domain,        // from the co-browse web app, e.g. assist.zoho.in
    websiteDomain = null,       // optional: the website the agent sees, e.g. www.example.com
)

CobrowseSession.INSTANCE
    .setCallbacks(callbacks)
    .start(config, MainActivity::class.java, R.drawable.ic_notification)
```

End the session with `CobrowseSession.INSTANCE.onCustomerEndSession()`. Forward the
Activity's `onResume` and `onStop` to `CobrowseSession.INSTANCE.onResume()` / `onStop()`.

Implement `SessionCallbacks` for session state. Mask sensitive UI with
`Modifier.redactable()` on a Compose node, and mask DOM nodes inside a WebView by wrapping its
client with `wrapWebViewClient` and registering CSS selectors with
`setWebViewRedactedSelectors`. See [SDK integration](#sdk-integration) for how this app wires
each piece.

---

## Running the sample project

**1. Clone**

```bash
git clone https://github.com/zoho/AssistCoBrowseSDK.git
cd AssistCoBrowseSDK
cp local.properties.sample local.properties
```

Android Studio writes `sdk.dir` on first sync; from the command line, set it in
`local.properties` yourself.

**2. Set your credentials**

In `app/src/main/java/com/zoho/assist/cobrowse/demo/BankApplication.kt`, paste the values
from the co-browse web app between the quotes:

```kotlin
CobrowseController.widgetCode = ""   // paste your widget code between the quotes
CobrowseController.domain = ""       // the domain it was issued for, e.g. assist.zoho.in
CobrowseController.websiteDomain = "" // optional: the website the agent sees, e.g. www.example.com
```

Keep that change local — never commit it: this repo is public. Set both back to `""` before
you commit; the pre-commit hook refuses the commit if you forget.

**3. Turn on the pre-commit hook** (once per clone). It is a second guard against committing
a widget code; see [Keeping credentials out of git](#keeping-credentials-out-of-git).

```bash
git config core.hooksPath tools/git-hooks
echo "<your widget code>" >> .git/info/blocked-strings   # optional: block that exact value
```

**4. Build and run**

Open the project in Android Studio and run the `app` configuration, or from the command line:

```bash
./gradlew :app:installDebug      # or ./gradlew :app:assembleDebug to build only
```

**5. Start a session**

1. In the co-browse web app, start a session — it shows a session code.
2. In the demo app, tap the screen-share action in the app bar.
3. Enter the 6-character session code and tap **Start**.

The app connects and begins sharing. On the web side the screen is visible with card
numbers, account numbers, balances and PIN fields masked.

If the widget code or domain is missing, the app still builds and runs, and Start is refused
with a message naming the missing value.

### Keeping credentials out of git

The committed widget code is `""`. The `tools/git-hooks/pre-commit` hook refuses a commit
that:

* stages `local.properties`
* assigns a quoted string of six or more characters to `widgetCode`
* contains a `zkms…` widget-code token
* contains any string listed in `.git/info/blocked-strings`, a private list for your clone
  (one string per line, `#` for comments). It lives inside `.git`, so it's never pushed.

Git doesn't copy hooks into a clone, which is why `core.hooksPath` has to be set once per
clone. If the hook ever blocks something wrongly, `git commit --no-verify` skips it.

An APK built with a widget code contains it. `*.apk` is gitignored; don't attach one to a
release or an issue either.

The session code is never stored: the customer types it and it stays in memory.

---

## SDK integration

The SDK is wired in at five points. Everything the app does with it goes through **one
object, `CobrowseController`**: screens read its `state` and call its functions, and none of
them touch `CobrowseSession.INSTANCE`.

**How a session flows:** the customer taps the share icon and enters the session code →
`CobrowseController.start()` opens the session → the SDK captures the Activity's screen with
every `Redactable` region blacked out → the agent sees the app live → `SessionCallbacks`
update `state`, and the UI follows.

### 1. Dependency — `settings.gradle`, `app/build.gradle`

```gradle
maven { url = "https://maven.zohodl.com/" }      // settings.gradle
api 'com.zoho.assist:cobrowse:1.0.0'      // app/build.gradle
```

Some AndroidX libraries are pinned to the minimum versions the SDK expects (`// SDK floor`).

### 2. Application class — `BankApplication.kt`

```kotlin
class BankApplication : AssistCobrowseApplication() {
    override fun onCreate() {
        super.onCreate()
        CobrowseSession.INSTANCE.setContext(this)
        CobrowseController.widgetCode = ""   // your widget code, locally only
        CobrowseController.domain = ""       // e.g. assist.zoho.in
        CobrowseController.websiteDomain = "" // optional
    }
}
```

* It **must extend `AssistCobrowseApplication`**: the SDK tracks the foreground Activity, backgrounding
  and configuration changes through it.
* `setContext` ties the session to the app process, so it survives rotation and Activity
  recreation.
* The widget code and domain are set here — see
  [Credentials](#credentials-widget-code-and-domain).

### 3. Activity hooks — `MainActivity.kt`

| Callback | Call | Why |
|---|---|---|
| `onCreate` | `CobrowseController.attach(this)` | Registers the SDK's permission launchers, so it must run **before** the Activity is started |
| `onResume` | `CobrowseController.onResume()` | Capture resumes and the socket recovers after unlock |
| `onStop` | `CobrowseController.onStop()` | Capture pauses in the background |

The SDK captures this Activity's own window.

### 4. The gateway — `CobrowseController.kt`

| Function | SDK call |
|---|---|
| `start(sessionKey)` | `CobrowseSession.start(config…)`: session code, widget code, domain and the optional website domain (from `BankApplication.kt`); the SDK streams on connect at its own quality |
| `pauseSharing()` / `resumeSharing()` | `onStopShare()` / `onStartShare()` — the session stays open, frames stop |
| `end()` | `onCustomerEndSession()` — not `onStopShare()`, which would leave the agent with a frozen view |
| Session logs | `listSessionLogs`, `readSessionLog`, `shareLogs`, `downloadLogs`, `saveLogs`, `exportLogs`, `clearLogs` (all on `Dispatchers.IO`) |
| `SessionCallbacks` | Drive `state` (`Idle` / `Connecting` / `Active`) and bump `redactionEpoch` when capture starts |

`CobrowseController` is an `object`, so a session survives rotation and Activity recreation.
`CobrowseSheet.kt` is the UI on top of it: the 6-character session-code field and the
start / pause / end controls.

### 5. Masking and logs

* **Masking** — `Redactable { … }` applies the SDK's `Modifier.redactable()` to native fields,
  and the statement WebView uses `wrapWebViewClient` plus `setWebViewRedactedSelectors(".redact")`.
  See [Masking](#masking-viewer-side-redaction).
* **Session logs** — More → Report a problem lists the SDK's on-device logs, which are
  already redacted, for the customer to view, download, share or email. See
  [Report a problem](#report-a-problem--session-logs).

## Masking (viewer-side redaction)

Redaction is what the **agent** sees. It never masks the customer's own view.

### Native fields — `Redactable`

`bank/ViewerRedaction.kt` is the single place where native screens are wired to the SDK.
`Redactable { … }` applies the SDK's `Modifier.redactable()`, and the SDK blacks out that
rectangle in the captured frame before encoding, so the agent's copy never contains the value.

* `redactable()` is applied **unconditionally**. It is a no-op with no session; gating it on
  session state would let an already-composed screen stream unmasked.
* `Redactable` keys its subtree on `CobrowseController.redactionEpoch`, which ticks whenever
  capture starts or resumes. Regions report their bounds from `onGloballyPositioned`, which
  only runs on a layout pass. Without the rebuild, a screen that was already laid out before
  the session began (the account-details sheet, for example) never announced its regions
  and only got masked after a scroll.
* `ValueRow` / `BankTextField` in `BankUi.kt` take a `sensitive` flag: the value is
  redacted and its label stays readable. A sensitive text field is masked only while it holds
  a value, so an empty field shows no bar.
* By default a sensitive `BankTextField` also pauses sharing while the customer types into it
  (`pauseForSensitiveInput` / `resumeAfterSensitiveInput`). The agent then sees the SDK's privacy
  placeholder, so neither the Show toggle nor the keyboard's character preview reaches them.
  Pass `pauseWhileTyping = false` to keep the live screen with only the value masked. Pay shows
  both: the transaction password pauses, and its confirmation does not.

### The statement WebView — selectors

View-level masking can't reach inside a document. The statement's sensitive values carry
`class="redact"`, and the SDK masks matching DOM nodes **only** when both halves are in place:

1. The WebView's client is wrapped: `redactableWebViewClient(delegate)`
   (`CobrowseSession.INSTANCE.wrapWebViewClient`).
2. The selector is registered: `start()` sets `.redact`, and the statement screen changes it
   with `CobrowseController.setWebViewRedactedSelectors(…)`.

The selector list is read when a page loads, so a change applies from the next load; the
statement keys its WebViews on the mode to force one. It offers three modes: *Nothing
hidden*, *Sensitive fields* (the selector), and *Whole page* (a native `Redactable` over the
whole WebView, because the page could paint before injected CSS runs).

### Where masking is applied

| File | Covers |
|---|---|
| `CardsScreen.kt` | Card number, CVV |
| `PayScreen.kt` | Transaction password and its confirmation, PIN field (including its digit counter, which would leak PIN length), the secure keypad's digit labels, the one-time passcode |
| `AccountsScreen.kt` | Account number, sort code, IBAN; list rows behind the *Hide balances from the agent* switch |
| `HomeScreen.kt` | Balance, account line, payee and amount in transaction rows |
| `MoreScreen.kt` | Statement (selector or whole page), history rows behind a switch, market values |

**Deliberately not masked:** payee names in the statement table, transaction dates, category
icons, account names, market symbols. An agent who can see none of that cannot help. Market
*values* are masked even though they are public quotes — revisit if that's wrong.

### On-device agent preview

`AgentPreview { … }` renders a second, local copy of a screen with every `Redactable` region
barred, labelled "Support agent's screen". It is an illustration for demos and QA, used on Pay; the
statement builds its own equivalent panel around a second WebView. It is **not** what masks anything — the SDK does that in the frame.
The redaction bars are near-black (`MaskSurface`), which is why the app defaults to the light
theme.

## Report a problem — session logs

The SDK keeps one already-redacted log per session on the device and never uploads it. More
→ **Report a problem** is how the customer gets one to support:

* A list of stored sessions; a tap opens the log with search and a *Warnings & errors* filter.
* Per session: **Download** (MediaStore on Android 10+, a *Save as* dialog below), **Share**,
  and **Email**. Email exports the session's zip (`exportLogFile`), shares it through the
  SDK's FileProvider (`${packageName}.cobrowse.fileprovider`) and opens a mail with no
  recipient, because the customer gets the support address from their ticket.
* For all sessions: Share all, Download all, Delete all. There is no "email all": the SDK's
  export takes one session key.

Every log call blocks until queued lines reach disk, so `CobrowseController` runs them on
`Dispatchers.IO`.

## Troubleshooting masking

When masking looks broken, check these first:

1. **Coverage** — a screen with no `Redactable` region streams in the clear, by design.
2. **Nothing to compare** — card number and CVV show as dots on the device until *Show
   number* is tapped, so masked and unmasked frames look the same.
3. **A stale APK** — an install that failed quietly leaves an older build on the device.

## The app

`BankApp` owns the app bar, the bottom bar and the back stack. Tabs switch in place; a
**route** (statement, history, markets, security, report a problem, transaction detail) is
pushed over the tabs, hides the bottom bar and shows a back arrow. System back unwinds
overlay → sheet → route → Home before a press exits the app.

| Tab | What's on it |
|---|---|
| Home | Balance card, quick actions, spend-by-category donut, recent transactions → transaction detail |
| Accounts | 100 accounts in a `LazyColumn`; a row opens the account-details sheet |
| Cards | Card tiles with reveal-number and freeze-card controls |
| Pay | Transfer journey — beneficiary form, PIN authorisation, review, one-time passcode, receipt |
| More | Profile, e-Statement (WebView), 1000-row history, live markets, security & privacy, report a problem |

**Security & privacy** holds *Always use the secure keypad* and *Hide balances*, and the More
tab's **Appearance** picks Light / Dark / Follow system. All three live in `BankSettings` and
are deliberately not persisted, so every launch starts in a known demo state.

### Behaviours worth knowing

* **Input shapes.** The Pay form covers a number field, a decimal field, a password field
  with a show/hide toggle, a system numeric-password field, and a read-only PIN field that
  opens no IME.
* **Secure keypad.** Replaces the system IME with an in-app keypad. Keys show no press
  feedback and the digits are scrambled each time it appears. It is the default: the system
  keyboard is a separate window, which the SDK has to map through screen coordinates.
* **In-window overlays.** The account sheet, transfer confirmation and one-time passcode are a
  scrim plus a `Surface` in the Activity's own tree. They are deliberately not `Popup()`,
  `AlertDialog` or `ModalBottomSheet`, each of which opens a second window. `BankApp` renders
  them so the scrim also covers the app bar and bottom bar.
* **Big lists.** History re-sorts with a single state write, so the list reorders at once
  instead of animating row by row.
* **Lifecycle.** Live markets animates continuously — press Home or lock the device to watch
  capture pause and resume.
* **Keyboard.** `windowSoftInputMode="adjustNothing"` and no `imePadding`: the keyboard
  overlays the window, so a form doesn't jump when a field takes focus.

## Theming

`BankTheme` (`common/BankTheme.kt`) owns colour, shape and system-bar appearance: a bundled M3 tonal
scheme generated from the brand blue, with a real dark theme. Dynamic colour (Material You) is
deliberately off, so every device shows the brand colours. `styles.xml` mirrors the same roles
for the Activity window. The app draws edge to edge.

## Layout

Paths under `app/src/main/java/com/zoho/assist/cobrowse/demo/`:

| Path | Role |
|---|---|
| `BankApplication.kt` | `AssistCobrowseApplication` subclass; SDK context, widget code and domain |
| `MainActivity.kt` | The only Activity — SDK lifecycle hooks, theme, hosts `BankApp` |
| `CobrowseController.kt` | The app's only contact with the SDK: session state, start/pause/end, logs |
| `bank/BankApp.kt` | App bar, tabs, routes, back stack, window-level overlays |
| `bank/CobrowseSheet.kt` | Session-code entry and the start / pause / end controls |
| `bank/ViewerRedaction.kt` | `Redactable`, `AgentPreview`, `redactableWebViewClient` |
| `bank/ReportProblemScreen.kt` | Session log list, viewer, download / share / email |
| `bank/BankSettings.kt` | Security & privacy preferences and theme choice |
| `bank/BankData.kt` | Every model and all the in-memory sample data |
| `bank/BankUi.kt` | Pieces shared by more than one screen |
| `bank/HomeScreen.kt` | Balance, quick actions, donut, transactions, detail |
| `bank/AccountsScreen.kt` | Accounts list + the account-details sheet |
| `bank/CardsScreen.kt` | Card tiles and card controls |
| `bank/PayScreen.kt` | Transfer form, secure keypad, review + OTP overlays, receipt |
| `bank/MoreScreen.kt` | Statement WebView, history, markets, security |
| `common/BankTheme.kt` | M3 colour/shape + system-bar appearance |

## Privacy policy

* [assist.zoho.com](https://www.zoho.com/privacy.html)
* [assist.zoho.eu](https://www.zoho.eu/privacy.html)

## Terms of service

* [assist.zoho.com](https://www.zoho.com/terms.html)
* [assist.zoho.eu](https://www.zoho.eu/terms.html)
