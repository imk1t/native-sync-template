package com.example.shared.designsystem

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepositoryRow(
    repository: RepositoryRowData,
    saved: Boolean,
    showDescription: Boolean,
    starsLabel: String,
    savedLabel: String,
    loadImage: suspend (String) -> ByteArray
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(repository.avatarURL, repository.owner, loadImage)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                repository.fullName,
                style = monoTextStyle()
            )
            if (showDescription) {
                repository.description?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        maxLines = 3
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repository.language?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(languageTint(it)))
                        Text(it, style = MaterialTheme.typography.labelMedium, color = muted)
                    }
                }
                MetaItem(DesignIcons.Star, starsLabel, muted)
                if (saved) MetaItem(DesignIcons.Bookmark, savedLabel, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun Avatar(url: String?, owner: String, loadImage: suspend (String) -> ByteArray) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        if (url != null) {
            try {
                val data = loadImage(url)
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                bitmap = null
            }
        }
    }
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(Tokens.Radius.avatar))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop)
        } else {
            Text(owner.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
        }
    }
}
