package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoItem
import com.example.ui.theme.MusicAccentRed
import com.example.ui.theme.MusicDarkBackground
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.MusicTextPrimary
import com.example.ui.theme.MusicTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ResizeMode(val label: String, val mode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)
}

@OptIn(UnstableApi::class)
@Composable
fun VlcVideoPlayerView(
    video: VideoItem,
    onClose: () -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // ExoPlayer initialization
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.contentUri))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var isBuffering by remember { mutableStateOf(false) }

    // Controls visibility
    var areControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var currentResizeMode by remember { mutableStateOf(ResizeMode.FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Gesture indicator overlays (VLC style)
    var gestureBrightnessLevel by remember { mutableFloatStateOf(-1f) } // 0f to 1f
    var gestureVolumeLevel by remember { mutableFloatStateOf(-1f) } // 0f to 1f
    var doubleTapIndicator by remember { mutableStateOf<String?>(null) } // "10s", "-10s"

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying && !isLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    // Sync playback position periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(1L)
            }
            delay(500)
        }
    }

    // Player state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(1L)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            // Reset screen orientation on exit
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            // Reset screen brightness
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    BackHandler {
        onClose()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("vlc_video_player"),
        color = Color.Black
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            // ExoPlayer AndroidView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = currentResizeMode.mode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.resizeMode = currentResizeMode.mode
                },
                modifier = Modifier.fillMaxSize()
            )

            // Gesture Detector (VLC Gestures: Left side brightness, Right side volume, Double-tap skip)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        if (isLocked) {
                            detectTapGestures(
                                onTap = { areControlsVisible = !areControlsVisible }
                            )
                        } else {
                            detectTapGestures(
                                onTap = {
                                    areControlsVisible = !areControlsVisible
                                },
                                onDoubleTap = { offset ->
                                    val isRightSide = offset.x > size.width / 2
                                    if (isRightSide) {
                                        // Forward 10s
                                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                        exoPlayer.seekTo(newPos)
                                        currentPositionMs = newPos
                                        doubleTapIndicator = "+10s"
                                        scope.launch {
                                            delay(800)
                                            doubleTapIndicator = null
                                        }
                                    } else {
                                        // Rewind 10s
                                        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                        exoPlayer.seekTo(newPos)
                                        currentPositionMs = newPos
                                        doubleTapIndicator = "-10s"
                                        scope.launch {
                                            delay(800)
                                            doubleTapIndicator = null
                                        }
                                    }
                                }
                            )
                        }
                    }
            )

            // Touch gesture overlay for Brightness (Left) & Volume (Right)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        if (!isLocked) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: continue
                                    if (change.pressed) {
                                        val isLeft = change.position.x < size.width / 2
                                        val deltaY = -change.scrollDelta.y // negative scroll or drag
                                        // Drag handling
                                        if (event.changes.size == 1 && change.previousPosition.y != change.position.y) {
                                            val dragDelta = change.previousPosition.y - change.position.y
                                            val fraction = dragDelta / size.height.toFloat()

                                            if (isLeft) {
                                                // Brightness adjustment
                                                activity?.window?.attributes?.let { lp ->
                                                    val curBrightness = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                                    val newBrightness = (curBrightness + fraction * 1.5f).coerceIn(0.01f, 1f)
                                                    lp.screenBrightness = newBrightness
                                                    activity.window.attributes = lp
                                                    gestureBrightnessLevel = newBrightness
                                                    scope.launch {
                                                        delay(1200)
                                                        gestureBrightnessLevel = -1f
                                                    }
                                                }
                                            } else {
                                                // Volume adjustment
                                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val step = if (dragDelta > 0) 1 else -1
                                                if (kotlin.math.abs(dragDelta) > 20) {
                                                    val newVol = (curVol + step).coerceIn(0, maxVol)
                                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                    gestureVolumeLevel = newVol.toFloat() / maxVol.toFloat()
                                                    scope.launch {
                                                        delay(1200)
                                                        gestureVolumeLevel = -1f
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            )

            // Central Buffering Indicator
            if (isBuffering) {
                CircularProgressIndicator(
                    color = MusicPrimary,
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.Center)
                )
            }

            // Double Tap Forward / Rewind Visual Indicator
            AnimatedVisibility(
                visible = doubleTapIndicator != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (doubleTapIndicator?.contains("+") == true) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = MusicPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = doubleTapIndicator ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // VLC Style Brightness Overlay Indicator
            AnimatedVisibility(
                visible = gestureBrightnessLevel >= 0f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessMedium,
                            contentDescription = "Brightness",
                            tint = MusicPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(gestureBrightnessLevel * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // VLC Style Volume Overlay Indicator
            AnimatedVisibility(
                visible = gestureVolumeLevel >= 0f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = MusicPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(gestureVolumeLevel * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Screen Lock button (stays visible if locked)
            AnimatedVisibility(
                visible = areControlsVisible || isLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        isLocked = !isLocked
                        if (!isLocked) areControlsVisible = true
                    },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isLocked) MusicPrimary else Color.Black.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Screen",
                        tint = if (isLocked) Color.Black else Color.White
                    )
                }
            }

            // Full Video Controls (Hidden when locked)
            AnimatedVisibility(
                visible = areControlsVisible && !isLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Bar (Gradient + Back Button + Title + Ratio + Speed + Delete)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.85f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${video.folderName} • ${video.formattedSize}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MusicTextSecondary,
                                    maxLines = 1
                                )
                            }

                            // Aspect Ratio Mode Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        currentResizeMode = when (currentResizeMode) {
                                            ResizeMode.FIT -> ResizeMode.FILL
                                            ResizeMode.FILL -> ResizeMode.ZOOM
                                            ResizeMode.ZOOM -> ResizeMode.FIXED_16_9
                                            ResizeMode.FIXED_16_9 -> ResizeMode.FIT
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text(
                                        text = currentResizeMode.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MusicPrimary
                                    )
                                }
                            }

                            // Speed Button
                            Box {
                                IconButton(
                                    onClick = { showSpeedMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Speed",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${speed}x",
                                                    fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (playbackSpeed == speed) MusicPrimary else Color.Unspecified
                                                )
                                            },
                                            onClick = {
                                                playbackSpeed = speed
                                                exoPlayer.playbackParameters = PlaybackParameters(speed)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Delete Video Button
                            IconButton(
                                onClick = { onDeleteVideo(video) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MusicAccentRed
                                )
                            }
                        }
                    }

                    // Center Play/Pause & Quick Seek Buttons
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Big Play / Pause
                        FilledIconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MusicPrimary),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Bottom Bar (Timeline Slider + Rotation Toggle + Time Format)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.9f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Seekbar Slider
                            var isUserSeeking by remember { mutableStateOf(false) }
                            var seekPosition by remember { mutableFloatStateOf(0f) }

                            val sliderValue = if (isUserSeeking) seekPosition else currentPositionMs.toFloat()

                            Slider(
                                value = sliderValue.coerceIn(0f, durationMs.toFloat()),
                                onValueChange = {
                                    isUserSeeking = true
                                    seekPosition = it
                                },
                                onValueChangeFinished = {
                                    exoPlayer.seekTo(seekPosition.toLong())
                                    currentPositionMs = seekPosition.toLong()
                                    isUserSeeking = false
                                },
                                valueRange = 0f..durationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MusicPrimary,
                                    activeTrackColor = MusicPrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Time format & Orientation control
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val curSec = (currentPositionMs / 1000)
                                val durSec = (durationMs / 1000)
                                val curFormatted = String.format("%02d:%02d", curSec / 60, curSec % 60)
                                val durFormatted = String.format("%02d:%02d", durSec / 60, durSec % 60)

                                Text(
                                    text = "$curFormatted / $durFormatted",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = Color.White
                                )

                                // Screen rotation toggle (Portrait <-> Landscape)
                                IconButton(
                                    onClick = {
                                        activity?.let { act ->
                                            if (act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "Rotate Screen",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
