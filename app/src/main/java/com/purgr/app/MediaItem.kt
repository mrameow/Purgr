package com.purgr.app

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long,
    val sizeBytes: Long,
    val name: String,
)
