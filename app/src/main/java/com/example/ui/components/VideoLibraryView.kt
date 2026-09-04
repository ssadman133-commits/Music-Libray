package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoItem
import com.example.ui.theme.MusicBorder
import com.example.ui.theme.MusicDarkSurface
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.MusicTextPrimary
import com.example.ui.theme.MusicTextSecondary

@Composable
fun VideoLibraryView(
    videos: List<VideoItem>,
    folders: List<String>,
    selectedFolder: String?,
    searchQuery: String,
    isLoading: Boolean,
    onFolderSelect: (String?) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        // Folders Filter Chips row
        if (folders.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFolder == null,
                    onClick = { onFolderSelect(null) },
                    label = {
                        Text(
                            text = "All Videos (${videos.size})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedFolder == null) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("video_chip_all"),
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MusicPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MusicDarkSurface,
                        labelColor = MusicTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFolder == null,
                        borderColor = if (selectedFolder == null) Color.Transparent else MusicBorder
                    )
                )

                folders.forEach { folder ->
                    val isSelected = selectedFolder.equals(folder, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFolderSelect(if (isSelected) null else folder) },
                        label = {
                            Text(
                                text = folder,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("video_chip_$folder"),
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MusicPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MusicDarkSurface,
                            labelColor = MusicTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color.Transparent else MusicBorder
                        )
                    )
                }
            }
        }

        // Videos List or Empty State
        if (videos.isEmpty()) {
            EmptyVideoState(
                searchQuery = searchQuery,
                isLoading = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = videos,
                    key = { it.id }
                ) { video ->
                    VideoItemCard(
                        video = video,
                        onVideoClick = { onVideoClick(video) },
                        onDeleteClick = { onDeleteVideo(video) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyVideoState(
    searchQuery: String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = MusicPrimary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val title = if (searchQuery.isNotEmpty()) "No video matches found" else "No videos found on device"
        val desc = if (searchQuery.isNotEmpty()) {
            "No videos match \"$searchQuery\". Try a different search."
        } else {
            "Videos downloaded from Chrome, WhatsApp, Facebook or recorded via Camera will automatically appear here."
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
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MusicTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRefresh,
            enabled = !isLoading,
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
                Text("Scanning videos...", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Videos Now", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
