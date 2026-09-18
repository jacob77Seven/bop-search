package com.jacob77.bopsearch.data

import com.jacob77.bopsearch.domain.CurationRules
import com.jacob77.bopsearch.player.Track
import kotlinx.coroutines.flow.Flow

class TrackMetaRepository(
    private val metaDao: TrackMetaDao,
    private val eventDao: CurationEventDao,
) {
    fun observeAll(): Flow<List<TrackMetaEntity>> = metaDao.observeAll()

    suspend fun get(trackId: String): TrackMetaEntity? = metaDao.get(trackId)

    suspend fun ensureMeta(track: Track): TrackMetaEntity {
        val existing = metaDao.get(track.id)
        if (existing != null) {
            if (existing.title != track.title || existing.path != track.path) {
                val updated = existing.copy(title = track.title, path = track.path)
                metaDao.upsert(updated)
                return updated
            }
            return existing
        }
        val created = TrackMetaEntity(
            trackId = track.id,
            title = track.title,
            path = track.path,
            rating = CurationRules.DEFAULT_RATING,
        )
        metaDao.upsert(created)
        return created
    }

    suspend fun ratingFor(trackId: String): Int {
        return metaDao.get(trackId)?.rating ?: CurationRules.DEFAULT_RATING
    }

    suspend fun applySkip(track: Track): Int {
        val meta = ensureMeta(track)
        val before = meta.rating
        val after = CurationRules.applySkip(before)
        persistRating(meta, before, after, "skip")
        return after
    }

    suspend fun applyFullListen(track: Track): Int {
        val meta = ensureMeta(track)
        val before = meta.rating
        val after = CurationRules.applyFullListen(before)
        val now = System.currentTimeMillis()
        metaDao.upsert(
            meta.copy(
                rating = after,
                playCount = meta.playCount + 1,
                lastPlayedEpochMs = now,
                updatedAtEpochMs = now,
                blacklisted = after < CurationRules.BLACKLIST_THRESHOLD,
            ),
        )
        eventDao.insert(
            CurationEventEntity(
                trackId = track.id,
                action = "full_listen",
                ratingBefore = before,
                ratingAfter = after,
            ),
        )
        return after
    }

    private suspend fun persistRating(
        meta: TrackMetaEntity,
        before: Int,
        after: Int,
        action: String,
    ) {
        val now = System.currentTimeMillis()
        metaDao.upsert(
            meta.copy(
                rating = after,
                updatedAtEpochMs = now,
                blacklisted = after < CurationRules.BLACKLIST_THRESHOLD,
            ),
        )
        eventDao.insert(
            CurationEventEntity(
                trackId = meta.trackId,
                action = action,
                ratingBefore = before,
                ratingAfter = after,
            ),
        )
    }
}
