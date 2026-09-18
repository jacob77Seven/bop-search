package com.jacob77.bopsearch

import android.app.Application
import com.jacob77.bopsearch.data.BopDatabase
import com.jacob77.bopsearch.data.MixRepository
import com.jacob77.bopsearch.data.MusicFoldersRepository
import com.jacob77.bopsearch.data.PlayQueueRepository
import com.jacob77.bopsearch.data.QueueRepository
import com.jacob77.bopsearch.data.SettingsRepository
import com.jacob77.bopsearch.data.TrackMetaRepository
import com.jacob77.bopsearch.player.LocalLibrary
import com.jacob77.bopsearch.player.LocalPlayer
import com.jacob77.bopsearch.sync.SyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BopSearchApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: BopDatabase
        private set
    lateinit var queueRepository: QueueRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var musicFoldersRepository: MusicFoldersRepository
        private set
    lateinit var mixRepository: MixRepository
        private set
    lateinit var trackMetaRepository: TrackMetaRepository
        private set
    lateinit var playQueueRepository: PlayQueueRepository
        private set
    lateinit var localLibrary: LocalLibrary
        private set
    lateinit var localPlayer: LocalPlayer
        private set
    lateinit var syncCoordinator: SyncCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        database = BopDatabase.get(this)
        queueRepository = QueueRepository(database.queueDao())
        settingsRepository = SettingsRepository(this)
        musicFoldersRepository = MusicFoldersRepository(this)
        mixRepository = MixRepository(database.mixDao())
        trackMetaRepository = TrackMetaRepository(
            metaDao = database.trackMetaDao(),
            eventDao = database.curationEventDao(),
        )
        playQueueRepository = PlayQueueRepository(database.playQueueDao())
        localLibrary = LocalLibrary(this)
        localPlayer = LocalPlayer(this)
        wireCurationCallbacks()
        syncCoordinator = SyncCoordinator(
            settingsRepository = settingsRepository,
            queueRepository = queueRepository,
            scope = appScope,
        )
        syncCoordinator.start()
    }

    private fun wireCurationCallbacks() {
        localPlayer.onSkipAway = { track ->
            appScope.launch {
                val rating = trackMetaRepository.applySkip(track)
                localPlayer.setDisplayRating(rating)
            }
        }
        localPlayer.onFullListen = { track ->
            appScope.launch {
                val rating = trackMetaRepository.applyFullListen(track)
                localPlayer.setDisplayRating(rating)
            }
        }
        localPlayer.onTrackStarted = { track ->
            appScope.launch {
                trackMetaRepository.ensureMeta(track)
                val rating = trackMetaRepository.ratingFor(track.id)
                localPlayer.setDisplayRating(rating)
            }
        }
    }
}
