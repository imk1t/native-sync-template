package com.example.core.network

data class GitHubIssue(
    val id: Long,
    val number: Long,
    val title: String,
    val state: String,
    val body: String? = null,
    val author: String? = null,
    val updatedAt: String = ""
)
