package com.example.shared.githubrest

sealed class GitHubError : Exception() {
    class NotFound : GitHubError()
    class InvalidQuery : GitHubError()
    class Forbidden : GitHubError()
    class RateLimited(val until: Long) : GitHubError()
    class Server(val status: Int) : GitHubError()
}
