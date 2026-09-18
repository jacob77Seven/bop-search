package com.jacob77.bopsearch.ui.queues

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacob77.bopsearch.player.PlaybackState
import com.jacob77.bopsearch.player.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayQueuesScreen(
    queue: List<Track>,
    playback: PlaybackState,
    onPlayFrom: (Int) -> Unit,
    onClear: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queues") },
                actions = {
                    if (queue.isNotEmpty()) {
                        TextButton(onClick = onClear) { Text("Clear") }
                    }
                },
            )
        },
    ) { padding ->
        if (queue.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Play queue is empty", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Play a track from Library or start a Mix to fill the queue. " +
                        "Generate jobs live under the Generate tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "${queue.size} track(s) · now #${playback.queueIndex + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                itemsIndexed(queue, key = { i, t -> "${i}-${t.id}" }) { index, track ->
                    val active = playback.track?.id == track.id && playback.queueIndex == index
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPlayFrom(index) },
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${index + 1}. ${track.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                Text(
                                    track.sourceLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (active) {
                                Text(
                                    if (playback.isPlaying) "Playing" else "Paused",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
