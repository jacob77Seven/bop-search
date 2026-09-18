package com.jacob77.bopsearch.player

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "BopPlayer"

data class PlaybackState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val error: String? = null,
)

class LocalPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun play(track: Track) {
        stopInternal()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(track.path)
                setOnCompletionListener {
                    _state.value = _state.value.copy(isPlaying = false)
                    Log.i(TAG, "completed ${track.title}")
                }
                setOnErrorListener { _, what, extra ->
                    val msg = "MediaPlayer error what=$what extra=$extra"
                    Log.e(TAG, msg)
                    _state.value = PlaybackState(track = track, isPlaying = false, error = msg)
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = mp
            _state.value = PlaybackState(track = track, isPlaying = true)
            Log.i(TAG, "playing ${track.path}")
        } catch (e: Exception) {
            val msg = e.message ?: e.javaClass.simpleName
            Log.e(TAG, "play failed: $msg")
            _state.value = PlaybackState(track = track, isPlaying = false, error = msg)
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        val track = _state.value.track ?: return
        if (mp.isPlaying) {
            mp.pause()
            _state.value = PlaybackState(track = track, isPlaying = false)
            Log.i(TAG, "paused ${track.title}")
        } else {
            mp.start()
            _state.value = PlaybackState(track = track, isPlaying = true)
            Log.i(TAG, "resumed ${track.title}")
        }
    }

    fun stop() {
        stopInternal()
        _state.value = PlaybackState()
    }

    fun release() {
        stopInternal()
    }

    private fun stopInternal() {
        try {
            mediaPlayer?.run {
                stop()
                release()
            }
        } catch (_: Exception) {
            // ignore teardown races
        }
        mediaPlayer = null
    }
}
