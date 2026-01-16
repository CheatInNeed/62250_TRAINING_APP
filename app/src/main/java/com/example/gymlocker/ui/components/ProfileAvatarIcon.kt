package com.example.gymlocker.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable
fun ProfileAvatarIcon(
    uriString: String?,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 1) Start from cache (instant if already loaded)
    val cached = remember(uriString) {
        uriString?.let { AvatarImageCache.get(it) }
    }

    var image by remember(uriString) { mutableStateOf<ImageBitmap?>(cached) }

    // 2) Only load if we have a uri AND it isn't cached yet
    LaunchedEffect(uriString) {
        if (uriString.isNullOrBlank()) {
            image = null
            return@LaunchedEffect
        }
        if (image != null) return@LaunchedEffect

        val loaded = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(uriString)).use { input ->
                    if (input == null) null
                    else BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }

        if (loaded != null) {
            AvatarImageCache.put(uriString, loaded)
        }
        image = loaded
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            image != null -> {
                Image(
                    bitmap = image!!,
                    contentDescription = null,
                    modifier = Modifier.size(size).clip(CircleShape)
                )
            }
            // KEY CHANGE:
            // If uri exists but we're still loading, show a neutral placeholder (no person flash)
            !uriString.isNullOrBlank() -> {
                // subtle placeholder dot/circle (optional)
                // or just leave it blank—still better than flashing Person
            }
            else -> {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.65f)
                )
            }
        }
    }
}
