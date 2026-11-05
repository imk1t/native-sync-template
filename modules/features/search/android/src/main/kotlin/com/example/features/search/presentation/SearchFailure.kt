package com.example.features.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.core.localization.Message as LocalizedMessage
import com.example.core.localization.localized
import com.example.core.network.GitHubError
import com.example.core.utils.DateTimeUtils
import java.time.Instant
import java.time.ZoneId

sealed interface SearchFailure {
    data class Message(val message: LocalizedMessage) : SearchFailure
    data class RateLimited(val until: Long) : SearchFailure

    companion object {
        fun from(error: Exception): SearchFailure = when (error) {
            is GitHubError.NotFound -> Message(LocalizedMessage.ErrorNotFound)
            is GitHubError.InvalidQuery -> Message(LocalizedMessage.ErrorInvalidQuery)
            is GitHubError.Forbidden -> Message(LocalizedMessage.ErrorForbidden)
            is GitHubError.Server -> Message(LocalizedMessage.ErrorServer(error.status))
            is GitHubError.RateLimited -> RateLimited(error.until)
            else -> Message(LocalizedMessage.ErrorNetwork)
        }
    }
}

@Composable
internal fun SearchFailure.localized(): String {
    val locale = LocalConfiguration.current.locales[0]
    val message = when (this) {
        is SearchFailure.Message -> message
        is SearchFailure.RateLimited -> LocalizedMessage.ErrorRateLimited(
            DateTimeUtils.formatTime(Instant.ofEpochMilli(until), locale, ZoneId.systemDefault())
        )
    }
    return message.resolve(LocalContext.current)
}

@Composable
internal fun issueStateLabel(state: String): String = when (state) {
    "open" -> LocalizedMessage.IssueOpen.localized()
    "closed" -> LocalizedMessage.IssueClosed.localized()
    else -> state
}
