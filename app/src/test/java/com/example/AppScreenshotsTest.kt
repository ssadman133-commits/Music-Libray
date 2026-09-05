package com.example

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.ui.components.BrandLogo
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.PermissionView
import com.example.ui.components.SongItem
import com.example.ui.components.VideoItemCard
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MusicBorder
import com.example.ui.theme.MusicDarkBackground
import com.example.ui.theme.MusicDarkSurface
import com.example.ui.theme.MusicDarkSurfaceVariant
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.MusicTextPrimary
import com.example.ui.theme.MusicTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AppScreenshotsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleSong1 = Song(
    id = 1L,
    title = "Starboy (feat. Daft Punk)",
    artist = "The Weeknd",
    album = "Starboy",
    albumId = 101L,
    durationMs = 230000L,
    contentUri = Uri.parse("content://media/external/audio/media/1"),
    albumArtUri = Uri.parse("content://media/external/audio/albumart/101"),
    dateAdded = 1700000000L,
    size = 9437184L,
    isFavorite = true
  )

  private val sampleSong2 = Song(
    id = 2L,
    title = "Blinding Lights",
    artist = "The Weeknd",
    album = "After Hours",
    albumId = 102L,
    durationMs = 200000L,
    contentUri = Uri.parse("content://media/external/audio/media/2"),
    albumArtUri = Uri.parse("content://media/external/audio/albumart/102"),
    dateAdded = 1700001000L,
    size = 8388608L,
    isFavorite = false
  )

  private val sampleSong3 = Song(
    id = 3L,
    title = "Midnight City",
    artist = "M83",
    album = "Hurry Up, We're Dreaming",
    albumId = 103L,
    durationMs = 243000L,
    contentUri = Uri.parse("content://media/external/audio/media/3"),
    albumArtUri = Uri.parse("content://media/external/audio/albumart/103"),
    dateAdded = 1700002000L,
    size = 10000000L,
    isFavorite = true
  )

  private val sampleVideo1 = VideoItem(
    id = 10L,
    title = "Interstellar - Docking Scene (4K IMAX)",
    displayName = "Interstellar_Docking_4K.mp4",
    durationMs = 312000L,
    size = 250000000L,
    contentUri = Uri.parse("content://media/external/video/media/10"),
    dateAdded = 1700003000L,
    resolution = "3840x2160",
    folderName = "Movies"
  )

  private val sampleVideo2 = VideoItem(
    id = 11L,
    title = "Coldplay - Fix You (Live at São Paulo)",
    displayName = "Coldplay_Live.mp4",
    durationMs = 285000L,
    size = 180000000L,
    contentUri = Uri.parse("content://media/external/video/media/11"),
    dateAdded = 1700004000L,
    resolution = "1920x1080",
    folderName = "Concerts"
  )

  @Test
  fun preview_splash_screen() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SplashScreen()
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/01_splash_screen.png")
  }

  @Test
  fun preview_permission_screen() {
    composeTestRule.setContent {
      MyApplicationTheme {
        PermissionView(
          onRequestPermission = {},
          onOpenSettings = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/02_permission_screen.png")
  }

  @Test
  fun preview_music_library_with_miniplayer() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MusicDarkBackground
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
              // Top Bar
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  BrandLogo(size = 38.dp, cornerRadius = 10.dp, showGlow = false)
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = "PulsePlay",
                      style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                      color = MusicTextPrimary
                    )
                    Text(
                      text = "3 Tracks Available",
                      style = MaterialTheme.typography.bodySmall,
                      color = MusicTextSecondary
                    )
                  }
                }
                IconButton(onClick = {}) {
                  Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MusicPrimary)
                }
              }

              // Search Bar
              TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search songs, artists, albums...", color = MusicTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MusicTextSecondary) },
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = TextFieldDefaults.colors(
                  focusedContainerColor = MusicDarkSurface,
                  unfocusedContainerColor = MusicDarkSurface,
                  focusedIndicatorColor = Color.Transparent,
                  unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
              )

              // Song List
              LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(listOf(sampleSong1, sampleSong2, sampleSong3)) { song ->
                  SongItem(
                    song = song,
                    isPlaying = song.id == sampleSong1.id,
                    isCurrentSong = song.id == sampleSong1.id,
                    onSongClick = {},
                    onFavoriteToggle = {},
                    onDeleteClick = {}
                  )
                }
              }

              // Mini Player
              MiniPlayer(
                song = sampleSong1,
                isPlaying = true,
                currentPositionMs = 85000L,
                durationMs = sampleSong1.durationMs,
                onPreviousClick = {},
                onPlayPauseClick = {},
                onNextClick = {},
                onClick = {},
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )

              // Bottom Navigation
              NavigationBar(
                containerColor = MusicDarkSurface,
                tonalElevation = 8.dp
              ) {
                NavigationBarItem(
                  selected = true,
                  onClick = {},
                  icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                  label = { Text("Music") },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MusicPrimary,
                    selectedTextColor = MusicPrimary,
                    unselectedIconColor = MusicTextSecondary,
                    unselectedTextColor = MusicTextSecondary,
                    indicatorColor = MusicDarkSurfaceVariant
                  )
                )
                NavigationBarItem(
                  selected = false,
                  onClick = {},
                  icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                  label = { Text("Videos") },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MusicPrimary,
                    selectedTextColor = MusicPrimary,
                    unselectedIconColor = MusicTextSecondary,
                    unselectedTextColor = MusicTextSecondary,
                    indicatorColor = MusicDarkSurfaceVariant
                  )
                )
              }
            }
          }
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/03_music_library.png")
  }

  @Test
  fun preview_video_library() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MusicDarkBackground
        ) {
          Column(modifier = Modifier.fillMaxSize()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              BrandLogo(size = 38.dp, cornerRadius = 10.dp, showGlow = false)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "PulsePlay Videos",
                  style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                  color = MusicTextPrimary
                )
                Text(
                  text = "2 Videos • VLC-Style Gesture Player",
                  style = MaterialTheme.typography.bodySmall,
                  color = MusicTextSecondary
                )
              }
            }

            LazyColumn(
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              items(listOf(sampleVideo1, sampleVideo2)) { video ->
                VideoItemCard(
                  video = video,
                  onVideoClick = {},
                  onDeleteClick = {}
                )
              }
            }

            NavigationBar(
              containerColor = MusicDarkSurface,
              tonalElevation = 8.dp
            ) {
              NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                label = { Text("Music") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MusicPrimary,
                  selectedTextColor = MusicPrimary,
                  unselectedIconColor = MusicTextSecondary,
                  unselectedTextColor = MusicTextSecondary,
                  indicatorColor = MusicDarkSurfaceVariant
                )
              )
              NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                label = { Text("Videos") },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MusicPrimary,
                  selectedTextColor = MusicPrimary,
                  unselectedIconColor = MusicTextSecondary,
                  unselectedTextColor = MusicTextSecondary,
                  indicatorColor = MusicDarkSurfaceVariant
                )
              )
            }
          }
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/04_video_library.png")
  }

  @Test
  fun preview_full_player_sheet() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MusicDarkBackground
        ) {
          FullPlayerSheet(
            song = sampleSong1,
            isPlaying = true,
            currentPositionMs = 95000L,
            durationMs = sampleSong1.durationMs,
            isShuffle = false,
            repeatMode = 1,
            volume = 0.85f,
            isVisible = true,
            onCollapse = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onSeek = {},
            onToggleShuffle = {},
            onCycleRepeat = {},
            onVolumeChange = {},
            onToggleFavorite = {}
          )
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/05_full_player.png")
  }
}
