package com.example.shared.github

import com.example.core.network.HttpTransport
import java.net.URI

class DefaultGitHubApi(transport: HttpTransport) : GitHubApi {
    private val client = ApiClient(transport)

    override suspend fun searchRepositories(query: String, sort: String?, page: Int): Page<RepositorySummary> {
        val response = client.searchRepos(query, sort, "desc", 20, page)
        return Page(
            response.value.items.map(::summary),
            if (page < 50) nextPage(response.headers) else null,
            response.value.incompleteResults,
            response.value.totalCount > 1_000
        )
    }

    override suspend fun repository(owner: String, repo: String): RepositorySummary {
        val dto = client.reposGet(owner, repo).value
        return RepositorySummary(
            dto.id, dto.name, dto.fullName, dto.owner.login, dto.owner.avatarUrl,
            dto.description, dto.language, dto.stargazersCount, dto.forksCount, dto.topics.orEmpty(), dto.htmlUrl
        )
    }

    override suspend fun issues(owner: String, repo: String, state: String, page: Int): Page<GitHubIssue> {
        val response = client.issuesListForRepo(owner, repo, state, 20, page)
        return Page(response.value.filter { it.pullRequest == null }.map(::issue), nextPage(response.headers))
    }

    override suspend fun issue(owner: String, repo: String, number: Long): GitHubIssue {
        if (number !in 1..Int.MAX_VALUE.toLong()) throw GitHubError.NotFound()
        val dto = client.issuesGet(owner, repo, number.toInt()).value
        if (dto.pullRequest != null) throw GitHubError.NotFound()
        return issue(dto)
    }

    private fun summary(dto: RepoSummaryDTO) = RepositorySummary(
        dto.id, dto.name, dto.fullName, dto.owner?.login ?: dto.fullName.substringBefore('/'),
        dto.owner?.avatarUrl, dto.description, dto.language, dto.stargazersCount, dto.forksCount,
        dto.topics.orEmpty(), dto.htmlUrl
    )

    private fun issue(dto: IssueDTO) = GitHubIssue(
        dto.id,
        dto.number.toLong(),
        dto.title,
        dto.state,
        dto.body,
        dto.user?.login,
        dto.updatedAt
    )

    companion object {
        internal fun nextPage(headers: Map<String, String>): Int? {
            for (link in headers["link"].orEmpty().split(',')) {
                val parts = link.split(';').map(String::trim)
                if (parts.drop(1).none { it == "rel=\"next\"" }) continue
                val uri = runCatching { URI(parts.first().removeSurrounding("<", ">")) }.getOrNull() ?: continue
                if (uri.scheme != "https" || uri.host != "api.github.com") continue
                val page = uri.rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("page=") }
                    ?.substringAfter('=')?.toIntOrNull()
                if (page != null && page > 0) return page
            }
            return null
        }
    }
}
