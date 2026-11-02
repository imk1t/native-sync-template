package com.example.featureitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ItemDetailState {
    data object Loading : ItemDetailState
    data class Loaded(val item: Item) : ItemDetailState
    data object Error : ItemDetailState
}

class ItemDetailViewModel(private val id: String, private val api: ItemsApi) : ViewModel() {
    private val _state = MutableStateFlow<ItemDetailState>(ItemDetailState.Loading)
    val state: StateFlow<ItemDetailState> = _state
    private var initialLoadStarted = false

    companion object {
        fun factory(id: String, api: ItemsApi): ViewModelProvider.Factory = viewModelFactory {
            initializer { ItemDetailViewModel(id, api) }
        }
    }

    fun loadIfNeeded() {
        if (initialLoadStarted) return
        initialLoadStarted = true
        load()
    }

    fun load() {
        viewModelScope.launch { loadNow() }
    }

    internal suspend fun loadNow() {
        _state.value = ItemDetailState.Loading
        _state.value = try {
            ItemDetailState.Loaded(api.getItem(id))
        } catch (e: CancellationException) {
            initialLoadStarted = false
            throw e
        } catch (e: Exception) {
            ItemDetailState.Error
        }
    }
}
