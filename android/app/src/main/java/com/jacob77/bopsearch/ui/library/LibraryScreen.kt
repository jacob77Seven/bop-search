package com.jacob77.bopsearch.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacob77.bopsearch.data.MusicFolder
import com.jacob77.bopsearch.player.PlaybackState
import com.jacob77.bopsearch.player.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    playback: PlaybackState,
    libraryPath: String,
    musicFolders: List<MusicFolder>,
    onRescan: () -> Unit,
    onAddFolder: (Uri) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onPlay: (Track) -> Unit,
    onToggle: () -> Unit,
) {
    val openTree = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) onAddFolder(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = { openTree.launch(null) }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Add music folder")
                    }
                    IconButton(onClick = onRescan) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan library")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "App library:\n$libraryPath",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Button(
                onClick = { openTree.launch(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Text("Add music folder")
            }

            if (musicFolders.isNotEmpty()) {
                Text(
                    "Added folders",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                musicFolders.forEach { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                folder.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onRemoveFolder(folder.uriString) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove folder")
                            }
                        }
                    }
                }
            }

            playback.track?.let { current ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 4.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(current.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (playback.isPlaying) "Playing" else "Paused",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            playback.error?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        IconButton(onClick = onToggle) {
                            Icon(
                                if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                            )
                        }
                    }
                }
            }
            if (tracks.isEmpty()) {
                Text(
                    "No audio files yet. Tap Add music folder to grant access to a phone " +
                        "directory, or push .mp3/.wav/… into the app library folder via " +
                        "Device File Explorer, then Rescan.",
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tracks, key = { it.id }) { track ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(track) },
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(track.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${track.sourceLabel} · ${track.sizeBytes / 1024} KB",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
