package com.example.featuregithub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.designsystem.ErrorState
import com.example.designsystem.LoadingIndicator
import com.example.designsystem.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryDetailScreen(viewModel: RepositoryDetailViewModel) {
    val state by viewModel.content.state.collectAsState()
    val bookmarks by viewModel.dependencies.bookmarks.items.collectAsState()
    val saving by viewModel.isSaving.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadIfNeeded() }
    PullToRefreshBox(state.isLoading && state.hasLoaded, viewModel::refresh, Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("リポジトリ", style = MaterialTheme.typography.headlineLarge) }
            state.items.firstOrNull()?.let { repository ->
                item {
                    RepositoryRow(
                        repository,
                        bookmarks.any {
                            it.id == repository.id
                        },
                        true,
                        viewModel.dependencies.images
                    )
                    Text("Fork ${repository.forks}")
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(repository.topics) { StatusBadge(it) }
                    }
                }
                item {
                    Button(viewModel::toggleBookmark, enabled = !saving) {
                        Text(if (bookmarks.any { it.id == repository.id }) "保存を解除" else "保存する")
                    }
                }
                item { Button(viewModel::showIssues) { Text("Issue を見る") } }
            }
            saveError?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.isLoading) item { LoadingIndicator() }
            state.error?.let { item { ErrorState(it, viewModel::refresh) } }
        }
    }
}
