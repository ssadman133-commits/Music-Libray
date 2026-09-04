package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MusicAccentRed
import com.example.ui.theme.MusicDarkSurface
import com.example.ui.theme.MusicTextPrimary
import com.example.ui.theme.MusicTextSecondary

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    isMusic: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val mediaType = if (isMusic) "song" else "video"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = "Delete",
                tint = MusicAccentRed
            )
        },
        title = {
            Text(
                text = "Delete $mediaType?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MusicTextPrimary
            )
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete \"$itemName\" from your device storage?",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MusicTextSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("dialog_confirm_delete"),
                colors = ButtonDefaults.textButtonColors(contentColor = MusicAccentRed)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_delete")
            ) {
                Text("Cancel", color = MusicTextPrimary)
            }
        },
        containerColor = MusicDarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
