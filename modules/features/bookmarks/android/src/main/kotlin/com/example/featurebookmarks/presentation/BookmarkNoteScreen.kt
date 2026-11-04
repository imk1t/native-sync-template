package com.example.featurebookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.corenavigation.Navigator
import com.example.designsystem.ErrorState
import com.example.designsystem.LoadingIndicator

@Composable
fun BookmarkNoteScreen(repositoryId: String, dependencies: BookmarksDependencies, navigator: Navigator) {
    val viewModel: BookmarkNoteViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BookmarkNoteViewModel(repositoryId, dependencies.bookmarks, navigator) }
        }
    )
    BookmarkNoteContent(viewModel)
}

@Composable
private fun BookmarkNoteContent(viewModel: BookmarkNoteViewModel) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("メモを編集", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            state.note,
            viewModel::edit,
            Modifier.fillMaxWidth(),
            enabled = state.isLoaded && !state.isSaving,
            label = { Text("気になったことを書いておきましょう") },
            minLines = 6,
            maxLines = 10,
            isError = state.characterCount > 1_000
        )
        Text("${state.characterCount} / 1,000文字")
        state.error?.let {
            if (state.isLoaded) Text(it, color = MaterialTheme.colorScheme.error) else ErrorState(it, viewModel::load)
        }
        if (state.isSaving) LoadingIndicator()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(viewModel::cancel, enabled = !state.isSaving) { Text("キャンセル") }
            Button(viewModel::save, enabled = state.canSave) { Text("保存") }
        }
    }
}
