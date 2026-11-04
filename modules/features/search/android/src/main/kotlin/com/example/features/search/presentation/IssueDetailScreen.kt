package com.example.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.example.corelocalization.Message
import com.example.corelocalization.localized
import com.example.shared.designsystem.ErrorState
import com.example.shared.designsystem.LoadingIndicator
import com.example.shared.designsystem.StatusBadge

@Composable
fun IssueDetailScreen(owner: String, repo: String, number: Long, dependencies: SearchDependencies) {
    val viewModel: IssueDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { IssueDetailViewModel(owner, repo, number, dependencies.repository) }
        }
    )
    IssueDetailContent(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueDetailContent(viewModel: IssueDetailViewModel) {
    val state by viewModel.content.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadIfNeeded() }
    PullToRefreshBox(state.isLoading && state.hasLoaded, viewModel::refresh, Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(Message.IssueTitle.localized(), style = MaterialTheme.typography.headlineLarge) }
            state.items.firstOrNull()?.let { issue ->
                item { Text(issue.title, style = MaterialTheme.typography.headlineSmall) }
                item { StatusBadge(issueStateLabel(issue.state), issue.state == "open") }
                item { Text("#${issue.number} · ${issue.author ?: Message.IssueUnknownAuthor.localized()}") }
                item {
                    SelectionContainer {
                        Text(issue.body?.takeIf { it.isNotEmpty() } ?: Message.IssueEmptyBody.localized())
                    }
                }
            }
            if (state.isLoading) item { LoadingIndicator(Message.CommonLoading.localized()) }
            state.error?.let {
                item { ErrorState(it.localized(), Message.CommonRetry.localized(), viewModel::refresh) }
            }
        }
    }
}
