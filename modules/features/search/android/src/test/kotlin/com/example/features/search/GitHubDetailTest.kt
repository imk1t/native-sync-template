package com.example.features.search

import com.example.core.navigation.AppTab
import com.example.core.navigation.Navigator
import com.example.core.navigation.Route
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    @Test fun repositoryIsLoadedOnce() = runTest {
        val transport = ScenarioTransport(ScenarioFixtures.GithubDetail.repository)
        val model = RepositoryDetailViewModel("octocat", "Hello-World", dependencies(transport), Navigator())
        model.loadIfNeeded()
        advanceUntilIdle()
        assertEquals("octocat/Hello-World", model.content.state.value.items.first().fullName)
        model.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(1, transport.requests.size)
    }

    // @scenario github-detail/not-found
    @Test fun notFoundIsRetryable() = runTest {
        val transport = ScenarioTransport(ScenarioFixtures.GithubDetail.notFound)
        val model = RepositoryDetailViewModel("octocat", "Hello-World", dependencies(transport), Navigator())
        model.loadIfNeeded()
        advanceUntilIdle()
        assertNotNull(model.content.state.value.error)
        model.refresh()
        advanceUntilIdle()
        assertNull(model.content.state.value.error)
        assertEquals("octocat/Hello-World", model.content.state.value.items.first().fullName)
        model.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(2, transport.requests.size)
    }

    // @scenario github-detail/issue-filter
    @Test fun issueFilterFetchesOpenAndClosedSeparately() = runTest {
        val transport = ScenarioTransport(ScenarioFixtures.GithubDetail.issueFilter)
        val issues = RepositoryIssuesViewModel(
            "octocat",
            "Hello-World",
            dependencies(transport).repository,
            Navigator()
        )
        issues.loadIfNeeded()
        advanceUntilIdle()
        issues.changeState("closed")
        advanceUntilIdle()
        assertTrue(transport.requests.last().query.contains("state" to "closed"))
        assertTrue(transport.requests.last().query.contains("page" to "1"))
    }

    // @scenario github-detail/issue-detail
    @Test fun selectedIssueNumberOpensItsDetails() = runTest {
        val transport = ScenarioTransport(ScenarioFixtures.GithubDetail.issueDetail)
        val repository = dependencies(transport).repository
        val navigator = Navigator()
        val issues = RepositoryIssuesViewModel("octocat", "Hello-World", repository, navigator)
        issues.loadIfNeeded()
        advanceUntilIdle()
        issues.select(issues.content.state.value.items.first())
        assertEquals(
            Route.IssueDetail("octocat", "Hello-World", 1),
            navigator.state.value.stacks[AppTab.Home]?.last()?.route
        )
        val detail = IssueDetailViewModel("octocat", "Hello-World", 1, repository)
        detail.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(1L, detail.content.state.value.items.first().number)
    }
}
