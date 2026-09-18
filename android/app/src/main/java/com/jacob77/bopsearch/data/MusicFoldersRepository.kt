package com.jacob77.bopsearch.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAG = "BopLibrary"

private val Context.musicFoldersStore: DataStore<Preferences> by preferencesDataStore(name = "music_folders")

data class MusicFolder(
    val uriString: String,
    val displayName: String,
)

class MusicFoldersRepository(private val context: Context) {
    private val urisKey = stringSetPreferencesKey("tree_uris")

    val folders: Flow<List<MusicFolder>> = context.musicFoldersStore.data.map { prefs ->
        (prefs[urisKey] ?: emptySet())
            .sorted()
            .map { uriString ->
                MusicFolder(
                    uriString = uriString,
                    displayName = resolveDisplayName(uriString),
                )
            }
    }

    suspend fun currentFolders(): List<MusicFolder> = folders.first()

    suspend fun add(uri: Uri) {
        takePersistable(uri)
        val uriString = uri.toString()
        context.musicFoldersStore.edit { prefs ->
            val current = prefs[urisKey] ?: emptySet()
            prefs[urisKey] = current + uriString
        }
        Log.i(TAG, "added music folder $uriString")
    }

    suspend fun remove(uriString: String) {
        releasePersistable(uriString)
        context.musicFoldersStore.edit { prefs ->
            val current = prefs[urisKey] ?: emptySet()
            prefs[urisKey] = current - uriString
        }
        Log.i(TAG, "removed music folder $uriString")
    }

    private fun takePersistable(uri: Uri) {
        val cr = context.contentResolver
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val write = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            cr.takePersistableUriPermission(uri, read or write)
            Log.i(TAG, "took persistable read+write for $uri")
        } catch (e: SecurityException) {
            cr.takePersistableUriPermission(uri, read)
            Log.i(TAG, "took persistable read-only for $uri (${e.message})")
        }
    }

    private fun releasePersistable(uriString: String) {
        val uri = Uri.parse(uriString)
        val cr = context.contentResolver
        val match = cr.persistedUriPermissions.firstOrNull {
            it.uri == uri || it.uri.toString() == uriString
        } ?: return
        val flags = (if (match.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
            (if (match.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
        if (flags == 0) return
        try {
            cr.releasePersistableUriPermission(match.uri, flags)
            Log.i(TAG, "released persistable permission for ${match.uri}")
        } catch (e: SecurityException) {
            Log.w(TAG, "releasePersistable failed: ${e.message}")
        }
    }

    private fun resolveDisplayName(uriString: String): String {
        val uri = Uri.parse(uriString)
        val name = DocumentFile.fromTreeUri(context, uri)?.name
        if (!name.isNullOrBlank()) return name
        return uri.lastPathSegment?.substringAfterLast(':') ?: uriString
    }
}
