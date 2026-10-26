package com.zoho.assist.cobrowse.demo.bank

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoho.assist.cobrowse.SessionLogInfo
import com.zoho.assist.cobrowse.demo.CobrowseController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/**
 * Report a problem: the customer's way to send a CoBrowse session log to support.
 * The SDK keeps the logs on the device only, so without this screen support has
 * nothing to look at.
 *
 * Lists the stored sessions; a tap opens one in [SessionLogViewer]. Share sends
 * one session (support knows its key) or all of them; Email opens a mail with
 * that session's log attached and the To field left for the customer, since the
 * address comes from their support ticket; Download saves a copy; Clear deletes
 * every log.
 */
@Composable
fun ReportProblemScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sessions by remember { mutableStateOf<List<SessionLogInfo>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Status?>(null) }
    var viewing by remember { mutableStateOf<SessionLogInfo?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    // Wrapped so "download all" (key = null) differs from "no request".
    var confirmDownload by remember { mutableStateOf<LogRequest?>(null) }
    var pendingSave by remember { mutableStateOf<LogRequest?>(null) }

    suspend fun reload() {
        sessions = CobrowseController.sessionLogs()
    }

    LaunchedEffect(Unit) { reload() }

    fun nothingFor(verb: String, key: String?) =
        Status.Error("Nothing to $verb" + (key?.let { " for session $it" } ?: "") + ".")

    fun share(key: String?) {
        if (busy) return
        busy = true
        scope.launch {
            val ok = CobrowseController.shareLogs(context, key)
            status = if (ok) null else nothingFor("share", key)
            busy = false
        }
    }

    fun email(info: SessionLogInfo) {
        if (busy) return
        busy = true
        scope.launch {
            val file = CobrowseController.exportLogFile(context, info.sessionKey)
            if (file == null) {
                status = nothingFor("email", info.sessionKey)
            } else {
                status = try {
                    context.startActivity(supportEmail(CobrowseController.logUri(context, file), info))
                    null
                } catch (e: ActivityNotFoundException) {
                    Status.Error("No email app on this device. Use Share instead.")
                }
            }
            busy = false
        }
    }

    // Android 9 and below cannot write to Downloads without a permission, so the
    // customer picks a folder in the system "Save as" dialog instead.
    val saveAs = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val key = pendingSave?.sessionKey
        pendingSave = null
        if (uri == null) {
            busy = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val ok = CobrowseController.saveLogs(context, uri, key)
            status = if (ok) Status.Ok("Saved.") else nothingFor("save", key)
            busy = false
        }
    }

    fun download(key: String?) {
        if (busy) return
        busy = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scope.launch {
                val name = CobrowseController.downloadLogs(context, key)?.let { uri ->
                    // MediaStore renames on a clash, so ask for the name it stored.
                    withContext(Dispatchers.IO) {
                        context.contentResolver
                            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    } ?: "the log zip"
                }
                status = if (name != null) Status.Ok("Saved to Downloads: $name") else nothingFor("download", key)
                busy = false
            }
        } else {
            pendingSave = LogRequest(key)
            saveAs.launch(CobrowseController.logFileName(key))
        }
    }

    // Downloads is shared storage: the zip outlives the app, so ask first. Below
    // Android 10 picking a folder in "Save as" is the confirmation.
    fun requestDownload(key: String?) {
        if (busy) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            confirmDownload = LogRequest(key)
        } else {
            download(key)
        }
    }

    confirmDownload?.let { req ->
        AlertDialog(
            onDismissRequest = { confirmDownload = null },
            icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
            title = {
                Text(req.sessionKey?.let { "Download the log for $it?" } ?: "Download all session logs?")
            },
            text = {
                Text(
                    "The file is saved to your Downloads folder and stays there, even if " +
                        "you uninstall ${BankData.BANK_NAME}. Passwords and session tokens " +
                        "are removed, but it still shows the session code, your device " +
                        "model and what happened during the session. To send it to support " +
                        "without keeping a copy, use Share.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDownload = null
                    download(req.sessionKey)
                }) { Text("Download") }
            },
            dismissButton = { TextButton(onClick = { confirmDownload = null }) { Text("Cancel") } },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("Delete all support logs?") },
            text = { Text("Every stored session log on this device is deleted. Support won't be able to look at past sessions.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        status = if (CobrowseController.clearLogs()) {
                            Status.Ok("All logs deleted.")
                        } else {
                            Status.Error("Couldn't delete the logs.")
                        }
                        reload()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }

    viewing?.let { info ->
        SessionLogViewer(
            info = info,
            status = status,
            busy = busy,
            onBack = {
                viewing = null
                status = null
            },
            onShare = { share(info.sessionKey) },
            onEmail = { email(info) },
            onDownload = { requestDownload(info.sessionKey) },
            modifier = modifier,
        )
        return
    }

    val hasLogs = !sessions.isNullOrEmpty()
    Column(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "intro") {
                InfoNote(
                    "Each screen-sharing session keeps a log on this device. If support " +
                        "asks for one, share the session with the code they mention. Logs " +
                        "never leave your phone unless you share them.",
                )
            }
            status?.let { item(key = "status") { StatusText(it) } }
            when (val list = sessions) {
                null -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> if (list.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "No session logs yet. One is kept for each session you share with support.",
                            modifier = Modifier.padding(vertical = 24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(list, key = { it.sessionKey + it.startedAtMs }) { s ->
                        SessionLogRow(
                            info = s,
                            enabled = !busy,
                            onOpen = { viewing = s },
                            onShare = { share(s.sessionKey) },
                            onEmail = { email(s) },
                            onDownload = { requestDownload(s.sessionKey) },
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { confirmClear = true }, enabled = !busy && hasLogs) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete all logs")
            }
            OutlinedButton(
                onClick = { requestDownload(null) },
                enabled = !busy && hasLogs,
                modifier = Modifier.weight(1f),
            ) { Text("Download all") }
            Button(
                onClick = { share(null) },
                enabled = !busy && hasLogs,
                modifier = Modifier.weight(1f),
            ) { Text("Share all") }
        }
    }
}

@Composable
private fun SessionLogRow(
    info: SessionLogInfo,
    enabled: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onEmail: () -> Unit,
    onDownload: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(info.sessionKey, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    formatTime(info.startedAtMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    listOf(summary(info), formatSize(info.sizeBytes)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDownload, enabled = enabled) {
                Icon(Icons.Rounded.Download, contentDescription = "Download the log for ${info.sessionKey}")
            }
            IconButton(onClick = onEmail, enabled = enabled) {
                Icon(Icons.Rounded.Email, contentDescription = "Email the log for ${info.sessionKey}")
            }
            FilledTonalButton(onClick = onShare, enabled = enabled) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Share")
            }
        }
    }
}

/**
 * One session's log, so the customer can see what it holds before sending it,
 * or read a line out to support. The SDK has already redacted it.
 */
@Composable
private fun SessionLogViewer(
    info: SessionLogInfo,
    status: Status?,
    busy: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEmail: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lines by remember { mutableStateOf<List<String>?>(null) }
    var missing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var problemsOnly by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    LaunchedEffect(info.sessionKey, info.startedAtMs) {
        val text = CobrowseController.readLog(info)
        if (text == null) missing = true else lines = text.lines().dropLastWhile { it.isBlank() }
    }

    val shown = remember(lines, query, problemsOnly) {
        lines.orEmpty().filter { line ->
            (!problemsOnly || isProblem(line)) && (query.isBlank() || line.contains(query, ignoreCase = true))
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(info.sessionKey, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    formatTime(info.startedAtMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onBack) { Text("All sessions") }
            IconButton(onClick = onDownload, enabled = !busy) {
                Icon(Icons.Rounded.Download, contentDescription = "Download this log")
            }
            IconButton(onClick = onEmail, enabled = !busy) {
                Icon(Icons.Rounded.Email, contentDescription = "Email this log")
            }
            IconButton(onClick = onShare, enabled = !busy) {
                Icon(Icons.Rounded.Share, contentDescription = "Share this log")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search this log") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = problemsOnly,
                onClick = { problemsOnly = !problemsOnly },
                label = { Text("Warnings & errors") },
            )
            lines?.size?.let { total ->
                Text(
                    if (shown.size == total) "$total lines" else "${shown.size} of $total lines",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        status?.let { StatusText(it) }
        when {
            missing -> Text(
                "This session's log is no longer on the device.",
                modifier = Modifier.padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lines == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            shown.isEmpty() -> Text(
                if (query.isNotBlank()) "No line contains \"$query\"." else "No warnings or errors in this session.",
                modifier = Modifier.padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> SelectionContainer {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(shown) { line ->
                        Text(
                            line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = when {
                                line.contains("[SEVERE]") -> MaterialTheme.colorScheme.error
                                isProblem(line) -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

private class LogRequest(val sessionKey: String?)

/**
 * A mail with the log zip attached and the subject and body filled in. No
 * recipient: the customer types the address from their support ticket. The
 * mailto: selector limits the chooser to email apps.
 */
private fun supportEmail(log: Uri, info: SessionLogInfo): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_SUBJECT, "${BankData.BANK_NAME} support: screen-sharing session ${info.sessionKey}")
        putExtra(
            Intent.EXTRA_TEXT,
            "Session code: ${info.sessionKey}\n" +
                "Started: ${formatTime(info.startedAtMs)}\n" +
                "Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}\n\n" +
                "What went wrong:\n\n",
        )
        putExtra(Intent.EXTRA_STREAM, log)
        // The read grant travels with ClipData, not with EXTRA_STREAM.
        clipData = ClipData.newRawUri(null, log)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
    }

private sealed interface Status {
    val text: String

    data class Ok(override val text: String) : Status
    data class Error(override val text: String) : Status
}

@Composable
private fun StatusText(status: Status) {
    Text(
        status.text,
        style = MaterialTheme.typography.bodySmall,
        color = if (status is Status.Ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
}

private fun isProblem(line: String) =
    line.contains("[SEVERE]") || line.contains("[WARNING]") ||
        line.contains("bytes dropped") || line.contains("line(s) dropped")

private fun formatTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

private fun summary(info: SessionLogInfo): String {
    val ended = info.endedAtMs ?: return "In progress"
    val s = ((ended - info.startedAtMs) / 1000).coerceAtLeast(0)
    val duration = when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
    // REMOTE_END → "Remote end"; free-text reasons like "process ended" pass through.
    val reason = info.endReason?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
    return listOfNotNull(duration, reason).joinToString(" · ")
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / 1048576.0)
}
