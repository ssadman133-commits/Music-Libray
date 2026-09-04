package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MusicBorder
import com.example.ui.theme.MusicDarkBackground
import com.example.ui.theme.MusicDarkSurface
import com.example.ui.theme.MusicDarkSurfaceVariant
import com.example.ui.theme.MusicPrimary
import com.example.ui.theme.MusicSecondary
import com.example.ui.theme.MusicTextPrimary
import com.example.ui.theme.MusicTextSecondary

@Composable
fun PermissionView(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusicDarkBackground)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Brand Logo with Neon Glow
        BrandLogo(
            size = 92.dp,
            cornerRadius = 22.dp,
            showGlow = true
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Access Local Media",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            color = MusicTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "To find and play your music & videos and automatically detect new downloads from Chrome, WhatsApp, or Telegram, please grant storage permission.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MusicTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Features Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MusicDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MusicBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PermissionFeatureRow(
                    icon = Icons.Default.FolderOpen,
                    title = "Automatic Media Scan",
                    description = "Finds all music and video files across your internal and SD storage without manual loading."
                )
                PermissionFeatureRow(
                    icon = Icons.Default.DownloadDone,
                    title = "Real-time Download Detection",
                    description = "Instant detection of new music and video files downloaded anywhere on your device."
                )
                PermissionFeatureRow(
                    icon = Icons.Default.Security,
                    title = "100% Private & Local",
                    description = "Your media stays strictly on your phone. No files are ever uploaded."
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("grant_permission_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MusicPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Grant Media Permission",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_settings_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MusicTextSecondary)
        ) {
            Text(text = "Open App Settings", fontSize = 14.sp)
        }
    }
}

@Composable
private fun PermissionFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MusicDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MusicSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MusicTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MusicTextSecondary
            )
        }
    }
}
