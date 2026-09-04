package com.example.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val size: Long,
    val contentUri: Uri,
    val dateAdded: Long,
    val resolution: String, // e.g. "1920x1080"
    val folderName: String
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "0:00"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val remMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remMinutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 MB"
            val mb = size / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                val gb = mb / 1024.0
                String.format("%.1f GB", gb)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
