package com.example.features.search

import com.example.shared.github.GitHubIssue
import com.example.shared.github.Page
import com.example.shared.github.RepositorySummary

interface GitHubRepository {
    suspend fun search(query: String, sort: String?, page: Int, refresh: Boolean): Page<RepositorySummary>
    suspend fun repository(owner: String, repo: String, refresh: Boolean): RepositorySummary
    suspend fun issues(owner: String, repo: String, state: String, page: Int, refresh: Boolean): Page<GitHubIssue>
    suspend fun issue(owner: String, repo: String, number: Long, refresh: Boolean): GitHubIssue
}
