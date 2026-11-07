package com.example.shared.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 押せる部品の最小の大きさ（44dp）。 */
private val TouchTarget = 44.dp

/** 画面上端の小さな中央タイトル（iOS のインライン・ナビゲーションタイトル）。戻るボタンは AppRoot が重ねる。 */
@Composable
fun DesignNavTitle(text: String) {
    Box(Modifier.fillMaxWidth().height(TouchTarget), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** カード群の上に置く小さな見出し。 */
@Composable
fun DesignSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** カードの中の、アイコンと文字の操作行。 */
@Composable
fun DesignActionRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier.heightIn(min = TouchTarget).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}

/** カード内の区切り線。 */
@Composable
fun DesignDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** 丸い戻るボタン（iOS のナビゲーションバーの戻るボタンに合わせる）。 */
@Composable
fun DesignBackButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(TouchTarget),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        IconButton(onClick) { Icon(DesignIcons.Back, contentDescription = label) }
    }
}
