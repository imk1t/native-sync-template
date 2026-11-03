package com.example.featuresettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.corestorage.DisplaySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(val isLoaded: Boolean = false, val isSaving: Boolean = false, val error: String? = null)

class SettingsViewModel(val settings: DisplaySettings) : ViewModel() {
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
            mutableState.value = state.value.copy(error = "設定を読み込めませんでした。再試行してください。")
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
            mutableState.value = state.value.copy(error = "設定を保存できませんでした。再試行してください。")
        } finally {
            mutableState.value = state.value.copy(isSaving = false)
        }
    }
}
