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
    private val blank = ScenarioStep("github-search/blank", emptyList())

    @BeforeTest fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun modelFor(step: ScenarioStep, navigator: Navigator = Navigator()) =
        ScenarioTransport(step).let { it to RepositorySearchViewModel(dependencies(it), navigator) }

    // @scenario github-search/load-succeeds
    @Test fun searchOnSubmitAndKeepState() = runTest {
        val navigator = Navigator()
        val (transport, model) = modelFor(ScenarioFixtures.GithubSearch.loadSucceeds, navigator)
        model.edit(SearchForm("swift"))
        model.submit()
        advanceUntilIdle()
        assertEquals(listOf(5_000_000_001), model.content.state.value.items.map { it.id })
        assertTrue(model.content.state.value.hasLoaded)
        assertFalse(model.content.state.value.isLoading)
        model.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(1, transport.requests.size)
        model.select(model.content.state.value.items.first())
        assertEquals(
            Route.RepositoryDetail("octocat", "Hello-World"),
            navigator.state.value.stacks.values.first().last().route
        )
    }

    // @scenario github-search/blank
    @Test fun whitespaceOnlyInputIsNotSubmitted() = runTest {
        val (transport, model) = modelFor(blank)
        for (query in listOf("   ", "\u0085 　\n")) {
            model.edit(SearchForm(query))
            model.submit()
            advanceUntilIdle()
            assertTrue(transport.requests.isEmpty())
        }
    }

    // @scenario github-search/change-conditions
    @Test fun conditionsAreAppliedAndResultsReset() = runTest {
        val (transport, model) = modelFor(ScenarioFixtures.GithubSearch.changeConditions)
        model.edit(SearchForm(" \u0085　swift  ", "Swift", "stars"))
        model.submit()
        advanceUntilIdle()
        assertTrue(transport.requests.first().query.contains("q" to "swift is:public language:Swift"))
        assertTrue(transport.requests.first().query.contains("sort" to "stars"))
        model.edit(SearchForm("kotlin", "Kotlin", "stars"))
        model.submit()
        assertTrue(model.content.state.value.items.isEmpty())
        advanceUntilIdle()
        assertEquals(2, transport.requests.size)
        assertTrue(transport.requests.last().query.contains("q" to "kotlin is:public language:Kotlin"))
        assertTrue(transport.requests.last().query.contains("page" to "1"))
    }

    // @scenario github-search/load-fails
    @Test fun failureCanBeRetried() = runTest {
        val (_, model) = modelFor(ScenarioFixtures.GithubSearch.loadFails)
        model.edit(SearchForm("swift"))
        model.submit()
        advanceUntilIdle()
        assertNotNull(model.content.state.value.error)
        assertFalse(model.content.state.value.isLoading)
        model.retry()
        advanceUntilIdle()
        assertNull(model.content.state.value.error)
        assertEquals(1, model.content.state.value.items.size)
    }

    // @scenario github-search/localized-failure
    @Test fun failureKeepsMessageKeyAndStatus() = runTest {
        val (_, model) = modelFor(ScenarioFixtures.GithubSearch.localizedFailure)
        model.edit(SearchForm("swift"))
        model.submit()
        advanceUntilIdle()
        assertEquals(
            SearchFailure.Message(com.example.core.localization.Message.ErrorServer(500)),
            model.content.state.value.error
        )
    }

    // @scenario github-search/empty
    @Test fun zeroResultsIsALoadedEmptyState() = runTest {
        val (_, model) = modelFor(ScenarioFixtures.GithubSearch.empty)
        model.edit(SearchForm("swift"))
        model.submit()
        advanceUntilIdle()
        assertTrue(model.content.state.value.hasLoaded)
        assertTrue(model.content.state.value.items.isEmpty())
    }

    // @scenario github-search/pagination
    @Test fun pagingKeepsItemsAfterFailureAndDropsDuplicates() = runTest {
        val (_, model) = modelFor(ScenarioFixtures.GithubSearch.pagination)
        val state = model.content.state
        model.edit(SearchForm("swift"))
        model.submit()
        advanceUntilIdle()
        assertEquals(listOf(1L), state.value.items.map { it.id })
        assertEquals(2, state.value.nextPage)
        model.nextPage()
        advanceUntilIdle()
        assertEquals(listOf(1L), state.value.items.map { it.id })
        assertEquals(2, state.value.nextPage)
        assertNotNull(state.value.error)
        model.retry()
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L), state.value.items.map { it.id })
        assertNull(state.value.nextPage)
        assertNull(state.value.error)
        assertTrue(state.value.incomplete && state.value.limitReached)
    }

    // @scenario github-search/cache
    @Test fun cacheExpiresAndRefreshBypassesIt() = runTest {
        var now = 0L
        val transport = ScenarioTransport(ScenarioFixtures.GithubSearch.cache)
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
