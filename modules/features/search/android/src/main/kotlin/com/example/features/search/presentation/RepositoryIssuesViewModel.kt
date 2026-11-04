package com.example.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.navigation.Navigator
import com.example.core.navigation.Route
import com.example.core.network.GitHubIssue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class RepositoryIssuesViewModel(
    private val owner: String,
    private val repo: String,
    private val repository: GitHubRepository,
    private val navigator: Navigator
) : ViewModel() {
    private val mutableIssueState = MutableStateFlow("open")
    val issueState: StateFlow<String> = mutableIssueState
    val content = PageLoader<GitHubIssue> { it.id }
    private var page = 1
    private var job: Job? = null
    fun changeState(state: String) {
        job?.cancel()
        mutableIssueState.value = state
        content.reset()
        page = 1
        job = viewModelScope.launch { loadNow(false) }
    }
    fun select(issue: GitHubIssue) = navigator.push(Route.IssueDetail(owner, repo, issue.number))
    fun nextPage() {
        if (content.state.value.isLoading) return
        page = content.state.value.nextPage ?: return
        job = viewModelScope.launch { loadNow(false) }
    }
    fun loadIfNeeded() {
        if (job == null) job = viewModelScope.launch { loadNow(false) }
    }
    fun retry() {
        if (!content.state.value.isLoading) job = viewModelScope.launch { loadNow(false) }
    }
    fun refresh() {
        if (content.state.value.isLoading) return
        job = viewModelScope.launch { loadNow(true) }
    }
    internal suspend fun loadNow(refresh: Boolean) {
        if (refresh) page = 1
        val page = page
        val state = issueState.value
        content.load(page) { repository.issues(owner, repo, state, page, refresh) }
    }
}
