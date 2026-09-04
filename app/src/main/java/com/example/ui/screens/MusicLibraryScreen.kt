package com.example.ui.screens

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.ui.components.BrandLogo
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.PermissionView
import com.example.ui.components.SongItem
import com.example.ui.components.VideoLibraryView
import com.example.ui.components.VlcVideoPlayerView
import com.example.ui.theme.MusicBorder
import com.example.ui.theme.MusicDarkBackground
import com.example.ui.theme.MusicDarkSurface
import com.example.ui.theme.MusicDarkSurfaceVariant
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.MusicSecondary
import com.example.ui.theme.MusicTextPrimary
import com.example.ui.theme.MusicTextSecondary
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.MainMediaTab
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryScreen(
    viewModel: MusicViewModel,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Deletion states
    var songPendingDeletion by remember { mutableStateOf<Song?>(null) }
    var videoPendingDeletion by remember { mutableStateOf<VideoItem?>(null) }

    // System delete launcher for Android 11+ Scoped Storage
    val systemDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            songPendingDeletion?.let { song ->
                viewModel.onSongDeletedFromSystem(song.id)
                Toast.makeText(context, "Song deleted successfully", Toast.LENGTH_SHORT).show()
                songPendingDeletion = null
            }
            videoPendingDeletion?.let { video ->
                viewModel.onVideoDeletedFromSystem(video.id)
                Toast.makeText(context, "Video deleted successfully", Toast.LENGTH_SHORT).show()
                videoPendingDeletion = null
            }
        } else {
            songPendingDeletion = null
            videoPendingDeletion = null
        }
    }

    if (!uiState.hasPermission) {
        PermissionView(
            onRequestPermission = onRequestPermission,
            onOpenSettings = onOpenSettings
        )
        return
    }

    // Video Player Fullscreen Modal (VLC Style)
    if (uiState.activeVideoPlaying != null) {
        VlcVideoPlayerView(
            video = uiState.activeVideoPlaying!!,
            onClose = { viewModel.closeVideoPlayer() },
            onDeleteVideo = { video ->
                videoPendingDeletion = video
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MusicDarkBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = MusicDarkBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // MiniPlayer if a song is loaded and we are not watching a video
                if (uiState.currentSong != null && uiState.activeVideoPlaying == null) {
                    MiniPlayer(
                        song = uiState.currentSong!!,
                        isPlaying = uiState.isPlaying,
                        currentPositionMs = uiState.currentPositionMs,
                        durationMs = uiState.durationMs,
                        onPreviousClick = { viewModel.skipToPrevious() },
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.skipToNext() },
                        onClick = { viewModel.setFullPlayerVisible(true) }
                    )
                }

                // Modern Navigation Bar for Music & Videos Tabs
                NavigationBar(
                    containerColor = MusicDarkSurface,
                    contentColor = MusicPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    NavigationBarItem(
                        selected = uiState.mainTab == MainMediaTab.MUSIC,
                        onClick = { viewModel.setMainTab(MainMediaTab.MUSIC) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Music",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Music",
                                fontWeight = if (uiState.mainTab == MainMediaTab.MUSIC) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MusicPrimary,
                            indicatorColor = MusicPrimary,
                            unselectedIconColor = MusicTextSecondary,
                            unselectedTextColor = MusicTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_music")
                    )

                    NavigationBarItem(
                        selected = uiState.mainTab == MainMediaTab.VIDEOS,
                        onClick = { viewModel.setMainTab(MainMediaTab.VIDEOS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Videos",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Videos (${uiState.videos.size})",
                                fontWeight = if (uiState.mainTab == MainMediaTab.VIDEOS) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MusicPrimary,
                            indicatorColor = MusicPrimary,
                            unselectedIconColor = MusicTextSecondary,
                            unselectedTextColor = MusicTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_videos")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Row with App Logo & Branding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BrandLogo(
                            size = 40.dp,
                            cornerRadius = 10.dp,
                            showGlow = true
                        )

                        Column {
                            Text(
                                text = if (uiState.mainTab == MainMediaTab.MUSIC) "PulsePlay Music" else "PulsePlay Video",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MusicTextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(MusicPrimary)
                                )
                                val mediaStats = if (uiState.mainTab == MainMediaTab.MUSIC) {
                                    "${uiState.totalSongCount} songs • Auto-detect active"
                                } else {
                                    "${uiState.videos.size} videos found • VLC gestures enabled"
                                }
                                Text(
                                    text = mediaStats,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MusicSecondary
                                )
                            }
                        }
                    }

                    // Refresh Button
                    FilledTonalButton(
                        onClick = { viewModel.refreshLibrary() },
                        modifier = Modifier.testTag("button_refresh_library"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MusicDarkSurfaceVariant,
                            contentColor = MusicPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        val isAnyLoading = uiState.isLoading || uiState.isVideoLoading
                        if (isAnyLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MusicPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Scanning...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Refresh",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Global Search Bar
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("search_input"),
                    placeholder = {
                        Text(
                            text = if (uiState.mainTab == MainMediaTab.MUSIC) {
                                "Search songs, artists, albums..."
                            } else {
                                "Search videos or folders..."
                            },
                            color = MusicTextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MusicTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MusicTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MusicDarkSurface,
                        unfocusedContainerColor = MusicDarkSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MusicTextPrimary,
                        unfocusedTextColor = MusicTextPrimary
                    )
                )

                // Render Active Tab Content
                if (uiState.mainTab == MainMediaTab.MUSIC) {
                    // Music Filter Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryFilterChip(
                            selected = uiState.selectedTab == LibraryTab.ALL_SONGS,
                            onClick = { viewModel.setTab(LibraryTab.ALL_SONGS) },
                            label = "All Songs (${uiState.songs.size})",
                            testTag = "tab_all_songs"
                        )

                        LibraryFilterChip(
                            selected = uiState.selectedTab == LibraryTab.RECENTLY_ADDED,
                            onClick = { viewModel.setTab(LibraryTab.RECENTLY_ADDED) },
                            label = "Recently Added (${uiState.recentlyAddedSongs.size})",
                            testTag = "tab_recently_added"
                        )

                        LibraryFilterChip(
                            selected = uiState.selectedTab == LibraryTab.FAVORITES,
                            onClick = { viewModel.setTab(LibraryTab.FAVORITES) },
                            label = "Favorites (${uiState.favoriteSongs.size})",
                            testTag = "tab_favorites"
                        )
                    }

                    // Music List
                    if (uiState.displaySongs.isEmpty()) {
                        EmptyLibraryState(
                            tab = uiState.selectedTab,
                            searchQuery = uiState.searchQuery,
                            isLoading = uiState.isLoading,
                            onScanClick = { viewModel.refreshLibrary() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = uiState.displaySongs,
                                key = { it.id }
                            ) { song ->
                                SongItem(
                                    song = song,
                                    isCurrentSong = song.id == uiState.currentSong?.id,
                                    isPlaying = uiState.isPlaying,
                                    onSongClick = {
                                        viewModel.playSong(song, uiState.displaySongs)
                                    },
                                    onFavoriteToggle = {
                                        viewModel.toggleFavorite(song)
                                    },
                                    onDeleteClick = {
                                        songPendingDeletion = song
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Video Part: VLC Video View
                    VideoLibraryView(
                        videos = uiState.displayVideos,
                        folders = uiState.videoFolders,
                        selectedFolder = uiState.selectedVideoFolder,
                        searchQuery = uiState.searchQuery,
                        isLoading = uiState.isVideoLoading,
                        onFolderSelect = { viewModel.setVideoFolder(it) },
                        onVideoClick = { viewModel.openVideoPlayer(it) },
                        onDeleteVideo = { video ->
                            videoPendingDeletion = video
                        },
                        onRefresh = { viewModel.refreshLibrary() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Full-Screen Music Player sheet
            FullPlayerSheet(
                song = uiState.currentSong,
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                isShuffle = uiState.isShuffle,
                repeatMode = uiState.repeatMode,
                volume = uiState.volume,
                isVisible = uiState.isFullPlayerVisible,
                onCollapse = { viewModel.setFullPlayerVisible(false) },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.skipToNext() },
                onPrevious = { viewModel.skipToPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onVolumeChange = { viewModel.setVolume(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) }
            )
        }
    }

    // Confirmation Dialog for Song Deletion
    songPendingDeletion?.let { song ->
        DeleteConfirmationDialog(
            itemName = song.title,
            isMusic = true,
            onConfirm = {
                val pendingIntent = viewModel.getSongDeleteIntent(song)
                if (pendingIntent != null) {
                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    systemDeleteLauncher.launch(intentSenderRequest)
                } else {
                    viewModel.deleteSongDirectly(song) { success ->
                        if (success) {
                            Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not delete song", Toast.LENGTH_SHORT).show()
                        }
                        songPendingDeletion = null
                    }
                }
            },
            onDismiss = { songPendingDeletion = null }
        )
    }

    // Confirmation Dialog for Video Deletion
    videoPendingDeletion?.let { video ->
        DeleteConfirmationDialog(
            itemName = video.title,
            isMusic = false,
            onConfirm = {
                val pendingIntent = viewModel.getVideoDeleteIntent(video)
                if (pendingIntent != null) {
                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    systemDeleteLauncher.launch(intentSenderRequest)
                } else {
                    viewModel.deleteVideoDirectly(video) { success ->
                        if (success) {
                            Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not delete video", Toast.LENGTH_SHORT).show()
                        }
                        videoPendingDeletion = null
                    }
                }
            },
            onDismiss = { videoPendingDeletion = null }
        )
    }
}

@Composable
private fun LibraryFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        modifier = Modifier.testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MusicPrimary,
            selectedLabelColor = Color.White,
            containerColor = MusicDarkSurface,
            labelColor = MusicTextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) Color.Transparent else MusicBorder
        )
    )
}

@Composable
private fun EmptyLibraryState(
    tab: LibraryTab,
    searchQuery: String,
    isLoading: Boolean,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandLogo(
            size = 76.dp,
            cornerRadius = 18.dp,
            showGlow = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        val title = when {
            searchQuery.isNotEmpty() -> "No matches found"
            tab == LibraryTab.FAVORITES -> "No favorites yet"
            tab == LibraryTab.RECENTLY_ADDED -> "No recent downloads detected"
            else -> "No audio files found in MediaStore"
        }

        val description = when {
            searchQuery.isNotEmpty() -> "We couldn't find any songs matching \"$searchQuery\"."
            tab == LibraryTab.FAVORITES -> "Tap the heart icon on any song to save it to your favorites."
            tab == LibraryTab.RECENTLY_ADDED -> "Downloaded audio files from Chrome, Telegram, WhatsApp, or Downloads folder will automatically appear here."
            else -> "The app queries Android's MediaStore for your local music. Any audio files stored in Music or Downloads will automatically appear here."
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MusicTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = MusicTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onScanClick,
            enabled = !isLoading,
            modifier = Modifier.testTag("button_scan_mediastore"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MusicPrimary,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scanning MediaStore...", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh MediaStore Library", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
