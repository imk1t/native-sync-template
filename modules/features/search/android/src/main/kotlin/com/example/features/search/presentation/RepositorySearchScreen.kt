package com.example.features.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.navigation.Navigator
import com.example.corelocalization.Message
import com.example.corelocalization.localized
import com.example.shared.designsystem.ErrorState
import com.example.shared.designsystem.RepositoryRow
import com.example.shared.designsystem.RepositoryRowData

@Composable
fun RepositorySearchScreen(dependencies: SearchDependencies, navigator: Navigator) {
    val viewModel: RepositorySearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RepositorySearchViewModel(dependencies, navigator) }
        }
    )
    RepositorySearchContent(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositorySearchContent(viewModel: RepositorySearchViewModel) {
    val form by viewModel.form.collectAsState()
    val state by viewModel.content.state.collectAsState()
    val storageError by viewModel.storageError.collectAsState()
    val bookmarks by viewModel.dependencies.bookmarks.items.collectAsState()
    val descriptions by viewModel.dependencies.settings.showDescriptions.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadIfNeeded() }
    PullToRefreshBox(state.isLoading && state.hasLoaded, viewModel::refresh, Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(Message.SearchTitle.localized(), style = MaterialTheme.typography.headlineLarge)
                OutlinedTextField(
                    form.query,
                    { viewModel.edit(form.copy(query = it)) },
                    Modifier.fillMaxWidth(),
                    label = { Text(Message.SearchPrompt.localized()) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.submit() })
                )
                Row {
                    FilterMenu(
                        Message.SearchLanguage.localized(),
                        form.language.ifEmpty { Message.SearchAll.localized() },
                        listOf(
                            "" to Message.SearchAll.localized(),
                            "Swift" to "Swift",
                            "Kotlin" to "Kotlin",
                            "TypeScript" to "TypeScript",
                            "Rust" to "Rust"
                        )
                    ) {
                        viewModel.edit(form.copy(language = it))
                    }
                    FilterMenu(
                        Message.SearchSort.localized(),
                        when (form.sort) {
                            "stars" -> Message.SearchStarsSort.localized()
                            "updated" -> Message.SearchUpdatedSort.localized()
                            else -> Message.SearchRelevance.localized()
                        },
                        listOf(
                            "" to Message.SearchRelevance.localized(),
                            "stars" to Message.SearchStarsSort.localized(),
                            "updated" to Message.SearchUpdatedSort.localized()
                        )
                    ) {
                        viewModel.edit(form.copy(sort = it))
                    }
                }
                Button(viewModel::submit) { Text(Message.SearchSubmit.localized()) }
            }
            storageError?.let { item { ErrorState(it.localized(), Message.CommonRetry.localized(), viewModel::retry) } }
            if (!state.hasLoaded && !state.isLoading && state.error == null) {
                item {
                    Text(Message.SearchIntro.localized())
                    Row {
                        listOf("swift", "compose", "developer tools").forEach { query ->
                            TextButton({ viewModel.example(query) }) { Text(query) }
                        }
                    }
                }
            }
            items(state.items, key = { it.id }) { repository ->
                Column(Modifier.fillMaxWidth().clickable { viewModel.select(repository) }) {
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
                        descriptions,
                        Message.RepositoryStars(repository.stars).localized(),
                        Message.CommonSaved.localized(),
                        viewModel.dependencies.images::load
                    )
                    HorizontalDivider()
                }
            }
            item { PageFooter(state, viewModel::retry, viewModel::nextPage) }
        }
    }
}

@Composable
private fun FilterMenu(label: String, value: String, options: List<Pair<String, String>>, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton({ expanded = true }) { Text(Message.SearchFilter(label, value).localized()) }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (key, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = {
                    select(key)
                    expanded =
                        false
                })
            }
        }
    }
}
