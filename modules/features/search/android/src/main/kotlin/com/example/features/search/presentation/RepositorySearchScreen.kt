package com.example.features.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.localization.Message
import com.example.core.localization.localized
import com.example.core.navigation.Navigator
import com.example.shared.designsystem.DesignActionRow
import com.example.shared.designsystem.DesignCard
import com.example.shared.designsystem.DesignDivider
import com.example.shared.designsystem.DesignIcons
import com.example.shared.designsystem.DesignNavTitle
import com.example.shared.designsystem.ErrorState
import com.example.shared.designsystem.RepositoryRow
import com.example.shared.designsystem.RepositoryRowData
import com.example.shared.designsystem.Tokens

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
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                DesignNavTitle(Message.SearchTitle.localized())
                SearchField(form.query, { viewModel.edit(form.copy(query = it)) }, viewModel::submit)
            }
            item {
                DesignCard {
                    FilterRow(
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
                    DesignDivider()
                    FilterRow(
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
                    DesignDivider()
                    DesignActionRow(DesignIcons.Search, Message.SearchSubmit.localized(), onClick = viewModel::submit)
                }
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
                DesignCard {
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
                    }
                }
            }
            item { PageFooter(state, viewModel::retry, viewModel::nextPage) }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(
        query,
        onChange,
        Modifier.fillMaxWidth(),
        placeholder = { Text(Message.SearchPrompt.localized()) },
        leadingIcon = { Icon(DesignIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton({ onChange("") }) { Icon(DesignIcons.Clear, contentDescription = null) }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(Tokens.Radius.pill),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
private fun FilterRow(label: String, value: String, options: List<Pair<String, String>>, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) { expanded = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                Icon(
                    DesignIcons.Unfold,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (key, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = {
                    select(key)
                    expanded = false
                })
            }
        }
    }
}
