package com.example.ui.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.MusicTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ResizeMode(val label: String, val mode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FIXED_4_3("4:3", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT)
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

    // Controls visibility & mode states
    var areControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var currentResizeMode by remember { mutableStateOf(ResizeMode.FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var isRepeatOne by remember { mutableStateOf(false) }
    var isBackgroundAudio by remember { mutableStateOf(false) }

    // Gesture indicator overlays (VLC style)
    var gestureBrightnessLevel by remember { mutableFloatStateOf(-1f) } // 0f to 1f
    var gestureVolumeLevel by remember { mutableFloatStateOf(-1f) } // 0f to 1f
    var doubleTapIndicator by remember { mutableStateOf<String?>(null) } // "+10s", "-10s"
    var horizontalSeekIndicator by remember { mutableStateOf<String?>(null) }
    var horizontalSeekTargetMs by remember { mutableLongStateOf(0L) }

    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }
    var singleTapJob by remember { mutableStateOf<Job?>(null) }
    var doubleTapJob by remember { mutableStateOf<Job?>(null) }

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var isMuted by remember { mutableStateOf(false) }
    var preMuteVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // VLC Sleep Timer logic
    LaunchedEffect(sleepTimerMinutes) {
        sleepTimerJob?.cancel()
        if (sleepTimerMinutes > 0) {
            sleepTimerJob = scope.launch {
                delay(sleepTimerMinutes * 60 * 1000L)
                exoPlayer.pause()
                Toast.makeText(context, "Sleep timer reached. Video playback paused ⏱️", Toast.LENGTH_LONG).show()
                sleepTimerMinutes = 0
            }
        }
    }

    // Automatically adapt to how the user holds the phone (sensor-based auto rotation like VLC)
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    // Auto-hide controls after 4 seconds of playback inactivity
    LaunchedEffect(areControlsVisible, isPlaying, isLocked) {
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
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

            // VLC Full-Screen Gesture Detector Layer (Unified Touch Engine)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (isLocked) {
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        areControlsVisible = !areControlsVisible
                                    }
                                    continue
                                }

                                val startPos = down.position
                                val isLeftSide = startPos.x < size.width / 2f
                                var isDragStarted = false
                                var dragType = 0 // 0: none, 1: vertical, 2: horizontal

                                val initialVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val lp = activity?.window?.attributes
                                val initialBrightness = if (lp != null && lp.screenBrightness >= 0f) lp.screenBrightness else 0.5f
                                val initialPos = exoPlayer.currentPosition

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) {
                                        // Finger released
                                        if (!isDragStarted) {
                                            val now = System.currentTimeMillis()
                                            val elapsedSinceLastTap = now - lastTapTime
                                            val dist = (change.position - lastTapPos).getDistance()

                                            if (elapsedSinceLastTap < 350L && dist < 120f) {
                                                // DOUBLE TAP (±10s seek)
                                                singleTapJob?.cancel()
                                                lastTapTime = 0L
                                                val isRight = change.position.x > size.width / 2f
                                                if (isRight) {
                                                    val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                                    exoPlayer.seekTo(newPos)
                                                    currentPositionMs = newPos
                                                    doubleTapIndicator = "+10s"
                                                } else {
                                                    val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                                    exoPlayer.seekTo(newPos)
                                                    currentPositionMs = newPos
                                                    doubleTapIndicator = "-10s"
                                                }
                                                doubleTapJob?.cancel()
                                                doubleTapJob = scope.launch {
                                                    delay(750)
                                                    doubleTapIndicator = null
                                                }
                                            } else {
                                                // SINGLE TAP (Toggle Controls)
                                                lastTapTime = now
                                                lastTapPos = change.position
                                                singleTapJob?.cancel()
                                                singleTapJob = scope.launch {
                                                    delay(320)
                                                    areControlsVisible = !areControlsVisible
                                                }
                                            }
                                        } else {
                                            // DRAG FINISHED
                                            if (dragType == 2) {
                                                exoPlayer.seekTo(horizontalSeekTargetMs)
                                                currentPositionMs = horizontalSeekTargetMs
                                                horizontalSeekIndicator = null
                                            }
                                            scope.launch {
                                                delay(650)
                                                gestureVolumeLevel = -1f
                                                gestureBrightnessLevel = -1f
                                            }
                                        }
                                        break
                                    }

                                    val deltaX = change.position.x - startPos.x
                                    val deltaY = change.position.y - startPos.y

                                    if (!isDragStarted) {
                                        if (kotlin.math.abs(deltaY) > 18f && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX)) {
                                            isDragStarted = true
                                            dragType = 1 // Vertical drag
                                            singleTapJob?.cancel()
                                        } else if (kotlin.math.abs(deltaX) > 28f && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)) {
                                            isDragStarted = true
                                            dragType = 2 // Horizontal drag
                                            singleTapJob?.cancel()
                                        }
                                    }

                                    if (isDragStarted) {
                                        change.consume()
                                        if (dragType == 1) {
                                            // Upward drag is positive, downward drag is negative
                                            val deltaDragY = startPos.y - change.position.y
                                            val fraction = deltaDragY / (size.height * 0.65f)

                                            if (isLeftSide) {
                                                // Smooth Screen Brightness (Left 50%)
                                                val newBrightness = (initialBrightness + fraction).coerceIn(0.01f, 1f)
                                                activity?.window?.attributes = activity?.window?.attributes?.apply {
                                                    screenBrightness = newBrightness
                                                }
                                                gestureBrightnessLevel = newBrightness
                                            } else {
                                                // Smooth Volume Stream (Right 50%)
                                                val volFraction = ((initialVol.toFloat() / maxVol.toFloat()) + fraction).coerceIn(0f, 1f)
                                                val targetVol = (volFraction * maxVol).roundToInt().coerceIn(0, maxVol)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                                gestureVolumeLevel = volFraction
                                                if (targetVol > 0 && isMuted) {
                                                    isMuted = false
                                                }
                                            }
                                        } else if (dragType == 2) {
                                            // Smooth Horizontal Timeline Scrubbing
                                            val deltaDragX = change.position.x - startPos.x
                                            val seekDeltaSec = ((deltaDragX / size.width) * 90f).toLong()
                                            val target = (initialPos + seekDeltaSec * 1000L).coerceIn(0L, durationMs)
                                            horizontalSeekTargetMs = target
                                            val diffSec = (target - initialPos) / 1000L
                                            val curSec = target / 1000L
                                            val totalSec = durationMs / 1000L
                                            horizontalSeekIndicator = "${if (diffSec >= 0) "+" else ""}${diffSec}s (${String.format("%02d:%02d", curSec / 60, curSec % 60)} / ${String.format("%02d:%02d", totalSec / 60, totalSec % 60)})"
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

            // Double Tap Forward / Rewind Visual Indicator (Left / Right Animated Badges)
            AnimatedVisibility(
                visible = doubleTapIndicator != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(if (doubleTapIndicator?.contains("+") == true) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.5.dp, MusicPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.size(88.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (doubleTapIndicator?.contains("+") == true) Icons.Default.Forward10 else Icons.Default.Replay10,
                            contentDescription = null,
                            tint = MusicPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = doubleTapIndicator ?: "",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // VLC Style Brightness Vertical Pill Gauge (Left Screen)
            AnimatedVisibility(
                visible = gestureBrightnessLevel >= 0f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .width(54.dp)
                        .height(180.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessMedium,
                            contentDescription = "Brightness",
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .weight(1f)
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(gestureBrightnessLevel.coerceIn(0f, 1f))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFFFFB703), Color(0xFFFB8500))
                                        )
                                    )
                            )
                        }
                        Text(
                            text = "${(gestureBrightnessLevel * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // VLC Style Volume Vertical Pill Gauge (Right Screen)
            AnimatedVisibility(
                visible = gestureVolumeLevel >= 0f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .width(54.dp)
                        .height(180.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val volIcon = when {
                            gestureVolumeLevel <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                            gestureVolumeLevel < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        }
                        Icon(
                            imageVector = volIcon,
                            contentDescription = "Volume",
                            tint = MusicPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .weight(1f)
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(gestureVolumeLevel.coerceIn(0f, 1f))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(MusicPrimary, Color(0xFF9D4EDD))
                                        )
                                    )
                            )
                        }
                        Text(
                            text = "${(gestureVolumeLevel * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Horizontal Seek Scrub Preview HUD (VLC Center Badge)
            AnimatedVisibility(
                visible = horizontalSeekIndicator != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MusicPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (horizontalSeekIndicator?.startsWith("-") == true) Icons.Default.FastRewind else Icons.Default.FastForward,
                            contentDescription = null,
                            tint = MusicPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = horizontalSeekIndicator ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
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
                    // Top Bar (Gradient + Back Button + Title + Ratio + Speed + Background Audio + Delete)
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
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                                            ResizeMode.FIXED_16_9 -> ResizeMode.FIXED_4_3
                                            ResizeMode.FIXED_4_3 -> ResizeMode.FIT
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
                                    listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
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

                            // Picture-in-Picture Button (VLC Style)
                            IconButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        activity?.enterPictureInPictureMode(params)
                                        areControlsVisible = false
                                    } else {
                                        Toast.makeText(context, "Picture-in-Picture requires Android 8.0+", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Picture-in-Picture",
                                    tint = Color.White
                                )
                            }

                            // Sleep Timer Button (VLC Style)
                            IconButton(
                                onClick = { showSleepTimerDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerMinutes > 0) MusicPrimary else Color.White
                                )
                            }

                            // Media Information Button (VLC Style)
                            IconButton(
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Video Information",
                                    tint = Color.White
                                )
                            }

                            // Background Audio / Headphone Mode
                            IconButton(
                                onClick = {
                                    isBackgroundAudio = !isBackgroundAudio
                                    Toast.makeText(
                                        context,
                                        if (isBackgroundAudio) "Background audio playback enabled 🎧" else "Background audio disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Background Audio",
                                    tint = if (isBackgroundAudio) MusicPrimary else Color.White
                                )
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

                    // Bottom Bar (Timeline Slider + Loop Toggle + Rotation Toggle + Time Format)
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

                            // Time format & Controls
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Quick Audio Mute / Unmute Toggle Button
                                    IconButton(
                                        onClick = {
                                            isMuted = !isMuted
                                            if (isMuted) {
                                                preMuteVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                                                gestureVolumeLevel = 0f
                                                Toast.makeText(context, "Muted 🔇", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val restoreVol = preMuteVolume.coerceAtLeast(1)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreVol, 0)
                                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                                gestureVolumeLevel = restoreVol.toFloat() / maxVol.toFloat()
                                                Toast.makeText(context, "Unmuted 🔊", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Quick Mute",
                                            tint = if (isMuted) MusicAccentRed else Color.White
                                        )
                                    }

                                    // Repeat / Loop Video Toggle
                                    IconButton(
                                        onClick = {
                                            isRepeatOne = !isRepeatOne
                                            exoPlayer.repeatMode = if (isRepeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                            Toast.makeText(
                                                context,
                                                if (isRepeatOne) "Loop video enabled 🔂" else "Loop video off 🔁",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRepeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                            contentDescription = "Repeat Mode",
                                            tint = if (isRepeatOne) MusicPrimary else Color.White
                                        )
                                    }

                                    // Screen rotation toggle (Portrait <-> Landscape)
                                    IconButton(
                                        onClick = {
                                            activity?.let { act ->
                                                val isLandscape = act.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                                act.requestedOrientation = if (isLandscape) {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                                } else {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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

    // VLC Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = {
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        0 to "Off",
                        15 to "15 minutes",
                        30 to "30 minutes",
                        45 to "45 minutes",
                        60 to "60 minutes"
                    )
                    options.forEach { (mins, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    sleepTimerMinutes = mins
                                    showSleepTimerDialog = false
                                    Toast.makeText(
                                        context,
                                        if (mins == 0) "Sleep timer turned off" else "Video will stop in $mins minutes ⏱️",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = sleepTimerMinutes == mins,
                                onClick = {
                                    sleepTimerMinutes = mins
                                    showSleepTimerDialog = false
                                    Toast.makeText(
                                        context,
                                        if (mins == 0) "Sleep timer turned off" else "Video will stop in $mins minutes ⏱️",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MusicPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close", color = MusicPrimary)
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }

    // VLC Video Information Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MusicPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Video Information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VideoInfoRow("Title", video.title)
                    VideoInfoRow("Duration", String.format("%02d:%02d", (video.durationMs / 1000) / 60, (video.durationMs / 1000) % 60))
                    VideoInfoRow("Size", video.formattedSize)
                    VideoInfoRow("Folder", video.folderName)
                    VideoInfoRow("Playback Speed", "${playbackSpeed}x")
                    VideoInfoRow("Aspect Ratio", currentResizeMode.label)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Done", color = MusicPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }
}

@Composable
private fun VideoInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MusicTextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}
