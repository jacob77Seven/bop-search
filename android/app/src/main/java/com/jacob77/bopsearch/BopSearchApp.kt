package com.jacob77.bopsearch

import android.app.Application
import com.jacob77.bopsearch.data.BopDatabase
import com.jacob77.bopsearch.data.QueueRepository
import com.jacob77.bopsearch.data.SettingsRepository
import com.jacob77.bopsearch.player.LocalLibrary
import com.jacob77.bopsearch.player.LocalPlayer
import com.jacob77.bopsearch.sync.SyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BopSearchApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: BopDatabase
        private set
    lateinit var queueRepository: QueueRepository
        private set
    lateinit var settingsRepository: SettingsRepository
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
        localLibrary = LocalLibrary(this)
        localPlayer = LocalPlayer()
        syncCoordinator = SyncCoordinator(
            settingsRepository = settingsRepository,
            queueRepository = queueRepository,
            scope = appScope,
        )
        syncCoordinator.start()
    }
}
