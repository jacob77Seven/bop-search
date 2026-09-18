package com.jacob77.bopsearch.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

private const val TAG = "BopLibrary"

data class Track(
    val id: String,
    val title: String,
    val path: String,
    val sizeBytes: Long,
    val sourceLabel: String = "App library",
)

class LocalLibrary(private val context: Context) {
    private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")

    fun libraryDir(): File {
        val dir = File(context.filesDir, "library")
        if (!dir.exists()) {
            dir.mkdirs()
            Log.i(TAG, "created library dir ${dir.absolutePath}")
        }
        return dir
    }

    /**
     * Scan app-private `files/library/` plus each persisted SAF tree URI.
     * [path] on each [Track] is either an absolute filesystem path or a content:// URI string.
     */
    fun scan(treeUriStrings: List<String> = emptyList()): List<Track> {
        val tracks = mutableListOf<Track>()
        tracks += scanAppLibrary()
        for (uriString in treeUriStrings) {
            tracks += scanDocumentTree(uriString)
        }
        val sorted = tracks.sortedBy { it.title.lowercase() }
        Log.i(TAG, "scan found ${sorted.size} track(s) " +
            "(app library + ${treeUriStrings.size} SAF folder(s))")
        return sorted
    }

    private fun scanAppLibrary(): List<Track> {
        val root = libraryDir()
        val label = "App library"
        return root.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            .map { file ->
                Track(
                    id = file.absolutePath,
                    title = file.nameWithoutExtension,
                    path = file.absolutePath,
                    sizeBytes = file.length(),
                    sourceLabel = label,
                )
            }
            .toList()
    }

    private fun scanDocumentTree(uriString: String): List<Track> {
        val treeUri = Uri.parse(uriString)
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.exists()) {
            Log.w(TAG, "SAF tree unavailable: $uriString")
            return emptyList()
        }
        val label = root.name?.takeIf { it.isNotBlank() }
            ?: treeUri.lastPathSegment?.substringAfterLast(':')
            ?: "Folder"
        val out = mutableListOf<Track>()
        walkDocuments(root, label, out)
        Log.i(TAG, "SAF folder \"$label\" → ${out.size} track(s)")
        return out
    }

    private fun walkDocuments(dir: DocumentFile, sourceLabel: String, out: MutableList<Track>) {
        val children = try {
            dir.listFiles()
        } catch (e: Exception) {
            Log.w(TAG, "listFiles failed under $sourceLabel: ${e.message}")
            return
        }
        for (child in children) {
            when {
                child.isDirectory -> walkDocuments(child, sourceLabel, out)
                child.isFile -> {
                    val name = child.name ?: continue
                    val ext = name.substringAfterLast('.', missingDelimiterValue = "")
                        .lowercase()
                    if (ext !in audioExtensions) continue
                    val uri = child.uri.toString()
                    out += Track(
                        id = uri,
                        title = name.substringBeforeLast('.'),
                        path = uri,
                        sizeBytes = child.length(),
                        sourceLabel = sourceLabel,
                    )
                }
            }
        }
    }
}
