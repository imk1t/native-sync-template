package com.example.featuresearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.corelocalization.Message
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.corenetwork.RepositorySummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchForm(val query: String = "", val language: String = "", val sort: String = "")

internal class RepositorySearchViewModel(val dependencies: SearchDependencies, private val navigator: Navigator) :
    ViewModel() {
    private val mutableForm = MutableStateFlow(SearchForm())
    val form: StateFlow<SearchForm> = mutableForm
    private val mutableStorageError = MutableStateFlow<Message?>(null)
    val storageError: StateFlow<Message?> = mutableStorageError
    val content = PageLoader<RepositorySummary> { it.id }
    private var submitted: Pair<String, String?>? = null
    private var requestedPage = 1
    private var job: Job? = null

    fun edit(form: SearchForm) {
        mutableForm.value = form
    }
    fun submit() {
        job?.cancel()
        content.reset()
        val form = form.value
        val query = form.query.trim()
        submitted = if (query.isEmpty()) {
            null
        } else {
            (query + " is:public" + if (form.language.isEmpty()) "" else " language:${form.language}") to
                form.sort.ifEmpty { null }
        }
        requestedPage = 1
        job = viewModelScope.launch { loadNow() }
    }
    fun example(query: String) {
        edit(form.value.copy(query = query))
        submit()
    }
    fun select(repository: RepositorySummary) =
        navigator.push(Route.RepositoryDetail(repository.owner, repository.name))
    fun loadIfNeeded() {
        if (job == null) job = viewModelScope.launch { loadNow() }
    }
    fun nextPage() {
        if (content.state.value.isLoading) return
        requestedPage = content.state.value.nextPage ?: return
        job = viewModelScope.launch { loadNow() }
    }
    fun retry() {
        if (!content.state.value.isLoading) job = viewModelScope.launch { loadNow() }
    }
    fun refresh() {
        if (content.state.value.isLoading) return
        job = viewModelScope.launch { refreshNow() }
    }

    internal suspend fun loadNow() {
        try {
            dependencies.loadLocalData()
            mutableStorageError.value = null
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (e: Exception) {
            mutableStorageError.value = Message.ErrorStorageLoad
        }
        val (query, sort) = submitted ?: return
        val page = requestedPage
        content.load(page) { dependencies.repository.search(query, sort, page, false) }
    }
    internal suspend fun refreshNow() {
        if (content.state.value.isLoading) return
        requestedPage = 1
        val (query, sort) = submitted ?: return
        content.load(1) { dependencies.repository.search(query, sort, 1, true) }
    }
}
