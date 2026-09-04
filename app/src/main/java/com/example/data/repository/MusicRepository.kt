package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteSongEntity
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val favoriteDao: FavoriteDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastScanResult = MutableStateFlow<String?>(null)
    val lastScanResult: StateFlow<String?> = _lastScanResult.asStateFlow()

    private var contentObserver: ContentObserver? = null

    // Combine raw songs from MediaStore with database favorites
    val songs: StateFlow<List<Song>> = combine(
        _rawSongs,
        favoriteDao.getAllFavoriteIds()
    ) { songList, favoriteIds ->
        val favoriteSet = favoriteIds.toSet()
        songList.map { song ->
            song.copy(isFavorite = favoriteSet.contains(song.id))
        }
    }.stateIn(
        scope = repositoryScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        registerContentObserver()
    }

    private fun registerContentObserver() {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    Log.d("MusicRepository", "MediaStore content change event received: $uri. Performing auto-scan...")
                    repositoryScope.launch {
                        scanSongsInternal()
                    }
                }
            }
            contentObserver = observer

            val externalUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.registerContentObserver(externalUri, true, observer)
            Log.d("MusicRepository", "Successfully registered ContentObserver on MediaStore.Audio.Media.EXTERNAL_CONTENT_URI")
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error registering MediaStore ContentObserver", e)
        }
    }

    suspend fun scanSongs(): List<Song> {
        return withContext(Dispatchers.IO) {
            scanSongsInternal()
        }
    }

    private suspend fun scanSongsInternal(): List<Song> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val songList = mutableListOf<Song>()

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )

        // Comprehensive audio selection:
        // Catches standard music files, any audio/* mime types, and known audio file extensions
        // ensuring newly downloaded files from browsers, messaging apps, and file managers are included.
        val selection = "(" +
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
            "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' OR " +
            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.mp3' OR " +
            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.m4a' OR " +
            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.flac' OR " +
            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.wav' OR " +
            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.ogg' OR " +
            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.aac' OR " +
            "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%.opus'" +
            ") AND ${MediaStore.Audio.Media.SIZE} > 0"

        // Default sort order: DATE_ADDED DESC (newly downloaded/added audio files first)
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val displayName = cursor.getString(displayNameCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val size = cursor.getLong(sizeCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = ContentUris.withAppendedId(albumArtBaseUri, albumId)

                    val title = when {
                        !rawTitle.isNullOrBlank() && rawTitle != "<unknown>" -> rawTitle.trim()
                        !displayName.isNullOrBlank() -> displayName.substringBeforeLast('.').trim()
                        else -> "Audio $id"
                    }

                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") {
                        "Unknown Artist"
                    } else {
                        rawArtist.trim()
                    }

                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") {
                        "Unknown Album"
                    } else {
                        rawAlbum.trim()
                    }

                    songList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            durationMs = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            dateAdded = dateAdded,
                            size = size
                        )
                    )
                }
            }
            _lastScanResult.value = "Found ${songList.size} audio files in MediaStore"
            Log.d("MusicRepository", "MediaStore scan complete. Found ${songList.size} audio files on device.")
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error querying MediaStore", e)
            _lastScanResult.value = "Error scanning MediaStore: ${e.message}"
        } finally {
            _isLoading.value = false
        }

        _rawSongs.value = songList
        songList
    }

    suspend fun toggleFavorite(songId: Long) {
        withContext(Dispatchers.IO) {
            val isFav = favoriteDao.isFavorite(songId)
            if (isFav) {
                favoriteDao.removeFavorite(songId)
            } else {
                favoriteDao.addFavorite(FavoriteSongEntity(songId = songId))
            }
        }
    }

    fun unregisterContentObserver() {
        contentObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
                Log.d("MusicRepository", "Unregistered MediaStore ContentObserver")
            } catch (e: Exception) {
                Log.e("MusicRepository", "Error unregistering ContentObserver", e)
            }
        }
    }
}
