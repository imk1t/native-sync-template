package com.example.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.storage.DisplaySettings
import com.example.shared.designsystem.ErrorState
import com.example.shared.designsystem.LoadingIndicator

@Composable
fun SettingsScreen(settings: DisplaySettings) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(settings) }
        }
    )
    SettingsContent(viewModel)
}

@Composable
private fun SettingsContent(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()
    val descriptions by viewModel.settings.showDescriptions.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("設定", style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("一覧に説明文を表示", Modifier.weight(1f))
            Switch(
                descriptions,
                viewModel::save,
                enabled = state.isLoaded && !state.isSaving,
                modifier = Modifier.semantics { contentDescription = "一覧に説明文を表示" }
            )
        }
        if (state.isSaving) LoadingIndicator()
        state.error?.let { ErrorState(it, viewModel::load) }
        Text("GitHub Explorer", style = MaterialTheme.typography.titleLarge)
        Text("公開リポジトリを探して保存する、iOS・Android のネイティブ開発サンプルです。")
        Text("ブックマークとメモはこの端末に保存されます。GitHub の Star とは連動しません。")
    }
}
