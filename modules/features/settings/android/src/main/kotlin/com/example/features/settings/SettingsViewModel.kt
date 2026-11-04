package com.example.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.localization.Message
import com.example.core.storage.DisplaySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(val isLoaded: Boolean = false, val isSaving: Boolean = false, val error: Message? = null)

internal class SettingsViewModel(val settings: DisplaySettings) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = mutableState
    fun load() {
        viewModelScope.launch { loadNow() }
    }
    internal suspend fun loadNow() {
        try {
            settings.load()
            mutableState.value = SettingsState(isLoaded = true)
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (e: Exception) {
            mutableState.value = state.value.copy(error = Message.ErrorSettingsLoad)
        }
    }
    fun save(value: Boolean) {
        viewModelScope.launch { saveNow(value) }
    }
    internal suspend fun saveNow(value: Boolean) {
        if (!state.value.isLoaded || state.value.isSaving) return
        mutableState.value = state.value.copy(isSaving = true)
        try {
            settings.setShowDescriptions(value)
            mutableState.value = SettingsState(isLoaded = true)
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (
            e: Exception
        ) {
            mutableState.value = state.value.copy(error = Message.ErrorSettingsSave)
        } finally {
            mutableState.value = state.value.copy(isSaving = false)
        }
    }
}
