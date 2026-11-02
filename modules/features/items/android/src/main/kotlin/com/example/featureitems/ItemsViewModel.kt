package com.example.featureitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ItemsState {
    data object Loading : ItemsState
    data class Loaded(val items: List<Item>) : ItemsState
    data object Error : ItemsState
}

class ItemsViewModel(private val api: ItemsApi, private val navigator: Navigator) : ViewModel() {
    private val _state = MutableStateFlow<ItemsState>(ItemsState.Loading)
    val state: StateFlow<ItemsState> = _state
    private var initialLoadStarted = false

    companion object {
        fun factory(api: ItemsApi, navigator: Navigator): ViewModelProvider.Factory = viewModelFactory {
            initializer { ItemsViewModel(api, navigator) }
        }
    }

    /** 詳細へ遷移する。 */
    fun select(item: Item) = navigator.push(Route.ItemDetail(item.id))

    fun loadIfNeeded() {
        if (initialLoadStarted) return
        initialLoadStarted = true
        load()
    }

    fun load() {
        viewModelScope.launch { loadNow() }
    }

    internal suspend fun loadNow() {
        _state.value = ItemsState.Loading
        _state.value = try {
            ItemsState.Loaded(api.listItems())
        } catch (e: CancellationException) {
            initialLoadStarted = false
            throw e
        } catch (e: Exception) {
            ItemsState.Error
        }
    }
}
