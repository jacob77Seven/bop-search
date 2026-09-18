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
)

/**
 * App-facing playback facade. Talks to [PlaybackService] via [MediaController]
 * so Library UI keeps the same [PlaybackState] / play / toggle API while ExoPlayer
 * and MediaSession run in a foreground media-playback service.
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
    private var released = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publishState(error = null)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            publishState(error = null)
        }

        override fun onPlayerError(error: PlaybackException) {
            val msg = error.message ?: "Playback error ${error.errorCode}"
            Log.e(TAG, "player error: $msg", error)
            publishState(error = msg)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val title = mediaItem?.mediaMetadata?.title?.toString()
            if (title != null && currentTrack != null && currentTrack?.title != title) {
                currentTrack = currentTrack?.copy(title = title)
            }
            publishState(error = null)
        }
    }

    fun play(track: Track) {
        if (released) return
        currentTrack = track
        _state.value = PlaybackState(track = track, isPlaying = false, error = null)
        ensureServiceStarted()
        val c = controller
        if (c == null) {
            pendingTrack = track
            connect()
            return
        }
        playOnController(c, track)
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

    fun stop() {
        pendingTrack = null
        controller?.run {
            stop()
            clearMediaItems()
        }
        currentTrack = null
        _state.value = PlaybackState()
    }

    fun release() {
        released = true
        pendingTrack = null
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        currentTrack = null
        _state.value = PlaybackState()
    }

    private fun ensureServiceStarted() {
        // Do NOT use startForegroundService here — MediaSessionService must call
        // startForeground itself when the media notification is posted on play.
        // A premature FGS start crashes with "did not then call startForeground".
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
                    val pending = pendingTrack
                    pendingTrack = null
                    if (pending != null) {
                        playOnController(c, pending)
                    } else {
                        publishState(error = null)
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

    private fun playOnController(c: MediaController, track: Track) {
        try {
            val uri = trackUri(track)
            val mediaItem = MediaItem.Builder()
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
            currentTrack = track
            c.setMediaItem(mediaItem)
            c.prepare()
            c.play()
            publishState(error = null)
            Log.i(TAG, "playing ${track.path}")
        } catch (e: Exception) {
            val msg = e.message ?: e.javaClass.simpleName
            Log.e(TAG, "play failed: $msg", e)
            _state.value = PlaybackState(track = track, isPlaying = false, error = msg)
        }
    }

    private fun publishState(error: String?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { publishState(error) }
            return
        }
        val c = controller
        val track = currentTrack
        val playing = c?.isPlaying == true
        _state.value = PlaybackState(
            track = track,
            isPlaying = playing,
            error = error ?: c?.playerError?.message,
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
