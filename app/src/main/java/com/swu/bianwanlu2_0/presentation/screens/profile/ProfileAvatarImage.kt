package com.swu.bianwanlu2_0.presentation.screens.profile

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swu.bianwanlu2_0.ui.theme.LocalAppIconTint

@Composable
fun ProfileAvatarImage(
    avatarUri: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
) {
    val context = LocalContext.current
    val iconTint = LocalAppIconTint.current
    val imageBitmap = remember(avatarUri) {
        avatarUri
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                runCatching {
                    val uri = Uri.parse(value)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(context.contentResolver, uri),
                        ).asImageBitmap()
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap()
                    }
                }.getOrNull()
            }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFEEEEEE)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "头像",
                tint = iconTint.copy(alpha = 0.7f),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
