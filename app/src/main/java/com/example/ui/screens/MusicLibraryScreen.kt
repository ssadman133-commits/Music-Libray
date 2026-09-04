package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.BrandLogo
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.PermissionView
import com.example.ui.components.SongItem
import com.example.ui.theme.MusicBorder
import com.example.ui.theme.MusicDarkBackground
import com.example.ui.theme.MusicDarkSurface
import com.example.ui.theme.MusicDarkSurfaceVariant
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.MusicSecondary
import com.example.ui.theme.MusicTextPrimary
import com.example.ui.theme.MusicTextSecondary
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryScreen(
    viewModel: MusicViewModel,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    if (!uiState.hasPermission) {
        PermissionView(
            onRequestPermission = onRequestPermission,
            onOpenSettings = onOpenSettings
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MusicDarkBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = MusicDarkBackground,
        bottomBar = {
            if (uiState.currentSong != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
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
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Row with App Logo & Branding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                text = "Music Library",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
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
                                Text(
                                    text = "MediaStore: ${uiState.totalSongCount} songs • Auto-detect active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MusicSecondary
                                )
                            }
                        }
                    }

                    // Prominent Refresh / Scan Button in Violet styling
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
                        if (uiState.isLoading) {
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
                                contentDescription = "Refresh Library",
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

                // Search Bar
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("search_input"),
                    placeholder = {
                        Text(
                            text = "Search songs, artists, albums...",
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

                // Tabs: All Songs, Recently Added, Favorites
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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

                // Songs List or Empty State
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
                                }
                            )
                        }
                    }
                }
            }

            // Full-Screen Player overlay
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
        // App Brand Logo with Soft Neon Glow
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
            else -> "The app queries Android's MediaStore for your local music. On a physical Android device, any audio files (MP3, M4A, FLAC, WAV, OGG) stored in Music or Downloads will automatically appear here."
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

        // Scan button directly in empty state
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
