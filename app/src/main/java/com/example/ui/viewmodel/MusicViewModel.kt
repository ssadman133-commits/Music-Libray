package com.example.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MusicDatabase
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.MusicRepository
import com.example.data.repository.VideoRepository
import com.example.service.MusicController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainMediaTab(val title: String) {
    MUSIC("Music"),
    VIDEOS("Videos")
}

enum class LibraryTab(val title: String) {
    ALL_SONGS("All Songs"),
    RECENTLY_ADDED("Recently Added"),
    FAVORITES("Favorites")
}

data class MusicUiState(
    val songs: List<Song> = emptyList(),
    val recentlyAddedSongs: List<Song> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val displaySongs: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: Int = 0,
    val volume: Float = 1f,
    val selectedTab: LibraryTab = LibraryTab.ALL_SONGS,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val isFullPlayerVisible: Boolean = false,
    val totalSongCount: Int = 0,
    val lastScanResult: String? = null,

    // Video State
    val mainTab: MainMediaTab = MainMediaTab.MUSIC,
    val videos: List<VideoItem> = emptyList(),
    val displayVideos: List<VideoItem> = emptyList(),
    val activeVideoPlaying: VideoItem? = null,
    val isVideoLoading: Boolean = false,
    val selectedVideoFolder: String? = null, // null for All Videos
    val videoFolders: List<String> = emptyList()
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MusicDatabase.getDatabase(application)
    val repository = MusicRepository(application, database.favoriteDao())
    val videoRepository = VideoRepository(application)
    val musicController = MusicController(application)

    private val _mainTab = MutableStateFlow(MainMediaTab.MUSIC)
    val mainTab: StateFlow<MainMediaTab> = _mainTab.asStateFlow()

    private val _selectedTab = MutableStateFlow(LibraryTab.ALL_SONGS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    private val _activeVideoPlaying = MutableStateFlow<VideoItem?>(null)
    val activeVideoPlaying: StateFlow<VideoItem?> = _activeVideoPlaying.asStateFlow()

    private val _selectedVideoFolder = MutableStateFlow<String?>(null)
    val selectedVideoFolder: StateFlow<String?> = _selectedVideoFolder.asStateFlow()

    // Combining all sources into single reactive UI state
    val uiState: StateFlow<MusicUiState> = combine(
        repository.songs,
        repository.isLoading,
        repository.lastScanResult,
        musicController.currentSongId,
        musicController.isPlaying,
        musicController.currentPositionMs,
        musicController.durationMs,
        musicController.isShuffleEnabled,
        musicController.repeatMode,
        musicController.volume,
        _selectedTab,
        _searchQuery,
        _hasPermission,
        _isFullPlayerVisible,
        _mainTab,
        videoRepository.videos,
        videoRepository.isLoading,
        _activeVideoPlaying,
        _selectedVideoFolder
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val songs = args[0] as List<Song>
        val isLoading = args[1] as Boolean
        val lastScanResult = args[2] as String?
        val currentSongId = args[3] as Long?
        val isPlaying = args[4] as Boolean
        val currentPositionMs = args[5] as Long
        val durationMs = args[6] as Long
        val isShuffle = args[7] as Boolean
        val repeatMode = args[8] as Int
        val volume = args[9] as Float
        val selectedTab = args[10] as LibraryTab
        val searchQuery = args[11] as String
        val hasPermission = args[12] as Boolean
        val isFullPlayerVisible = args[13] as Boolean
        val mainTab = args[14] as MainMediaTab
        val videos = args[15] as List<VideoItem>
        val isVideoLoading = args[16] as Boolean
        val activeVideo = args[17] as VideoItem?
        val selectedFolder = args[18] as String?

        val currentSong = songs.find { it.id == currentSongId }

        // Recently Added sorted by dateAdded descending
        val recentlyAdded = songs.sortedByDescending { it.dateAdded }
        val favorites = songs.filter { it.isFavorite }

        // Filter songs by tab
        val tabSongs = when (selectedTab) {
            LibraryTab.ALL_SONGS -> songs
            LibraryTab.RECENTLY_ADDED -> recentlyAdded
            LibraryTab.FAVORITES -> favorites
        }

        // Apply search query across title, artist, and album for songs
        val displaySongs = if (searchQuery.isBlank()) {
            tabSongs
        } else {
            val query = searchQuery.trim().lowercase()
            tabSongs.filter {
                it.title.lowercase().contains(query) ||
                    it.artist.lowercase().contains(query) ||
                    it.album.lowercase().contains(query)
            }
        }

        // Distinct folders for videos
        val folders = videos.map { it.folderName }.distinct().sorted()

        // Filter videos by folder & search query
        val folderFilteredVideos = if (selectedFolder == null) {
            videos
        } else {
            videos.filter { it.folderName.equals(selectedFolder, ignoreCase = true) }
        }

        val displayVideos = if (searchQuery.isBlank()) {
            folderFilteredVideos
        } else {
            val query = searchQuery.trim().lowercase()
            folderFilteredVideos.filter {
                it.title.lowercase().contains(query) ||
                    it.displayName.lowercase().contains(query) ||
                    it.folderName.lowercase().contains(query)
            }
        }

        MusicUiState(
            songs = songs,
            recentlyAddedSongs = recentlyAdded,
            favoriteSongs = favorites,
            displaySongs = displaySongs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = if (durationMs > 0L) durationMs else (currentSong?.durationMs ?: 0L),
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            volume = volume,
            selectedTab = selectedTab,
            searchQuery = searchQuery,
            isLoading = isLoading,
            hasPermission = hasPermission,
            isFullPlayerVisible = isFullPlayerVisible,
            totalSongCount = songs.size,
            lastScanResult = lastScanResult,
            mainTab = mainTab,
            videos = videos,
            displayVideos = displayVideos,
            activeVideoPlaying = activeVideo,
            isVideoLoading = isVideoLoading,
            selectedVideoFolder = selectedFolder,
            videoFolders = folders
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MusicUiState()
    )

    fun onPermissionResult(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            repository.scanSongs()
        }
        viewModelScope.launch {
            videoRepository.scanVideos()
        }
    }

    fun setMainTab(tab: MainMediaTab) {
        _mainTab.value = tab
    }

    fun setTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun setVideoFolder(folder: String?) {
        _selectedVideoFolder.value = folder
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFullPlayerVisible(visible: Boolean) {
        _isFullPlayerVisible.value = visible
    }

    fun openVideoPlayer(video: VideoItem) {
        // Pause music if playing when user opens a video
        if (musicController.isPlaying.value) {
            musicController.togglePlayPause()
        }
        _activeVideoPlaying.value = video
    }

    fun closeVideoPlayer() {
        _activeVideoPlaying.value = null
    }

    fun playSong(song: Song, playlist: List<Song>) {
        val index = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        musicController.playSongList(playlist, index)
    }

    fun togglePlayPause() {
        musicController.togglePlayPause()
    }

    fun skipToNext() {
        musicController.skipToNext()
    }

    fun skipToPrevious() {
        musicController.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        musicController.seekTo(positionMs)
    }

    fun toggleShuffle() {
        musicController.toggleShuffle()
    }

    fun cycleRepeatMode() {
        musicController.cycleRepeatMode()
    }

    fun setVolume(volume: Float) {
        musicController.setVolume(volume)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id)
        }
    }

    // Delete Operations
    fun getSongDeleteIntent(song: Song): PendingIntent? {
        return repository.createDeleteIntent(listOf(song.contentUri))
    }

    fun deleteSongDirectly(song: Song, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteDirectly(song.contentUri)
            if (success) {
                repository.removeSongFromLocalState(song.id)
            }
            onComplete(success)
        }
    }

    fun onSongDeletedFromSystem(songId: Long) {
        repository.removeSongFromLocalState(songId)
        viewModelScope.launch {
            repository.scanSongs()
        }
    }

    fun getVideoDeleteIntent(video: VideoItem): PendingIntent? {
        return videoRepository.createDeleteIntent(listOf(video.contentUri))
    }

    fun deleteVideoDirectly(video: VideoItem, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = videoRepository.deleteDirectly(video.contentUri)
            if (success) {
                videoRepository.removeVideoFromLocalState(video.id)
                if (_activeVideoPlaying.value?.id == video.id) {
                    _activeVideoPlaying.value = null
                }
            }
            onComplete(success)
        }
    }

    fun onVideoDeletedFromSystem(videoId: Long) {
        videoRepository.removeVideoFromLocalState(videoId)
        if (_activeVideoPlaying.value?.id == videoId) {
            _activeVideoPlaying.value = null
        }
        viewModelScope.launch {
            videoRepository.scanVideos()
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.unregisterContentObserver()
        videoRepository.unregisterContentObserver()
        musicController.release()
    }
}
