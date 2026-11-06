package com.example.features.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.localization.Message
import com.example.core.localization.localized
import com.example.core.navigation.Navigator
import com.example.shared.bookmarks.Bookmark
import com.example.shared.designsystem.DesignCard
import com.example.shared.designsystem.EmptyState
import com.example.shared.designsystem.ErrorState
import com.example.shared.designsystem.LoadingIndicator
import com.example.shared.designsystem.RepositoryRow
import com.example.shared.designsystem.RepositoryRowData

@Composable
fun BookmarksScreen(dependencies: BookmarksDependencies, navigator: Navigator) {
    val viewModel: BookmarksViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BookmarksViewModel(dependencies, navigator) }
        }
    )
    BookmarksContent(viewModel)
}

@Composable
private fun BookmarksContent(viewModel: BookmarksViewModel) {
    val bookmarks by viewModel.dependencies.bookmarks.items.collectAsState()
    val descriptions by viewModel.dependencies.settings.showDescriptions.collectAsState()
    val error by viewModel.error.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val loaded by viewModel.isLoaded.collectAsState()
    var pendingDelete by remember { mutableStateOf<Bookmark?>(null) }
    LaunchedEffect(viewModel) { viewModel.load() }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(Message.CommonSaved.localized(), style = MaterialTheme.typography.headlineLarge) }
        if (loading) item { LoadingIndicator(Message.CommonLoading.localized()) }
        error?.let { item { ErrorState(it.localized(), Message.CommonRetry.localized(), viewModel::load) } }
        if (loaded &&
            bookmarks.isEmpty()
        ) {
            item { EmptyState(Message.BookmarksEmptyTitle.localized(), Message.BookmarksEmptyMessage.localized()) }
        }
        items(bookmarks, key = { it.id }) { bookmark ->
            DesignCard {
                Column(Modifier.fillMaxWidth().clickable { viewModel.select(bookmark) }) {
                    RepositoryRow(
                        RepositoryRowData(
                            bookmark.repository.fullName,
                            bookmark.repository.owner,
                            bookmark.repository.avatarURL,
                            bookmark.repository.description,
                            bookmark.repository.language,
                            bookmark.repository.stars
                        ),
                        true,
                        descriptions,
                        Message.RepositoryStars(bookmark.repository.stars).localized(),
                        Message.CommonSaved.localized(),
                        viewModel.dependencies.images::load
                    )
                    if (bookmark.note.isNotEmpty()) Text(bookmark.note, maxLines = 2)
                }
                Row {
                    TextButton({ viewModel.edit(bookmark) }) { Text(Message.BookmarksEditNote.localized()) }
                    TextButton({
                        pendingDelete = bookmark
                    }) { Text(Message.CommonDelete.localized(), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    pendingDelete?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(Message.BookmarksDeleteTitle.localized()) },
            text = { Text(Message.BookmarksDeleteMessage.localized()) },
            confirmButton = {
                TextButton({
                    pendingDelete = null
                    viewModel.remove(bookmark.id)
                }) { Text(Message.CommonDelete.localized()) }
            },
            dismissButton = { TextButton({ pendingDelete = null }) { Text(Message.CommonCancel.localized()) } }
        )
    }
}
