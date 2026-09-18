package com.jacob77.bopsearch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        QueueItemEntity::class,
        MixEntity::class,
        TrackMetaEntity::class,
        CurationEventEntity::class,
        PlayQueueItemEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BopDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao
    abstract fun mixDao(): MixDao
    abstract fun trackMetaDao(): TrackMetaDao
    abstract fun curationEventDao(): CurationEventDao
    abstract fun playQueueDao(): PlayQueueDao

    companion object {
        @Volatile
        private var instance: BopDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mixes (
                      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      mixId TEXT NOT NULL,
                      name TEXT NOT NULL,
                      icon TEXT,
                      genreAllow TEXT NOT NULL,
                      genreRequire TEXT NOT NULL,
                      genreExclude TEXT NOT NULL,
                      toneMin REAL,
                      toneMax REAL,
                      energyMin REAL,
                      energyMax REAL,
                      ratingMin REAL,
                      ratingMax REAL,
                      likeBias REAL NOT NULL,
                      newBias REAL NOT NULL,
                      seed INTEGER,
                      createdAtEpochMs INTEGER NOT NULL,
                      updatedAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS track_meta (
                      trackId TEXT NOT NULL PRIMARY KEY,
                      title TEXT NOT NULL,
                      path TEXT NOT NULL,
                      rating INTEGER NOT NULL,
                      playCount INTEGER NOT NULL,
                      lastPlayedEpochMs INTEGER,
                      dateAddedEpochMs INTEGER NOT NULL,
                      genreIds TEXT NOT NULL,
                      tone REAL,
                      energy REAL,
                      blacklisted INTEGER NOT NULL,
                      updatedAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS curation_events (
                      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      trackId TEXT NOT NULL,
                      action TEXT NOT NULL,
                      ratingBefore INTEGER NOT NULL,
                      ratingAfter INTEGER NOT NULL,
                      createdAtEpochMs INTEGER NOT NULL,
                      synced INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS play_queue (
                      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      position INTEGER NOT NULL,
                      trackId TEXT NOT NULL,
                      title TEXT NOT NULL,
                      path TEXT NOT NULL,
                      sizeBytes INTEGER NOT NULL,
                      sourceLabel TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): BopDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BopDatabase::class.java,
                    "bop-search.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
