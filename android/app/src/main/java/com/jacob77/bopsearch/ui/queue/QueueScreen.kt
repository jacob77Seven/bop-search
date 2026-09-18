package com.jacob77.bopsearch.ui.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jacob77.bopsearch.data.QueueItemEntity
import com.jacob77.bopsearch.data.QueueStatus
import com.jacob77.bopsearch.sync.SyncUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    items: List<QueueItemEntity>,
    syncState: SyncUiState,
    onAddPrompt: (String) -> Unit,
    onCuration: (Long, String, String, Int?) -> Unit,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onKickSync: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var promptDraft by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var notesDraft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue") },
                actions = {
                    TextButton(onClick = onKickSync) { Text("Sync now") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = !showAdd }) {
                Icon(Icons.Default.Add, contentDescription = "Add prompt")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "PC: ${syncState.presence}" +
                    (syncState.lastDrainSummary?.let { " · $it" } ?: " · no drain yet") +
                    (syncState.lastError?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (showAdd) {
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("New generation prompt", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = promptDraft,
                            onValueChange = { promptDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Prompt") },
                            minLines = 2,
                        )
                        Button(
                            onClick = {
                                if (promptDraft.isNotBlank()) {
                                    onAddPrompt(promptDraft)
                                    promptDraft = ""
                                    showAdd = false
                                }
                            },
                        ) { Text("Enqueue") }
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(item.kind, style = MaterialTheme.typography.labelLarge)
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            when (item.status) {
                                                QueueStatus.SYNCED -> "SYNCED"
                                                QueueStatus.PENDING -> "PENDING"
                                                QueueStatus.FAILED -> "FAILED"
                                            }
                                        )
                                    },
                                    colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                        containerColor = when (item.status) {
                                            QueueStatus.SYNCED -> Color(0xFF1B5E20)
                                            QueueStatus.PENDING -> Color(0xFF4E342E)
                                            QueueStatus.FAILED -> Color(0xFFB71C1C)
                                        },
                                        labelColor = Color.White,
                                    ),
                                )
                            }
                            Text(item.prompt, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                buildString {
                                    append(formatTime(item.createdAtEpochMs))
                                    item.remoteJobId?.let { append(" · PC job "); append(it) }
                                    item.lastError?.let { append(" · "); append(it) }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (editingId == item.id) {
                                OutlinedTextField(
                                    value = notesDraft,
                                    onValueChange = { notesDraft = it },
                                    label = { Text("Curation notes") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        onCuration(item.id, "like", notesDraft, item.rating)
                                        editingId = null
                                    }) { Text("Like") }
                                    TextButton(onClick = {
                                        onCuration(item.id, "skip", notesDraft, item.rating)
                                        editingId = null
                                    }) { Text("Skip") }
                                    TextButton(onClick = {
                                        onCuration(item.id, "rate", notesDraft, 4)
                                        editingId = null
                                    }) { Text("Rate 4") }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = {
                                        editingId = item.id
                                        notesDraft = item.curationNotes
                                    }) { Text("Edit curation") }
                                    if (item.status == QueueStatus.FAILED) {
                                        TextButton(onClick = { onRetry(item.id) }) { Text("Retry") }
                                    }
                                    TextButton(onClick = { onDelete(item.id) }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(epochMs: Long): String {
    val fmt = SimpleDateFormat("MMM d HH:mm", Locale.getDefault())
    return fmt.format(Date(epochMs))
}
