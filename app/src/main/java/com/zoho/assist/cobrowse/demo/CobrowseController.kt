package com.zoho.assist.cobrowse.demo

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.zoho.assist.cobrowse.CobrowseSession
import com.zoho.assist.cobrowse.SessionCallbacks
import com.zoho.assist.cobrowse.SessionEndReason
import com.zoho.assist.cobrowse.SessionLogInfo
import com.zoho.assist.cobrowse.SessionStartFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The app's single point of contact with the CoBrowse SDK. The UI reads [state]
 * and calls [start] / [end]; screens never touch `CobrowseSession.INSTANCE`.
 *
 * An `object` because a session must survive rotation and Activity recreation.
 */
object CobrowseController : SessionCallbacks {

    /** What the UI renders. */
    sealed interface State {
        /** No session. [message] carries the outcome of the previous one, if any. */
        data class Idle(val message: String? = null) : State

        /** `start()` accepted, waiting for the gateway to validate and connect. */
        data object Connecting : State

        /** Connected. [sharing] tracks whether frames are actually going out. */
        data class Active(val sharing: Boolean) : State
    }

    var state: State by mutableStateOf(State.Idle())
        private set

    /**
     * Ticks every time capture starts. [Redactable] keys its subtree on this so
     * regions re-register and report fresh bounds; a screen laid out before the
     * session began would otherwise never re-announce them and stream unmasked.
     */
    var redactionEpoch: Int by mutableStateOf(0)
        private set

    /**
     * The widget code [start] authenticates with. Never shown to the customer.
     *
     * The demo sets it in [BankApplication]. A production app fetches it from
     * its backend instead and assigns it before the customer taps Share:
     *
     * ```
     * lifecycleScope.launch {
     *     val code = api.fetchCobrowseWidgetCode()   // your call, your auth
     *     CobrowseController.widgetCode = code
     * }
     * ```
     *
     * Kept in memory only: it is a credential.
     */
    @Volatile
    var widgetCode: String = ""

    /**
     * The domain [widgetCode] was issued for, as a bare host from the CoBrowse
     * widget settings (e.g. `assist.zoho.in`). Required: the SDK has no default.
     */
    @Volatile
    var domain: String = ""

    /**
     * The website the agent sees the session coming from, as a bare host
     * (`www.example.com`) or an `https` URL. Optional: blank leaves the SDK's
     * default, `assist.zoho.com`.
     */
    @Volatile
    var websiteDomain: String = ""

    /**
     * Replace the CSS selectors masked inside wrapped WebViews. Applies from the
     * next page load; [start] re-registers the app default.
     */
    fun setWebViewRedactedSelectors(vararg selectors: String) {
        CobrowseSession.INSTANCE.setWebViewRedactedSelectors(arrayOf(*selectors))
    }

    /**
     * Registers the SDK's permission launchers and seeds orientation. Must run
     * from `Activity.onCreate`, before the Activity is STARTED.
     */
    fun attach(activity: Activity) {
        CobrowseSession.INSTANCE.onCreate(activity, this)

        // The session outlives the Activity, so resync with a live one.
        if (CobrowseSession.INSTANCE.isSessionAlive()) {
            state = State.Active(sharing = CobrowseSession.INSTANCE.isScreenSharing())
        }
    }

    fun onResume() = CobrowseSession.INSTANCE.onResume()

    fun onStop() = CobrowseSession.INSTANCE.onStop()

    /** Join the session identified by [sessionKey], the code the agent reads out. */
    fun start(sessionKey: String, hostActivity: Class<*>) {
        val key = sessionKey.trim()
        val widget = widgetCode.trim()
        val host = domain.trim()
        if (key.isEmpty()) {
            state = State.Idle("Enter the session code the agent gave you.")
            return
        }
        if (widget.isEmpty()) {
            state = State.Idle(
                "Widget code not set. Add yours in BankApplication.kt.",
            )
            return
        }
        if (host.isEmpty()) {
            state = State.Idle("Domain not set. Add yours in BankApplication.kt.")
            return
        }

        state = State.Connecting

        CobrowseSession.INSTANCE
            .setCallbacks(this)
            // The SDK streams on connect at its own quality and frame rate, and
            // shares everything except what the app marks: sensitive values are
            // withheld per field via Modifier.redactable().
            // Masks DOM nodes inside the statement WebView. Requires
            // redactableWebViewClient() (ViewerRedaction.kt). Set per start
            // because the SDK clears it when a session closes.
            .setWebViewRedactedSelectors(arrayOf(REDACTED_CSS_SELECTOR))

        val config = CobrowseSession.CobrowseSessionConfig(
            sessionKey = key,
            widgetCode = widget,
            domain = host,
            websiteDomain = websiteDomain.trim().ifEmpty { null },
        )

        CobrowseSession.INSTANCE.start(
            config,
            hostActivity,
            R.drawable.ic_cobrowse_notification,
        )
    }

    /**
     * End the session. Not `onStopShare()`, which only pauses capture and leaves
     * the agent with a frozen view.
     */
    fun end() = CobrowseSession.INSTANCE.onCustomerEndSession()

    /** Pause frame delivery without leaving the session. */
    fun pauseSharing() {
        CobrowseSession.INSTANCE.onStopShare()
        // SessionCallbacks has no "share stopped" callback.
        state = State.Active(sharing = false)
    }

    /**
     * Withhold the screen while the customer types into a password or PIN; the
     * viewer shows the SDK's privacy placeholder. Nests with [pauseSharing].
     */
    fun pauseForSensitiveInput() = CobrowseSession.INSTANCE.pauseForSensitiveInput()

    /** Counterpart to [pauseForSensitiveInput], once the field loses focus. */
    fun resumeAfterSensitiveInput() = CobrowseSession.INSTANCE.resumeAfterSensitiveInput()

    /** Resume frame delivery after [pauseSharing]. */
    fun resumeSharing() {
        redactionEpoch++
        CobrowseSession.INSTANCE.onStartShare()
        state = State.Active(sharing = true)
    }

    // ── Session logs ────────────────────────────────────────────────────────
    // The SDK keeps one redacted log per session on the device and never uploads
    // it; these are how the customer gets one to support. Each call waits for
    // queued lines to reach disk, so all of them run on Dispatchers.IO.

    /** Session logs still kept on the device. */
    suspend fun sessionLogs(): List<SessionLogInfo> = io { CobrowseSession.INSTANCE.listSessionLogs() }

    /** One session's log as text, or null once it has been pruned. */
    suspend fun readLog(info: SessionLogInfo): String? =
        io { CobrowseSession.INSTANCE.readSessionLog(info.sessionKey, info.startedAtMs) }

    /** Open the share sheet for one session, or for every log when [sessionKey] is null. */
    suspend fun shareLogs(context: Context, sessionKey: String?): Boolean = io {
        if (sessionKey != null) {
            CobrowseSession.INSTANCE.shareLogs(context, sessionKey)
        } else {
            CobrowseSession.INSTANCE.shareLogs(context)
        }
    }

    /** Save to Downloads (Android 10+ only; null below, use [saveLogs]). */
    suspend fun downloadLogs(context: Context, sessionKey: String?): Uri? =
        io { CobrowseSession.INSTANCE.downloadLogs(context, sessionKey) }

    /** Write to a location the customer picked in a "Save as" dialog. */
    suspend fun saveLogs(context: Context, uri: Uri, sessionKey: String?): Boolean =
        io { CobrowseSession.INSTANCE.saveLogs(context, uri, sessionKey) }

    /**
     * One session's log zip as a file, for attaching to your own support ticket or
     * mail, or null when there is nothing to export. Exported to the app's cache;
     * the next export replaces it. [logUri] turns it into a link another app can read.
     */
    suspend fun exportLogFile(context: Context, sessionKey: String): File? =
        io { CobrowseSession.INSTANCE.exportLogs(context, sessionKey) }

    /** A content:// link to an [exportLogFile] result, served by the SDK's FileProvider. */
    fun logUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.cobrowse.fileprovider", file)

    /** Suggested file name for the "Save as" dialog. */
    fun logFileName(sessionKey: String?): String = CobrowseSession.INSTANCE.logFileName(sessionKey)

    /** Delete every SDK log on the device. */
    suspend fun clearLogs(): Boolean = io { CobrowseSession.INSTANCE.clearLogs() }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    // ── SessionCallbacks ────────────────────────────────────────────────────

    override fun onSessionStarted() {
        redactionEpoch++
        state = State.Active(sharing = false)
    }

    override fun onScreenShareStarted() {
        redactionEpoch++
        state = State.Active(sharing = true)
    }

    override fun onSessionStartFailed(failure: SessionStartFailure) {
        // The SDK's default only logs.
        state = State.Idle("Couldn't start: ${failure.message.ifBlank { failure.name }}")
    }

    override fun onSessionEnded(reason: SessionEndReason) {
        // Customer-ended needs no message; START_FAILED was already reported.
        state = State.Idle(
            when (reason) {
                SessionEndReason.ENDED_BY_CUSTOMER, SessionEndReason.START_FAILED -> null
                else -> "Session ended (${reason.name.lowercase().replace('_', ' ')})."
            },
        )
    }

    override fun reconnectionLimitExceeded() {
        state = State.Idle("Lost connection to the agent and couldn't reconnect.")
    }
}

/** The CSS class marking sensitive values in the bank's HTML, and the SDK's WebView mask selector. */
internal const val REDACTED_CSS_SELECTOR = ".redact"
