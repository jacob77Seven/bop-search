package com.jacob77.bopsearch.player

import android.content.Context
import android.util.Log
import java.io.File

private const val TAG = "BopLibrary"

data class Track(
    val id: String,
    val title: String,
    val path: String,
    val sizeBytes: Long,
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

    fun scan(): List<Track> {
        val root = libraryDir()
        val files = root.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            .sortedBy { it.name.lowercase() }
            .map { file ->
                Track(
                    id = file.absolutePath,
                    title = file.nameWithoutExtension,
                    path = file.absolutePath,
                    sizeBytes = file.length(),
                )
            }
            .toList()
        Log.i(TAG, "scan found ${files.size} track(s) under ${root.absolutePath}")
        return files
    }
}
