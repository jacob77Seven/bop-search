package com.jacob77.bopsearch.data

import com.jacob77.bopsearch.domain.MixDocument
import com.jacob77.bopsearch.domain.MixRules
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class MixRepository(private val dao: MixDao) {
    fun observeAll(): Flow<List<MixEntity>> = dao.observeAll()

    suspend fun get(id: Long): MixEntity? = dao.getById(id)

    suspend fun save(name: String, rules: MixRules, existingRowId: Long = 0): Long {
        val now = System.currentTimeMillis()
        return if (existingRowId > 0) {
            val existing = dao.getById(existingRowId)
            val entity = MixEntity.fromRules(
                name = name,
                rules = rules,
                rowId = existingRowId,
                mixId = existing?.mixId,
            ).copy(
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now,
            )
            dao.update(entity)
            existingRowId
        } else {
            dao.insert(
                MixEntity.fromRules(name, rules).copy(
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                ),
            )
        }
    }

    suspend fun saveDocument(doc: MixDocument, existingRowId: Long = 0): Long {
        val now = System.currentTimeMillis()
        val entity = MixEntity.fromDocument(doc, existingRowId).copy(
            mixId = doc.id.ifBlank { UUID.randomUUID().toString() },
            updatedAtEpochMs = now,
            createdAtEpochMs = if (existingRowId > 0) {
                dao.getById(existingRowId)?.createdAtEpochMs ?: now
            } else {
                now
            },
        )
        return if (existingRowId > 0) {
            dao.update(entity)
            existingRowId
        } else {
            dao.insert(entity.copy(id = 0))
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
