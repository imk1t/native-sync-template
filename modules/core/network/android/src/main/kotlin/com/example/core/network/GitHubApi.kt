package com.example.core.network

interface GitHubApi {
    suspend fun searchRepositories(query: String, sort: String?, page: Int): Page<RepositorySummary>
    suspend fun repository(owner: String, repo: String): RepositorySummary
    suspend fun issues(owner: String, repo: String, state: String, page: Int): Page<GitHubIssue>
    suspend fun issue(owner: String, repo: String, number: Long): GitHubIssue
}
