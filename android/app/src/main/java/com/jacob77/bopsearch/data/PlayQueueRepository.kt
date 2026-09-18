package com.jacob77.bopsearch.data

import com.jacob77.bopsearch.player.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayQueueRepository(private val dao: PlayQueueDao) {
    fun observeTracks(): Flow<List<Track>> = dao.observeAll().map { rows ->
        rows.map {
            Track(
                id = it.trackId,
                title = it.title,
                path = it.path,
                sizeBytes = it.sizeBytes,
                sourceLabel = it.sourceLabel,
            )
        }
    }

    suspend fun replaceWith(tracks: List<Track>) {
        val rows = tracks.mapIndexed { index, t ->
            PlayQueueItemEntity(
                position = index,
                trackId = t.id,
                title = t.title,
                path = t.path,
                sizeBytes = t.sizeBytes,
                sourceLabel = t.sourceLabel,
            )
        }
        dao.replaceAll(rows)
    }

    suspend fun clear() = dao.clear()

    suspend fun listTracks(): List<Track> = dao.listAll().map {
        Track(
            id = it.trackId,
            title = it.title,
            path = it.path,
            sizeBytes = it.sizeBytes,
            sourceLabel = it.sourceLabel,
        )
    }
}
