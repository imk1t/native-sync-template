package com.example.features.search

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.navigation.Navigator
import com.example.corelocalization.Message
import com.example.corelocalization.localized
import com.example.shared.designsystem.ErrorState
import com.example.shared.designsystem.LoadingIndicator
import com.example.shared.designsystem.RepositoryRow
import com.example.shared.designsystem.RepositoryRowData
import com.example.shared.designsystem.StatusBadge

@Composable
fun RepositoryDetailScreen(owner: String, repo: String, dependencies: SearchDependencies, navigator: Navigator) {
    val viewModel: RepositoryDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RepositoryDetailViewModel(owner, repo, dependencies, navigator) }
        }
    )
    RepositoryDetailContent(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositoryDetailContent(viewModel: RepositoryDetailViewModel) {
    val state by viewModel.content.state.collectAsState()
    val bookmarks by viewModel.dependencies.bookmarks.items.collectAsState()
    val saving by viewModel.isSaving.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadIfNeeded() }
    PullToRefreshBox(state.isLoading && state.hasLoaded, viewModel::refresh, Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(Message.RepositoryTitle.localized(), style = MaterialTheme.typography.headlineLarge) }
            state.items.firstOrNull()?.let { repository ->
                item {
                    RepositoryRow(
                        RepositoryRowData(
                            repository.fullName,
                            repository.owner,
                            repository.avatarURL,
                            repository.description,
                            repository.language,
                            repository.stars
                        ),
                        bookmarks.any {
                            it.id == repository.id
                        },
                        true,
                        Message.RepositoryStars(repository.stars).localized(),
                        Message.CommonSaved.localized(),
                        viewModel.dependencies.images::load
                    )
                    Text(Message.RepositoryForks(repository.forks).localized())
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(repository.topics) { StatusBadge(it) }
                    }
                }
                item {
                    Button(viewModel::toggleBookmark, enabled = !saving) {
                        Text(
                            if (bookmarks.any {
                                    it.id == repository.id
                                }
                            ) {
                                Message.RepositoryUnsave.localized()
                            } else {
                                Message.RepositorySave.localized()
                            }
                        )
                    }
                }
                item { Button(viewModel::showIssues) { Text(Message.RepositoryIssues.localized()) } }
            }
            saveError?.let { item { Text(it.localized(), color = MaterialTheme.colorScheme.error) } }
            if (state.isLoading) item { LoadingIndicator(Message.CommonLoading.localized()) }
            state.error?.let {
                item { ErrorState(it.localized(), Message.CommonRetry.localized(), viewModel::refresh) }
            }
        }
    }
}
