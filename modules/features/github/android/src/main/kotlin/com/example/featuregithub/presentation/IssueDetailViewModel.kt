package com.example.featuregithub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.corenetwork.GitHubIssue
import com.example.corenetwork.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class IssueDetailViewModel(
    private val owner: String,
    private val repo: String,
    private val number: Long,
    private val repository: GitHubRepository
) : ViewModel() {
    val content = PageLoader<GitHubIssue> { it.id }
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
        content.load(1) { Page(listOf(repository.issue(owner, repo, number, refresh))) }
    }
}
