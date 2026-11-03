package com.example.featuregithub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.designsystem.EmptyState
import com.example.designsystem.ErrorState
import com.example.designsystem.LoadingIndicator

@Composable
fun BookmarksScreen(viewModel: BookmarksViewModel) {
    val bookmarks by viewModel.dependencies.bookmarks.items.collectAsState()
    val descriptions by viewModel.dependencies.settings.showDescriptions.collectAsState()
    val error by viewModel.error.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val loaded by viewModel.isLoaded.collectAsState()
    var pendingDelete by remember { mutableStateOf<Bookmark?>(null) }
    LaunchedEffect(viewModel) { viewModel.load() }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("保存済み", style = MaterialTheme.typography.headlineLarge) }
        if (loading) item { LoadingIndicator() }
        error?.let { item { ErrorState(it, viewModel::load) } }
        if (loaded && bookmarks.isEmpty()) item { EmptyState("まだ保存されていません", "「探す」からリポジトリを開き、保存してみましょう。") }
        items(bookmarks, key = { it.id }) { bookmark ->
            Column {
                Column(Modifier.fillMaxWidth().clickable { viewModel.select(bookmark) }) {
                    RepositoryRow(bookmark.repository, true, descriptions, viewModel.dependencies.images)
                    if (bookmark.note.isNotEmpty()) Text(bookmark.note, maxLines = 2)
                }
                Row {
                    TextButton({ viewModel.edit(bookmark) }) { Text("メモを編集") }
                    TextButton({ pendingDelete = bookmark }) { Text("削除", color = MaterialTheme.colorScheme.error) }
                }
                HorizontalDivider()
            }
        }
    }
    pendingDelete?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("保存を解除しますか？") },
            text = { Text("このリポジトリのメモも削除されます。") },
            confirmButton = {
                TextButton({
                    pendingDelete = null
                    viewModel.remove(bookmark.id)
                }) { Text("削除") }
            },
            dismissButton = { TextButton({ pendingDelete = null }) { Text("キャンセル") } }
        )
    }
}
