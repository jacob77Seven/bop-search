package com.jacob77.bopsearch.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "BopPlayer"

data class PlaybackState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val error: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queueSize: Int = 0,
    val queueIndex: Int = -1,
    /** 0–100 display rating; null if unknown. */
    val rating: Int? = null,
)

/**
 * App-facing playback facade. Talks to [PlaybackService] via [MediaController]
 * so Library UI keeps the same [PlaybackState] / play / toggle API while ExoPlayer
 * and MediaSession run in a foreground media-playback service.
 *
 * Supports a play queue (Mixes / Queues), seek, and skip next/previous.
 */
class LocalPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var currentTrack: Track? = null
    private var pendingTrack: Track? = null
    private var pendingQueue: List<Track>? = null
    private var pendingStartIndex: Int = 0
    private var playQueue: List<Track> = emptyList()
    private var released = false
    private var displayRating: Int? = null

    /** Fired when the user skips (next) so curation can lower rating. */
    var onSkipAway: ((Track) -> Unit)? = null

    /** Fired when a track reaches END (full listen). */
    var onFullListen: ((Track) -> Unit)? = null

    /** Fired when a track starts playing (bump play count / ensure meta). */
    var onTrackStarted: ((Track) -> Unit)? = null

    private var lastEndedTrackId: String? = null

    private val progressTicker = object : Runnable {
        override fun run() {
            if (released) return
            publishState(error = null)
            if (controller?.isPlaying == true) {
                mainHandler.postDelayed(this, 500L)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishState(error = null)
            mainHandler.removeCallbacks(progressTicker)
            if (isPlaying) mainHandler.post(progressTicker)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val ended = currentTrack
                if (ended != null && ended.id != lastEndedTrackId) {
                    lastEndedTrackId = ended.id
                    onFullListen?.invoke(ended)
                }
            }
            publishState(error = null)
        }

        override fun onPlayerError(error: PlaybackException) {
            val msg = error.message ?: "Playback error ${error.errorCode}"
            Log.e(TAG, "player error: $msg", error)
            publishState(error = msg)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId
            if (mediaId != null) {
                val fromQueue = playQueue.firstOrNull { it.id == mediaId }
                if (fromQueue != null) {
                    currentTrack = fromQueue
                    onTrackStarted?.invoke(fromQueue)
                } else if (currentTrack != null && currentTrack?.id != mediaId) {
                    val title = mediaItem.mediaMetadata.title?.toString()
                    if (title != null) {
                        currentTrack = currentTrack?.copy(title = title)
                    }
                }
            }
            lastEndedTrackId = null
            publishState(error = null)
        }
    }

    fun setDisplayRating(rating: Int?) {
        displayRating = rating
        publishState(error = null)
    }

    fun play(track: Track) {
        if (released) return
        playQueue = listOf(track)
        currentTrack = track
        displayRating = null
        _state.value = PlaybackState(track = track, isPlaying = false, error = null, queueSize = 1, queueIndex = 0)
        ensureServiceStarted()
        val c = controller
        if (c == null) {
            pendingQueue = null
            pendingTrack = track
            connect()
            return
        }
        playQueueOnController(c, listOf(track), 0)
    }

    /** Replace the play queue and start at [startIndex]. */
    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (released) return
        if (tracks.isEmpty()) {
            stop()
            return
        }
        val idx = startIndex.coerceIn(0, tracks.lastIndex)
        playQueue = tracks
        currentTrack = tracks[idx]
        displayRating = null
        _state.value = PlaybackState(
            track = tracks[idx],
            isPlaying = false,
            error = null,
            queueSize = tracks.size,
            queueIndex = idx,
        )
        ensureServiceStarted()
        val c = controller
        if (c == null) {
            pendingTrack = null
            pendingQueue = tracks
            pendingStartIndex = idx
            connect()
            return
        }
        playQueueOnController(c, tracks, idx)
    }

    fun togglePlayPause() {
        if (released) return
        val c = controller ?: return
        val track = currentTrack ?: _state.value.track ?: return
        if (c.isPlaying) {
            c.pause()
            Log.i(TAG, "paused ${track.title}")
        } else {
            ensureServiceStarted()
            c.play()
            Log.i(TAG, "resumed ${track.title}")
        }
        publishState(error = null)
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        publishState(error = null)
    }

    fun skipNext() {
        val c = controller ?: return
        val leaving = currentTrack
        if (leaving != null) onSkipAway?.invoke(leaving)
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
            c.play()
        } else {
            Log.i(TAG, "skipNext: end of queue")
        }
        publishState(error = null)
    }

    fun skipPrevious() {
        val c = controller ?: return
        if (c.currentPosition > 3_000L) {
            c.seekTo(0L)
        } else if (c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
            c.play()
        } else {
            c.seekTo(0L)
        }
        publishState(error = null)
    }

    fun stop() {
        pendingTrack = null
        pendingQueue = null
        controller?.run {
            stop()
            clearMediaItems()
        }
        playQueue = emptyList()
        currentTrack = null
        displayRating = null
        _state.value = PlaybackState()
    }

    fun release() {
        released = true
        pendingTrack = null
        pendingQueue = null
        mainHandler.removeCallbacks(progressTicker)
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        currentTrack = null
        playQueue = emptyList()
        _state.value = PlaybackState()
    }

    private fun ensureServiceStarted() {
        // Do NOT use startForegroundService here — MediaSessionService must call
        // startForeground itself when the media notification is posted on play.
        val intent = Intent(appContext, PlaybackService::class.java)
        try {
            appContext.startService(intent)
            Log.i(TAG, "startService(PlaybackService)")
        } catch (e: Exception) {
            Log.w(TAG, "startService failed (controller connect will retry): ${e.message}")
        }
    }

    private fun connect() {
        if (released || controller != null || controllerFuture != null) return
        ensureServiceStarted()
        val token = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    if (released) {
                        MediaController.releaseFuture(future)
                        return@addListener
                    }
                    val c = future.get()
                    controller = c
                    c.addListener(playerListener)
                    Log.i(TAG, "MediaController connected")
                    val queue = pendingQueue
                    val single = pendingTrack
                    pendingQueue = null
                    pendingTrack = null
                    when {
                        queue != null -> playQueueOnController(c, queue, pendingStartIndex)
                        single != null -> playQueueOnController(c, listOf(single), 0)
                        else -> publishState(error = null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "MediaController connect failed: ${e.message}", e)
                    controllerFuture = null
                    controller = null
                    val track = currentTrack ?: pendingTrack
                    _state.value = PlaybackState(
                        track = track,
                        isPlaying = false,
                        error = e.message ?: "Failed to connect playback service",
                    )
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun playQueueOnController(c: MediaController, tracks: List<Track>, startIndex: Int) {
        try {
            playQueue = tracks
            val items = tracks.map { toMediaItem(it) }
            val idx = startIndex.coerceIn(0, items.lastIndex)
            currentTrack = tracks[idx]
            c.setMediaItems(items, idx, 0L)
            c.prepare()
            c.play()
            onTrackStarted?.invoke(tracks[idx])
            publishState(error = null)
            Log.i(TAG, "playing queue size=${tracks.size} start=$idx ${tracks[idx].path}")
        } catch (e: Exception) {
            val msg = e.message ?: e.javaClass.simpleName
            Log.e(TAG, "playQueue failed: $msg", e)
            _state.value = PlaybackState(
                track = tracks.getOrNull(startIndex),
                isPlaying = false,
                error = msg,
                queueSize = tracks.size,
                queueIndex = startIndex,
            )
        }
    }

    private fun toMediaItem(track: Track): MediaItem {
        val uri = trackUri(track)
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setDisplayTitle(track.title)
                    .setArtist(track.sourceLabel.ifBlank { "Bop-Search" })
                    .setAlbumTitle(track.sourceLabel.ifBlank { "Library" })
                    .setIsPlayable(true)
                    .build(),
            )
            .build()
    }

    private fun publishState(error: String?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { publishState(error) }
            return
        }
        val c = controller
        val track = currentTrack
        val playing = c?.isPlaying == true
        val index = c?.currentMediaItemIndex ?: playQueue.indexOfFirst { it.id == track?.id }
        val duration = c?.duration?.takeIf { it > 0 } ?: 0L
        val position = c?.currentPosition?.coerceAtLeast(0L) ?: 0L
        _state.value = PlaybackState(
            track = track,
            isPlaying = playing,
            error = error ?: c?.playerError?.message,
            positionMs = position,
            durationMs = duration,
            queueSize = playQueue.size,
            queueIndex = index,
            rating = displayRating,
        )
    }

    private fun trackUri(track: Track): Uri {
        val path = track.path
        return when {
            path.startsWith("content:", ignoreCase = true) -> Uri.parse(path)
            path.startsWith("file:", ignoreCase = true) -> Uri.parse(path)
            else -> Uri.fromFile(File(path))
        }
    }
}
