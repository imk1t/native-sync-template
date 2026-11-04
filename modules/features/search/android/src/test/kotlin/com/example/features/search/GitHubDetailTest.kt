package com.example.features.search

import com.example.core.navigation.AppTab
import com.example.core.navigation.Navigator
import com.example.core.navigation.Route
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class GitHubDetailTest {
    @BeforeTest fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest fun tearDown() {
        Dispatchers.resetMain()
    }

    // @scenario github-detail/repository
    // @scenario github-detail/not-found
    @Test fun repositoryLoadsOnceAndNotFoundIsRetryable() = runTest {
        val transport = RecordingTransport().apply { failure = 404 }
        val model = RepositoryDetailViewModel("octocat", "Hello-World", dependencies(transport), Navigator())
        model.loadIfNeeded()
        advanceUntilIdle()
        assertNotNull(model.content.state.value.error)
        model.refresh()
        advanceUntilIdle()
        assertEquals("octocat/Hello-World", model.content.state.value.items.first().fullName)
        model.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(2, transport.requests.size)
    }

    // @scenario github-detail/issue-filter
    // @scenario github-detail/issue-detail
    @Test fun issueFilterAndDetail() = runTest {
        val transport = RecordingTransport()
        val deps = dependencies(transport)
        val navigator = Navigator()
        val issues = RepositoryIssuesViewModel("octocat", "Hello-World", deps.repository, navigator)
        issues.loadIfNeeded()
        advanceUntilIdle()
        issues.select(issues.content.state.value.items.first())
        assertEquals(
            Route.IssueDetail("octocat", "Hello-World", 1),
            navigator.state.value.stacks[AppTab.Home]?.last()?.route
        )
        issues.changeState("closed")
        advanceUntilIdle()
        assertTrue(transport.requests.last().query.contains("state" to "closed"))
        val detail = IssueDetailViewModel("octocat", "Hello-World", 1, deps.repository)
        detail.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(1L, detail.content.state.value.items.first().number)
    }
}
