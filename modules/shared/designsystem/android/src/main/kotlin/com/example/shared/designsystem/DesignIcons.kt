package com.example.shared.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector

/** iOS の SF Symbols に近い形のアイコン。画面からは Material のアイコン集ではなくここを使う。 */
object DesignIcons {
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Bookmark: ImageVector = Icons.Filled.Bookmark
    val BookmarkOutline: ImageVector = Icons.Outlined.BookmarkBorder
    val Clear: ImageVector = Icons.Filled.Cancel
    val Delete: ImageVector = Icons.Filled.Delete
    val Edit: ImageVector = Icons.Filled.Edit
    val Issue: ImageVector = Icons.Outlined.ErrorOutline
    val Search: ImageVector = Icons.Filled.Search
    val Settings: ImageVector = Icons.Filled.Settings
    val Star: ImageVector = Icons.Outlined.StarBorder
    val Unfold: ImageVector = Icons.Filled.UnfoldMore
}
