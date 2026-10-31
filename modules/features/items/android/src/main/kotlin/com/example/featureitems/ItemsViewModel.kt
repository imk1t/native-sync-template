package com.example.featureitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ItemsState {
    data object Loading : ItemsState
    data class Loaded(val items: List<Item>) : ItemsState
    data object Error : ItemsState
}

class ItemsViewModel(private val api: ItemsApi) : ViewModel() {
    private val _state = MutableStateFlow<ItemsState>(ItemsState.Loading)
    val state: StateFlow<ItemsState> = _state

    fun load() {
        viewModelScope.launch { loadNow() }
    }

    internal suspend fun loadNow() {
        _state.value = ItemsState.Loading
        _state.value = try {
            ItemsState.Loaded(api.listItems())
        } catch (e: Exception) {
            ItemsState.Error
        }
    }
}
