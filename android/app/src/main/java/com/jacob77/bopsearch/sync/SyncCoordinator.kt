package com.jacob77.bopsearch.sync

import android.util.Log
import com.jacob77.bopsearch.data.PeerSettings
import com.jacob77.bopsearch.data.QueueRepository
import com.jacob77.bopsearch.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private const val TAG = "BopSync"

enum class PeerPresence { UNKNOWN, ONLINE, OFFLINE }

data class SyncUiState(
    val presence: PeerPresence = PeerPresence.UNKNOWN,
    val lastProbeAtEpochMs: Long? = null,
    val lastError: String? = null,
    val lastDrainSummary: String? = null,
    val backoffSeconds: Long = 0,
)

/**
 * Periodically probes the PC peer and drains PENDING queue items when online.
 * Uses exponential backoff while offline (cap 5 minutes).
 */
class SyncCoordinator(
    private val settingsRepository: SettingsRepository,
    private val queueRepository: QueueRepository,
    private val api: PcApiClient = PcApiClient(),
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var backoffMs: Long = INITIAL_BACKOFF_MS

    fun start() {
        if (loopJob?.isActive == true) return
        Log.i(TAG, "sync loop start")
        loopJob = scope.launch {
            while (isActive) {
                runOnce()
                val wait = if (_state.value.presence == PeerPresence.ONLINE) {
                    ONLINE_POLL_MS
                } else {
                    backoffMs
                }
                _state.value = _state.value.copy(backoffSeconds = wait / 1000)
                delay(wait)
            }
        }
    }

    fun stop() {
        Log.i(TAG, "sync loop stop")
        loopJob?.cancel()
        loopJob = null
    }

    /** Call on Activity resume for a faster presence check. */
    fun kick() {
        scope.launch { runOnce() }
    }

    private suspend fun runOnce() = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        Log.d(TAG, "probe host=${settings.host} port=${settings.port}")
        val health = api.probeHealth(settings)
        val now = System.currentTimeMillis()
        if (!health.online) {
            backoffMs = min(backoffMs * 2, MAX_BACKOFF_MS)
            _state.value = SyncUiState(
                presence = PeerPresence.OFFLINE,
                lastProbeAtEpochMs = now,
                lastError = health.error,
                lastDrainSummary = _state.value.lastDrainSummary,
                backoffSeconds = backoffMs / 1000,
            )
            Log.i(TAG, "peer offline; next backoff=${backoffMs}ms err=${health.error}")
            return@withContext
        }

        backoffMs = INITIAL_BACKOFF_MS
        val drained = drainPending(settings)
        _state.value = SyncUiState(
            presence = PeerPresence.ONLINE,
            lastProbeAtEpochMs = now,
            lastError = null,
            lastDrainSummary = drained,
            backoffSeconds = ONLINE_POLL_MS / 1000,
        )
    }

    private suspend fun drainPending(settings: PeerSettings): String {
        val pending = queueRepository.pending()
        if (pending.isEmpty()) {
            Log.d(TAG, "drain: nothing pending")
            return "0 pending"
        }
        var ok = 0
        var fail = 0
        for (item in pending) {
            val result = api.postJob(settings, item)
            if (result.ok && result.jobId != null) {
                queueRepository.markSynced(item.id, result.jobId)
                ok++
            } else {
                queueRepository.markFailed(item.id, result.error ?: "unknown")
                fail++
            }
        }
        val summary = "drained ok=$ok fail=$fail of ${pending.size}"
        Log.i(TAG, summary)
        return summary
    }

    companion object {
        private const val INITIAL_BACKOFF_MS = 5_000L
        private const val MAX_BACKOFF_MS = 300_000L
        private const val ONLINE_POLL_MS = 30_000L
    }
}
