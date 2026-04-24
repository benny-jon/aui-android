package com.bennyjon.auiandroid.settings

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

internal const val SYSTEM_PROMPT_FILENAME = "aui_catalog_prompt.md"
private const val SYSTEM_PROMPT_MIME_TYPE = "text/markdown"

internal fun saveSystemPromptToDownloads(context: Context, content: String): String? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveScoped(context, content)
    } else {
        saveLegacy(context, content)
    }
}

private fun saveScoped(context: Context, content: String): String? {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, SYSTEM_PROMPT_FILENAME)
        put(MediaStore.MediaColumns.MIME_TYPE, SYSTEM_PROMPT_MIME_TYPE)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null

    return try {
        resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(content)
        } ?: return null

        val finalizeValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        resolver.update(uri, finalizeValues, null, null)
        SYSTEM_PROMPT_FILENAME
    } catch (_: Exception) {
        resolver.delete(uri, null, null)
        null
    }
}

private fun saveLegacy(context: Context, content: String): String? {
    val hasPermission = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return null

    @Suppress("DEPRECATION")
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloadsDir.exists() && !downloadsDir.mkdirs()) return null

    val file = uniqueFile(downloadsDir, SYSTEM_PROMPT_FILENAME)
    return try {
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(content)
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(SYSTEM_PROMPT_MIME_TYPE),
            null,
        )
        file.name
    } catch (_: Exception) {
        null
    }
}

private fun uniqueFile(directory: File, filename: String): File {
    val dotIndex = filename.lastIndexOf('.')
    val base = if (dotIndex > 0) filename.substring(0, dotIndex) else filename
    val extension = if (dotIndex > 0) filename.substring(dotIndex) else ""
    var candidate = File(directory, filename)
    var counter = 1

    while (candidate.exists()) {
        candidate = File(directory, "$base ($counter)$extension")
        counter++
    }

    return candidate
}
