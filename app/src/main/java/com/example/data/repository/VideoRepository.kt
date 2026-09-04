package com.example.data.repository

import android.app.Activity
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(
    private val context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastScanResult = MutableStateFlow<String?>(null)
    val lastScanResult: StateFlow<String?> = _lastScanResult.asStateFlow()

    private var contentObserver: ContentObserver? = null

    init {
        registerContentObserver()
    }

    private fun registerContentObserver() {
        try {
            contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    Log.d("VideoRepository", "MediaStore video changed at: $uri. Auto re-scanning...")
                    repositoryScope.launch {
                        scanVideos()
                    }
                }
            }

            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver!!
            )
            Log.d("VideoRepository", "Successfully registered MediaStore Video ContentObserver")
        } catch (e: Exception) {
            Log.e("VideoRepository", "Failed to register ContentObserver for videos", e)
        }
    }

    suspend fun scanVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val videoList = mutableListOf<VideoItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val displayName = cursor.getString(nameCol) ?: "Video_$id"
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)

                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else ""

                    val folderName = if (bucketCol != -1) cursor.getString(bucketCol) ?: "Videos" else "Videos"

                    val title = if (!rawTitle.isNullOrBlank()) rawTitle else displayName

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    videoList.add(
                        VideoItem(
                            id = id,
                            title = title,
                            displayName = displayName,
                            durationMs = duration,
                            size = size,
                            contentUri = contentUri,
                            dateAdded = dateAdded,
                            resolution = resolution,
                            folderName = folderName
                        )
                    )
                }
            }
            _lastScanResult.value = "Found ${videoList.size} videos in storage"
            Log.d("VideoRepository", "Video scan complete. Found ${videoList.size} videos.")
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error querying Video MediaStore", e)
            _lastScanResult.value = "Error scanning videos: ${e.message}"
        } finally {
            _isLoading.value = false
        }

        _videos.value = videoList
        videoList
    }

    /**
     * Delete video with modern Android 10+ createDeleteRequest or fallback direct delete
     */
    fun createDeleteIntent(uris: List<Uri>): PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return MediaStore.createDeleteRequest(context.contentResolver, uris)
        }
        return null
    }

    suspend fun deleteDirectly(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) {
                scanVideos()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Direct delete failed", e)
            false
        }
    }

    fun removeVideoFromLocalState(videoId: Long) {
        _videos.value = _videos.value.filter { it.id != videoId }
    }

    fun unregisterContentObserver() {
        contentObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e("VideoRepository", "Error unregistering ContentObserver", e)
            }
        }
    }
}
