package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.model.VideoItem
import com.example.ui.theme.MusicDarkSurfaceVariant
import com.example.ui.theme.MusicTextSecondary

@Composable
fun VideoThumbnail(
    video: VideoItem,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp
) {
    val context = LocalContext.current

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(video.contentUri)
            .decoderFactory { result, options, _ ->
                VideoFrameDecoder(result.source, options)
            }
            .videoFrameMillis(1000)
            .crossfade(true)
            .build()
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MusicDarkSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = "Thumbnail of ${video.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Fallback or overlay icon when loading
        if (painter.state is coil.compose.AsyncImagePainter.State.Error) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = MusicTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
