package com.jacob77.bopsearch

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jacob77.bopsearch.data.MixEntity
import com.jacob77.bopsearch.data.MixRepository
import com.jacob77.bopsearch.data.MusicFolder
import com.jacob77.bopsearch.data.MusicFoldersRepository
import com.jacob77.bopsearch.data.PeerSettings
import com.jacob77.bopsearch.data.PlayQueueRepository
import com.jacob77.bopsearch.data.QueueItemEntity
import com.jacob77.bopsearch.data.QueueRepository
import com.jacob77.bopsearch.data.SettingsRepository
import com.jacob77.bopsearch.data.TrackMetaEntity
import com.jacob77.bopsearch.data.TrackMetaRepository
import com.jacob77.bopsearch.domain.MixCandidate
import com.jacob77.bopsearch.domain.MixEvaluator
import com.jacob77.bopsearch.domain.MixRules
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
    private val mixRepository: MixRepository,
    private val trackMetaRepository: TrackMetaRepository,
    private val playQueueRepository: PlayQueueRepository,
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

    val mixes: StateFlow<List<MixEntity>> = mixRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trackMeta: StateFlow<List<TrackMetaEntity>> = trackMetaRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playQueue: StateFlow<List<Track>> = playQueueRepository.observeTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncState: StateFlow<SyncUiState> = syncCoordinator.state

    val playback: StateFlow<PlaybackState> = localPlayer.state

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _mixMessage = MutableStateFlow<String?>(null)
    val mixMessage: StateFlow<String?> = _mixMessage.asStateFlow()

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

    fun play(track: Track) {
        viewModelScope.launch {
            playQueueRepository.replaceWith(listOf(track))
            localPlayer.play(track)
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            playQueueRepository.replaceWith(tracks)
            localPlayer.playQueue(tracks, startIndex)
        }
    }

    fun togglePlayPause() = localPlayer.togglePlayPause()

    fun seekTo(positionMs: Long) = localPlayer.seekTo(positionMs)

    fun skipNext() = localPlayer.skipNext()

    fun skipPrevious() = localPlayer.skipPrevious()

    fun ratingFor(trackId: String?): Int? {
        if (trackId == null) return null
        return trackMeta.value.firstOrNull { it.trackId == trackId }?.rating
            ?: playback.value.rating
    }

    fun saveMix(name: String, rules: MixRules, existingId: Long = 0) {
        viewModelScope.launch {
            mixRepository.save(name, rules, existingId)
            _mixMessage.value = "Saved “${name}”"
        }
    }

    fun deleteMix(id: Long) {
        viewModelScope.launch { mixRepository.delete(id) }
    }

    fun playMix(mix: MixEntity) {
        viewModelScope.launch {
            val library = _tracks.value
            if (library.isEmpty()) {
                _mixMessage.value = "Library empty — add music folders first"
                return@launch
            }
            val metaById = trackMeta.value.associateBy { it.trackId }
            val candidates = library.map { tr ->
                val m = metaById[tr.id]
                MixCandidate(
                    id = tr.id,
                    title = tr.title,
                    path = tr.path,
                    sizeBytes = tr.sizeBytes,
                    sourceLabel = tr.sourceLabel,
                    rating = (m?.rating ?: 50).toDouble(),
                    playCount = m?.playCount ?: 0,
                    genreIds = m?.genreIds?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
                    tone = m?.tone,
                    energy = m?.energy,
                    blacklist = m?.blacklisted == true,
                )
            }
            val picked = withContext(Dispatchers.Default) {
                MixEvaluator.evaluate(mix.toRules(), candidates)
            }
            if (picked.isEmpty()) {
                _mixMessage.value = "No tracks matched “${mix.name}” (try lower rating min / clear filters)"
                return@launch
            }
            val tracks = picked.map {
                Track(
                    id = it.id,
                    title = it.title,
                    path = it.path,
                    sizeBytes = it.sizeBytes,
                    sourceLabel = it.sourceLabel,
                )
            }
            playQueueRepository.replaceWith(tracks)
            localPlayer.playQueue(tracks, 0)
            _mixMessage.value = "Playing “${mix.name}” · ${tracks.size} track(s)"
        }
    }

    fun clearPlayQueue() {
        viewModelScope.launch {
            playQueueRepository.clear()
        }
    }

    fun clearMixMessage() {
        _mixMessage.value = null
    }

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
                        mixRepository = app.mixRepository,
                        trackMetaRepository = app.trackMetaRepository,
                        playQueueRepository = app.playQueueRepository,
                        localLibrary = app.localLibrary,
                        localPlayer = app.localPlayer,
                        syncCoordinator = app.syncCoordinator,
                    ) as T
                }
            }
    }
}
