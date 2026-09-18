package com.jacob77.bopsearch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jacob77.bopsearch.player.Track
import com.jacob77.bopsearch.ui.generate.GenerateScreen
import com.jacob77.bopsearch.ui.library.LibraryScreen
import com.jacob77.bopsearch.ui.mixes.MixesScreen
import com.jacob77.bopsearch.ui.nowplaying.NowPlayingScreen
import com.jacob77.bopsearch.ui.player.MiniPlayerBar
import com.jacob77.bopsearch.ui.playlists.PlaylistsScreen
import com.jacob77.bopsearch.ui.queues.PlayQueuesScreen
import com.jacob77.bopsearch.ui.settings.SettingsScreen
import com.jacob77.bopsearch.ui.theme.BopSearchTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.factory(application as BopSearchApp)
    }

    /** Pending play after the user answers the notification prompt. */
    private var pendingPlayTrack: Track? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val track = pendingPlayTrack
        pendingPlayTrack = null
        if (granted) {
            track?.let { viewModel.play(it) }
            return@registerForActivityResult
        }
        // Permanently denied (or OEM greyed): send them to app notification settings.
        if (Build.VERSION.SDK_INT >= 33 &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            openAppNotificationSettings()
        }
        // Still start playback so audio works; shade stays empty until they allow.
        track?.let { viewModel.play(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Warm-request on launch so the system dialog is not only tied to play.
        maybeRequestNotificationPermission(playAfter = null)
        enableEdgeToEdge()
        setContent {
            BopSearchTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.kickSync()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                BopSearchNav(
                    viewModel = viewModel,
                    onPlay = { track -> ensureNotificationsThenPlay(track) },
                    onOpenNotificationSettings = { openAppNotificationSettings() },
                )
            }
        }
    }

    private fun notificationsGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun maybeRequestNotificationPermission(playAfter: Track?) {
        if (Build.VERSION.SDK_INT < 33) {
            playAfter?.let { viewModel.play(it) }
            return
        }
        if (notificationsGranted()) {
            playAfter?.let { viewModel.play(it) }
            return
        }
        pendingPlayTrack = playAfter
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun ensureNotificationsThenPlay(track: Track) {
        if (notificationsGranted()) {
            viewModel.play(track)
            return
        }
        maybeRequestNotificationPermission(playAfter = track)
    }

    private fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra("app_package", packageName)
            putExtra("app_uid", applicationInfo.uid)
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null),
                ),
            )
        }
    }
}

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Dest("library", "Library", Icons.Default.LibraryMusic),
    Dest("queues", "Queues", Icons.Default.QueueMusic),
    Dest("playlists", "Playlists", Icons.Default.PlaylistPlay),
    Dest("mixes", "Mixes", Icons.Default.Shuffle),
    Dest("now", "Now playing", Icons.Default.GraphicEq),
    Dest("generate", "Generate", Icons.Default.AutoAwesome),
    Dest("settings", "Settings", Icons.Default.Settings),
)

@Composable
private fun BopSearchNav(
    viewModel: AppViewModel,
    onPlay: (Track) -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val musicFolders by viewModel.musicFolders.collectAsStateWithLifecycle()
    val generateQueue by viewModel.queueItems.collectAsStateWithLifecycle()
    val playQueue by viewModel.playQueue.collectAsStateWithLifecycle()
    val mixes by viewModel.mixes.collectAsStateWithLifecycle()
    val mixMessage by viewModel.mixMessage.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()

    val showMini = playback.track != null && current != "now"

    Scaffold(
        bottomBar = {
            Column {
                if (showMini) {
                    MiniPlayerBar(
                        playback = playback,
                        onExpand = {
                            nav.navigate("now") {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onToggle = viewModel::togglePlayPause,
                        onSkipNext = viewModel::skipNext,
                    )
                }
                ScrollableBottomNav(
                    currentRoute = current,
                    onNavigate = { route ->
                        nav.navigate(route) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "library",
            modifier = Modifier.padding(padding),
        ) {
            composable("library") {
                LibraryScreen(
                    tracks = tracks,
                    playback = playback,
                    libraryPath = viewModel.libraryPath,
                    musicFolders = musicFolders,
                    onRescan = viewModel::rescanLibrary,
                    onAddFolder = viewModel::addMusicFolder,
                    onRemoveFolder = viewModel::removeMusicFolder,
                    onPlay = onPlay,
                    onToggle = viewModel::togglePlayPause,
                )
            }
            composable("queues") {
                PlayQueuesScreen(
                    queue = playQueue,
                    playback = playback,
                    onPlayFrom = { index ->
                        val q = playQueue
                        if (q.isNotEmpty()) viewModel.playTracks(q, index)
                    },
                    onClear = viewModel::clearPlayQueue,
                )
            }
            composable("playlists") {
                PlaylistsScreen()
            }
            composable("mixes") {
                MixesScreen(
                    mixes = mixes,
                    message = mixMessage,
                    onClearMessage = viewModel::clearMixMessage,
                    onSave = viewModel::saveMix,
                    onDelete = viewModel::deleteMix,
                    onPlay = viewModel::playMix,
                )
            }
            composable("now") {
                NowPlayingScreen(
                    playback = playback,
                    onToggle = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                )
            }
            composable("generate") {
                GenerateScreen(
                    items = generateQueue,
                    syncState = sync,
                    onAddPrompt = viewModel::addPrompt,
                    onCuration = viewModel::updateCuration,
                    onRetry = viewModel::retry,
                    onDelete = viewModel::delete,
                    onKickSync = viewModel::kickSync,
                )
            }
            composable("settings") {
                SettingsScreen(
                    settings = settings,
                    syncState = sync,
                    onSave = viewModel::saveSettings,
                    onKickSync = viewModel::kickSync,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                )
            }
        }
    }
}

@Composable
private fun ScrollableBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val scroll = rememberScrollState()
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        // Musicolet-style dense row: scroll horizontally when 7 tabs don't fit.
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 4.dp),
        ) {
            destinations.forEach { dest ->
                NavigationBarItem(
                    selected = currentRoute == dest.route,
                    onClick = { onNavigate(dest.route) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = {
                        Text(
                            dest.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.widthIn(min = 72.dp),
                )
            }
        }
    }
}
