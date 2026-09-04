package com.example.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri,
    val dateAdded: Long, // MediaStore DATE_ADDED (in seconds)
    val size: Long,
    val isFavorite: Boolean = false
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
}
