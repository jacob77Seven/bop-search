package com.jacob77.bopsearch.data

import kotlinx.coroutines.flow.Flow

class QueueRepository(private val dao: QueueDao) {
    fun observeAll(): Flow<List<QueueItemEntity>> = dao.observeAll()

    suspend fun pending(): List<QueueItemEntity> = dao.listByStatus(QueueStatus.PENDING)

    suspend fun failed(): List<QueueItemEntity> = dao.listByStatus(QueueStatus.FAILED)

    suspend fun getById(id: Long): QueueItemEntity? = dao.getById(id)

    suspend fun addGeneratePrompt(prompt: String): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            QueueItemEntity(
                kind = "generate",
                prompt = prompt.trim(),
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    suspend fun addCuration(
        trackHint: String,
        action: String,
        notes: String,
        rating: Int?,
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            QueueItemEntity(
                kind = "curation",
                prompt = trackHint.trim(),
                curationAction = action,
                curationNotes = notes.trim(),
                rating = rating,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    /**
     * Update curation fields on an existing row.
     * Re-queues (PENDING) only when the row itself is a curation job or was never synced.
     */
    suspend fun updateCuration(
        id: Long,
        action: String?,
        notes: String,
        rating: Int?,
    ) {
        val existing = dao.getById(id) ?: return
        val shouldRequeue =
            existing.kind == "curation" || existing.status != QueueStatus.SYNCED
        dao.update(
            existing.copy(
                curationAction = action ?: existing.curationAction,
                curationNotes = notes,
                rating = rating,
                status = if (shouldRequeue) QueueStatus.PENDING else existing.status,
                lastError = if (shouldRequeue) null else existing.lastError,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markSynced(id: Long, remoteJobId: String) {
        val now = System.currentTimeMillis()
        dao.markSyncResult(
            id = id,
            status = QueueStatus.SYNCED,
            remoteJobId = remoteJobId,
            lastError = null,
            syncedAt = now,
            updatedAt = now,
        )
    }

    suspend fun markFailed(id: Long, error: String) {
        val now = System.currentTimeMillis()
        dao.markSyncResult(
            id = id,
            status = QueueStatus.FAILED,
            remoteJobId = null,
            lastError = error.take(500),
            syncedAt = null,
            updatedAt = now,
        )
    }

    suspend fun retryFailed(id: Long) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                status = QueueStatus.PENDING,
                lastError = null,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
