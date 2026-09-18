package com.jacob77.bopsearch

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jacob77.bopsearch.data.MusicFolder
import com.jacob77.bopsearch.data.MusicFoldersRepository
import com.jacob77.bopsearch.data.PeerSettings
import com.jacob77.bopsearch.data.QueueItemEntity
import com.jacob77.bopsearch.data.QueueRepository
import com.jacob77.bopsearch.data.SettingsRepository
import com.jacob77.bopsearch.player.LocalLibrary
import com.jacob77.bopsearch.player.LocalPlayer
import com.jacob77.bopsearch.player.PlaybackState
import com.jacob77.bopsearch.player.Track
import com.jacob77.bopsearch.sync.SyncCoordinator
import com.jacob77.bopsearch.sync.SyncUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(
    private val queueRepository: QueueRepository,
    private val settingsRepository: SettingsRepository,
    private val musicFoldersRepository: MusicFoldersRepository,
    private val localLibrary: LocalLibrary,
    private val localPlayer: LocalPlayer,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    val queueItems: StateFlow<List<QueueItemEntity>> = queueRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<PeerSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeerSettings())

    val musicFolders: StateFlow<List<MusicFolder>> = musicFoldersRepository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncState: StateFlow<SyncUiState> = syncCoordinator.state

    val playback: StateFlow<PlaybackState> = localPlayer.state

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    val libraryPath: String get() = localLibrary.libraryDir().absolutePath

    init {
        rescanLibrary()
    }

    fun rescanLibrary() {
        viewModelScope.launch { doRescan() }
    }

    fun addMusicFolder(uri: Uri) {
        viewModelScope.launch {
            musicFoldersRepository.add(uri)
            doRescan()
        }
    }

    fun removeMusicFolder(uriString: String) {
        viewModelScope.launch {
            musicFoldersRepository.remove(uriString)
            doRescan()
        }
    }

    private suspend fun doRescan() {
        val folders = musicFoldersRepository.currentFolders()
        _tracks.value = withContext(Dispatchers.IO) {
            localLibrary.scan(folders.map { it.uriString })
        }
    }

    fun play(track: Track) = localPlayer.play(track)

    fun togglePlayPause() = localPlayer.togglePlayPause()

    fun addPrompt(prompt: String) {
        viewModelScope.launch {
            queueRepository.addGeneratePrompt(prompt)
            syncCoordinator.kick()
        }
    }

    fun updateCuration(id: Long, action: String, notes: String, rating: Int?) {
        viewModelScope.launch {
            val item = queueRepository.getById(id) ?: return@launch
            if (item.kind == "curation") {
                queueRepository.updateCuration(id, action, notes, rating)
            } else {
                // Keep generate row as-is; enqueue a separate curation job for the PC.
                queueRepository.updateCuration(id, action, notes, rating)
                queueRepository.addCuration(
                    trackHint = item.prompt,
                    action = action,
                    notes = notes,
                    rating = rating,
                )
            }
            syncCoordinator.kick()
        }
    }

    fun retry(id: Long) {
        viewModelScope.launch {
            queueRepository.retryFailed(id)
            syncCoordinator.kick()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { queueRepository.delete(id) }
    }

    fun saveSettings(host: String, port: Int) {
        viewModelScope.launch {
            settingsRepository.update(host, port)
            syncCoordinator.kick()
        }
    }

    fun kickSync() = syncCoordinator.kick()

    override fun onCleared() {
        // Player lives at Application scope; do not release here.
        super.onCleared()
    }

    companion object {
        fun factory(app: BopSearchApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(
                        queueRepository = app.queueRepository,
                        settingsRepository = app.settingsRepository,
                        musicFoldersRepository = app.musicFoldersRepository,
                        localLibrary = app.localLibrary,
                        localPlayer = app.localPlayer,
                        syncCoordinator = app.syncCoordinator,
                    ) as T
                }
            }
    }
}
