package com.example.featuregithub

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.corenetwork.ImageLoader
import com.example.corenetwork.RepositorySummary
import kotlinx.coroutines.CancellationException

@Composable
internal fun RepositoryRow(
    repository: RepositorySummary,
    saved: Boolean,
    showDescription: Boolean,
    images: ImageLoader
) {
    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(repository.avatarURL, repository.owner, images)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(repository.fullName, style = MaterialTheme.typography.titleMedium)
            if (showDescription) {
                repository.description?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Star ${repository.stars}", style = MaterialTheme.typography.labelMedium)
                repository.language?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                if (saved) Text("保存済み", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Avatar(url: String?, owner: String, loader: ImageLoader) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        if (url != null) {
            try {
                val data = loader.load(url)
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                bitmap = null
            }
        }
    }
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
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
