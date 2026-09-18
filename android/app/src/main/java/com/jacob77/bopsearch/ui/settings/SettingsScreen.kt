package com.jacob77.bopsearch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacob77.bopsearch.data.PeerSettings
import com.jacob77.bopsearch.sync.SyncUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: PeerSettings,
    syncState: SyncUiState,
    onSave: (host: String, port: Int) -> Unit,
    onKickSync: () -> Unit,
    onOpenNotificationSettings: () -> Unit = {},
) {
    var host by remember(settings.host) { mutableStateOf(settings.host) }
    var portText by remember(settings.port) { mutableStateOf(settings.port.toString()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "PC peer over HTTP (Tailscale MagicDNS name or IP). " +
                    "Emulator → host machine: use 10.0.2.2",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Peer host / MagicDNS") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: settings.port
                    onSave(host, port)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            Button(
                onClick = onKickSync,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Probe / sync now")
            }
            Button(
                onClick = onOpenNotificationSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Notification settings")
            }
            Text(
                "Android 13+: media controls need Notifications allowed. " +
                    "If the shade stays empty while audio plays, tap above " +
                    "(or App info → Notifications → allow Bop-Search / Now playing).",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Presence: ${syncState.presence}\n" +
                    "Backoff: ${syncState.backoffSeconds}s\n" +
                    (syncState.lastError?.let { "Last error: $it\n" } ?: "") +
                    (syncState.lastDrainSummary?.let { "Last drain: $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
