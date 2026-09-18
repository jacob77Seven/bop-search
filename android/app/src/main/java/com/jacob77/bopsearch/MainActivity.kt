package com.jacob77.bopsearch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jacob77.bopsearch.ui.library.LibraryScreen
import com.jacob77.bopsearch.ui.queue.QueueScreen
import com.jacob77.bopsearch.ui.settings.SettingsScreen
import com.jacob77.bopsearch.ui.theme.BopSearchTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.factory(application as BopSearchApp)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* MediaSession notification works best when granted on API 33+ */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
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
                BopSearchNav(viewModel)
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Dest("library", "Library", Icons.Default.LibraryMusic),
    Dest("queue", "Queue", Icons.Default.QueueMusic),
    Dest("settings", "Settings", Icons.Default.Settings),
)

@Composable
private fun BopSearchNav(viewModel: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val musicFolders by viewModel.musicFolders.collectAsStateWithLifecycle()
    val queue by viewModel.queueItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { dest ->
                    NavigationBarItem(
                        selected = current == dest.route,
                        onClick = {
                            nav.navigate(dest.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
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
                    onPlay = viewModel::play,
                    onToggle = viewModel::togglePlayPause,
                )
            }
            composable("queue") {
                QueueScreen(
                    items = queue,
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
                )
            }
        }
    }
}
