package com.bennyjon.aui.compose.internal

import android.Manifest
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

internal fun saveFileToDownloads(
    context: Context,
    filename: String?,
    language: String?,
    content: String,
): String? {
    val resolvedFilename = resolveFilename(filename = filename, language = language)
    val mimeType = inferMimeType(filename = resolvedFilename, language = language)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveFileToDownloadsScoped(
            context = context,
            filename = resolvedFilename,
            mimeType = mimeType,
            content = content,
        )
    } else {
        saveFileToDownloadsLegacy(
            context = context,
            filename = resolvedFilename,
            mimeType = mimeType,
            content = content,
        )
    }
}

internal fun openDownloadsFolder(context: Context): Boolean {
    return try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

private fun saveFileToDownloadsScoped(
    context: Context,
    filename: String,
    mimeType: String,
    content: String,
): String? {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
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
        filename
    } catch (_: Exception) {
        resolver.delete(uri, null, null)
        null
    }
}

private fun saveFileToDownloadsLegacy(
    context: Context,
    filename: String,
    mimeType: String,
    content: String,
): String? {
    val hasPermission = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return null

    @Suppress("DEPRECATION")
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloadsDir.exists() && !downloadsDir.mkdirs()) return null

    val file = uniqueFile(downloadsDir, filename)
    return try {
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(content)
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType),
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

private fun resolveFilename(filename: String?, language: String?): String {
    val sanitized = filename
        ?.trim()
        ?.replace('\\', '_')
        ?.replace('/', '_')
        ?.takeIf { it.isNotEmpty() }
    if (sanitized != null) return sanitized

    val extension = when (language?.lowercase()) {
        "markdown", "md" -> "md"
        "json" -> "json"
        "yaml", "yml" -> "yml"
        "xml" -> "xml"
        "html" -> "html"
        "swift" -> "swift"
        "kotlin", "kt" -> "kt"
        "java" -> "java"
        "javascript", "js" -> "js"
        "typescript", "ts" -> "ts"
        "bash", "shell", "sh" -> "sh"
        else -> "txt"
    }
    return "artifact.$extension"
}

private fun inferMimeType(filename: String, language: String?): String {
    val extension = filename.substringAfterLast('.', "").ifBlank {
        when (language?.lowercase()) {
            "markdown", "md" -> "md"
            "json" -> "json"
            "yaml", "yml" -> "yml"
            "xml" -> "xml"
            "html" -> "html"
            else -> ""
        }
    }
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
        ?: when (language?.lowercase()) {
            "markdown", "md" -> "text/markdown"
            "json" -> "application/json"
            "yaml", "yml" -> "application/yaml"
            "xml" -> "application/xml"
            "html" -> "text/html"
            else -> "text/plain"
        }
}
