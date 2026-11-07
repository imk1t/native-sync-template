package com.example.features.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.core.localization.Message
import com.example.core.localization.localized
import com.example.core.navigation.Navigator
import com.example.shared.designsystem.DesignCard
import com.example.shared.designsystem.DesignNavTitle
import com.example.shared.designsystem.StatusBadge

@Composable
fun RepositoryIssuesScreen(owner: String, repo: String, dependencies: SearchDependencies, navigator: Navigator) {
    val viewModel: RepositoryIssuesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RepositoryIssuesViewModel(owner, repo, dependencies.repository, navigator) }
        }
    )
    RepositoryIssuesContent(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositoryIssuesContent(viewModel: RepositoryIssuesViewModel) {
    val state by viewModel.content.state.collectAsState()
    val filter by viewModel.issueState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadIfNeeded() }
    PullToRefreshBox(state.isLoading && state.hasLoaded, viewModel::refresh, Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                DesignNavTitle(Message.IssuesTitle.localized())
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val choices = listOf("open" to Message.IssueOpen, "closed" to Message.IssueClosed)
                    choices.forEachIndexed { index, (key, label) ->
                        SegmentedButton(
                            selected = filter == key,
                            onClick = { viewModel.changeState(key) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.surface,
                                activeContentColor = MaterialTheme.colorScheme.onSurface,
                                activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                                inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            icon = {}
                        ) { Text(label.localized()) }
                    }
                }
            }
            items(state.items, key = { it.id }) { issue ->
                DesignCard(Modifier.clickable { viewModel.select(issue) }) {
                    Text(issue.title, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusBadge(issueStateLabel(issue.state), issue.state == "open")
                        Text("#${issue.number}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        issue.updatedAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { PageFooter(state, viewModel::retry, viewModel::nextPage) }
        }
    }
}
