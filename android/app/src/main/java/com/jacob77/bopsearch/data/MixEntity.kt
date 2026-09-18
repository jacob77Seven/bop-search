package com.jacob77.bopsearch.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jacob77.bopsearch.domain.MixDocument
import com.jacob77.bopsearch.domain.MixRules
import java.util.UUID

/**
 * Room row for a Mix document (docs/schema/mix.schema.json).
 * [mixId] is the schema string id; [id] is the local autoincrement PK.
 */
@Entity(tableName = "mixes")
data class MixEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mixId: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String? = null,
    val genreAllow: String = "",
    val genreRequire: String = "",
    val genreExclude: String = "",
    val toneMin: Double? = null,
    val toneMax: Double? = null,
    val energyMin: Double? = null,
    val energyMax: Double? = null,
    val ratingMin: Double? = null,
    val ratingMax: Double? = null,
    val likeBias: Double = 0.5,
    val newBias: Double = 0.5,
    val seed: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
) {
    fun toRules(): MixRules = MixRules(
        genreAllow = splitCsv(genreAllow),
        genreRequire = splitCsv(genreRequire),
        genreExclude = splitCsv(genreExclude),
        toneMin = toneMin,
        toneMax = toneMax,
        energyMin = energyMin,
        energyMax = energyMax,
        ratingMin = ratingMin,
        ratingMax = ratingMax,
        likeBias = likeBias,
        newBias = newBias,
        seed = seed,
    )

    fun toDocument(): MixDocument = MixDocument(
        id = mixId,
        name = name,
        icon = icon,
        rules = toRules(),
    )

    companion object {
        fun fromDocument(doc: MixDocument, rowId: Long = 0): MixEntity {
            val r = doc.rules
            return MixEntity(
                id = rowId,
                mixId = doc.id.ifBlank { UUID.randomUUID().toString() },
                name = doc.name,
                icon = doc.icon,
                genreAllow = r.genreAllow.joinToString(","),
                genreRequire = r.genreRequire.joinToString(","),
                genreExclude = r.genreExclude.joinToString(","),
                toneMin = r.toneMin,
                toneMax = r.toneMax,
                energyMin = r.energyMin,
                energyMax = r.energyMax,
                ratingMin = r.ratingMin,
                ratingMax = r.ratingMax,
                likeBias = r.likeBias,
                newBias = r.newBias,
                seed = r.seed,
            )
        }

        fun fromRules(name: String, rules: MixRules, rowId: Long = 0, mixId: String? = null): MixEntity =
            fromDocument(
                MixDocument(id = mixId ?: UUID.randomUUID().toString(), name = name, rules = rules),
                rowId,
            )

        private fun splitCsv(s: String): List<String> =
            s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
