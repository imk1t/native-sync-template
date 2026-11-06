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
import com.example.core.localization.Message
import com.example.core.localization.localized
import com.example.core.storage.DisplaySettings
import com.example.shared.designsystem.DesignCard
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
    val descriptionsLabel = Message.SettingsDescriptions.localized()
    val state by viewModel.state.collectAsState()
    val descriptions by viewModel.settings.showDescriptions.collectAsState()
    LaunchedEffect(viewModel) { viewModel.load() }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(Message.SettingsTitle.localized(), style = MaterialTheme.typography.headlineLarge)
        DesignCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Message.SettingsDescriptions.localized(), Modifier.weight(1f))
                Switch(
                    descriptions,
                    viewModel::save,
                    enabled = state.isLoaded && !state.isSaving,
                    modifier = Modifier.semantics { contentDescription = descriptionsLabel }
                )
            }
        }
        if (state.isSaving) LoadingIndicator(Message.CommonLoading.localized())
        state.error?.let { ErrorState(it.localized(), Message.CommonRetry.localized(), viewModel::load) }
        DesignCard {
            Text(Message.AppName.localized(), style = MaterialTheme.typography.titleLarge)
            Text(Message.SettingsDescription.localized())
            Text(
                Message.SettingsStorageNote.localized(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
