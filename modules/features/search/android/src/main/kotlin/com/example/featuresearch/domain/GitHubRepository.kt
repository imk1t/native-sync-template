package com.example.featuresearch

import com.example.corenetwork.GitHubIssue
import com.example.corenetwork.Page
import com.example.corenetwork.RepositorySummary

interface GitHubRepository {
    suspend fun search(query: String, sort: String?, page: Int, refresh: Boolean): Page<RepositorySummary>
    suspend fun repository(owner: String, repo: String, refresh: Boolean): RepositorySummary
    suspend fun issues(owner: String, repo: String, state: String, page: Int, refresh: Boolean): Page<GitHubIssue>
    suspend fun issue(owner: String, repo: String, number: Long, refresh: Boolean): GitHubIssue
}
