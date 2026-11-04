package com.example.featuresearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.corenetwork.Page
import com.example.corenetwork.RepositorySummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class RepositoryDetailViewModel(
    private val owner: String,
    private val repo: String,
    val dependencies: SearchDependencies,
    private val navigator: Navigator
) : ViewModel() {
    val content = PageLoader<RepositorySummary> { it.id }
    private val mutableSaveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = mutableSaveError
    private val mutableSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = mutableSaving
    private var initialStarted = false
    fun loadIfNeeded() {
        if (initialStarted) return
        initialStarted = true
        viewModelScope.launch {
            try {
                loadNow(false)
            } catch (e: CancellationException) {
                initialStarted = false
                throw e
            }
        }
    }
    fun refresh() {
        viewModelScope.launch { loadNow(true) }
    }
    internal suspend fun loadNow(refresh: Boolean) {
        try {
            dependencies.bookmarks.load()
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (e: Exception) {
            mutableSaveError.value = "保存データを読み込めませんでした。"
        }
        content.load(1) { Page(listOf(dependencies.repository.repository(owner, repo, refresh))) }
    }
    fun showIssues() = navigator.push(Route.RepositoryIssues(owner, repo))
    fun toggleBookmark() {
        viewModelScope.launch { toggleBookmarkNow() }
    }
    internal suspend fun toggleBookmarkNow() {
        if (isSaving.value) return
        val repository = content.state.value.items.firstOrNull() ?: return
        mutableSaving.value = true
        try {
            dependencies.bookmarks.toggle(repository)
            mutableSaveError.value = null
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (
            e: Exception
        ) {
            mutableSaveError.value = e.message ?: "保存できませんでした。"
        } finally {
            mutableSaving.value = false
        }
    }
}
