package com.purgr.app

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(context: Context) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val filesUri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    /** Every photo and video on the device, newest first. Trashed items are excluded by MediaStore. */
    suspend fun loadLibrary(): List<MediaItem> = query(trashed = false)

    /** Items currently sitting in the system trash (restorable). */
    suspend fun loadTrashed(): List<MediaItem> = query(trashed = true)

    private suspend fun query(trashed: Boolean): List<MediaItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.MediaColumns.DURATION,
        )
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                ),
            )
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns._ID} DESC",
            )
            putInt(
                MediaStore.QUERY_ARG_MATCH_TRASHED,
                if (trashed) MediaStore.MATCH_ONLY else MediaStore.MATCH_EXCLUDE,
            )
        }
        val result = ArrayList<MediaItem>()
        resolver.query(filesUri, projection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val durCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val isVideo = c.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                }
                result += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(base, id),
                    isVideo = isVideo,
                    durationMs = if (isVideo) c.getLong(durCol) else 0L,
                    sizeBytes = c.getLong(sizeCol),
                    name = c.getString(nameCol).orEmpty(),
                )
            }
        }
        result
    }

    /** System dialog asking permission to move [items] to the trash (or back out of it). */
    fun trashRequest(items: List<MediaItem>, trash: Boolean): IntentSender =
        MediaStore.createTrashRequest(resolver, items.map { it.uri }, trash).intentSender

    /** System dialog asking permission to permanently delete [items]. */
    fun deleteRequest(items: List<MediaItem>): IntentSender =
        MediaStore.createDeleteRequest(resolver, items.map { it.uri }).intentSender
}
