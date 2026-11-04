package com.example.features.search

import com.example.core.network.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PageLoader<T>(private val id: (T) -> Long) {
    private val mutableState = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = mutableState
    private var generation = 0

    fun reset() {
        generation += 1
        mutableState.value = PagedState()
    }

    suspend fun load(page: Int, fetch: suspend () -> Page<T>) {
        if (state.value.isLoading) return
        generation += 1
        val current = generation
        mutableState.value = state.value.copy(isLoading = true, error = null)
        try {
            val result = fetch()
            currentCoroutineContext().ensureActive()
            if (current != generation) return
            val combined = (if (page == 1) emptyList() else state.value.items) + result.items
            mutableState.value = PagedState(
                combined.distinctBy(id),
                true,
                true,
                null,
                result.nextPage,
                result.incomplete,
                result.limitReached
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (current ==
                generation
            ) {
                mutableState.value = state.value.copy(error = e.message ?: "通信に失敗しました。再試行してください。")
            }
        } finally {
            if (current == generation) mutableState.value = state.value.copy(isLoading = false)
        }
    }
}
