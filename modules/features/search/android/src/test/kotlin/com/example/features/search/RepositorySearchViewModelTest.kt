package com.example.features.search

import com.example.core.navigation.Navigator
import com.example.core.navigation.Route
import com.example.shared.githubrest.DefaultGitHubApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
class RepositorySearchViewModelTest {
    @BeforeTest fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest fun tearDown() {
        Dispatchers.resetMain()
    }

    // @scenario github-search/load-succeeds
    // @scenario github-search/blank
    // @scenario github-search/change-conditions
    @Test fun searchOnSubmitAndKeepState() = runTest {
        val transport = RecordingTransport()
        val navigator = Navigator()
        val model = RepositorySearchViewModel(dependencies(transport), navigator)
        for (query in listOf("   ", "\u0085\u00A0\u3000\n")) {
            model.edit(SearchForm(query))
            model.submit()
            advanceUntilIdle()
            assertTrue(transport.requests.isEmpty())
        }
        model.edit(SearchForm(" \u0085\u3000swift\u00A0 ", "Swift", "stars"))
        model.submit()
        advanceUntilIdle()
        assertEquals(5_000_000_001, model.content.state.value.items.first().id)
        assertTrue(model.content.state.value.hasLoaded)
        assertFalse(model.content.state.value.isLoading)
        model.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(1, transport.requests.size)
        assertTrue(transport.requests.first().query.contains("q" to "swift is:public language:Swift"))
        assertTrue(transport.requests.first().query.contains("sort" to "stars"))
        model.select(model.content.state.value.items.first())
        assertEquals(
            Route.RepositoryDetail("octocat", "Hello-World"),
            navigator.state.value.stacks.values.first().last().route
        )
        model.edit(SearchForm("kotlin", "Kotlin"))
        model.submit()
        assertTrue(model.content.state.value.items.isEmpty())
        advanceUntilIdle()
        assertEquals(2, transport.requests.size)
    }

    // @scenario github-search/load-fails
    // @scenario github-search/localized-failure
    @Test fun failureCanBeRetried() = runTest {
        val transport = RecordingTransport().apply { failure = 500 }
        val model = RepositorySearchViewModel(dependencies(transport), Navigator())
        model.edit(SearchForm("swift"))
        model.submit()
        advanceUntilIdle()
        assertEquals(
            SearchFailure.Message(com.example.core.localization.Message.ErrorServer(500)),
            model.content.state.value.error
        )
        assertFalse(model.content.state.value.isLoading)
        model.retry()
        advanceUntilIdle()
        assertNull(model.content.state.value.error)
        assertEquals(1, model.content.state.value.items.size)
    }

    // @scenario github-search/cache
    @Test fun cacheExpiresAndRefreshBypassesIt() = runTest {
        var now = 0L
        val transport = RecordingTransport()
        val repo = GitHubRepositoryImpl(DefaultGitHubApi(transport)) { now }
        repo.search("swift", null, 1, false)
        repo.search("swift", null, 1, false)
        assertEquals(1, transport.requests.size)
        repo.search("swift", null, 1, true)
        assertEquals(2, transport.requests.size)
        now += 301_000
        repo.search("swift", null, 1, false)
        repo.search("swift", "stars", 1, false)
        assertEquals(4, transport.requests.size)
    }
}
